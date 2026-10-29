-- Migración: Pedidos y Checkout Seguro para Hardbuntu
-- 1. Versiona las tablas orders / order_items (ya existentes en el proyecto remoto).
-- 2. Cierra el acceso directo del cliente a pedidos (datos personales) mediante RLS:
--    cada usuario autenticado solo puede leer sus propios pedidos.
-- 3. Expone la función create_order: valida stock, calcula montos con los precios del servidor,
--    inserta pedido + ítems y descuenta stock en una sola transacción.
-- Los precios de products incluyen IGV: total = suma de ítems, IGV = total - total / 1.18.
-- Idempotente: puede ejecutarse sobre la base remota existente o sobre una base local nueva.

-- 1. Tablas de pedidos
create table if not exists public.orders (
    id uuid primary key default gen_random_uuid(),
    customer_name text not null,
    customer_email text,
    customer_phone text not null,
    delivery_address text not null,
    subtotal numeric(12, 2) not null,
    tax numeric(12, 2) not null,
    total numeric(12, 2) not null,
    status text not null default 'pending',
    notes text,
    created_at timestamptz not null default now()
);

create table if not exists public.order_items (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null references public.orders(id) on delete cascade,
    product_id uuid references public.products(id) on delete set null,
    product_name text not null,
    product_price numeric(12, 2) not null,
    quantity integer not null check (quantity > 0),
    subtotal numeric(12, 2) not null,
    created_at timestamptz not null default now()
);

-- Dueño del pedido (null para compras como invitado)
alter table public.orders
    add column if not exists user_id uuid references auth.users(id) on delete set null;

create index if not exists idx_orders_user_id on public.orders(user_id);
create index if not exists idx_order_items_order_id on public.order_items(order_id);
create index if not exists idx_order_items_product_id on public.order_items(product_id);

-- 2. Seguridad: los pedidos solo se crean mediante create_order y solo su dueño puede leerlos
alter table public.orders enable row level security;
alter table public.order_items enable row level security;

-- Eliminar cualquier política previa (creadas desde el dashboard, posiblemente permisivas)
do $$
declare
    pol record;
begin
    for pol in
        select policyname, tablename
        from pg_policies
        where schemaname = 'public'
          and tablename in ('orders', 'order_items')
    loop
        execute format('drop policy %I on public.%I', pol.policyname, pol.tablename);
    end loop;
end;
$$;

revoke all on public.orders from anon, authenticated;
revoke all on public.order_items from anon, authenticated;
grant select on public.orders to authenticated;
grant select on public.order_items to authenticated;

create policy "orders_select_own"
    on public.orders
    for select
    to authenticated
    using ((select auth.uid()) = user_id);

create policy "order_items_select_own"
    on public.order_items
    for select
    to authenticated
    using (
        exists (
            select 1
            from public.orders o
            where o.id = order_id
              and o.user_id = (select auth.uid())
        )
    );

-- 3. Función transaccional de checkout
-- p_items: [{"product_id": "<uuid>", "quantity": <int>}, ...]
create or replace function public.create_order(
    p_customer_name text,
    p_customer_phone text,
    p_delivery_address text,
    p_items jsonb,
    p_customer_email text default null,
    p_notes text default null
)
returns public.orders
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_order public.orders;
    v_item record;
    v_requested_count integer;
    v_locked_count integer := 0;
    v_total numeric(12, 2) := 0;
    v_subtotal numeric(12, 2);
    v_phone text := regexp_replace(coalesce(p_customer_phone, ''), '[\s\-]', '', 'g');
    v_email text := nullif(btrim(coalesce(p_customer_email, '')), '');
begin
    -- Validación de datos del cliente
    if coalesce(btrim(p_customer_name), '') = '' then
        raise exception 'El nombre del cliente es obligatorio' using errcode = '22023';
    end if;
    if v_phone !~ '^\+?[0-9]{7,15}$' then
        raise exception 'El número de teléfono no es válido' using errcode = '22023';
    end if;
    if coalesce(btrim(p_delivery_address), '') = '' then
        raise exception 'La dirección de entrega es obligatoria' using errcode = '22023';
    end if;
    if v_email is not null and v_email !~ '^[^@\s]+@[^@\s]+\.[^@\s]+$' then
        raise exception 'El correo electrónico no es válido' using errcode = '22023';
    end if;

    -- Validación del carrito
    if p_items is null or jsonb_typeof(p_items) <> 'array' or jsonb_array_length(p_items) = 0 then
        raise exception 'El carrito de compras está vacío' using errcode = '22023';
    end if;
    if exists (
        select 1
        from jsonb_array_elements(p_items) e
        where coalesce((e ->> 'quantity')::integer, 0) <= 0
    ) then
        raise exception 'Las cantidades deben ser mayores a cero' using errcode = '22023';
    end if;

    select count(distinct (e ->> 'product_id')::uuid)
    into v_requested_count
    from jsonb_array_elements(p_items) e;

    -- Bloquear productos (orden estable para evitar deadlocks) y validar stock con precios del servidor
    for v_item in
        select p.id, p.name, p.price, p.stock, p.is_active, req.quantity
        from (
            select (e ->> 'product_id')::uuid as product_id,
                   sum((e ->> 'quantity')::integer) as quantity
            from jsonb_array_elements(p_items) e
            group by 1
        ) req
        join public.products p on p.id = req.product_id
        order by p.id
        for update of p
    loop
        if not v_item.is_active then
            raise exception 'El producto "%" ya no está disponible', v_item.name using errcode = 'P0001';
        end if;
        if v_item.stock < v_item.quantity then
            raise exception 'Stock insuficiente para "%": disponibles %, solicitados %',
                v_item.name, v_item.stock, v_item.quantity using errcode = 'P0001';
        end if;
        v_total := v_total + round(v_item.price * v_item.quantity, 2);
        v_locked_count := v_locked_count + 1;
    end loop;

    if v_locked_count <> v_requested_count then
        raise exception 'Uno o más productos del carrito ya no existen' using errcode = 'P0001';
    end if;

    -- Precios con IGV incluido: se desglosa la base imponible y el IGV (18%)
    v_subtotal := round(v_total / 1.18, 2);

    insert into public.orders (
        customer_name, customer_email, customer_phone, delivery_address,
        subtotal, tax, total, status, notes, user_id
    )
    values (
        btrim(p_customer_name), v_email, v_phone, btrim(p_delivery_address),
        v_subtotal, v_total - v_subtotal, v_total, 'pending', nullif(btrim(coalesce(p_notes, '')), ''),
        auth.uid()
    )
    returning * into v_order;

    insert into public.order_items (order_id, product_id, product_name, product_price, quantity, subtotal)
    select v_order.id, p.id, p.name, p.price, req.quantity, round(p.price * req.quantity, 2)
    from (
        select (e ->> 'product_id')::uuid as product_id,
               sum((e ->> 'quantity')::integer) as quantity
        from jsonb_array_elements(p_items) e
        group by 1
    ) req
    join public.products p on p.id = req.product_id;

    update public.products p
    set stock = p.stock - req.quantity
    from (
        select (e ->> 'product_id')::uuid as product_id,
               sum((e ->> 'quantity')::integer) as quantity
        from jsonb_array_elements(p_items) e
        group by 1
    ) req
    where p.id = req.product_id;

    return v_order;
end;
$$;

revoke all on function public.create_order(text, text, text, jsonb, text, text) from public;
grant execute on function public.create_order(text, text, text, jsonb, text, text) to anon, authenticated;

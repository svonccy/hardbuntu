-- Migración: funciones de la vista de usuario (perfil, direcciones, entrega/recojo, formas de pago,
-- precios por mayor y cancelación de pedidos).
--
-- Los datos del negocio NO están inventados: store_settings y payment_methods se editan desde el
-- dashboard de Supabase (Table Editor). La app oculta todo lo que esté vacío o inactivo.
--
-- Estados de pedido que entiende la app: pending, processing, delivered, cancelled
-- (el valor legado "completed" se muestra como entregado).
-- Idempotente: puede ejecutarse varias veces sin pisar los datos que edites.

-- 1. Configuración del local (una sola fila)
create table if not exists public.store_settings (
    id boolean primary key default true check (id),
    store_name text not null default 'Hardbuntu',
    address text,
    phone text,
    whatsapp text,              -- con código de país y sin espacios, ej. 51987654321
    email text,
    opening_hours text,         -- texto libre, ej. "Lun a Sáb 8:00 - 18:00"
    pickup_enabled boolean not null default true,
    delivery_enabled boolean not null default true,
    delivery_fee numeric(12, 2) not null default 0 check (delivery_fee >= 0),
    free_delivery_from numeric(12, 2) check (free_delivery_from >= 0),
    updated_at timestamptz not null default now()
);

insert into public.store_settings (id) values (true) on conflict (id) do nothing;

drop trigger if exists tr_store_settings_updated_at on public.store_settings;
create trigger tr_store_settings_updated_at
    before update on public.store_settings
    for each row
    execute function public.handle_updated_at();

-- 2. Formas de pago (solo se muestran las activas)
create table if not exists public.payment_methods (
    code text primary key,
    name text not null,
    instructions text,          -- se muestra al cliente al confirmar el pedido (ej. número de Yape)
    is_active boolean not null default false,
    sort_order integer not null default 0
);

-- Solo "pago al recibir/recoger" viene activo porque no requiere datos de cuenta.
-- Completa las instrucciones y activa los demás métodos cuando corresponda.
insert into public.payment_methods (code, name, instructions, is_active, sort_order)
values
    ('cash', 'Pago al recibir o recoger', 'Pagas al recibir tu pedido o al recogerlo en tienda.', true, 1),
    ('transfer', 'Transferencia bancaria', null, false, 2),
    ('yape', 'Yape', null, false, 3),
    ('plin', 'Plin', null, false, 4)
on conflict (code) do nothing;

-- 3. Perfil y direcciones del cliente
create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    full_name text,
    phone text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

drop trigger if exists tr_profiles_updated_at on public.profiles;
create trigger tr_profiles_updated_at
    before update on public.profiles
    for each row
    execute function public.handle_updated_at();

create table if not exists public.addresses (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    label text not null default 'Casa',
    address text not null,
    reference text,
    is_default boolean not null default false,
    created_at timestamptz not null default now()
);

create index if not exists idx_addresses_user_id on public.addresses(user_id);

-- 4. Pedidos: tipo de entrega, costo de envío y forma de pago
alter table public.orders
    add column if not exists fulfillment_type text not null default 'delivery',
    add column if not exists delivery_fee numeric(12, 2) not null default 0,
    add column if not exists payment_method text references public.payment_methods(code) on update cascade on delete set null;

alter table public.orders drop constraint if exists orders_fulfillment_type_check;
alter table public.orders add constraint orders_fulfillment_type_check check (fulfillment_type in ('delivery', 'pickup'));

-- 5. Productos: precios por mayor. Formato: [{"min_quantity": 10, "unit_price": 8.50}, ...]
alter table public.products
    add column if not exists price_tiers jsonb not null default '[]'::jsonb;

alter table public.products drop constraint if exists products_price_tiers_is_array;
alter table public.products add constraint products_price_tiers_is_array check (jsonb_typeof(price_tiers) = 'array');

-- 6. Seguridad (RLS)
alter table public.store_settings enable row level security;
alter table public.payment_methods enable row level security;
alter table public.profiles enable row level security;
alter table public.addresses enable row level security;

drop policy if exists "allow_public_read_store_settings" on public.store_settings;
create policy "allow_public_read_store_settings"
    on public.store_settings for select to anon, authenticated using (true);

drop policy if exists "allow_public_read_active_payment_methods" on public.payment_methods;
create policy "allow_public_read_active_payment_methods"
    on public.payment_methods for select to anon, authenticated using (is_active);

drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own"
    on public.profiles for select to authenticated using ((select auth.uid()) = id);
drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own"
    on public.profiles for insert to authenticated with check ((select auth.uid()) = id);
drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own"
    on public.profiles for update to authenticated
    using ((select auth.uid()) = id) with check ((select auth.uid()) = id);

drop policy if exists "addresses_select_own" on public.addresses;
create policy "addresses_select_own"
    on public.addresses for select to authenticated using ((select auth.uid()) = user_id);
drop policy if exists "addresses_insert_own" on public.addresses;
create policy "addresses_insert_own"
    on public.addresses for insert to authenticated with check ((select auth.uid()) = user_id);
drop policy if exists "addresses_update_own" on public.addresses;
create policy "addresses_update_own"
    on public.addresses for update to authenticated
    using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
drop policy if exists "addresses_delete_own" on public.addresses;
create policy "addresses_delete_own"
    on public.addresses for delete to authenticated using ((select auth.uid()) = user_id);

grant select on public.store_settings to anon, authenticated;
grant select on public.payment_methods to anon, authenticated;
grant select, insert, update on public.profiles to authenticated;
grant select, insert, update, delete on public.addresses to authenticated;

-- 7. Precio unitario aplicable según la cantidad (precios por mayor)
create or replace function public.effective_unit_price(p_price numeric, p_tiers jsonb, p_quantity integer)
returns numeric
language sql
immutable
set search_path = ''
as $$
    select coalesce(
        (
            select (t ->> 'unit_price')::numeric
            from jsonb_array_elements(coalesce(p_tiers, '[]'::jsonb)) t
            where (t ->> 'min_quantity')::integer <= p_quantity
            order by (t ->> 'min_quantity')::integer desc
            limit 1
        ),
        p_price
    );
$$;

-- 8. Checkout: ahora considera entrega/recojo, costo de envío, forma de pago y precios por mayor.
-- Los precios incluyen IGV; el envío también. total = productos + envío; subtotal = total / 1.18.
drop function if exists public.create_order(text, text, text, jsonb, text, text);

create or replace function public.create_order(
    p_customer_name text,
    p_customer_phone text,
    p_delivery_address text,
    p_items jsonb,
    p_customer_email text default null,
    p_notes text default null,
    p_fulfillment_type text default 'delivery',
    p_payment_method text default null
)
returns public.orders
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_order public.orders;
    v_settings public.store_settings;
    v_item record;
    v_requested_count integer;
    v_locked_count integer := 0;
    v_items_total numeric(12, 2) := 0;
    v_fee numeric(12, 2) := 0;
    v_total numeric(12, 2);
    v_subtotal numeric(12, 2);
    v_phone text := regexp_replace(coalesce(p_customer_phone, ''), '[\s\-]', '', 'g');
    v_email text := nullif(btrim(coalesce(p_customer_email, '')), '');
    v_address text := nullif(btrim(coalesce(p_delivery_address, '')), '');
begin
    select * into v_settings from public.store_settings limit 1;

    -- Validación de datos del cliente
    if coalesce(btrim(p_customer_name), '') = '' then
        raise exception 'El nombre del cliente es obligatorio' using errcode = '22023';
    end if;
    if v_phone !~ '^\+?[0-9]{7,15}$' then
        raise exception 'El número de teléfono no es válido' using errcode = '22023';
    end if;
    if v_email is not null and v_email !~ '^[^@\s]+@[^@\s]+\.[^@\s]+$' then
        raise exception 'El correo electrónico no es válido' using errcode = '22023';
    end if;

    -- Entrega o recojo
    if p_fulfillment_type is null or p_fulfillment_type not in ('delivery', 'pickup') then
        raise exception 'El tipo de entrega no es válido' using errcode = '22023';
    end if;
    if p_fulfillment_type = 'delivery' then
        if not coalesce(v_settings.delivery_enabled, true) then
            raise exception 'El delivery no está disponible por ahora' using errcode = 'P0001';
        end if;
        if v_address is null then
            raise exception 'La dirección de entrega es obligatoria' using errcode = '22023';
        end if;
    else
        if not coalesce(v_settings.pickup_enabled, true) then
            raise exception 'El recojo en tienda no está disponible por ahora' using errcode = 'P0001';
        end if;
        v_address := coalesce(v_address, 'Recojo en tienda');
    end if;

    -- Forma de pago (obligatoria si existe al menos un método activo)
    if exists (select 1 from public.payment_methods where is_active) then
        if p_payment_method is null
           or not exists (select 1 from public.payment_methods where code = p_payment_method and is_active) then
            raise exception 'Selecciona una forma de pago válida' using errcode = '22023';
        end if;
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
        select p.id, p.name, p.stock, p.is_active, req.quantity,
               public.effective_unit_price(p.price, p.price_tiers, req.quantity::integer) as unit_price
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
        v_items_total := v_items_total + round(v_item.unit_price * v_item.quantity, 2);
        v_locked_count := v_locked_count + 1;
    end loop;

    if v_locked_count <> v_requested_count then
        raise exception 'Uno o más productos del carrito ya no existen' using errcode = 'P0001';
    end if;

    -- Costo de envío (gratis a partir del monto configurado)
    if p_fulfillment_type = 'delivery' then
        v_fee := case
            when v_settings.free_delivery_from is not null and v_items_total >= v_settings.free_delivery_from then 0
            else coalesce(v_settings.delivery_fee, 0)
        end;
    end if;

    v_total := v_items_total + v_fee;
    v_subtotal := round(v_total / 1.18, 2);

    insert into public.orders (
        customer_name, customer_email, customer_phone, delivery_address,
        subtotal, tax, total, status, notes, user_id,
        fulfillment_type, delivery_fee, payment_method
    )
    values (
        btrim(p_customer_name), v_email, v_phone, v_address,
        v_subtotal, v_total - v_subtotal, v_total, 'pending', nullif(btrim(coalesce(p_notes, '')), ''), auth.uid(),
        p_fulfillment_type, v_fee, p_payment_method
    )
    returning * into v_order;

    insert into public.order_items (order_id, product_id, product_name, product_price, quantity, subtotal)
    select v_order.id, p.id, p.name,
           public.effective_unit_price(p.price, p.price_tiers, req.quantity::integer),
           req.quantity,
           round(public.effective_unit_price(p.price, p.price_tiers, req.quantity::integer) * req.quantity, 2)
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

revoke all on function public.create_order(text, text, text, jsonb, text, text, text, text) from public;
grant execute on function public.create_order(text, text, text, jsonb, text, text, text, text) to anon, authenticated;

-- 9. Cancelar un pedido propio mientras esté pendiente (devuelve el stock)
create or replace function public.cancel_order(p_order_id uuid)
returns public.orders
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_order public.orders;
begin
    if auth.uid() is null then
        raise exception 'Debes iniciar sesión para cancelar un pedido' using errcode = '42501';
    end if;

    select * into v_order
    from public.orders
    where id = p_order_id and user_id = auth.uid()
    for update;

    if not found then
        raise exception 'No encontramos el pedido' using errcode = 'P0001';
    end if;
    if v_order.status <> 'pending' then
        raise exception 'Solo se pueden cancelar los pedidos pendientes' using errcode = 'P0001';
    end if;

    update public.products p
    set stock = p.stock + oi.quantity
    from public.order_items oi
    where oi.order_id = v_order.id and oi.product_id = p.id;

    update public.orders set status = 'cancelled' where id = v_order.id returning * into v_order;

    return v_order;
end;
$$;

revoke all on function public.cancel_order(uuid) from public;
grant execute on function public.cancel_order(uuid) to authenticated;

-- 10. Marcar una dirección como predeterminada (una sola por usuario)
create or replace function public.set_default_address(p_address_id uuid)
returns void
language sql
security invoker
set search_path = ''
as $$
    update public.addresses
    set is_default = (id = p_address_id)
    where user_id = (select auth.uid());
$$;

revoke all on function public.set_default_address(uuid) from public;
grant execute on function public.set_default_address(uuid) to authenticated;

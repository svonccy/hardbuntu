-- Migración Inicial: Esquema de Catálogo Ferretero para Hardbuntu
-- Refleja el esquema real del proyecto Supabase (columnas icon, specs, image_url).
-- Cumple con las directrices de idempotencia para permitir ejecuciones repetidas sin errores.

-- 1. Función utilitaria para actualización automática de timestamps
create or replace function public.handle_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

-- 2. Tabla de Categorías Ferreteras
create table if not exists public.categories (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    slug text not null unique,
    description text,
    icon text,
    created_at timestamptz not null default now()
);

-- 3. Tabla de Productos Ferreteros
-- "price" es el precio de venta al público con IGV incluido.
-- "specs" guarda pares clave/valor de texto (la app los decodifica como Map<String, String>).
create table if not exists public.products (
    id uuid primary key default gen_random_uuid(),
    category_id uuid references public.categories(id) on delete set null,
    name text not null,
    slug text not null unique,
    brand text not null,
    sku text not null unique,
    description text,
    price numeric(12, 2) not null check (price >= 0),
    stock integer not null default 0 check (stock >= 0),
    unit text not null default 'unidad',
    specs jsonb not null default '{}'::jsonb,
    image_url text,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Trigger idempotente para updated_at en products
drop trigger if exists tr_products_updated_at on public.products;
create trigger tr_products_updated_at
    before update on public.products
    for each row
    execute function public.handle_updated_at();

-- 4. Índices para optimización de consultas
create index if not exists idx_products_category_id on public.products(category_id);
create index if not exists idx_products_brand on public.products(brand);
create index if not exists idx_products_is_active on public.products(is_active) where is_active = true;
create index if not exists idx_products_specs on public.products using gin (specs);

-- 5. Seguridad: Row Level Security (RLS)
alter table public.categories enable row level security;
alter table public.products enable row level security;

-- Políticas de lectura pública idempotentes (drop previo)
drop policy if exists "allow_public_read_categories" on public.categories;
create policy "allow_public_read_categories"
    on public.categories
    for select
    to anon, authenticated
    using (true);

drop policy if exists "allow_public_read_active_products" on public.products;
create policy "allow_public_read_active_products"
    on public.products
    for select
    to anon, authenticated
    using (is_active = true);

-- 6. Concesión de permisos al API REST (PostgREST)
grant usage on schema public to anon, authenticated;
grant select on public.categories to anon, authenticated;
grant select on public.products to anon, authenticated;

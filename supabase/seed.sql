-- Datos Semilla (Seed Data) para Hardbuntu
-- Catálogo de prueba para desarrollo local y testing

-- Insertar Categorías
insert into public.categories (id, name, slug, description, icon_name, display_order)
values
    ('11111111-1111-1111-1111-111111111111', 'Herramientas Eléctricas', 'herramientas-electricas', 'Taladros, rotomartillos, esmeriles, sierras circulares y amoladoras profesionales.', 'bolt', 1),
    ('22222222-2222-2222-2222-222222222222', 'Herramientas Manuales', 'herramientas-manuales', 'Martillos, destornilladores, llaves de impacto, alicates y juegos de dados.', 'handyman', 2),
    ('33333333-3333-3333-3333-333333333333', 'Tornillería y Fijaciones', 'tornilleria-fijaciones', 'Pernos hexagonales, tornillos drywall, tarugos, remaches y arandelas.', 'build', 3),
    ('44444444-4444-4444-4444-444444444444', 'Gasfitería y Plomería', 'gasfiteria-plomeria', 'Tuberías PVC, válvulas de bola, codos, uniones universales y pegamentos.', 'plumbing', 4),
    ('55555555-5555-5555-5555-555555555555', 'Seguridad Industrial', 'seguridad-industrial', 'Cascos, guantes de nitrilo, lentes balísticos, respiradores y botas dieléctricas.', 'shield', 5)
on conflict (id) do nothing;

-- Insertar Productos
insert into public.products (
    id, category_id, name, slug, brand, sku, description, price, stock, unit, technical_specs, images, is_featured
)
values
    (
        'aaaaaaa1-1111-1111-1111-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        'Rotomartillo SDS Plus Bosch GBH 2-24 D 820W',
        'rotomartillo-bosch-gbh-2-24-d',
        'Bosch',
        'BSH-GBH224D',
        'Rotomartillo profesional de 820W con encastre SDS Plus. Alta tasa de perforación y rendimiento de cincelado superior.',
        589.90,
        15,
        'unidad',
        '{"potencia": "820 W", "voltaje": "220 V", "fuerza_impacto": "2.7 J", "rpm_max": 1300, "mandril": "SDS Plus", "peso": "2.8 kg"}'::jsonb,
        array['https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=800&q=80'],
        true
    ),
    (
        'aaaaaaa2-2222-2222-2222-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        'Esmeril Angular DeWalt DWE4020 800W 4-1/2"',
        'esmeril-dewalt-dwe4020-800w',
        'DeWalt',
        'DWT-DWE4020',
        'Amoladora angular compacta de 800W para disco de 4-1/2 pulgadas (115 mm). Sistema de expulsión de polvo para mayor durabilidad.',
        249.00,
        28,
        'unidad',
        '{"potencia": "800 W", "voltaje": "220 V", "diametro_disco": "4-1/2 pulg (115mm)", "rpm_max": 12000, "peso": "1.8 kg"}'::jsonb,
        array['https://images.unsplash.com/photo-1572981779307-38b8cabb2407?auto=format&fit=crop&w=800&q=80'],
        true
    ),
    (
        'aaaaaaa3-3333-3333-3333-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        'Taladro Percutor Inalámbrico Makita DHP482Z 18V',
        'taladro-inalambrico-makita-dhp482z',
        'Makita',
        'MKT-DHP482Z',
        'Taladro atornillador percutor a batería de 18V LXT. 2 velocidades mecánicas, luz LED doble de trabajo y tecnología XPT resistente al polvo.',
        429.50,
        12,
        'unidad',
        '{"voltaje": "18 V", "tipo_bateria": "Li-ion LXT", "torque_max": "62 Nm", "mandril": "13 mm (1/2 pulg)", "rpm_max": 1900}'::jsonb,
        array['https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=800&q=80'],
        true
    ),
    (
        'aaaaaaa4-4444-4444-4444-aaaaaaaaaaaa',
        '22222222-2222-2222-2222-222222222222',
        'Juego de Destornilladores Stanley FatMax 6 Piezas',
        'juego-destornilladores-stanley-fatmax',
        'Stanley',
        'STN-65242',
        'Set de destornilladores profesionales planos y estrella con mangos ergonómicos bimaterial y puntas magnéticas fosfatadas.',
        69.90,
        45,
        'juego',
        '{"piezas": 6, "tipo_puntas": ["Plano 3.5mm", "Plano 5.5mm", "Plano 6.5mm", "PH0", "PH1", "PH2"], "material": "Acero Cromo Vanadio"}'::jsonb,
        array['https://images.unsplash.com/photo-1581783342308-f792dbdd27c5?auto=format&fit=crop&w=800&q=80'],
        false
    ),
    (
        'aaaaaaa5-5555-5555-5555-aaaaaaaaaaaa',
        '33333333-3333-3333-3333-333333333333',
        'Tornillo Autoperforante Drywall 6x1-1/4" Punta Fina (Caja x 1000)',
        'tornillo-drywall-6x1-1-4-caja-1000',
        'Fijaciones Perú',
        'FIJ-DRY6114',
        'Tornillos para placas de yeso-cartón drywall con recubrimiento fosfatado negro anticorrosivo. Caja de 1000 unidades.',
        28.50,
        120,
        'caja',
        '{"medida": "6 x 1-1/4 pulg", "tipo_punta": "Fina", "cabeza": "Trompeta", "acabado": "Fosfatado negro", "unidades_caja": 1000}'::jsonb,
        array['https://images.unsplash.com/photo-1586864387967-d02ef85d93e8?auto=format&fit=crop&w=800&q=80'],
        false
    ),
    (
        'aaaaaaa6-6666-6666-6666-aaaaaaaaaaaa',
        '55555555-5555-5555-5555-555555555555',
        'Casco de Seguridad 3M H-700 Blanco con Suspensión Ratchet',
        'casco-seguridad-3m-h700-blanco',
        '3M',
        '3M-H700-WHT',
        'Casco dieléctrico de protección clase E tipo 1 con suspensión de 4 puntos y ajuste tipo cremallera ratchet.',
        42.00,
        60,
        'unidad',
        '{"norma": "ANSI/ISEA Z89.1-2014 Tipo 1 Clase E", "color": "Blanco", "suspension": "Ratchet 4 puntos", "dielectrico": true}'::jsonb,
        array['https://images.unsplash.com/photo-1578873375973-585a9df6178c?auto=format&fit=crop&w=800&q=80'],
        true
    )
on conflict (id) do nothing;

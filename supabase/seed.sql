-- Datos Semilla (Seed Data) para Hardbuntu
-- Sincronizado con el catálogo real del proyecto Supabase (pwvttubqvhcyzgwaymdn)
-- Nota: los valores de "specs" deben ser siempre texto (la app los decodifica como Map<String, String>)

-- Insertar Categorías
insert into public.categories (id, name, slug, description, icon)
values
    ('11111111-1111-1111-1111-111111111111', 'Herramientas Eléctricas', 'herramientas-electricas', 'Taladros, rotomartillos, amoladoras y sierras circulares.', 'bolt'),
    ('22222222-2222-2222-2222-222222222222', 'Herramientas Manuales', 'herramientas-manuales', 'Martillos, destornilladores, alicates y juegos de dados.', 'handyman'),
    ('33333333-3333-3333-3333-333333333333', 'Tornillería y Fijaciones', 'tornilleria-fijaciones', 'Tornillos drywall, pernos, tarugos y remaches.', 'build'),
    ('44444444-4444-4444-4444-444444444444', 'Gasfitería y Plomería', 'gasfiteria-plomeria', 'Tuberías, conexiones PVC, válvulas y pegamentos.', 'plumbing'),
    ('55555555-5555-5555-5555-555555555555', 'Seguridad Industrial', 'seguridad-industrial', 'Cascos, guantes, respiradores y lentes de protección.', 'shield')
on conflict (id) do nothing;

-- Insertar Productos
insert into public.products (
    id, category_id, name, slug, brand, sku, description, price, stock, unit, specs, image_url, is_active
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
        '{"peso": "2.8 kg", "mandril": "SDS Plus", "rpm_max": "1300 RPM", "voltaje": "220 V", "potencia": "820 W", "fuerza_impacto": "2.7 J"}'::jsonb,
        'https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=800&q=80',
        true
    ),
    (
        'aaaaaaa2-2222-2222-2222-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        'Amoladora Angular DeWalt DWE4020 800W 4-1/2"',
        'esmeril-dewalt-dwe4020-800w',
        'DeWalt',
        'DWT-DWE4020',
        'Amoladora angular compacta de 800W para disco de 4-1/2 pulgadas (115 mm). Sistema de expulsión de polvo para mayor durabilidad.',
        249.00,
        28,
        'unidad',
        '{"peso": "1.8 kg", "rpm_max": "12000 RPM", "voltaje": "220 V", "potencia": "800 W", "diametro_disco": "4-1/2 pulg (115mm)"}'::jsonb,
        'https://images.unsplash.com/photo-1572981779307-38b8cabb2407?auto=format&fit=crop&w=800&q=80',
        true
    ),
    (
        'aaaaaaa3-3333-3333-3333-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        'Taladro Percutor Inalámbrico Makita DHP482Z 18V LXT',
        'taladro-inalambrico-makita-dhp482z',
        'Makita',
        'MKT-DHP482Z',
        'Taladro percutor a batería de 18V LXT. 2 velocidades mecánicas, luz LED doble de trabajo y tecnología XPT.',
        429.50,
        12,
        'unidad',
        '{"mandril": "13 mm (1/2 pulg)", "rpm_max": "1900 RPM", "voltaje": "18 V", "torque_max": "62 Nm", "tipo_bateria": "Li-ion LXT"}'::jsonb,
        'https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=800&q=80',
        true
    ),
    (
        'aaaaaaa4-4444-4444-4444-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        'Sierra Circular Bosch GKS 150 1500W 7-1/4"',
        'sierra-circular-bosch-gks-150',
        'Bosch',
        'BSH-GKS150',
        'Sierra circular potente de 1500W con disco de 7-1/4 pulgadas. Ideal para cortes rectos y biselados precisos en maderas.',
        479.00,
        10,
        'unidad',
        '{"rpm_max": "6000 RPM", "voltaje": "220 V", "potencia": "1500 W", "diametro_disco": "7-1/4 pulg", "profundidad_corte_90": "65 mm"}'::jsonb,
        'https://images.unsplash.com/photo-1581783342308-f792dbdd27c5?auto=format&fit=crop&w=800&q=80',
        true
    ),
    (
        'aaaaaaa5-5555-5555-5555-aaaaaaaaaaaa',
        '22222222-2222-2222-2222-222222222222',
        'Juego de Destornilladores Stanley FatMax 6 Piezas',
        'juego-destornilladores-stanley-fatmax',
        'Stanley',
        'STN-65242',
        'Set de destornilladores profesionales planos y estrella con mangos bimaterial ergonómicos y puntas magnéticas fosfatadas.',
        69.90,
        45,
        'juego',
        '{"piezas": "6", "puntas": "Planos 3.5mm, 5.5mm, 6.5mm / PH0, PH1, PH2", "material": "Acero Cromo Vanadio"}'::jsonb,
        'https://images.unsplash.com/photo-1581783342308-f792dbdd27c5?auto=format&fit=crop&w=800&q=80',
        true
    ),
    (
        'aaaaaaa6-6666-6666-6666-aaaaaaaaaaaa',
        '22222222-2222-2222-2222-222222222222',
        'Llave Francesa Ajustable Bahco 8" Fosfatada',
        'llave-francesa-bahco-8-pulg',
        'Bahco',
        'BHC-8071',
        'Llave ajustable de 8 pulgadas de máxima precisión. Escala milimétrica grabada y mango antideslizante con acabado fosfatado.',
        89.00,
        30,
        'unidad',
        '{"peso": "255 g", "acabado": "Fosfatado anticorrosion", "longitud": "8 pulg (205 mm)", "apertura_max": "27 mm"}'::jsonb,
        'https://images.unsplash.com/photo-1581783342308-f792dbdd27c5?auto=format&fit=crop&w=800&q=80',
        true
    ),
    (
        'aaaaaaa7-7777-7777-7777-aaaaaaaaaaaa',
        '33333333-3333-3333-3333-333333333333',
        'Tornillo Drywall Autoperforante 6x1-1/4" (Caja x 1000)',
        'tornillo-drywall-6x1-1-4-caja-1000',
        'Fijaciones Perú',
        'FIJ-DRY6114',
        'Tornillos para fijación de planchas de yeso a perfiles metálicos drywall. Cabeza trompeta fosfatada negra.',
        28.50,
        150,
        'caja',
        '{"medida": "6 x 1-1/4 pulg", "acabado": "Fosfatado negro", "tipo_punta": "Fina autorroscante", "unidades_caja": "1000"}'::jsonb,
        'https://images.unsplash.com/photo-1586864387967-d02ef85d93e8?auto=format&fit=crop&w=800&q=80',
        true
    ),
    (
        'aaaaaaa8-8888-8888-8888-aaaaaaaaaaaa',
        '33333333-3333-3333-3333-333333333333',
        'Disco de Corte para Metal DeWalt Extra Fino 4-1/2" x 1.0mm',
        'disco-corte-metal-dewalt-4-1-2',
        'DeWalt',
        'DWT-DW44618',
        'Disco de corte fino de óxido de aluminio de grano fino para acero y metales ferrosos. Corte ultra rápido sin rebabas.',
        5.50,
        300,
        'unidad',
        '{"eje": "7/8 pulg (22.2 mm)", "espesor": "1.0 mm", "rpm_max": "13300 RPM", "diametro": "4-1/2 pulg (115 mm)"}'::jsonb,
        'https://images.unsplash.com/photo-1572981779307-38b8cabb2407?auto=format&fit=crop&w=800&q=80',
        true
    ),
    (
        'aaaaaaa9-9999-9999-9999-aaaaaaaaaaaa',
        '44444444-4444-4444-4444-444444444444',
        'Válvula de Bola PVC Pavco Pesada 1/2" Roscada',
        'valvula-bola-pvc-pavco-1-2',
        'Pavco',
        'PVC-VBL12',
        'Válvula de corte esférica en PVC para instalaciones domiciliarias e industriales de agua fría a presión.',
        14.90,
        80,
        'unidad',
        '{"diametro": "1/2 pulg", "material": "PVC virgen", "tipo_union": "Roscada NPT", "presion_max": "150 PSI"}'::jsonb,
        'https://images.unsplash.com/photo-1585704032915-c3400ca199e7?auto=format&fit=crop&w=800&q=80',
        true
    ),
    (
        'aaaaaaaa-0000-0000-0000-aaaaaaaaaaaa',
        '55555555-5555-5555-5555-555555555555',
        'Casco de Seguridad 3M H-700 Blanco con Suspensión Ratchet',
        'casco-seguridad-3m-h700-blanco',
        '3M',
        '3M-H700-WHT',
        'Casco dieléctrico de protección clase E tipo 1 con arnés de 4 puntos y ajuste tipo cremallera ratchet.',
        42.00,
        65,
        'unidad',
        '{"color": "Blanco", "norma": "ANSI/ISEA Z89.1-2014 Tipo 1 Clase E", "suspension": "Ratchet 4 puntos", "dieléctrico": "Si"}'::jsonb,
        'https://images.unsplash.com/photo-1578873375973-585a9df6178c?auto=format&fit=crop&w=800&q=80',
        true
    )
on conflict (id) do nothing;

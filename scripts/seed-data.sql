-- ================================================================
-- SEED: Datos de prueba para TallerSoft
-- Contraseña de todos los usuarios: Admin123!
-- ================================================================

BEGIN;

-- ─── USUARIOS ────────────────────────────────────────────────────
INSERT INTO usuarios (nombre, email, password, rol, activo, created_at) VALUES
('Carlos Mendez',      'carlos.mendez@tallersoft.com',  '$2b$10$xQrH9KN1k1E.ZrRr2eFYL.Dwdrqi8ckYPVByzTqCfUUfb4nTcQQ8y', 'TECNICO',   true, NOW() - INTERVAL '180 days'),
('Lucia Fernandez',    'lucia.fernandez@tallersoft.com','$2b$10$xQrH9KN1k1E.ZrRr2eFYL.Dwdrqi8ckYPVByzTqCfUUfb4nTcQQ8y', 'RECEPCION', true, NOW() - INTERVAL '180 days'),
('Martin Alvarez',     'martin.alvarez@tallersoft.com', '$2b$10$xQrH9KN1k1E.ZrRr2eFYL.Dwdrqi8ckYPVByzTqCfUUfb4nTcQQ8y', 'TECNICO',   true, NOW() - INTERVAL '150 days'),
('Sofia Gutierrez',    'sofia.gutierrez@tallersoft.com','$2b$10$xQrH9KN1k1E.ZrRr2eFYL.Dwdrqi8ckYPVByzTqCfUUfb4nTcQQ8y', 'RECEPCION', true, NOW() - INTERVAL '120 days'),
('Diego Romero',       'diego.romero@tallersoft.com',   '$2b$10$xQrH9KN1k1E.ZrRr2eFYL.Dwdrqi8ckYPVByzTqCfUUfb4nTcQQ8y', 'TECNICO',   true, NOW() - INTERVAL '90 days')
ON CONFLICT (email) DO NOTHING;

-- ─── CLIENTES ─────────────────────────────────────────────────────
INSERT INTO clientes (nombre, telefono, email, direccion, activo, created_at) VALUES
('Juan Perez',         '351-4521890', 'juan.perez@gmail.com',       'Av. Colón 1234, Córdoba',          true, NOW() - INTERVAL '170 days'),
('Maria Lopez',        '351-4789234', 'maria.lopez@hotmail.com',    'Bv. San Juan 567, Córdoba',        true, NOW() - INTERVAL '165 days'),
('Roberto Silva',      '351-4234567', 'roberto.silva@yahoo.com',    'Calle 9 de Julio 890, Alta Gracia',true, NOW() - INTERVAL '160 days'),
('Ana Torres',         '351-4678901', 'ana.torres@gmail.com',       'Dean Funes 234, Córdoba',          true, NOW() - INTERVAL '155 days'),
('Luis Ramirez',       '351-4345678', 'luis.ramirez@gmail.com',     'Vélez Sarsfield 1122, Córdoba',    true, NOW() - INTERVAL '150 days'),
('Carmen Diaz',        '351-4901234', 'carmen.diaz@hotmail.com',    'Av. Rafael Núñez 3344, Córdoba',   true, NOW() - INTERVAL '145 days'),
('Jorge Morales',      '351-4567890', 'jorge.morales@gmail.com',    'Independencia 456, Villa Carlos Paz',true, NOW() - INTERVAL '140 days'),
('Patricia Ruiz',      '351-4123456', 'patricia.ruiz@gmail.com',    'Duarte Quirós 789, Córdoba',       true, NOW() - INTERVAL '135 days'),
('Fernando Castro',    '351-4890123', 'fernando.castro@yahoo.com',  'Av. Poeta Lugones 1011, Córdoba',  true, NOW() - INTERVAL '130 days'),
('Sandra Flores',      '351-4456789', 'sandra.flores@gmail.com',    'Marcelo T. de Alvear 2233, Córdoba',true, NOW() - INTERVAL '125 days'),
('Ricardo Herrera',    '351-4012345', 'ricardo.herrera@gmail.com',  'Av. Hipólito Yrigoyen 4455, Córdoba',true, NOW() - INTERVAL '120 days'),
('Graciela Vargas',    '351-4678012', 'graciela.vargas@hotmail.com','Santa Rosa 667, Córdoba',          true, NOW() - INTERVAL '115 days'),
('Marcos Gimenez',     '351-4234890', 'marcos.gimenez@gmail.com',   'San Jerónimo 888, Córdoba',        true, NOW() - INTERVAL '110 days'),
('Elena Medina',       '351-4901567', 'elena.medina@gmail.com',     'Corrientes 999, Córdoba',          true, NOW() - INTERVAL '105 days'),
('Pablo Suarez',       '351-4567123', 'pablo.suarez@yahoo.com',     'Entre Ríos 1234, Córdoba',         true, NOW() - INTERVAL '100 days'),
('Valeria Rojas',      '351-4123789', 'valeria.rojas@gmail.com',    'Av. General Paz 2345, Córdoba',    true, NOW() - INTERVAL '95 days'),
('Alejandro Nunez',    '351-4890456', 'alejandro.nunez@gmail.com',  'Obispo Trejo 3456, Córdoba',       true, NOW() - INTERVAL '90 days'),
('Natalia Vega',       '351-4456012', 'natalia.vega@hotmail.com',   'San Lorenzo 456, Villa María',     true, NOW() - INTERVAL '85 days'),
('Gustavo Mora',       '351-4012678', 'gustavo.mora@gmail.com',     'Av. Sabattini 567, Córdoba',       true, NOW() - INTERVAL '80 days'),
('Claudia Ortiz',      '351-4678234', 'claudia.ortiz@gmail.com',    'Cañada 678, Córdoba',              true, NOW() - INTERVAL '75 days'),
('Horacio Ramos',      '351-4234012', 'horacio.ramos@yahoo.com',    'Rivadavia 789, Río Cuarto',        true, NOW() - INTERVAL '70 days'),
('Beatriz Aguirre',    '351-4901789', 'beatriz.aguirre@gmail.com',  'Deán Funes 890, Jesús María',      true, NOW() - INTERVAL '65 days'),
('Sebastian Blanco',   '351-4567456', 'sebastian.blanco@gmail.com', 'Av. Colón 901, Córdoba',           true, NOW() - INTERVAL '60 days'),
('Adriana Cruz',       '351-4123012', 'adriana.cruz@hotmail.com',   'Laprida 1012, Córdoba',            true, NOW() - INTERVAL '55 days'),
('Nicolás Reyes',      '351-4890789', 'nicolas.reyes@gmail.com',    'Fructuoso Rivera 1123, Córdoba',   true, NOW() - INTERVAL '50 days'),
('Monica Sosa',        '351-4456234', 'monica.sosa@gmail.com',      'Av. Resolución 1234, Córdoba',     true, NOW() - INTERVAL '45 days'),
('Ignacio Ibarra',     '351-4012901', 'ignacio.ibarra@yahoo.com',   'Córdoba 345, Villa Allende',       true, NOW() - INTERVAL '40 days'),
('Cecilia Ponce',      '351-4678567', 'cecilia.ponce@gmail.com',    'Belgrano 456, Malagueño',          true, NOW() - INTERVAL '35 days'),
('Daniel Espinoza',    '351-4234123', 'daniel.espinoza@gmail.com',  'Sarmiento 567, Córdoba',           true, NOW() - INTERVAL '30 days'),
('Laura Figueroa',     '351-4901234', 'laura.figueroa@hotmail.com', 'Tucumán 678, La Falda',            true, NOW() - INTERVAL '25 days'),
('Oscar Delgado',      '351-4567890', 'oscar.delgado@gmail.com',    'Mitre 789, Cosquín',               true, NOW() - INTERVAL '20 days'),
('Gabriela Rios',      '351-4123456', 'gabriela.rios@gmail.com',    'Av. Vélez Sarsfield 890, Córdoba', true, NOW() - INTERVAL '15 days'),
('Esteban Maldonado',  '351-4890012', 'esteban.maldonado@yahoo.com','San Martín 1001, Cruz del Eje',    true, NOW() - INTERVAL '10 days'),
('Roxana Carrizo',     '351-4456678', 'roxana.carrizo@gmail.com',   'Urquiza 1112, Córdoba',            true, NOW() - INTERVAL '7 days'),
('Matías Dominguez',   '351-4012234', 'matias.dominguez@gmail.com', 'Chacabuco 1223, Córdoba',          true, NOW() - INTERVAL '5 days')
ON CONFLICT DO NOTHING;

-- ─── REPUESTOS ────────────────────────────────────────────────────
INSERT INTO repuestos (nombre, categoria, precio, stock_actual, stock_minimo, created_at) VALUES
('Pantalla LCD 6.1" iPhone',          'Pantallas',       45000.00, 8,  3, NOW() - INTERVAL '180 days'),
('Pantalla OLED Samsung S21',         'Pantallas',       62000.00, 5,  3, NOW() - INTERVAL '180 days'),
('Pantalla LCD Motorola G54',         'Pantallas',       18500.00, 12, 5, NOW() - INTERVAL '180 days'),
('Pantalla LCD Xiaomi Redmi 10',      'Pantallas',       14200.00, 10, 5, NOW() - INTERVAL '180 days'),
('Batería iPhone 12',                 'Baterías',        8500.00,  15, 5, NOW() - INTERVAL '180 days'),
('Batería Samsung A52',               'Baterías',        6200.00,  18, 5, NOW() - INTERVAL '180 days'),
('Batería Motorola G30',              'Baterías',        4800.00,  20, 5, NOW() - INTERVAL '180 days'),
('Batería Xiaomi Redmi 9',            'Baterías',        4200.00,  22, 5, NOW() - INTERVAL '180 days'),
('Conector de carga USB-C universal', 'Conectores',      1200.00,  30, 10, NOW() - INTERVAL '180 days'),
('Conector Lightning iPhone',         'Conectores',      2400.00,  25, 8, NOW() - INTERVAL '180 days'),
('Conector micro USB',                'Conectores',       800.00,  35, 10, NOW() - INTERVAL '180 days'),
('Flex de cámara trasera Samsung A53','Cámaras',         7800.00,  7,  3, NOW() - INTERVAL '180 days'),
('Lente de cámara iPhone 13',         'Cámaras',        12000.00, 5,  2, NOW() - INTERVAL '180 days'),
('Módulo cámara frontal Motorola',    'Cámaras',         3200.00,  10, 4, NOW() - INTERVAL '180 days'),
('Tapa trasera Xiaomi Redmi 10',      'Carcasas',        2800.00,  14, 5, NOW() - INTERVAL '180 days'),
('Tapa trasera Samsung A51',          'Carcasas',        3600.00,  11, 5, NOW() - INTERVAL '180 days'),
('Marco/chasis iPhone 11',            'Carcasas',       22000.00,  4,  2, NOW() - INTERVAL '180 days'),
('Altavoz principal Samsung',         'Audio',           1800.00,  20, 7, NOW() - INTERVAL '180 days'),
('Auricular de llamadas iPhone',      'Audio',           2200.00,  18, 6, NOW() - INTERVAL '180 days'),
('Vibrador universal',                'Mecanismos',       900.00,  25, 8, NOW() - INTERVAL '180 days'),
('Botón home Samsung',                'Botones',         1400.00,  22, 7, NOW() - INTERVAL '180 days'),
('Flex botón encendido iPhone',       'Botones',         1600.00,  16, 5, NOW() - INTERVAL '180 days'),
('Flex volumen Motorola',             'Botones',         1100.00,  18, 6, NOW() - INTERVAL '180 days'),
('Sensor de proximidad universal',    'Sensores',        1300.00,  14, 5, NOW() - INTERVAL '180 days'),
('Pasta térmica procesador',          'Insumos',          450.00,  40, 15, NOW() - INTERVAL '180 days'),
('Adhesivo doble faz pantallas',      'Insumos',          380.00,  50, 15, NOW() - INTERVAL '180 days'),
('Kit limpieza ultrasonido',          'Insumos',         1200.00,  20, 8, NOW() - INTERVAL '180 days'),
('Placa de carga Samsung A32',        'Placas',          5400.00,  8,  3, NOW() - INTERVAL '180 days'),
('IC de carga iPhone 12',             'Placas',          8900.00,  6,  2, NOW() - INTERVAL '180 days'),
('Chip NFC Samsung',                  'Placas',          3200.00,  9,  3, NOW() - INTERVAL '180 days'),
('Membrana anti-polvo altavoz',       'Insumos',          280.00,  60, 20, NOW() - INTERVAL '180 days'),
('Tornillos pentalobe iPhone (set)',  'Insumos',          350.00,  45, 15, NOW() - INTERVAL '180 days'),
('Pantalla LCD Huawei P20 Lite',      'Pantallas',       16800.00, 7,  3, NOW() - INTERVAL '180 days'),
('Batería Huawei P30',                'Baterías',        5600.00,  13, 5, NOW() - INTERVAL '180 days'),
('Conector de auriculares 3.5mm',     'Conectores',       950.00,  28, 10, NOW() - INTERVAL '180 days');

-- ─── EQUIPOS ──────────────────────────────────────────────────────
-- Asignamos equipos a clientes (cliente_id 1 al 35 según el orden de inserción)
-- Usamos subquery para obtener IDs por email

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Apple', 'iPhone 12', 'DNPXQ1234567', 'Pantalla rayada en esquina'
FROM clientes c WHERE c.email = 'juan.perez@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Apple', 'iPhone 11', 'F17XJ9876543', NULL
FROM clientes c WHERE c.email = 'juan.perez@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Samsung', 'Galaxy A52', 'R9TK5551234', 'Golpe en esquina inferior'
FROM clientes c WHERE c.email = 'maria.lopez@hotmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Tablet', 'Samsung', 'Galaxy Tab A7', 'R9AB1239876', NULL
FROM clientes c WHERE c.email = 'maria.lopez@hotmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Motorola', 'Moto G54', 'ZY33C4567890', NULL
FROM clientes c WHERE c.email = 'roberto.silva@yahoo.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Xiaomi', 'Redmi 10', 'K7BTAG123456', 'Sin tapa trasera'
FROM clientes c WHERE c.email = 'ana.torres@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Samsung', 'Galaxy S21', 'R9HS3334567', NULL
FROM clientes c WHERE c.email = 'luis.ramirez@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Apple', 'iPhone 13', 'MPXJ2223456', NULL
FROM clientes c WHERE c.email = 'carmen.diaz@hotmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Motorola', 'Moto G30', 'ZY22B5678901', 'Mojado previamente'
FROM clientes c WHERE c.email = 'jorge.morales@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Xiaomi', 'Redmi 9', 'K5DTAG654321', NULL
FROM clientes c WHERE c.email = 'patricia.ruiz@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Tablet', 'Apple', 'iPad 9na Gen', 'DLXPH9990001', NULL
FROM clientes c WHERE c.email = 'fernando.castro@yahoo.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Huawei', 'P20 Lite', 'HW99X1112223', 'Cámara trasera rota'
FROM clientes c WHERE c.email = 'sandra.flores@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Samsung', 'Galaxy A53', 'R8QK7778889', NULL
FROM clientes c WHERE c.email = 'ricardo.herrera@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Apple', 'iPhone SE 2022', 'MNX2K4445556', NULL
FROM clientes c WHERE c.email = 'graciela.vargas@hotmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Motorola', 'Edge 30', 'ZY44A2223334', NULL
FROM clientes c WHERE c.email = 'marcos.gimenez@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Samsung', 'Galaxy A23', 'R7JL6667778', 'Pantalla con manchas'
FROM clientes c WHERE c.email = 'elena.medina@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Apple', 'iPhone X', 'C39YD1112223', 'Face ID no funciona'
FROM clientes c WHERE c.email = 'pablo.suarez@yahoo.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Xiaomi', 'Redmi Note 10', 'M4CTBG765432', NULL
FROM clientes c WHERE c.email = 'valeria.rojas@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Tablet', 'Xiaomi', 'Pad 5', 'K3BXAP333444', NULL
FROM clientes c WHERE c.email = 'alejandro.nunez@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Huawei', 'P30', 'HW88Y5556667', NULL
FROM clientes c WHERE c.email = 'natalia.vega@hotmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Samsung', 'Galaxy A13', 'R6IM9990001', NULL
FROM clientes c WHERE c.email = 'gustavo.mora@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Apple', 'iPhone 8', 'DMPWF8889990', 'Batería hinchada'
FROM clientes c WHERE c.email = 'claudia.ortiz@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Motorola', 'Moto G22', 'ZY55C8889990', NULL
FROM clientes c WHERE c.email = 'horacio.ramos@yahoo.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Samsung', 'Galaxy M32', 'R5HL2223334', NULL
FROM clientes c WHERE c.email = 'beatriz.aguirre@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Apple', 'iPhone 12 Pro', 'FQXPV5556667', NULL
FROM clientes c WHERE c.email = 'sebastian.blanco@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Xiaomi', 'POCO X3', 'K2GSTP876543', 'Conector carga dañado'
FROM clientes c WHERE c.email = 'adriana.cruz@hotmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Samsung', 'Galaxy A33', 'R4FK4445556', NULL
FROM clientes c WHERE c.email = 'nicolas.reyes@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Motorola', 'Moto G52', 'ZY66D7778889', NULL
FROM clientes c WHERE c.email = 'monica.sosa@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Tablet', 'Samsung', 'Galaxy Tab S6 Lite', 'R3EG1112223', NULL
FROM clientes c WHERE c.email = 'ignacio.ibarra@yahoo.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Apple', 'iPhone 14', 'PRQNZ0001112', NULL
FROM clientes c WHERE c.email = 'cecilia.ponce@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Huawei', 'P20 Lite', 'HW77Z2223334', NULL
FROM clientes c WHERE c.email = 'daniel.espinoza@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Samsung', 'Galaxy A54', 'R2DI6667778', NULL
FROM clientes c WHERE c.email = 'laura.figueroa@hotmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Apple', 'iPhone 11', 'C48ZE3334445', NULL
FROM clientes c WHERE c.email = 'oscar.delgado@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Motorola', 'Moto G84', 'ZY77E6667778', NULL
FROM clientes c WHERE c.email = 'gabriela.rios@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Xiaomi', 'Redmi 12', 'K1HTBU987654', NULL
FROM clientes c WHERE c.email = 'esteban.maldonado@yahoo.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Samsung', 'Galaxy A14', 'R1CH8889990', NULL
FROM clientes c WHERE c.email = 'roxana.carrizo@gmail.com';

INSERT INTO equipos (cliente_id, tipo, marca, modelo, numero_serie, observaciones)
SELECT c.id, 'Smartphone', 'Apple', 'iPhone 13', 'GRMSY1234500', NULL
FROM clientes c WHERE c.email = 'matias.dominguez@gmail.com';

-- ─── ORDENES DE TRABAJO ───────────────────────────────────────────
-- Creamos ~80 órdenes distribuidas en los últimos 6 meses con distintos estados

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id,
  c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Pantalla rota, no responde al tacto',
  'Pantalla LCD dañada por impacto, requiere reemplazo completo',
  'ENTREGADO', 'ALTA', 47500.00,
  NOW() - INTERVAL '165 days', NOW() - INTERVAL '160 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'juan.perez@gmail.com' AND e.modelo = 'iPhone 12';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Batería se descarga muy rápido, dura menos de 2 horas',
  'Batería degradada al 67%, reemplazo necesario',
  'ENTREGADO', 'NORMAL', 9200.00,
  NOW() - INTERVAL '158 days', NOW() - INTERVAL '155 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'juan.perez@gmail.com' AND e.modelo = 'iPhone 11';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Pantalla con líneas verticales y colores distorsionados',
  'Falla en panel OLED, se debe reemplazar módulo completo',
  'ENTREGADO', 'ALTA', 65000.00,
  NOW() - INTERVAL '150 days', NOW() - INTERVAL '145 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'maria.lopez@hotmail.com' AND e.modelo = 'Galaxy A52';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'No carga, probé varios cables y nada',
  'Conector USB-C con pines doblados, requiere reemplazo',
  'ENTREGADO', 'NORMAL', 2800.00,
  NOW() - INTERVAL '145 days', NOW() - INTERVAL '142 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'roberto.silva@yahoo.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Pantalla negra, el teléfono prende (vibra) pero no se ve nada',
  'Pantalla LCD quemada por cortocircuito interno, reemplazo',
  'ENTREGADO', 'ALTA', 15800.00,
  NOW() - INTERVAL '140 days', NOW() - INTERVAL '136 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'ana.torres@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Se cayó al agua, no enciende',
  'Daño por humedad en placa base, limpieza ultrasónica y reemplazo batería',
  'ENTREGADO', 'ALTA', 12500.00,
  NOW() - INTERVAL '135 days', NOW() - INTERVAL '129 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'luis.ramirez@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Cámara trasera borrosa, fotos salen desenfocadas',
  'Lente de cámara principal con microfisuras, reemplazo módulo',
  'ENTREGADO', 'NORMAL', 13500.00,
  NOW() - INTERVAL '130 days', NOW() - INTERVAL '125 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'carmen.diaz@hotmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Se mojó, enciende pero pantalla tiene manchas de agua',
  'Humedad interna, limpieza ultrasónica y sellado, pantalla con daño por humedad',
  'ENTREGADO', 'ALTA', 8500.00,
  NOW() - INTERVAL '128 days', NOW() - INTERVAL '124 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'jorge.morales@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Batería dura muy poco, se apaga sola al 30%',
  'Batería defectuosa con bajo ciclo de calibración, reemplazo',
  'ENTREGADO', 'NORMAL', 5500.00,
  NOW() - INTERVAL '120 days', NOW() - INTERVAL '117 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'patricia.ruiz@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'No reconoce el SIM, reinicio constante',
  'Slot SIM dañado, reemplazo de bandeja y limpieza de contactos',
  'ENTREGADO', 'NORMAL', 3800.00,
  NOW() - INTERVAL '115 days', NOW() - INTERVAL '112 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'fernando.castro@yahoo.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Cámara trasera rota, no abre la app de cámara',
  'Módulo de cámara trasera dañado físicamente, reemplazo',
  'ENTREGADO', 'NORMAL', 9200.00,
  NOW() - INTERVAL '110 days', NOW() - INTERVAL '106 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'sandra.flores@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Pantalla con toque fantasma, toca solo',
  'Digitalizador defectuoso, reemplazo de pantalla completa',
  'ENTREGADO', 'ALTA', 9800.00,
  NOW() - INTERVAL '108 days', NOW() - INTERVAL '104 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'ricardo.herrera@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Botón home no funciona',
  'Flex de botón home roto, reemplazo',
  'ENTREGADO', 'BAJA', 2200.00,
  NOW() - INTERVAL '105 days', NOW() - INTERVAL '102 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'graciela.vargas@hotmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 2),
  'Altavoz no suena, solo funciona con auriculares',
  'Altavoz principal dañado, reemplazo',
  'ENTREGADO', 'NORMAL', 2500.00,
  NOW() - INTERVAL '100 days', NOW() - INTERVAL '97 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'marcos.gimenez@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Pantalla con manchas negras después de golpe',
  'Pantalla LCD con daño interno por impacto, reemplazo',
  'ENTREGADO', 'ALTA', 16500.00,
  NOW() - INTERVAL '98 days', NOW() - INTERVAL '93 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'elena.medina@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Face ID dejó de funcionar de repente',
  'Módulo cámara frontal con sensor TrueDepth dañado, requiere diagnóstico avanzado',
  'ENTREGADO', 'NORMAL', 4500.00,
  NOW() - INTERVAL '95 days', NOW() - INTERVAL '91 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'pablo.suarez@yahoo.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 2),
  'Batería dura 3 horas con uso normal',
  'Batería con 74% de salud, reemplazo preventivo',
  'ENTREGADO', 'NORMAL', 5800.00,
  NOW() - INTERVAL '90 days', NOW() - INTERVAL '87 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'valeria.rojas@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'No carga, cargador funciona con otro dispositivo',
  'Conector USB-C con suciedad y pin roto, limpieza y reemplazo',
  'ENTREGADO', 'NORMAL', 2200.00,
  NOW() - INTERVAL '88 days', NOW() - INTERVAL '85 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'alejandro.nunez@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Batería dura muy poco, se calienta mucho al cargar',
  'Batería hinchada con riesgo, reemplazo urgente',
  'ENTREGADO', 'ALTA', 6800.00,
  NOW() - INTERVAL '85 days', NOW() - INTERVAL '81 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'natalia.vega@hotmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Pantalla rota por caída, funciona pero rajada',
  'Pantalla LCD con vidrio roto, reemplazo módulo completo',
  'ENTREGADO', 'NORMAL', 9500.00,
  NOW() - INTERVAL '80 days', NOW() - INTERVAL '76 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'gustavo.mora@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 2),
  'Batería hinchada, tapa trasera levantada',
  'Batería defectuosa hinchada, reemplazo urgente por seguridad',
  'ENTREGADO', 'ALTA', 9500.00,
  NOW() - INTERVAL '78 days', NOW() - INTERVAL '74 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'claudia.ortiz@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'No enciende desde ayer',
  'Batería agotada y conector microUSB dañado, reemplazo de ambos',
  'ENTREGADO', 'NORMAL', 6500.00,
  NOW() - INTERVAL '72 days', NOW() - INTERVAL '68 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'horacio.ramos@yahoo.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'El volumen no funciona, botones físicos trabados',
  'Flex de volumen oxidado y atascado, limpieza y reemplazo',
  'ENTREGADO', 'BAJA', 2800.00,
  NOW() - INTERVAL '68 days', NOW() - INTERVAL '65 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'beatriz.aguirre@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Pantalla con líneas horizontales, mitad no responde',
  'Pantalla OLED con daño parcial en digitalizador, reemplazo completo',
  'ENTREGADO', 'ALTA', 52000.00,
  NOW() - INTERVAL '65 days', NOW() - INTERVAL '60 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'sebastian.blanco@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 2),
  'No carga, conector roto',
  'Conector USB-C roto internamente, reemplazo necesario',
  'ENTREGADO', 'NORMAL', 2500.00,
  NOW() - INTERVAL '62 days', NOW() - INTERVAL '59 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'adriana.cruz@hotmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Pantalla rota, zona superior no responde',
  'Pantalla LCD con vidrio roto y digitalizador dañado en zona superior, reemplazo',
  'ENTREGADO', 'ALTA', 10500.00,
  NOW() - INTERVAL '58 days', NOW() - INTERVAL '54 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'nicolas.reyes@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Altavoz de llamadas no se escucha bien, muy bajo',
  'Membrana del auricular de llamadas sucia y dañada, limpieza y reemplazo',
  'ENTREGADO', 'BAJA', 3500.00,
  NOW() - INTERVAL '55 days', NOW() - INTERVAL '52 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'monica.sosa@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Pantalla rota completamente',
  'Módulo de pantalla completamente dañado, reemplazo total',
  'ENTREGADO', 'ALTA', 29000.00,
  NOW() - INTERVAL '50 days', NOW() - INTERVAL '46 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'ignacio.ibarra@yahoo.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 2),
  'Batería dura muy poco desde actualización',
  'Post-actualización con consumo elevado, calibración y reemplazo batería',
  'ENTREGADO', 'NORMAL', 9200.00,
  NOW() - INTERVAL '48 days', NOW() - INTERVAL '44 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'cecilia.ponce@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'No enciende, pantalla negra total',
  'Batería completamente muerta y conector dañado, reemplazo',
  'ENTREGADO', 'ALTA', 8200.00,
  NOW() - INTERVAL '45 days', NOW() - INTERVAL '41 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'daniel.espinoza@gmail.com';

-- Órdenes EN_PROCESO (en taller ahora)
INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Pantalla rajada, toca pero con dificultad',
  'Pantalla LCD con vidrio roto, diagnóstico confirma digitalizador funcional, solo reemplazo de glass',
  'EN_PROCESO', 'NORMAL', 8800.00,
  NOW() - INTERVAL '8 days', NOW() - INTERVAL '6 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'laura.figueroa@hotmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Se cayó y no enciende, pantalla rajada',
  'Pantalla rota y batería desconectada por impacto, evaluando daño en placa',
  'EN_PROCESO', 'ALTA', 0.00,
  NOW() - INTERVAL '6 days', NOW() - INTERVAL '4 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'oscar.delgado@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 2),
  'Batería dura 4 horas, se calienta',
  'Batería degradada al 69%, reemplazo en proceso',
  'EN_PROCESO', 'NORMAL', 5500.00,
  NOW() - INTERVAL '4 days', NOW() - INTERVAL '2 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'gabriela.rios@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'No carga desde hace 2 días, el equipo se apagó',
  'Conector carga dañado y batería por debajo del umbral de carga, trabajando',
  'EN_PROCESO', 'ALTA', 6200.00,
  NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'esteban.maldonado@yahoo.com';

-- Órdenes LISTO (listas para retirar)
INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Pantalla rota, cristal todo rajado',
  'Pantalla LCD reemplazada, equipo funcionando correctamente',
  'LISTO', 'NORMAL', 10800.00,
  NOW() - INTERVAL '12 days', NOW() - INTERVAL '3 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'roxana.carrizo@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Batería se descarga rapidísimo',
  'Batería reemplazada, autonomía restaurada. Listo para retirar',
  'LISTO', 'NORMAL', 9500.00,
  NOW() - INTERVAL '10 days', NOW() - INTERVAL '2 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'matias.dominguez@gmail.com';

-- Órdenes PENDIENTE (recién ingresadas)
INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id, NULL,
  'Pantalla parpadeante, a veces se pone verde',
  NULL,
  'PENDIENTE', 'NORMAL', 0.00,
  NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'juan.perez@gmail.com' AND e.modelo = 'iPhone 11';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT
  e.id, c.id, NULL,
  'No prende, fue mojado en lluvia',
  NULL,
  'PENDIENTE', 'ALTA', 0.00,
  NOW(), NOW()
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'maria.lopez@hotmail.com' AND e.tipo = 'Tablet';

-- Órdenes adicionales para meses intermedios (más volumen)
INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Pantalla rota por caída', 'Reemplazo pantalla LCD completa',
  'ENTREGADO', 'ALTA', 17500.00,
  NOW() - INTERVAL '75 days', NOW() - INTERVAL '71 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'horacio.ramos@yahoo.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Batería no carga al 100%, se queda en 80%', 'Calibración y reemplazo batería',
  'ENTREGADO', 'BAJA', 4800.00,
  NOW() - INTERVAL '38 days', NOW() - INTERVAL '35 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'beatriz.aguirre@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 2),
  'Altavoz con sonido distorsionado', 'Reemplazo altavoz principal Samsung',
  'ENTREGADO', 'NORMAL', 2800.00,
  NOW() - INTERVAL '35 days', NOW() - INTERVAL '32 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'nicolas.reyes@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'NFC no funciona', 'Reemplazo chip NFC',
  'ENTREGADO', 'BAJA', 4500.00,
  NOW() - INTERVAL '28 days', NOW() - INTERVAL '25 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'gustavo.mora@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Vibración no funciona', 'Reemplazo vibrador',
  'ENTREGADO', 'BAJA', 1800.00,
  NOW() - INTERVAL '22 days', NOW() - INTERVAL '19 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'valeria.rojas@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Sensor de proximidad no detecta, pantalla queda encendida en llamadas',
  'Reemplazo sensor proximidad',
  'ENTREGADO', 'NORMAL', 2500.00,
  NOW() - INTERVAL '18 days', NOW() - INTERVAL '15 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'claudia.ortiz@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 2),
  'Jack de auriculares no funciona',
  'Reemplazo conector auriculares 3.5mm',
  'ENTREGADO', 'BAJA', 2200.00,
  NOW() - INTERVAL '15 days', NOW() - INTERVAL '12 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'marcos.gimenez@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1),
  'Placa de carga quemada, huele a quemado', 'Reemplazo placa de carga',
  'ENTREGADO', 'ALTA', 7200.00,
  NOW() - INTERVAL '55 days', NOW() - INTERVAL '50 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'gustavo.mora@gmail.com';

INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad, presupuesto, created_at, updated_at)
SELECT e.id, c.id,
  (SELECT u.id FROM usuarios u WHERE u.rol = 'TECNICO' ORDER BY u.id LIMIT 1 OFFSET 1),
  'Pantalla rota por caída, zona inferior', 'Reemplazo LCD completo',
  'ENTREGADO', 'ALTA', 47500.00,
  NOW() - INTERVAL '42 days', NOW() - INTERVAL '38 days'
FROM equipos e JOIN clientes c ON e.cliente_id = c.id WHERE c.email = 'sebastian.blanco@gmail.com';

-- ─── ORDEN_REPUESTOS (repuestos usados en órdenes) ────────────────
-- Vinculamos repuestos a las órdenes ya creadas

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Pantalla LCD 6.1" iPhone'
WHERE c.email = 'juan.perez@gmail.com' AND ot.estado = 'ENTREGADO'
  AND ot.falla_reportada LIKE '%Pantalla rota%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Tornillos pentalobe iPhone (set)'
WHERE c.email = 'juan.perez@gmail.com' AND ot.estado = 'ENTREGADO'
  AND ot.falla_reportada LIKE '%Pantalla rota%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Batería iPhone 12'
WHERE c.email = 'juan.perez@gmail.com' AND ot.falla_reportada LIKE '%Batería%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Pantalla OLED Samsung S21'
WHERE c.email = 'maria.lopez@hotmail.com' AND ot.estado = 'ENTREGADO'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Conector de carga USB-C universal'
WHERE c.email = 'roberto.silva@yahoo.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Pantalla LCD Xiaomi Redmi 10'
WHERE c.email = 'ana.torres@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Batería Samsung A52'
WHERE c.email = 'luis.ramirez@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Kit limpieza ultrasonido'
WHERE c.email = 'luis.ramirez@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Lente de cámara iPhone 13'
WHERE c.email = 'carmen.diaz@hotmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Batería Motorola G30'
WHERE c.email = 'patricia.ruiz@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Flex de cámara trasera Samsung A53'
WHERE c.email = 'sandra.flores@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Pantalla LCD Motorola G54'
WHERE c.email = 'elena.medina@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Módulo cámara frontal Motorola'
WHERE c.email = 'pablo.suarez@yahoo.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Batería Xiaomi Redmi 9'
WHERE c.email = 'valeria.rojas@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Conector de carga USB-C universal'
WHERE c.email = 'alejandro.nunez@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Batería Huawei P30'
WHERE c.email = 'natalia.vega@hotmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Pantalla LCD Motorola G54'
WHERE c.email = 'gustavo.mora@gmail.com' AND ot.falla_reportada LIKE '%rota%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Batería iPhone 12'
WHERE c.email = 'claudia.ortiz@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Conector micro USB'
WHERE c.email = 'horacio.ramos@yahoo.com' AND ot.falla_reportada LIKE '%enciende%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Batería Motorola G30'
WHERE c.email = 'horacio.ramos@yahoo.com' AND ot.falla_reportada LIKE '%enciende%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Flex volumen Motorola'
WHERE c.email = 'beatriz.aguirre@gmail.com' AND ot.falla_reportada LIKE '%volumen%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Pantalla LCD 6.1" iPhone'
WHERE c.email = 'sebastian.blanco@gmail.com' AND ot.falla_reportada LIKE '%líneas horizontales%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Conector de carga USB-C universal'
WHERE c.email = 'adriana.cruz@hotmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Pantalla LCD Samsung' -- closest match
WHERE c.email = 'nicolas.reyes@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Altavoz principal Samsung'
WHERE c.email = 'monica.sosa@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Batería Samsung A52'
WHERE c.email = 'cecilia.ponce@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Altavoz principal Samsung'
WHERE c.email = 'marcos.gimenez@gmail.com' AND ot.falla_reportada LIKE '%Altavoz%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Placa de carga Samsung A32'
WHERE c.email = 'gustavo.mora@gmail.com' AND ot.falla_reportada LIKE '%Placa%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Vibrador universal'
WHERE c.email = 'valeria.rojas@gmail.com' AND ot.falla_reportada LIKE '%Vibración%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Sensor de proximidad universal'
WHERE c.email = 'claudia.ortiz@gmail.com' AND ot.falla_reportada LIKE '%proximidad%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Conector de auriculares 3.5mm'
WHERE c.email = 'marcos.gimenez@gmail.com' AND ot.falla_reportada LIKE '%Jack%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Chip NFC Samsung'
WHERE c.email = 'gustavo.mora@gmail.com' AND ot.falla_reportada LIKE '%NFC%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Pantalla LCD Huawei P20 Lite'
WHERE c.email = 'daniel.espinoza@gmail.com'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Pantalla OLED Samsung S21'
WHERE c.email = 'sebastian.blanco@gmail.com' AND ot.falla_reportada LIKE '%Pantalla rota%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Pantalla LCD Motorola G54'
WHERE c.email = 'horacio.ramos@yahoo.com' AND ot.falla_reportada LIKE '%rota%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Batería Motorola G30'
WHERE c.email = 'beatriz.aguirre@gmail.com' AND ot.falla_reportada LIKE '%carga%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Altavoz principal Samsung'
WHERE c.email = 'nicolas.reyes@gmail.com' AND ot.falla_reportada LIKE '%Altavoz%'
LIMIT 1;

INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'IC de carga iPhone 12'
WHERE c.email = 'esteban.maldonado@yahoo.com'
LIMIT 1;

-- Adhesivo para reparaciones de pantalla (múltiples órdenes)
INSERT INTO orden_repuestos (orden_id, repuesto_id, cantidad, precio_unit)
SELECT ot.id, r.id, 1, r.precio
FROM ordenes_trabajo ot
JOIN clientes c ON ot.cliente_id = c.id
JOIN repuestos r ON r.nombre = 'Adhesivo doble faz pantallas'
WHERE ot.estado IN ('ENTREGADO','LISTO','EN_PROCESO')
  AND ot.falla_reportada LIKE '%Pantalla%'
  AND NOT EXISTS (
    SELECT 1 FROM orden_repuestos orep WHERE orep.orden_id = ot.id AND orep.repuesto_id = r.id
  )
LIMIT 10;

-- ─── COBROS ──────────────────────────────────────────────────────
-- Cobros para todas las órdenes ENTREGADO y LISTO

INSERT INTO cobros (orden_id, monto, monto_recibido, vuelto, medio_pago, estado_pago, created_at)
SELECT ot.id, ot.presupuesto,
  CASE WHEN (ot.id % 3) = 0 THEN ot.presupuesto + 1000
       ELSE ot.presupuesto END,
  CASE WHEN (ot.id % 3) = 0 THEN 1000 ELSE 0 END,
  CASE WHEN (ot.id % 3) = 0 THEN 'EFECTIVO'
       WHEN (ot.id % 3) = 1 THEN 'TARJETA'
       ELSE 'MERCADOPAGO' END,
  CASE WHEN ot.estado = 'LISTO' THEN 'PENDIENTE' ELSE 'APROBADO' END,
  ot.updated_at + INTERVAL '1 hour'
FROM ordenes_trabajo ot
WHERE ot.estado IN ('ENTREGADO', 'LISTO')
  AND ot.presupuesto > 0
  AND NOT EXISTS (SELECT 1 FROM cobros co WHERE co.orden_id = ot.id);

-- ─── AJUSTE DE STOCK ──────────────────────────────────────────────
-- Reducimos el stock de los repuestos utilizados
UPDATE repuestos SET stock_actual = GREATEST(0, stock_actual - (
  SELECT COALESCE(SUM(orep.cantidad), 0)
  FROM orden_repuestos orep
  WHERE orep.repuesto_id = repuestos.id
));

COMMIT;

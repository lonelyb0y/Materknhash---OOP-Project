-- Seed data for MySQL / TiDB.
-- Default users are inserted programmatically by DatabaseBootstrap with real bcrypt hashes.

-- ============= SUPPLIERS (10) =============
INSERT IGNORE INTO suppliers(id,name,phone,email,address) VALUES
  ( 1,'Bosch Egypt',           '+20-2-23456789','sales@bosch.eg',     'Cairo, Nasr City'),
  ( 2,'Denso Distributors',    '+20-2-34567890','contact@denso.eg',   'Giza, 6th of October'),
  ( 3,'NGK Spark Plugs Egypt', '+20-2-45678901','info@ngk.eg',        'Alexandria, Smouha'),
  ( 4,'Local Auto Importer',   '+20-1-09876543','importer@local.eg',  'Cairo, Abbasia'),
  ( 5,'Mahle Filters',         '+20-2-56789012','sales@mahle.eg',     'Cairo, New Cairo'),
  ( 6,'Brembo Brakes',         '+20-2-67890123','orders@brembo.eg',   'Cairo, Maadi'),
  ( 7,'Continental Tires',     '+20-3-78901234','eg@continental.com', 'Alexandria, Borg El Arab'),
  ( 8,'Exide Batteries',       '+20-2-89012345','batteries@exide.eg', 'Cairo, 10th of Ramadan'),
  ( 9,'KYB Suspension',        '+20-2-90123456','kyb@local.eg',       'Cairo, Heliopolis'),
  (10,'Valeo Egypt',           '+20-2-11223344','support@valeo.eg',   'Giza, Sheikh Zayed');

-- ============= PARTS (30) =============
INSERT IGNORE INTO parts(id,sku,name,category,car_make,car_model,cost_price,sell_price,quantity,min_qty,supplier_id) VALUES
  ( 1,'SP-001','Spark Plug NGK BKR6E',        'Ignition',    'Toyota',    'Corolla',  45,   75, 120, 20, 3),
  ( 2,'SP-002','Spark Plug Iridium IX',       'Ignition',    'Honda',     'Civic',    95,  150,  85, 15, 3),
  ( 3,'SP-003','Spark Plug Platinum',         'Ignition',    'Nissan',    'Sentra',   70,  115,  60, 15, 3),
  ( 4,'OF-001','Oil Filter Bosch P7',         'Filters',     'Hyundai',   'Elantra',  55,   95,  80, 15, 1),
  ( 5,'OF-002','Oil Filter Mahle OC205',      'Filters',     'Toyota',    'Camry',    60,  105,  45, 10, 5),
  ( 6,'AF-001','Air Filter Denso',            'Filters',     'Nissan',    'Sunny',    70,  120,  60, 15, 2),
  ( 7,'AF-002','Air Filter K&N Sport',        'Filters',     'Honda',     'Accord',  280,  450,  25,  8, 5),
  ( 8,'CF-001','Cabin Filter Activated Carbon','Filters',    'Hyundai',   'Tucson',  130,  220,  50, 12, 5),
  ( 9,'FF-001','Fuel Filter Bosch',           'Filters',     'Mitsubishi','Lancer',   90,  150,  38, 10, 1),
  (10,'BR-001','Brake Pads Front Brembo',     'Brakes',      'Kia',       'Cerato',  220,  360,  40, 10, 6),
  (11,'BR-002','Brake Pads Rear Brembo',      'Brakes',      'Kia',       'Cerato',  180,  300,  35, 10, 6),
  (12,'BR-003','Brake Disc Front Vented',     'Brakes',      'Toyota',    'Corolla', 420,  650,  18,  6, 6),
  (13,'BR-004','Brake Disc Rear Solid',       'Brakes',      'Toyota',    'Corolla', 300,  470,  22,  8, 6),
  (14,'BR-005','Brake Fluid DOT4 1L',         'Brakes',      'Universal', '-',        45,   80, 120, 30, 6),
  (15,'BT-001','Battery 70Ah Exide',          'Electric',    'Chevrolet', 'Aveo',    900, 1300,  15,  5, 8),
  (16,'BT-002','Battery 90Ah Exide',          'Electric',    'Hyundai',   'Sonata', 1150, 1700,  10,  4, 8),
  (17,'BT-003','Battery 60Ah Exide',          'Electric',    'Suzuki',    'Swift',   750, 1100,  18,  5, 8),
  (18,'TM-001','Timing Belt Kit Gates',       'Engine',      'Toyota',    'Corolla', 450,  700,   8,  5, 1),
  (19,'TM-002','Timing Belt Kit + Pump',      'Engine',      'Hyundai',   'Accent',  680, 1050,   5,  4, 1),
  (20,'WP-001','Water Pump Aisin',            'Cooling',     'Hyundai',   'Verna',   380,  560,   6,  5, 1),
  (21,'TH-001','Thermostat 82C',              'Cooling',     'Nissan',    'Tiida',   120,  200,  55, 12, 1),
  (22,'SL-001','Steering Link Outer',         'Steering',    'Nissan',    'Sentra',  180,  270,  25, 10, 2),
  (23,'SL-002','Steering Link Inner',         'Steering',    'Nissan',    'Sentra',  160,  240,  28, 10, 2),
  (24,'SH-001','Shock Absorber Front KYB',    'Suspension',  'Kia',       'Picanto', 310,  480,  18,  8, 9),
  (25,'SH-002','Shock Absorber Rear KYB',     'Suspension',  'Kia',       'Picanto', 280,  430,  20,  8, 9),
  (26,'SH-003','Shock Absorber Front',        'Suspension',  'Toyota',    'Corolla', 340,  510,  14,  6, 9),
  (27,'CL-001','Clutch Kit Valeo',            'Transmission','Chevrolet', 'Optra',   850, 1250,   4,  3, 10),
  (28,'CL-002','Clutch Kit Sachs',            'Transmission','Hyundai',   'Accent',  780, 1180,   6,  3, 10),
  (29,'TR-001','Tire 195/65R15',              'Tires',       'Universal', '-',       950, 1400,  24,  8, 7),
  (30,'TR-002','Tire 205/55R16',              'Tires',       'Universal', '-',      1180, 1700,  20,  8, 7);

-- ============= SAMPLE SALES HISTORY (last 30 days) =============
-- seller_id is set programmatically after default users exist; we hard-code id=3 (default seller)
INSERT IGNORE INTO sales(id,seller_id,total,created_at) VALUES
  ( 1,3,  225.00, DATE_SUB(NOW(), INTERVAL 29 DAY)),
  ( 2,3,  450.00, DATE_SUB(NOW(), INTERVAL 28 DAY)),
  ( 3,3,  720.00, DATE_SUB(NOW(), INTERVAL 27 DAY)),
  ( 4,3, 1300.00, DATE_SUB(NOW(), INTERVAL 26 DAY)),
  ( 5,3,  175.00, DATE_SUB(NOW(), INTERVAL 25 DAY)),
  ( 6,3,  340.00, DATE_SUB(NOW(), INTERVAL 24 DAY)),
  ( 7,3,  580.00, DATE_SUB(NOW(), INTERVAL 23 DAY)),
  ( 8,3, 1700.00, DATE_SUB(NOW(), INTERVAL 22 DAY)),
  ( 9,3,  260.00, DATE_SUB(NOW(), INTERVAL 21 DAY)),
  (10,3,  920.00, DATE_SUB(NOW(), INTERVAL 20 DAY)),
  (11,3,  150.00, DATE_SUB(NOW(), INTERVAL 19 DAY)),
  (12,3,  410.00, DATE_SUB(NOW(), INTERVAL 18 DAY)),
  (13,3, 1100.00, DATE_SUB(NOW(), INTERVAL 17 DAY)),
  (14,3,  650.00, DATE_SUB(NOW(), INTERVAL 16 DAY)),
  (15,3,  300.00, DATE_SUB(NOW(), INTERVAL 15 DAY)),
  (16,3,  470.00, DATE_SUB(NOW(), INTERVAL 14 DAY)),
  (17,3, 1250.00, DATE_SUB(NOW(), INTERVAL 13 DAY)),
  (18,3,  830.00, DATE_SUB(NOW(), INTERVAL 12 DAY)),
  (19,3,  220.00, DATE_SUB(NOW(), INTERVAL 11 DAY)),
  (20,3,  540.00, DATE_SUB(NOW(), INTERVAL 10 DAY)),
  (21,3,  990.00, DATE_SUB(NOW(), INTERVAL  9 DAY)),
  (22,3, 1450.00, DATE_SUB(NOW(), INTERVAL  8 DAY)),
  (23,3,  370.00, DATE_SUB(NOW(), INTERVAL  7 DAY)),
  (24,3,  680.00, DATE_SUB(NOW(), INTERVAL  6 DAY)),
  (25,3,  240.00, DATE_SUB(NOW(), INTERVAL  5 DAY)),
  (26,3,  860.00, DATE_SUB(NOW(), INTERVAL  4 DAY)),
  (27,3, 1100.00, DATE_SUB(NOW(), INTERVAL  3 DAY)),
  (28,3,  550.00, DATE_SUB(NOW(), INTERVAL  2 DAY)),
  (29,3,  720.00, DATE_SUB(NOW(), INTERVAL  1 DAY)),
  (30,3,  390.00, NOW());

INSERT IGNORE INTO sale_items(sale_id,part_id,quantity,unit_price,subtotal) VALUES
  ( 1, 1, 3,   75.00,  225.00),
  ( 2,10, 1,  360.00,  360.00), ( 2,14, 1,   80.00,   80.00),
  ( 3,18, 1,  700.00,  700.00),
  ( 4,15, 1, 1300.00, 1300.00),
  ( 5, 4, 1,   95.00,   95.00), ( 5, 6, 1,  120.00,  120.00),
  ( 6,11, 1,  300.00,  300.00),
  ( 7,12, 1,  650.00,  650.00),
  ( 8,16, 1, 1700.00, 1700.00),
  ( 9, 8, 1,  220.00,  220.00),
  (10,29, 1,  920.00,  920.00),
  (11, 1, 2,   75.00,  150.00),
  (12,22, 1,  270.00,  270.00), (12,23, 1,  240.00,  240.00),
  (13,27, 1, 1250.00, 1250.00),
  (14, 3, 1,  650.00,  650.00),
  (15,11, 1,  300.00,  300.00),
  (16,10, 1,  360.00,  360.00),
  (17,30, 1, 1700.00, 1700.00),
  (18,28, 1,  830.00,  830.00),
  (19, 8, 1,  220.00,  220.00),
  (20, 7, 1,  450.00,  450.00),
  (21,29, 1,  990.00,  990.00),
  (22,30, 1, 1450.00, 1450.00),
  (23, 9, 1,  150.00,  150.00),
  (24, 3, 1,  650.00,  650.00),
  (25, 8, 1,  220.00,  220.00),
  (26,28, 1,  860.00,  860.00),
  (27,27, 1, 1100.00, 1100.00),
  (28,24, 1,  550.00,  550.00),
  (29,18, 1,  700.00,  700.00),
  (30, 6, 1,  120.00,  120.00), (30, 1, 3,   75.00,  225.00);

-- ============= SAMPLE PURCHASES =============
INSERT IGNORE INTO purchases(id,supplier_id,supplier_name,user_id,total,created_at) VALUES
  (1, 1,'Bosch Egypt',           1, 12000.00, DATE_SUB(NOW(), INTERVAL 15 DAY)),
  (2, 3,'NGK Spark Plugs Egypt', 1,  4500.00, DATE_SUB(NOW(), INTERVAL 12 DAY)),
  (3, 6,'Brembo Brakes',         1,  8800.00, DATE_SUB(NOW(), INTERVAL  8 DAY)),
  (4, 8,'Exide Batteries',       1, 18000.00, DATE_SUB(NOW(), INTERVAL  4 DAY));

INSERT IGNORE INTO purchase_items(purchase_id,part_id,quantity,unit_cost,subtotal) VALUES
  (1, 4,30,  55.00, 1650.00), (1, 9,20,  90.00, 1800.00), (1,18,10, 450.00, 4500.00), (1,20, 6, 380.00, 2280.00),
  (2, 1,40,  45.00, 1800.00), (2, 2,15,  95.00, 1425.00), (2, 3,15,  70.00, 1050.00),
  (3,10,15, 220.00, 3300.00), (3,12, 6, 420.00, 2520.00), (3,14,40,  45.00, 1800.00),
  (4,15, 8, 900.00, 7200.00), (4,16, 6,1150.00, 6900.00), (4,17, 5, 750.00, 3750.00);

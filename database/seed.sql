-- Seed data (suppliers + parts).
-- Default users are inserted programmatically by DatabaseBootstrap
-- using real BCrypt hashes for: admin/admin123, employee/emp123, seller/sell123

INSERT OR IGNORE INTO suppliers(id,name,phone,email,address) VALUES
  (1,'Bosch Egypt',     '+20-2-1111111','sales@bosch.eg',     'Cairo, Nasr City'),
  (2,'Denso Distrib.',  '+20-2-2222222','contact@denso.eg',   'Giza, 6 October'),
  (3,'NGK Parts',       '+20-2-3333333','info@ngk.eg',        'Alexandria'),
  (4,'Local Importer',  '+20-1-4444444','importer@local.eg',  'Cairo, Abbasia');

INSERT OR IGNORE INTO parts(sku,name,category,car_make,car_model,cost_price,sell_price,quantity,min_qty,supplier_id) VALUES
  ('SP-001','Spark Plug NGK BKR6E', 'Ignition','Toyota','Corolla',     45,  75,  120, 20, 3),
  ('OF-002','Oil Filter Bosch P7',  'Filters', 'Hyundai','Elantra',    55,  95,   80, 15, 1),
  ('AF-003','Air Filter Denso',     'Filters', 'Nissan','Sunny',       70, 120,   60, 15, 2),
  ('BR-004','Brake Pads Front',     'Brakes',  'Kia','Cerato',        220, 360,   40, 10, 4),
  ('BT-005','Battery 70Ah',         'Electric','Chevrolet','Aveo',    900,1300,   15,  5, 4),
  ('TM-006','Timing Belt Kit',      'Engine',  'Toyota','Corolla',    450, 700,    8,  5, 1),
  ('WP-007','Water Pump',           'Cooling', 'Hyundai','Verna',     380, 560,    6,  5, 1),
  ('SL-008','Steering Link',        'Steering','Nissan','Sentra',     180, 270,   25, 10, 2),
  ('SH-009','Shock Absorber Rear',  'Suspension','Kia','Picanto',     310, 480,   18,  8, 4),
  ('CL-010','Clutch Kit',           'Transmission','Chevrolet','Optra',850,1250,   4,  3, 2);

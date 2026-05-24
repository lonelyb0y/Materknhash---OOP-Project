package com.matraknhash.tools;

import com.matraknhash.db.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


public final class DbDemoSeed {

    public static void main(String[] args) throws SQLException {
        try (Connection c = ConnectionFactory.get();
             Statement st = c.createStatement()) {

            // -------- suppliers --------
            st.executeUpdate("INSERT IGNORE INTO suppliers(id,name,phone,email,address,trusted) VALUES " +
                    "(1,'Mido''s Spare Parts Wholesale','+20-100-1112233','mido@parts.eg','Cairo, Nasr City',1)," +
                    "(2,'El-Tagamoa Auto Parts',       '+20-100-2223344','sales@tagamoa.eg','New Cairo',1)," +
                    "(3,'Brake Brothers Egypt',        '+20-100-3334455','hello@brakebros.eg','Giza, Dokki',0)");

            // -------- parts (10) -- all owned by the default seller (id=3) --------
            st.executeUpdate("INSERT IGNORE INTO parts" +
                    "(id,sku,name,category,car_make,car_model,cost_price,sell_price,quantity,min_qty,supplier_id,seller_id,listing_status) VALUES " +
                    "( 1,'SP-LOL','Spark Plug \"Stage 3\" (definitely OEM trust me)','Ignition','Toyota','Corolla',50,90,30,10,1,3,'LIVE')," +
                    "( 2,'OF-MID','Oil Filter Mido Premium',                          'Filters', 'Hyundai','Elantra',60,110,40,12,1,3,'LIVE')," +
                    "( 3,'AF-GFY','Air Filter Sport (goofy edition)',                 'Filters', 'Honda','Civic',80,150,25,8,1,3,'LIVE')," +
                    "( 4,'CF-BNZ','Cabin Filter Anti-Bonzai',                         'Filters', 'Kia','Cerato',120,210,20,5,2,3,'LIVE')," +
                    "( 5,'BR-FRT','Brake Pads Front - Brake Bros Combat',             'Brakes',  'Toyota','Corolla',230,380,18,6,3,3,'LIVE')," +
                    "( 6,'BR-RR', 'Brake Pads Rear - Brake Bros Lite',                'Brakes',  'Toyota','Corolla',190,310,18,6,3,3,'LIVE')," +
                    "( 7,'BT-70', 'Battery 70Ah \"Wakes the neighbours\"',            'Electric','Hyundai','Sonata',900,1350,8,3,2,3,'LIVE')," +
                    "( 8,'TM-VR', 'Timing Belt Kit (do not skip)',                    'Engine',  'Hyundai','Accent',650,1000,5,3,2,3,'LIVE')," +
                    "( 9,'SH-FR', 'Shock Absorber Front - Smooth Operator',           'Suspension','Kia','Picanto',310,490,12,4,3,3,'LIVE')," +
                    "(10,'TR-15', 'Tire 195/65R15 - Round, Black, Rubbery',           'Tires',   'Universal','-',950,1450,16,6,1,3,'LIVE')");

            // Counters land just past our seeded ids.
            st.execute("ALTER TABLE suppliers AUTO_INCREMENT = 4");
            st.execute("ALTER TABLE parts     AUTO_INCREMENT = 11");

            System.out.println("[demo-seed] inserted 3 suppliers + 10 parts.");
        }
        ConnectionFactory.shutdown();
    }
}

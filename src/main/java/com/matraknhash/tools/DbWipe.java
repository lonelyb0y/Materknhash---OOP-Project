package com.matraknhash.tools;

import com.matraknhash.db.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * One-shot utility: wipes all transactional/seed data and removes a named
 * problem account. Schema and the three default users (admin/employee/seller)
 * survive so the app still boots cleanly.
 *
 * Usage:
 *   mvn -q compile exec:java \
 *       -Dexec.mainClass=com.matraknhash.tools.DbWipe \
 *       -Dexec.classpathScope=runtime
 */
public final class DbWipe {

    public static void main(String[] args) throws SQLException {
        String[] statements = {
                // Service feature
                "DELETE FROM service_requests",
                "DELETE FROM service_offers",
                // Marketplace + POS history
                "DELETE FROM sale_items",
                "DELETE FROM sales",
                "DELETE FROM purchase_items",
                "DELETE FROM purchases",
                // Notifications + listings (parts is also "listings" in M3+)
                "DELETE FROM notifications",
                "DELETE FROM parts",
                "DELETE FROM suppliers",
                // Drop the 3bkr account specifically + any non-default users
                "DELETE FROM users WHERE username = '3bkr'",
                // Reset counters so new ids start from 1
                "ALTER TABLE sales            AUTO_INCREMENT = 1",
                "ALTER TABLE sale_items       AUTO_INCREMENT = 1",
                "ALTER TABLE purchases        AUTO_INCREMENT = 1",
                "ALTER TABLE purchase_items   AUTO_INCREMENT = 1",
                "ALTER TABLE parts            AUTO_INCREMENT = 1",
                "ALTER TABLE suppliers        AUTO_INCREMENT = 1",
                "ALTER TABLE service_offers   AUTO_INCREMENT = 1",
                "ALTER TABLE service_requests AUTO_INCREMENT = 1",
                "ALTER TABLE notifications    AUTO_INCREMENT = 1",
        };

        try (Connection c = ConnectionFactory.get();
             Statement st = c.createStatement()) {
            // Disable FK checks so the deletes can run in any order without juggling.
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String sql : statements) {
                System.out.println("[wipe] " + sql);
                try { st.executeUpdate(sql); }
                catch (SQLException e) { System.err.println("        skipped: " + e.getMessage()); }
            }
            st.execute("SET FOREIGN_KEY_CHECKS = 1");
            System.out.println("[wipe] done.");
        }
        // Shut the Hikari pool down so the JVM can exit promptly.
        ConnectionFactory.shutdown();
    }
}

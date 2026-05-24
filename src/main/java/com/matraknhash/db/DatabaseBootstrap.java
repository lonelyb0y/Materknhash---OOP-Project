package com.matraknhash.db;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Runs once at application startup:
 *   1. Apply schema.sql (CREATE TABLE IF NOT EXISTS).
 *   2. Insert default users (admin/employee/seller) with real bcrypt hashes.
 *   3. Apply seed.sql (suppliers, parts, historical sales/purchases).
 *
 * The user step runs BEFORE the seed step because the sample sales rows
 * reference users(id) via foreign key.
 */
public final class DatabaseBootstrap {

    private static final Path SCHEMA = Path.of("database", "schema.sql");
    private static final Path SEED   = Path.of("database", "seed.sql");

    private DatabaseBootstrap() {}

    public static void run() throws SQLException, IOException {
        ensureDatabase();
        try (Connection c = ConnectionFactory.get()) {
            if (!isSchemaAlreadyApplied(c)) {
                System.out.println("[DatabaseBootstrap] Initializing database schema...");
                applySql(c, SCHEMA);
                ensureDefaultUsers(c);
                // Demo data is opt-in: run once with -Dmatraknhash.seed=on for a
                // fresh demo machine, then leave it off so the operator's own data
                // is never overwritten.
                if ("on".equalsIgnoreCase(System.getProperty("matraknhash.seed", "off"))) {
                    applySql(c, SEED);
                }
            } else {
                System.out.println("[DatabaseBootstrap] Database schema already applied. Skipping initialization.");
            }
            // Always clean up default placeholder seed data (IDs 1-30) to provide a fresh merchant experience
            cleanupDefaultSeedData(c);
        }
    }

    private static boolean isSchemaAlreadyApplied(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = 'users' LIMIT 1")) {
            ps.setString(1, ConnectionFactory.dbName());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1 FROM users LIMIT 1")) {
                return true;
            } catch (SQLException ex) {
                return false;
            }
        }
    }

    /** Make sure the target database exists on the server before we connect to it. */
    private static void ensureDatabase() throws SQLException {
        try (Connection c = ConnectionFactory.serverOnly();
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + ConnectionFactory.dbName()
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private static void applySql(Connection c, Path file) throws SQLException, IOException {
        if (!Files.exists(file)) return;
        String sql = stripLineComments(Files.readString(file, StandardCharsets.UTF_8));
        try (Statement st = c.createStatement()) {
            for (String stmt : sql.split(";")) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    st.execute(trimmed);
                } catch (SQLException e) {
                    if (isAlreadyExists(e, trimmed)) continue; // re-run: index/constraint already there
                    throw e;
                }
            }
        }
    }

    /**
     * True for idempotent-safe errors on schema-evolution statements (CREATE INDEX,
     * ALTER TABLE ADD/DROP COLUMN/CONSTRAINT/CHECK) when the migration block runs
     * a second time. These should be ignored so the bootstrap is a no-op on warm DBs.
     */
    private static boolean isAlreadyExists(SQLException e, String stmt) {
        String m = String.valueOf(e.getMessage()).toLowerCase();
        String s = stmt.toLowerCase();

        if (s.startsWith("create index") || s.startsWith("create unique index")) {
            return m.contains("duplicate key name") || m.contains("already exists");
        }
        if (s.startsWith("alter table")) {
            // ADD COLUMN re-run
            if (m.contains("duplicate column")) return true;
            // ADD CONSTRAINT / ADD FOREIGN KEY re-run
            if (m.contains("duplicate foreign key") || m.contains("already exists")
                    || m.contains("duplicate key on write or update")) return true;
            // DROP CHECK / DROP CONSTRAINT when it was never there (fresh DB) or already removed
            if (m.contains("check constraint") && m.contains("does not exist")) return true;
            if (m.contains("doesn't exist") || m.contains("doesn t exist")
                    || m.contains("check") && m.contains("not found")) return true;
        }
        return false;
    }

    /** Strip SQL line-comments so our naive split-on-';' doesn't mis-classify chunks. */
    private static String stripLineComments(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        for (String line : sql.split("\\R")) {
            int idx = line.indexOf("--");
            out.append(idx >= 0 ? line.substring(0, idx) : line).append('\n');
        }
        return out.toString();
    }

    private static void ensureDefaultUsers(Connection c) throws SQLException {
        // Force the seeded ids so FK references in seed.sql line up across machines.
        ensureUser(c, 1, "admin",    "admin123", "System Administrator", "ADMIN");
        ensureUser(c, 2, "employee", "emp123",   "Default Employee",     "EMPLOYEE");
        ensureUser(c, 3, "seller",   "sell123",  "Default Seller",       "SELLER");
    }

    private static void ensureUser(Connection c, int id, String user, String pwd, String fullName, String role) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE id = ? OR username = ?")) {
            ps.setInt(1, id);
            ps.setString(2, user);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return;
            }
        }
        String hash = BCrypt.withDefaults().hashToString(10, pwd.toCharArray());
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users(id,username,password_hash,full_name,role) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, id);
            ps.setString(2, user);
            ps.setString(3, hash);
            ps.setString(4, fullName);
            ps.setString(5, role);
            ps.executeUpdate();
        }
    }

    /** Clean up the 30 default placeholder parts, sales, and purchases to ensure a completely clean system. */
    private static void cleanupDefaultSeedData(Connection c) {
        System.out.println("[DatabaseBootstrap] Wiping default placeholder seed data...");
        try (Statement st = c.createStatement()) {
            // Safe condition targeting only the default seeded parts by SKU format, default seller (id=3), and ID <= 30.
            // This prevents deleting newly registered merchant parts if they happen to have IDs <= 30 on clean databases.
            String partCondition = "id <= 30 AND seller_id = 3 AND (sku LIKE 'SP-%' OR sku LIKE 'OF-%' OR sku LIKE 'AF-%' OR sku LIKE 'CF-%' OR sku LIKE 'FF-%' OR sku LIKE 'BR-%' OR sku LIKE 'BT-%' OR sku LIKE 'TM-%' OR sku LIKE 'WP-%' OR sku LIKE 'TH-%' OR sku LIKE 'SL-%' OR sku LIKE 'SH-%' OR sku LIKE 'CL-%' OR sku LIKE 'TR-%')";
            
            // Delete referencing items first
            st.executeUpdate("DELETE FROM sale_items WHERE part_id <= 30 AND part_id IN (SELECT id FROM parts WHERE " + partCondition + ")");
            st.executeUpdate("DELETE FROM purchase_items WHERE part_id <= 30 AND part_id IN (SELECT id FROM parts WHERE " + partCondition + ")");
            st.executeUpdate("DELETE FROM parts WHERE " + partCondition);
            
            // Wiping seeded sales/purchases history with IDs <= 30
            st.executeUpdate("DELETE FROM sales WHERE id <= 30 AND seller_id = 3");
            st.executeUpdate("DELETE FROM purchases WHERE id <= 30 AND supplier_id <= 10");
            
            // Auto-migrate any listings stuck in PENDING_ADMIN from the old workflow to LIVE
            int migrated = st.executeUpdate("UPDATE parts SET listing_status = 'LIVE' WHERE listing_status = 'PENDING_ADMIN'");
            if (migrated > 0) {
                System.out.println("[DatabaseBootstrap] Auto-migrated " + migrated + " stuck PENDING_ADMIN listings to LIVE!");
            }
            
            System.out.println("[DatabaseBootstrap] Default placeholder seed data successfully removed!");
        } catch (SQLException e) {
            System.err.println("[DatabaseBootstrap] Error cleaning up placeholder seed data: " + e.getMessage());
        }
    }
}

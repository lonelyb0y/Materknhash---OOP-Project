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
        Connection c = ConnectionFactory.get();
        applySql(c, SCHEMA);
        ensureDefaultUsers(c);
        applySql(c, SEED);
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

    /** True for idempotent-safe "already exists" errors on CREATE INDEX / INSERT / CREATE TABLE re-runs. */
    private static boolean isAlreadyExists(SQLException e, String stmt) {
        String m = String.valueOf(e.getMessage()).toLowerCase();
        String s = stmt.toLowerCase();
        return (s.startsWith("create index") || s.startsWith("create unique index"))
                && (m.contains("duplicate key name") || m.contains("already exists"));
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
}

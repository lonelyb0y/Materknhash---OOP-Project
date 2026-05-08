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
 * Runs once at application startup to:
 *   1. Apply schema.sql if tables don't exist.
 *   2. Apply seed.sql for suppliers/parts.
 *   3. Insert default users (admin/employee/seller) with real BCrypt hashes.
 */
public final class DatabaseBootstrap {

    private static final Path SCHEMA = Path.of("database", "schema.sql");
    private static final Path SEED   = Path.of("database", "seed.sql");

    private DatabaseBootstrap() {}

    public static void run() throws SQLException, IOException {
        Connection c = ConnectionFactory.get();
        applySql(c, SCHEMA);
        applySql(c, SEED);
        ensureDefaultUsers(c);
    }

    private static void applySql(Connection c, Path file) throws SQLException, IOException {
        if (!Files.exists(file)) return;
        String sql = Files.readString(file, StandardCharsets.UTF_8);
        try (Statement st = c.createStatement()) {
            for (String stmt : sql.split(";")) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                st.execute(trimmed);
            }
        }
    }

    private static void ensureDefaultUsers(Connection c) throws SQLException {
        if (userExists(c, "admin")    == false) insertUser(c, "admin",    "admin123", "System Administrator", "ADMIN");
        if (userExists(c, "employee") == false) insertUser(c, "employee", "emp123",   "Default Employee",     "EMPLOYEE");
        if (userExists(c, "seller")   == false) insertUser(c, "seller",   "sell123",  "Default Seller",       "SELLER");
    }

    private static boolean userExists(Connection c, String username) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void insertUser(Connection c, String user, String pwd, String fullName, String role) throws SQLException {
        String hash = BCrypt.withDefaults().hashToString(10, pwd.toCharArray());
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users(username,password_hash,full_name,role) VALUES (?,?,?,?)")) {
            ps.setString(1, user);
            ps.setString(2, hash);
            ps.setString(3, fullName);
            ps.setString(4, role);
            ps.executeUpdate();
        }
    }
}

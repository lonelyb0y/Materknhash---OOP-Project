package com.matraknhash.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single point of access to the SQLite database connection.
 * Uses a single shared connection because SQLite is file-based
 * and JavaFX is single-process. Foreign keys are enabled on
 * every new connection.
 */
public final class ConnectionFactory {

    private static final String DB_PATH =
            System.getProperty("matraknhash.db", "database/matraknhash.db");
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    private static Connection shared;

    private ConnectionFactory() {}

    public static synchronized Connection get() throws SQLException {
        if (shared == null || shared.isClosed()) {
            shared = DriverManager.getConnection(URL);
            try (var st = shared.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return shared;
    }

    public static String dbPath() { return DB_PATH; }
}

package com.matraknhash.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Single point of access to the database connection.
 * Reads /application.properties from the classpath so the team can
 * point every PC at the same hosted MySQL/TiDB instance without
 * recompiling the app.
 */
public final class ConnectionFactory {

    private static Connection shared;
    private static String jdbcUrl;
    private static String jdbcUrlNoDb;
    private static String dbName;
    private static String user;
    private static String password;

    static {
        Properties p = loadProps();
        String host = sysOrProp(p, "db.host");
        String port = sysOrProp(p, "db.port");
        dbName      = sysOrProp(p, "db.name");
        user        = sysOrProp(p, "db.user");
        password    = sysOrProp(p, "db.password");
        boolean tls = Boolean.parseBoolean(sysOrProp(p, "db.tls"));

        String suffix = "?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8"
                + (tls
                    ? "&sslMode=REQUIRED&enabledTLSProtocols=TLSv1.2,TLSv1.3"
                    : "&useSSL=false&allowPublicKeyRetrieval=true");

        String base = "jdbc:mysql://" + host + ":" + port + "/";
        jdbcUrl     = base + dbName + suffix;
        jdbcUrlNoDb = base + suffix;
    }

    /** Connects WITHOUT a target database — used once at bootstrap to CREATE DATABASE. */
    public static Connection serverOnly() throws SQLException {
        return DriverManager.getConnection(jdbcUrlNoDb, user, password);
    }

    public static String dbName() { return dbName; }

    private ConnectionFactory() {}

    public static synchronized Connection get() throws SQLException {
        if (shared == null || shared.isClosed()) {
            shared = DriverManager.getConnection(jdbcUrl, user, password);
        }
        return shared;
    }

    public static String dbPath() { return jdbcUrl; }

    /**
     * Returns a FRESH dedicated connection. Caller MUST close it (try-with-resources).
     * Use this for any code path that runs a multi-statement transaction (setAutoCommit(false))
     * or that runs on a worker thread; the shared {@link #get()} connection is not safe to share
     * across threads when transactions are in flight.
     */
    public static Connection borrow() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    // ---------------- helpers ----------------

    private static Properties loadProps() {
        Properties p = new Properties();
        try (InputStream in = ConnectionFactory.class.getResourceAsStream("/application.properties")) {
            if (in != null) p.load(in);
        } catch (IOException e) {
            System.err.println("[ConnectionFactory] Could not read application.properties: " + e.getMessage());
        }
        return p;
    }

    /** -Ddb.user=... on the command line wins over the file, so CI / per-machine overrides work. */
    private static String sysOrProp(Properties p, String key) {
        String v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v;
        v = p.getProperty(key);
        return v == null ? "" : v;
    }
}

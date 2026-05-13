package com.matraknhash.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Single point of access to database connections, backed by a HikariCP pool.
 *
 * <p>Why a pool? Railway is hosted ~hundreds of ms away and every fresh JDBC
 * connection re-runs the full TLS handshake. With the pool, every UI action
 * grabs a pre-warmed connection in &lt;1ms and the app feels snappy.
 *
 * <p>Both {@link #get()} and {@link #borrow()} now return a pooled connection
 * \u2014 callers MUST close them (try-with-resources). The pool reclaims the
 * physical connection instead of tearing it down.
 */
public final class ConnectionFactory {

    private static HikariDataSource pool;
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
                + "&cachePrepStmts=true&prepStmtCacheSize=256&prepStmtCacheSqlLimit=2048"
                + "&useServerPrepStmts=true&rewriteBatchedStatements=true"
                + (tls
                    ? "&sslMode=REQUIRED&enabledTLSProtocols=TLSv1.2,TLSv1.3"
                    : "&useSSL=false&allowPublicKeyRetrieval=true");

        String base = "jdbc:mysql://" + host + ":" + port + "/";
        jdbcUrl     = base + dbName + suffix;
        jdbcUrlNoDb = base + suffix;
    }

    /** Connects WITHOUT a target database \u2014 used once at bootstrap to CREATE DATABASE. */
    public static Connection serverOnly() throws SQLException {
        return DriverManager.getConnection(jdbcUrlNoDb, user, password);
    }

    public static String dbName() { return dbName; }
    public static String dbPath() { return jdbcUrl; }

    private ConnectionFactory() {}

    /** Initialise the pool lazily; safe to call from multiple threads. */
    private static synchronized HikariDataSource pool() {
        if (pool == null) {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(jdbcUrl);
            cfg.setUsername(user);
            cfg.setPassword(password);
            cfg.setPoolName("MatraknhashPool");
            cfg.setMaximumPoolSize(10);          // plenty for a desktop app
            cfg.setMinimumIdle(2);
            cfg.setConnectionTimeout(8_000);     // fail fast if Railway is dead
            cfg.setIdleTimeout(60_000);
            cfg.setMaxLifetime(25 * 60_000);     // recycle before MySQL kills idle conns
            cfg.setKeepaliveTime(2 * 60_000);    // ping every 2 min so pool stays warm
            cfg.setAutoCommit(true);             // transactional callers flip this off
            pool = new HikariDataSource(cfg);
        }
        return pool;
    }

    /**
     * Returns a pooled connection. Caller MUST close it (try-with-resources) so the
     * pool can reclaim it. Thread-safe.
     */
    public static Connection get() throws SQLException {
        return pool().getConnection();
    }

    /** Alias for {@link #get()}; kept for readability in transactional call sites. */
    public static Connection borrow() throws SQLException {
        return pool().getConnection();
    }

    /** Closes the pool. Called at app shutdown so the JVM doesn't hang on Hikari threads. */
    public static synchronized void shutdown() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
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

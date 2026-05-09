package com.matraknhash.dao;

import com.matraknhash.db.ConnectionFactory;
import com.matraknhash.model.Sale;
import com.matraknhash.model.SaleItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SaleDao isn't a CRUD-on-single-table DAO so it doesn't extend BaseDao.
 * It owns the multi-table transaction that creates a sale + items and
 * decrements stock atomically.
 */
public class SaleDao {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Connection c() {
        try { return ConnectionFactory.get(); }
        catch (SQLException e) { throw new DaoException("conn failed", e); }
    }

    /**
     * Persist a sale atomically: insert sale, insert items, decrement stock.
     * Throws DaoException with a friendly message on insufficient stock.
     */
    public Sale create(Sale sale) {
        Connection c = c();
        try {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO sales(seller_id,total) VALUES (?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, sale.getSellerId());
                ps.setDouble(2, sale.getTotal());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) sale.setId(keys.getInt(1));
                }
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO sale_items(sale_id,part_id,quantity,unit_price) VALUES (?,?,?,?)");
                 PreparedStatement dec = c.prepareStatement(
                    "UPDATE parts SET quantity = quantity - ? WHERE id = ? AND quantity >= ?")) {

                for (SaleItem it : sale.getItems()) {
                    dec.setInt(1, it.getQuantity());
                    dec.setInt(2, it.getPartId());
                    dec.setInt(3, it.getQuantity());
                    if (dec.executeUpdate() == 0) {
                        throw new DaoException("Not enough stock for part " + it.getPartName());
                    }

                    ps.setInt(1, sale.getId());
                    ps.setInt(2, it.getPartId());
                    ps.setInt(3, it.getQuantity());
                    ps.setDouble(4, it.getUnitPrice());
                    ps.executeUpdate();
                }
            }
            c.commit();
            return sale;
        } catch (SQLException | DaoException e) {
            try { c.rollback(); } catch (SQLException ignore) {}
            if (e instanceof DaoException de) throw de;
            throw new DaoException("create sale failed", e);
        } finally {
            try { c.setAutoCommit(true); } catch (SQLException ignore) {}
        }
    }

    public double totalSalesSince(LocalDateTime since) {
        String sql = "SELECT COALESCE(SUM(total),0) FROM sales WHERE created_at >= ?";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, since.format(TS));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("totalSalesSince failed", e);
        }
    }

    public double totalProfitSince(LocalDateTime since) {
        String sql = "SELECT COALESCE(SUM((si.unit_price - p.cost_price) * si.quantity),0) " +
                     "FROM sale_items si JOIN parts p ON p.id = si.part_id " +
                     "JOIN sales s ON s.id = si.sale_id WHERE s.created_at >= ?";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, since.format(TS));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("totalProfitSince failed", e);
        }
    }

    /** Daily sales totals for the last N days, ordered ascending by day. */
    public Map<String, Double> dailyTotals(int days) {
        String sql = "SELECT date(created_at) d, SUM(total) t FROM sales " +
                     "WHERE created_at >= date('now', ?) GROUP BY d ORDER BY d";
        Map<String, Double> out = new LinkedHashMap<>();
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, "-" + days + " day");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString("d"), rs.getDouble("t"));
            }
        } catch (SQLException e) {
            throw new DaoException("dailyTotals failed", e);
        }
        return out;
    }

    /** Top selling parts by total quantity sold. */
    public List<Object[]> topSelling(int limit) {
        String sql = "SELECT p.name, SUM(si.quantity) q FROM sale_items si " +
                     "JOIN parts p ON p.id = si.part_id GROUP BY si.part_id ORDER BY q DESC LIMIT ?";
        List<Object[]> out = new ArrayList<>();
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Object[]{rs.getString(1), rs.getInt(2)});
            }
        } catch (SQLException e) {
            throw new DaoException("topSelling failed", e);
        }
        return out;
    }

    public int countAll() {
        try (PreparedStatement ps = c().prepareStatement("SELECT COUNT(*) FROM sales");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { throw new DaoException("countAll sales failed", e); }
    }
}

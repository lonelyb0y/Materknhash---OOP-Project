package com.matraknhash.dao;

import com.matraknhash.model.Part;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PartDao extends BaseDao<Part> {

    @Override protected String table() { return "parts"; }
    @Override protected String[] columns() {
        return new String[]{"sku","name","category","car_make","car_model",
                "cost_price","sell_price","quantity","min_qty","supplier_id"};
    }

    @Override protected Part extract(ResultSet rs) throws SQLException {
        Part p = new Part();
        p.setId(rs.getInt("id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setCategory(rs.getString("category"));
        p.setCarMake(rs.getString("car_make"));
        p.setCarModel(rs.getString("car_model"));
        p.setCostPrice(rs.getDouble("cost_price"));
        p.setSellPrice(rs.getDouble("sell_price"));
        p.setQuantity(rs.getInt("quantity"));
        p.setMinQty(rs.getInt("min_qty"));
        int sid = rs.getInt("supplier_id");
        p.setSupplierId(rs.wasNull() ? null : sid);
        return p;
    }

    @Override protected void bindInsert(PreparedStatement ps, Part p) throws SQLException {
        ps.setString(1, p.getSku());
        ps.setString(2, p.getName());
        ps.setString(3, p.getCategory());
        ps.setString(4, p.getCarMake());
        ps.setString(5, p.getCarModel());
        ps.setDouble(6, p.getCostPrice());
        ps.setDouble(7, p.getSellPrice());
        ps.setInt(8, p.getQuantity());
        ps.setInt(9, p.getMinQty());
        if (p.getSupplierId() == null) ps.setNull(10, java.sql.Types.INTEGER);
        else ps.setInt(10, p.getSupplierId());
    }

    @Override protected void bindUpdate(PreparedStatement ps, Part p) throws SQLException {
        bindInsert(ps, p);
        ps.setInt(11, p.getId());
    }

    @Override protected int idOf(Part p) { return p.getId(); }
    @Override protected void setId(Part p, int id) { p.setId(id); }

    public List<Part> findLowStock() {
        String sql = "SELECT * FROM parts WHERE quantity <= min_qty ORDER BY quantity ASC";
        List<Part> out = new ArrayList<>();
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(extract(rs));
        } catch (SQLException e) {
            throw new DaoException("findLowStock failed", e);
        }
        return out;
    }

    public List<Part> search(String term) {
        String sql = "SELECT * FROM parts WHERE sku LIKE ? OR name LIKE ? ORDER BY name";
        List<Part> out = new ArrayList<>();
        String like = "%" + term + "%";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(extract(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("search failed", e);
        }
        return out;
    }

    public int countAll() {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM parts");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DaoException("countAll failed", e);
        }
    }

    /** Atomically decrement stock. Returns true if stock was sufficient. */
    public boolean decrementStock(int partId, int qty) {
        String sql = "UPDATE parts SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, partId);
            ps.setInt(3, qty);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("decrementStock failed", e);
        }
    }

    public void incrementStock(int partId, int qty) {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(
                "UPDATE parts SET quantity = quantity + ? WHERE id = ?")) {
            ps.setInt(1, qty);
            ps.setInt(2, partId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("incrementStock failed", e);
        }
    }
}

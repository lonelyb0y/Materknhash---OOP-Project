package com.matraknhash.dao;

import com.matraknhash.db.ConnectionFactory;
import com.matraknhash.model.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO for the reviews table. */
public class ReviewDao {

    private Review extract(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getInt("id"));
        r.setSaleId(rs.getInt("sale_id"));
        r.setCustomerId(rs.getInt("customer_id"));
        r.setSellerId(rs.getInt("seller_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null) r.setCreatedAt(t.toLocalDateTime());
        try { r.setCustomerName(rs.getString("customer_name")); } catch (SQLException ignore) {}
        try { r.setSellerName(rs.getString("seller_name")); } catch (SQLException ignore) {}
        return r;
    }

    public Review create(Review r) {
        String sql = "INSERT INTO reviews(sale_id,customer_id,seller_id,rating,comment) VALUES (?,?,?,?,?)";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getSaleId());
            ps.setInt(2, r.getCustomerId());
            ps.setInt(3, r.getSellerId());
            ps.setInt(4, r.getRating());
            ps.setString(5, r.getComment());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) r.setId(k.getInt(1));
            }
            return r;
        } catch (SQLException e) {
            throw new DaoException("create review failed", e);
        }
    }

    public Review findBySale(int saleId) {
        String sql = "SELECT r.*, cu.full_name AS customer_name, su.full_name AS seller_name " +
                     "FROM reviews r JOIN users cu ON cu.id = r.customer_id " +
                     "JOIN users su ON su.id = r.seller_id WHERE r.sale_id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? extract(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("findBySale failed", e);
        }
    }

    public List<Review> findBySeller(int sellerId) {
        String sql = "SELECT r.*, cu.full_name AS customer_name, su.full_name AS seller_name " +
                     "FROM reviews r JOIN users cu ON cu.id = r.customer_id " +
                     "JOIN users su ON su.id = r.seller_id " +
                     "WHERE r.seller_id = ? ORDER BY r.created_at DESC";
        List<Review> out = new ArrayList<>();
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(extract(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("findBySeller failed", e);
        }
        return out;
    }

    public double averageRating(int sellerId) {
        String sql = "SELECT COALESCE(AVG(rating), 0) FROM reviews WHERE seller_id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("averageRating failed", e);
        }
    }

    public int countBySeller(int sellerId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE seller_id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("countBySeller failed", e);
        }
    }
}

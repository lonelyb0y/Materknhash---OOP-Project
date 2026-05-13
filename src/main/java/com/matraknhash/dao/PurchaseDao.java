package com.matraknhash.dao;

import com.matraknhash.db.ConnectionFactory;
import com.matraknhash.model.Purchase;
import com.matraknhash.model.PurchaseItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PurchaseDao {

    private Connection c() {
        try { return ConnectionFactory.get(); }
        catch (SQLException e) { throw new DaoException("conn failed", e); }
    }

    public Purchase create(Purchase pur) {
        try (Connection c = ConnectionFactory.borrow()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO purchases(supplier_id,supplier_name,user_id,total) VALUES (?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, pur.getSupplierId());
                    ps.setString(2, pur.getSupplierName() == null ? "" : pur.getSupplierName());
                    ps.setInt(3, pur.getUserId());
                    ps.setDouble(4, pur.getTotal());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) pur.setId(keys.getInt(1));
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO purchase_items(purchase_id,part_id,quantity,unit_cost,subtotal) VALUES (?,?,?,?,?)");
                     PreparedStatement inc = c.prepareStatement(
                        "UPDATE parts SET quantity = quantity + ? WHERE id = ?")) {

                    for (PurchaseItem it : pur.getItems()) {
                        inc.setInt(1, it.getQuantity());
                        inc.setInt(2, it.getPartId());
                        inc.executeUpdate();

                        double subtotal = it.getUnitCost() * it.getQuantity();
                        ps.setInt(1, pur.getId());
                        ps.setInt(2, it.getPartId());
                        ps.setInt(3, it.getQuantity());
                        ps.setDouble(4, it.getUnitCost());
                        ps.setDouble(5, subtotal);
                        ps.executeUpdate();
                    }
                }
                c.commit();
                return pur;
            } catch (SQLException e) {
                try { c.rollback(); } catch (SQLException ignore) {}
                throw new DaoException("create purchase failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new DaoException("create purchase failed: " + e.getMessage(), e);
        }
    }

    public double totalPurchasesAllTime() {
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(total),0) FROM purchases");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) {
            throw new DaoException("totalPurchases failed", e);
        }
    }
}

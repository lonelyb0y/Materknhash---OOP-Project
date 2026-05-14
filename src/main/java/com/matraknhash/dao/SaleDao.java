package com.matraknhash.dao;

import com.matraknhash.db.ConnectionFactory;
import com.matraknhash.model.Sale;
import com.matraknhash.model.SaleItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
     * Persist a sale atomically as PENDING. Stock is NOT touched here — the approval
     * step decrements stock once an Admin/Employee approves the invoice.
     */
    public Sale create(Sale sale) {
        try (Connection c = ConnectionFactory.borrow()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO sales(seller_id,total,status) VALUES (?,?, 'PENDING')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, sale.getSellerId());
                    ps.setDouble(2, sale.getTotal());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) sale.setId(keys.getInt(1));
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO sale_items(sale_id,part_id,quantity,unit_price,subtotal) VALUES (?,?,?,?,?)")) {
                    for (SaleItem it : sale.getItems()) {
                        double subtotal = it.getUnitPrice() * it.getQuantity();
                        ps.setInt(1, sale.getId());
                        ps.setInt(2, it.getPartId());
                        ps.setInt(3, it.getQuantity());
                        ps.setDouble(4, it.getUnitPrice());
                        ps.setDouble(5, subtotal);
                        ps.executeUpdate();
                    }
                }
                c.commit();
                sale.setStatus(Sale.Status.PENDING);
                return sale;
            } catch (SQLException e) {
                try { c.rollback(); } catch (SQLException ignore) {}
                throw new DaoException("create sale failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new DaoException("create sale failed: " + e.getMessage(), e);
        }
    }

    /**
     * Approve a pending sale: deduct stock per line (with stock check) and flip status.
     * Atomic across stock + status. Throws DaoException with a clear cause on failure.
     */
    public void approve(int saleId, int approverId) {
        try (Connection c = ConnectionFactory.borrow()) {
            c.setAutoCommit(false);
            try {
                String currentStatus = readStatus(c, saleId);
                if (currentStatus == null) throw new DaoException("Sale #" + saleId + " not found");
                if (!"PENDING".equals(currentStatus))
                    throw new DaoException("Sale #" + saleId + " is already " + currentStatus);

                try (PreparedStatement loadItems = c.prepareStatement(
                            "SELECT part_id, quantity FROM sale_items WHERE sale_id = ?");
                     PreparedStatement dec = c.prepareStatement(
                            "UPDATE parts SET quantity = quantity - ? WHERE id = ? AND quantity >= ?")) {
                    loadItems.setInt(1, saleId);
                    try (ResultSet rs = loadItems.executeQuery()) {
                        while (rs.next()) {
                            int partId = rs.getInt("part_id");
                            int qty    = rs.getInt("quantity");
                            dec.setInt(1, qty);
                            dec.setInt(2, partId);
                            dec.setInt(3, qty);
                            if (dec.executeUpdate() == 0) {
                                throw new DaoException("Insufficient stock for part #" + partId
                                        + " (need " + qty + "). Approval cancelled.");
                            }
                        }
                    }
                }
                try (PreparedStatement upd = c.prepareStatement(
                        "UPDATE sales SET status='APPROVED', approver_id=?, approved_at=NOW(), reject_reason=NULL WHERE id=?")) {
                    upd.setInt(1, approverId);
                    upd.setInt(2, saleId);
                    upd.executeUpdate();
                }
                c.commit();
            } catch (SQLException | DaoException e) {
                try { c.rollback(); } catch (SQLException ignore) {}
                if (e instanceof DaoException de) throw de;
                throw new DaoException("approve sale failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new DaoException("approve sale failed: " + e.getMessage(), e);
        }
    }

    /** Reject a pending sale. No stock change. */
    public void reject(int saleId, int approverId, String reason) {
        try (Connection c = ConnectionFactory.borrow()) {
            String currentStatus = readStatus(c, saleId);
            if (currentStatus == null) throw new DaoException("Sale #" + saleId + " not found");
            if (!"PENDING".equals(currentStatus))
                throw new DaoException("Sale #" + saleId + " is already " + currentStatus);
            try (PreparedStatement upd = c.prepareStatement(
                    "UPDATE sales SET status='REJECTED', approver_id=?, approved_at=NOW(), reject_reason=? WHERE id=?")) {
                upd.setInt(1, approverId);
                upd.setString(2, reason == null ? "" : reason);
                upd.setInt(3, saleId);
                upd.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DaoException("reject sale failed: " + e.getMessage(), e);
        }
    }

    private static String readStatus(Connection c, int saleId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT status FROM sales WHERE id = ?")) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Returns sales by status (most recent first), with their line items eagerly loaded. */
    public List<Sale> findByStatus(Sale.Status status) {
        String sql = "SELECT s.id, s.seller_id, s.total, s.status, s.approver_id, s.approved_at, " +
                     "       s.reject_reason, s.created_at, u.full_name AS seller_name, " +
                     "       au.full_name AS approver_name " +
                     "FROM sales s " +
                     "LEFT JOIN users u  ON u.id  = s.seller_id " +
                     "LEFT JOIN users au ON au.id = s.approver_id " +
                     "WHERE s.status = ? ORDER BY s.created_at DESC, s.id DESC";
        List<Sale> out = new ArrayList<>();
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sale s = new Sale();
                    s.setId(rs.getInt("id"));
                    s.setSellerId(rs.getInt("seller_id"));
                    s.setTotal(rs.getDouble("total"));
                    s.setStatus(Sale.Status.valueOf(rs.getString("status")));
                    int approver = rs.getInt("approver_id");
                    if (!rs.wasNull()) s.setApproverId(approver);
                    Timestamp approvedAt = rs.getTimestamp("approved_at");
                    if (approvedAt != null) s.setApprovedAt(approvedAt.toLocalDateTime());
                    s.setRejectReason(rs.getString("reject_reason"));
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) s.setCreatedAt(createdAt.toLocalDateTime());
                    s.setSellerName(rs.getString("seller_name"));
                    s.setApproverName(rs.getString("approver_name"));
                    out.add(s);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("findByStatus failed: " + e.getMessage(), e);
        }
        // Eager-load items for each sale (small N for pending list).
        for (Sale s : out) {
            for (SaleItem it : findItems(s.getId())) s.getItems().add(it);
        }
        return out;
    }

    /** Load line items for a single sale (used by the approval screen). */
    public List<SaleItem> findItems(int saleId) {
        String sql = "SELECT si.id, si.sale_id, si.part_id, si.quantity, si.unit_price, " +
                     "       p.sku, p.name FROM sale_items si " +
                     "LEFT JOIN parts p ON p.id = si.part_id " +
                     "WHERE si.sale_id = ? ORDER BY si.id";
        List<SaleItem> out = new ArrayList<>();
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleItem it = new SaleItem();
                    it.setId(rs.getInt("id"));
                    it.setSaleId(rs.getInt("sale_id"));
                    it.setPartId(rs.getInt("part_id"));
                    it.setQuantity(rs.getInt("quantity"));
                    it.setUnitPrice(rs.getDouble("unit_price"));
                    it.setPartSku(rs.getString("sku"));
                    it.setPartName(rs.getString("name"));
                    out.add(it);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("findItems failed: " + e.getMessage(), e);
        }
        return out;
    }

    // ====================================================================
    //   MARKETPLACE ORDER FLOW
    //   PLACED  ->  SELLER_ACK  ->  APPROVED  (stock deducted on APPROVED)
    // ====================================================================

    /**
     * Customer places a marketplace order. Stock is DEDUCTED immediately to prevent
     * overselling. The order remains PLACED until the seller acknowledges it.
     */
    public Sale placeOrder(Sale sale, int buyerId) {
        try (Connection c = ConnectionFactory.borrow()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement dec = c.prepareStatement(
                        "UPDATE parts SET quantity = quantity - ? WHERE id = ? AND quantity >= ?")) {
                    for (SaleItem it : sale.getItems()) {
                        dec.setInt(1, it.getQuantity());
                        dec.setInt(2, it.getPartId());
                        dec.setInt(3, it.getQuantity());
                        if (dec.executeUpdate() == 0) {
                            throw new DaoException("Insufficient stock for part #" + it.getPartId());
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO sales(seller_id,total,status,buyer_id) VALUES (?,?,'PLACED',?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, sale.getSellerId());
                    ps.setDouble(2, sale.getTotal());
                    ps.setInt(3, buyerId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) sale.setId(keys.getInt(1));
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO sale_items(sale_id,part_id,quantity,unit_price,subtotal) VALUES (?,?,?,?,?)")) {
                    for (SaleItem it : sale.getItems()) {
                        double subtotal = it.getUnitPrice() * it.getQuantity();
                        ps.setInt(1, sale.getId());
                        ps.setInt(2, it.getPartId());
                        ps.setInt(3, it.getQuantity());
                        ps.setDouble(4, it.getUnitPrice());
                        ps.setDouble(5, subtotal);
                        ps.executeUpdate();
                    }
                }
                c.commit();
                sale.setStatus(Sale.Status.PLACED);
                sale.setBuyerId(buyerId);
                return sale;
            } catch (SQLException e) {
                try { c.rollback(); } catch (SQLException ignore) {}
                throw new DaoException("place order failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new DaoException("place order failed: " + e.getMessage(), e);
        }
    }

    /** Seller "prints the receipt": PLACED -> APPROVED. */
    public void sellerAck(int saleId) {
        try (Connection c = ConnectionFactory.borrow()) {
            String st = readStatus(c, saleId);
            if (!"PLACED".equals(st))
                throw new DaoException("Order #" + saleId + " is " + st + ", cannot ack");
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE sales SET status='APPROVED', seller_ack_at=NOW() WHERE id=?")) {
                ps.setInt(1, saleId);
                ps.executeUpdate();
            }
        } catch (SQLException e) { throw new DaoException("sellerAck failed: " + e.getMessage(), e); }
    }

    /** Orders placed by a given customer. */
    public List<Sale> findByBuyer(int buyerId) {
        String sql = "SELECT s.*, u.full_name AS seller_name FROM sales s " +
                     "LEFT JOIN users u ON u.id = s.seller_id " +
                     "WHERE s.buyer_id = ? ORDER BY s.created_at DESC, s.id DESC";
        return loadSales(sql, ps -> ps.setInt(1, buyerId));
    }

    /** Marketplace orders waiting on a given seller (or already moved past). */
    public List<Sale> findForSeller(int sellerId, Sale.Status... statuses) {
        StringBuilder sb = new StringBuilder(
                "SELECT s.*, b.full_name AS buyer_name FROM sales s " +
                "LEFT JOIN users b ON b.id = s.buyer_id " +
                "WHERE s.seller_id = ? AND s.buyer_id IS NOT NULL");
        if (statuses != null && statuses.length > 0) {
            sb.append(" AND s.status IN (");
            for (int i = 0; i < statuses.length; i++) sb.append(i == 0 ? "?" : ",?");
            sb.append(")");
        }
        sb.append(" ORDER BY s.created_at DESC, s.id DESC");
        return loadSales(sb.toString(), ps -> {
            ps.setInt(1, sellerId);
            if (statuses != null)
                for (int i = 0; i < statuses.length; i++) ps.setString(2 + i, statuses[i].name());
        });
    }

    /** Generic "load sales + items" used by the marketplace queries. */
    private List<Sale> loadSales(String sql, SqlBinder binder) {
        List<Sale> out = new ArrayList<>();
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sale s = new Sale();
                    s.setId(rs.getInt("id"));
                    s.setSellerId(rs.getInt("seller_id"));
                    s.setTotal(rs.getDouble("total"));
                    s.setStatus(Sale.Status.valueOf(rs.getString("status")));
                    int buyer = rs.getInt("buyer_id");
                    if (!rs.wasNull()) s.setBuyerId(buyer);
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) s.setCreatedAt(createdAt.toLocalDateTime());
                    Timestamp ackAt = rs.getTimestamp("seller_ack_at");
                    if (ackAt != null) s.setSellerAckAt(ackAt.toLocalDateTime());
                    try { s.setSellerName(rs.getString("seller_name")); } catch (SQLException ignore) {}
                    try { s.setBuyerName(rs.getString("buyer_name"));   } catch (SQLException ignore) {}
                    out.add(s);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("loadSales failed: " + e.getMessage(), e);
        }
        for (Sale s : out) s.getItems().addAll(findItems(s.getId()));
        return out;
    }

    @FunctionalInterface
    private interface SqlBinder { void bind(PreparedStatement ps) throws SQLException; }

    public int countPending() {
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM sales WHERE status='PENDING'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { throw new DaoException("countPending failed: " + e.getMessage(), e); }
    }

    public double totalSalesSince(LocalDateTime since) {
        String sql = "SELECT COALESCE(SUM(total),0) FROM sales WHERE status='APPROVED' AND created_at >= ?";
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, since.format(TS));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("totalSalesSince failed", e);
        }
    }

    public double totalSalesSinceForSeller(LocalDateTime since, int sellerId) {
        String sql = "SELECT COALESCE(SUM(total),0) FROM sales WHERE status='APPROVED' AND created_at >= ? AND seller_id = ?";
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, since.format(TS));
            ps.setInt(2, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("totalSalesSinceForSeller failed", e);
        }
    }

    public double totalProfitSince(LocalDateTime since) {
        String sql = "SELECT COALESCE(SUM((si.unit_price - p.cost_price) * si.quantity),0) " +
                     "FROM sale_items si JOIN parts p ON p.id = si.part_id " +
                     "JOIN sales s ON s.id = si.sale_id " +
                     "WHERE s.status='APPROVED' AND s.created_at >= ?";
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, since.format(TS));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("totalProfitSince failed", e);
        }
    }

    public double totalProfitSinceForSeller(LocalDateTime since, int sellerId) {
        String sql = "SELECT COALESCE(SUM((si.unit_price - p.cost_price) * si.quantity),0) " +
                     "FROM sale_items si JOIN parts p ON p.id = si.part_id " +
                     "JOIN sales s ON s.id = si.sale_id " +
                     "WHERE s.status='APPROVED' AND s.created_at >= ? AND s.seller_id = ?";
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, since.format(TS));
            ps.setInt(2, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("totalProfitSinceForSeller failed", e);
        }
    }

    /** Daily sales totals for the last N days, ordered ascending by day. */
    public Map<String, Double> dailyTotals(int days) {
        String sql = "SELECT DATE(created_at) d, SUM(total) t FROM sales " +
                     "WHERE status='APPROVED' AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                     "GROUP BY d ORDER BY d";
        Map<String, Double> out = new LinkedHashMap<>();
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString("d"), rs.getDouble("t"));
            }
        } catch (SQLException e) {
            throw new DaoException("dailyTotals failed", e);
        }
        return out;
    }

    public Map<String, Double> dailyTotalsForSeller(int days, int sellerId) {
        String sql = "SELECT DATE(created_at) d, SUM(total) t FROM sales " +
                     "WHERE status='APPROVED' AND seller_id = ? AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                     "GROUP BY d ORDER BY d";
        Map<String, Double> out = new LinkedHashMap<>();
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            ps.setInt(2, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString("d"), rs.getDouble("t"));
            }
        } catch (SQLException e) {
            throw new DaoException("dailyTotalsForSeller failed", e);
        }
        return out;
    }

    /** Top selling parts by total quantity sold. */
    public List<Object[]> topSelling(int limit) {
        String sql = "SELECT p.name, SUM(si.quantity) q FROM sale_items si " +
                     "JOIN parts p ON p.id = si.part_id " +
                     "JOIN sales s ON s.id = si.sale_id " +
                     "WHERE s.status='APPROVED' " +
                     "GROUP BY si.part_id ORDER BY q DESC LIMIT ?";
        List<Object[]> out = new ArrayList<>();
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Object[]{rs.getString(1), rs.getInt(2)});
            }
        } catch (SQLException e) {
            throw new DaoException("topSelling failed", e);
        }
        return out;
    }

    /** Top sellers ranked by total approved revenue. */
    public List<Object[]> topSellersByRevenue(int limit) {
        String sql = "SELECT u.full_name, COALESCE(SUM(s.total), 0) rev " +
                     "FROM sales s JOIN users u ON u.id = s.seller_id " +
                     "WHERE s.status = 'APPROVED' " +
                     "GROUP BY s.seller_id ORDER BY rev DESC LIMIT ?";
        List<Object[]> out = new ArrayList<>();
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Object[]{rs.getString(1), rs.getDouble(2)});
            }
        } catch (SQLException e) {
            throw new DaoException("topSellersByRevenue failed", e);
        }
        return out;
    }

    public int countAll() {
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM sales");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { throw new DaoException("countAll sales failed", e); }
    }

    /** Customer requests a return on an APPROVED order. */
    public void requestReturn(int saleId, String reason) {
        String sql = "UPDATE sales SET status = 'RETURN_REQUESTED', return_reason = ?, " +
                     "return_requested_at = NOW() WHERE id = ? AND status = 'APPROVED'";
        try (Connection c = c();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, reason == null ? "" : reason);
            ps.setInt(2, saleId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DaoException("Order not eligible for return (must be APPROVED)");
        } catch (SQLException e) {
            throw new DaoException("requestReturn failed", e);
        }
    }

    /** Seller approves the return: status → RETURNED, stock restored atomically. */
    public void approveReturn(int saleId) {
        try (Connection c = ConnectionFactory.borrow()) {
            c.setAutoCommit(false);
            try {
                // Check current status
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT status FROM sales WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, saleId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !"RETURN_REQUESTED".equals(rs.getString(1)))
                            throw new DaoException("Order not in RETURN_REQUESTED state");
                    }
                }
                // Restore stock for each item
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE parts p JOIN sale_items si ON p.id = si.part_id " +
                        "SET p.quantity = p.quantity + si.quantity WHERE si.sale_id = ?")) {
                    ps.setInt(1, saleId);
                    ps.executeUpdate();
                }
                // Flip status
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE sales SET status = 'RETURNED', return_approved_at = NOW() WHERE id = ?")) {
                    ps.setInt(1, saleId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw ex instanceof DaoException ? (DaoException) ex : new DaoException("approveReturn failed", ex);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (DaoException e) {
            throw e;
        } catch (Exception e) {
            throw new DaoException("approveReturn failed", e);
        }
    }
}

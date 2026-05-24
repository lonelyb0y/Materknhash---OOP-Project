package com.matraknhash.dao;

import com.matraknhash.db.ConnectionFactory;
import com.matraknhash.model.ServiceOffer;
import com.matraknhash.model.ServiceRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ServiceCenterDao {

    // ---------------------------------------------------------- offers ----
    private ServiceOffer extractOffer(ResultSet rs) throws SQLException {
        ServiceOffer o = new ServiceOffer();
        o.setId(rs.getInt("id"));
        o.setCenterId(rs.getInt("center_id"));
        o.setTitle(rs.getString("title"));
        o.setDescription(rs.getString("description"));
        o.setPrice(rs.getDouble("price"));
        o.setStatus(ServiceOffer.Status.valueOf(rs.getString("status")));
        o.setReason(rs.getString("reason"));
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null) o.setCreatedAt(t.toLocalDateTime());
        try { o.setCenterName(rs.getString("center_name")); } catch (SQLException ignore) {}
        return o;
    }

    public ServiceOffer createOffer(ServiceOffer o) {
        String sql = "INSERT INTO service_offers(center_id,title,description,price,status) VALUES (?,?,?,?,?)";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getCenterId());
            ps.setString(2, o.getTitle());
            ps.setString(3, o.getDescription());
            ps.setDouble(4, o.getPrice());
            ps.setString(5, ServiceOffer.Status.PENDING_ADMIN.name());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) o.setId(k.getInt(1));
            }
            o.setStatus(ServiceOffer.Status.PENDING_ADMIN);
            return o;
        } catch (SQLException e) {
            throw new DaoException("create service offer failed", e);
        }
    }

    public List<ServiceOffer> offersByCenter(int centerId) {
        String sql = "SELECT * FROM service_offers WHERE center_id = ? ORDER BY id DESC";
        return queryOffers(sql, ps -> ps.setInt(1, centerId));
    }

    public List<ServiceOffer> liveOffers() {
        String sql = "SELECT o.*, u.full_name AS center_name " +
                     "FROM service_offers o JOIN users u ON u.id = o.center_id " +
                     "WHERE o.status = 'LIVE' ORDER BY o.created_at DESC";
        return queryOffers(sql, ps -> {});
    }

    public List<ServiceOffer> offersByStatus(ServiceOffer.Status status) {
        String sql = "SELECT o.*, u.full_name AS center_name " +
                     "FROM service_offers o JOIN users u ON u.id = o.center_id " +
                     "WHERE o.status = ? ORDER BY o.created_at ASC";
        return queryOffers(sql, ps -> ps.setString(1, status.name()));
    }

    public boolean updateOfferStatus(int offerId, ServiceOffer.Status status, String reason) {
        String sql = "UPDATE service_offers SET status = ?, reason = ? WHERE id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, reason);
            ps.setInt(3, offerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("update service offer failed", e);
        }
    }

    @FunctionalInterface
    private interface PsBinder { void bind(PreparedStatement ps) throws SQLException; }

    private List<ServiceOffer> queryOffers(String sql, PsBinder binder) {
        List<ServiceOffer> out = new ArrayList<>();
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(extractOffer(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new DaoException("query service offers failed", e);
        }
    }

    // -------------------------------------------------------- requests ----
    private ServiceRequest extractRequest(ResultSet rs) throws SQLException {
        ServiceRequest r = new ServiceRequest();
        r.setId(rs.getInt("id"));
        r.setCustomerId(rs.getInt("customer_id"));
        r.setOfferId(rs.getInt("offer_id"));
        r.setVehicleNote(rs.getString("vehicle_note"));
        r.setStatus(ServiceRequest.Status.valueOf(rs.getString("status")));
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null) r.setCreatedAt(t.toLocalDateTime());
        try { r.setCustomerName(rs.getString("customer_name")); } catch (SQLException ignore) {}
        try { r.setOfferTitle(rs.getString("offer_title"));     } catch (SQLException ignore) {}
        try { r.setOfferPrice(rs.getDouble("offer_price"));     } catch (SQLException ignore) {}
        try { r.setCenterId(rs.getInt("center_id"));            } catch (SQLException ignore) {}
        try { r.setCenterName(rs.getString("center_name"));     } catch (SQLException ignore) {}
        return r;
    }

    public ServiceRequest createRequest(ServiceRequest r) {
        String sql = "INSERT INTO service_requests(customer_id,offer_id,vehicle_note,status) VALUES (?,?,?,?)";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getCustomerId());
            ps.setInt(2, r.getOfferId());
            ps.setString(3, r.getVehicleNote());
            ps.setString(4, ServiceRequest.Status.REQUESTED.name());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) r.setId(k.getInt(1));
            }
            r.setStatus(ServiceRequest.Status.REQUESTED);
            return r;
        } catch (SQLException e) {
            throw new DaoException("create service request failed", e);
        }
    }

    public boolean updateRequestStatus(int requestId, ServiceRequest.Status status) {
        String sql = "UPDATE service_requests SET status = ? WHERE id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("update service request failed", e);
        }
    }

    /** Requests received by a given service centre (joined with offer + customer). */
    public List<ServiceRequest> requestsForCenter(int centerId) {
        String sql = "SELECT r.*, o.title AS offer_title, o.price AS offer_price, " +
                     "       o.center_id AS center_id, " +
                     "       cu.full_name AS customer_name, ce.full_name AS center_name " +
                     "FROM service_requests r " +
                     "JOIN service_offers o ON o.id = r.offer_id " +
                     "JOIN users cu ON cu.id = r.customer_id " +
                     "JOIN users ce ON ce.id = o.center_id " +
                     "WHERE o.center_id = ? ORDER BY r.created_at DESC";
        return queryRequests(sql, ps -> ps.setInt(1, centerId));
    }

    /** Requests placed by a given customer. */
    public List<ServiceRequest> requestsByCustomer(int customerId) {
        String sql = "SELECT r.*, o.title AS offer_title, o.price AS offer_price, " +
                     "       o.center_id AS center_id, " +
                     "       cu.full_name AS customer_name, ce.full_name AS center_name " +
                     "FROM service_requests r " +
                     "JOIN service_offers o ON o.id = r.offer_id " +
                     "JOIN users cu ON cu.id = r.customer_id " +
                     "JOIN users ce ON ce.id = o.center_id " +
                     "WHERE r.customer_id = ? ORDER BY r.created_at DESC";
        return queryRequests(sql, ps -> ps.setInt(1, customerId));
    }

    private List<ServiceRequest> queryRequests(String sql, PsBinder binder) {
        List<ServiceRequest> out = new ArrayList<>();
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(extractRequest(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new DaoException("query service requests failed", e);
        }
    }
}

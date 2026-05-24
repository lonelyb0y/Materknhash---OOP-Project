package com.matraknhash.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.matraknhash.dao.DaoException;
import com.matraknhash.dao.UserDao;
import com.matraknhash.db.ConnectionFactory;
import com.matraknhash.model.Role;
import com.matraknhash.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class UserService {

    private final UserDao dao;
    public UserService(UserDao dao) { this.dao = dao; }

    public List<User> all() { return dao.findAll(); }

    public User createUser(String username, String rawPassword, String fullName, Role role) {
        String hash = BCrypt.withDefaults().hashToString(10, rawPassword.toCharArray());
        User u = User.from(0, username, hash, fullName, role, true);
        return dao.insert(u);
    }

    public boolean delete(int id) { return dao.delete(id); }

   
    public boolean purgeUser(int id) {
        try (Connection c = ConnectionFactory.get()) {
            c.setAutoCommit(false);
            try {
                // Clear order history referencing this user to prevent foreign key errors
                exec(c, "DELETE FROM sale_items WHERE sale_id IN (SELECT id FROM sales WHERE seller_id = ?)", id);
                exec(c, "DELETE FROM sale_items WHERE sale_id IN (SELECT id FROM sales WHERE buyer_id = ?)", id);
                exec(c, "DELETE FROM sales WHERE seller_id = ?", id);
                exec(c, "DELETE FROM sales WHERE buyer_id = ?", id);
                
                // Clear purchase history referencing this user
                exec(c, "DELETE FROM purchase_items WHERE purchase_id IN (SELECT id FROM purchases WHERE user_id = ?)", id);
                exec(c, "DELETE FROM purchases WHERE user_id = ?", id);

                exec(c, "DELETE FROM service_requests WHERE customer_id = ?", id);
                exec(c, "DELETE FROM service_requests WHERE offer_id IN " +
                       "(SELECT id FROM service_offers WHERE center_id = ?)", id);
                exec(c, "DELETE FROM service_offers   WHERE center_id  = ?", id);
                exec(c, "DELETE FROM notifications    WHERE user_id    = ?", id);
                exec(c, "DELETE FROM parts            WHERE seller_id  = ?", id);
                exec(c, "DELETE FROM users            WHERE id         = ?", id);
                c.commit();
                return true;
            } catch (SQLException ex) {
                c.rollback();
                throw new DaoException("purge user failed: " + ex.getMessage(), ex);
            }
        } catch (SQLException e) {
            throw new DaoException("purge user failed", e);
        }
    }

    private static void exec(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * "Soft delete" -- leave history (listings, orders) intact but block the
     * account from logging in or appearing in marketplace queries.
     */
    public boolean deactivate(int id) {
        return dao.findById(id).map(u -> {
            u.setStatus(User.Status.SUSPENDED);
            u.setActive(false);
            return dao.update(u);
        }).orElse(false);
    }

    public boolean toggleActive(User u) {
        u.setActive(!u.isActive());
        return dao.update(u);
    }

    /** Sellers and service-centres awaiting admin sign-off, oldest first. */
    public List<User> listPendingApprovals() {
        return dao.findAll().stream()
                .filter(u -> u.getStatus() == User.Status.PENDING_APPROVAL
                          && (u.getRole() == Role.SELLER || u.getRole() == Role.SERVICE_CENTER))
                .toList();
    }

    /** Flip a PENDING account to ACTIVE so they can log in. */
    public boolean approveAccount(int userId) {
        return dao.findById(userId).map(u -> {
            u.setStatus(User.Status.ACTIVE);
            u.setActive(true);
            return dao.update(u);
        }).orElse(false);
    }

    /** Refuse an application -- keeps the username reserved but blocks login. */
    public boolean rejectAccount(int userId) {
        return dao.findById(userId).map(u -> {
            u.setStatus(User.Status.SUSPENDED);
            u.setActive(false);
            return dao.update(u);
        }).orElse(false);
    }
}

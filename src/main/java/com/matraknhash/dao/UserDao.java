package com.matraknhash.dao;

import com.matraknhash.model.Role;
import com.matraknhash.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDao extends BaseDao<User> {

    @Override protected String table() { return "users"; }

    @Override protected String[] columns() {
        return new String[]{"username","password_hash","full_name","role","active"};
    }

    @Override protected User extract(ResultSet rs) throws SQLException {
        return User.from(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("full_name"),
                Role.of(rs.getString("role")),
                rs.getInt("active") == 1
        );
    }

    @Override protected void bindInsert(PreparedStatement ps, User u) throws SQLException {
        ps.setString(1, u.getUsername());
        ps.setString(2, u.getPasswordHash());
        ps.setString(3, u.getFullName());
        ps.setString(4, u.getRole().name());
        ps.setInt(5, u.isActive() ? 1 : 0);
    }

    @Override protected void bindUpdate(PreparedStatement ps, User u) throws SQLException {
        bindInsert(ps, u);
        ps.setInt(6, u.getId());
    }

    @Override protected int idOf(User u) { return u.getId(); }
    @Override protected void setId(User u, int id) { u.setId(id); }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(extract(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("findByUsername failed", e);
        }
    }
}

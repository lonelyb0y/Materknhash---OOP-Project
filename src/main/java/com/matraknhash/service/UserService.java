package com.matraknhash.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.matraknhash.dao.UserDao;
import com.matraknhash.model.Role;
import com.matraknhash.model.User;

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

    public boolean toggleActive(User u) {
        u.setActive(!u.isActive());
        return dao.update(u);
    }

    /** Sellers awaiting admin sign-off, oldest first. */
    public List<User> listPendingSellers() {
        return dao.findAll().stream()
                .filter(u -> u.getRole() == Role.SELLER && u.getStatus() == User.Status.PENDING_APPROVAL)
                .toList();
    }

    /** Flip a PENDING seller to ACTIVE so they can log in and start listing. */
    public boolean approveSeller(int sellerId) {
        return dao.findById(sellerId).map(u -> {
            u.setStatus(User.Status.ACTIVE);
            u.setActive(true);
            return dao.update(u);
        }).orElse(false);
    }

    /** Refuse a seller application -- keeps the username reserved but blocks login. */
    public boolean rejectSeller(int sellerId) {
        return dao.findById(sellerId).map(u -> {
            u.setStatus(User.Status.SUSPENDED);
            u.setActive(false);
            return dao.update(u);
        }).orElse(false);
    }
}

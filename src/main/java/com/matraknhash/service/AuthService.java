package com.matraknhash.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.matraknhash.dao.UserDao;
import com.matraknhash.model.Role;
import com.matraknhash.model.User;
import com.matraknhash.util.Result;

import java.util.Optional;

public class AuthService {

    private final UserDao userDao;

    public AuthService(UserDao userDao) { this.userDao = userDao; }

    public Result<User> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty())
            return Result.fail("Username and password are required.");

        Optional<User> opt = userDao.findByUsername(username.trim());
        if (opt.isEmpty()) return Result.fail("Invalid username or password.");
        User u = opt.get();
        if (!u.isActive()) return Result.fail("Account is inactive.");

        // Pending seller signups can't log in until an admin reviews them.
        if (u.getStatus() == User.Status.PENDING_APPROVAL)
            return Result.fail("Your seller account is awaiting admin approval.");
        if (u.getStatus() == User.Status.SUSPENDED)
            return Result.fail("Account suspended. Contact the administrator.");

        BCrypt.Result r = BCrypt.verifyer().verify(password.toCharArray(), u.getPasswordHash());
        if (!r.verified) return Result.fail("Invalid username or password.");
        return Result.ok(u);
    }

    /**
     * Self-service signup. Only CUSTOMER and SELLER may register themselves.
     * Customers are immediately ACTIVE; sellers land in PENDING_APPROVAL and
     * need admin sign-off before they can log in or list anything.
     */
    public Result<User> signup(String username, String password, String fullName, Role role) {
        if (username == null || username.isBlank())   return Result.fail("Username is required.");
        if (password == null || password.length() < 4) return Result.fail("Password must be at least 4 characters.");
        if (fullName == null || fullName.isBlank())   return Result.fail("Full name is required.");
        if (role != Role.CUSTOMER && role != Role.SELLER)
            return Result.fail("Only customers or sellers can sign up here.");

        if (userDao.findByUsername(username.trim()).isPresent())
            return Result.fail("That username is already taken.");

        String hash = BCrypt.withDefaults().hashToString(10, password.toCharArray());
        User u = User.from(0, username.trim(), hash, fullName.trim(), role, true);
        u.setStatus(role == Role.SELLER ? User.Status.PENDING_APPROVAL : User.Status.ACTIVE);
        return Result.ok(userDao.insert(u));
    }

    public String hash(String pwd) {
        return BCrypt.withDefaults().hashToString(10, pwd.toCharArray());
    }
}

package com.matraknhash.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.matraknhash.dao.UserDao;
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

        BCrypt.Result r = BCrypt.verifyer().verify(password.toCharArray(), u.getPasswordHash());
        if (!r.verified) return Result.fail("Invalid username or password.");
        return Result.ok(u);
    }

    public String hash(String pwd) {
        return BCrypt.withDefaults().hashToString(10, pwd.toCharArray());
    }
}

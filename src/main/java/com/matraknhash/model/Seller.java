package com.matraknhash.model;

public class Seller extends User {
    public Seller(int id, String username, String passwordHash, String fullName, boolean active) {
        super(id, username, passwordHash, fullName, Role.SELLER, active);
    }
    @Override public String permissions() {
        return "pos,sales";
    }
}

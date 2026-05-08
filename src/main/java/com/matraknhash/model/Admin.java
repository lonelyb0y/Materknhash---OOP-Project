package com.matraknhash.model;

public class Admin extends User {
    public Admin(int id, String username, String passwordHash, String fullName, boolean active) {
        super(id, username, passwordHash, fullName, Role.ADMIN, active);
    }
    @Override public String permissions() {
        return "users,parts,suppliers,sales,reports,settings";
    }
}

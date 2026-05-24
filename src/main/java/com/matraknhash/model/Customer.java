package com.matraknhash.model;


public class Customer extends User {
    public Customer(int id, String username, String passwordHash, String fullName, boolean active) {
        super(id, username, passwordHash, fullName, Role.CUSTOMER, active);
    }
    @Override public String permissions() {
        return "catalog,buy,return";
    }
}

package com.matraknhash.model;

/**
 * Customer = end buyer on the marketplace. Self-registers, auto-active,
 * can place orders against any LIVE listing and request returns on the
 * ones they've already received.
 */
public class Customer extends User {
    public Customer(int id, String username, String passwordHash, String fullName, boolean active) {
        super(id, username, passwordHash, fullName, Role.CUSTOMER, active);
    }
    @Override public String permissions() {
        return "catalog,buy,return";
    }
}

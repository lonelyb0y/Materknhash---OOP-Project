package com.matraknhash.model;

public enum Role {
    ADMIN, EMPLOYEE, SELLER, CUSTOMER;

    public static Role of(String s) {
        return Role.valueOf(s.trim().toUpperCase());
    }
}

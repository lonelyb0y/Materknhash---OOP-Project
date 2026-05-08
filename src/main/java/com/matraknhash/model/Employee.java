package com.matraknhash.model;

public class Employee extends User {
    public Employee(int id, String username, String passwordHash, String fullName, boolean active) {
        super(id, username, passwordHash, fullName, Role.EMPLOYEE, active);
    }
    @Override public String permissions() {
        return "parts,suppliers,reports";
    }
}

package com.matraknhash.model;

import java.time.LocalDateTime;

/**
 * Base user. Concrete subclasses (Admin, Employee, Seller, Customer)
 * exist to demonstrate inheritance + polymorphism via {@link #permissions()}.
 */
public abstract class User {

    /**
     * Account-level lifecycle (separate from role). Customers and the seeded
     * staff users are ACTIVE; new seller signups land in PENDING_APPROVAL until
     * an admin reviews them; SUSPENDED accounts can't log in.
     */
    public enum Status { ACTIVE, PENDING_APPROVAL, SUSPENDED }

    protected int id;
    protected String username;
    protected String passwordHash;
    protected String fullName;
    protected Role role;
    protected boolean active;
    protected Status status = Status.ACTIVE;
    protected LocalDateTime createdAt;

    protected User() {}

    protected User(int id, String username, String passwordHash, String fullName, Role role, boolean active) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
    }

    public abstract String permissions();

    public static User from(int id, String username, String passwordHash, String fullName, Role role, boolean active) {
        return switch (role) {
            case ADMIN    -> new Admin(id, username, passwordHash, fullName, active);
            case EMPLOYEE -> new Employee(id, username, passwordHash, fullName, active);
            case SELLER   -> new Seller(id, username, passwordHash, fullName, active);
            case CUSTOMER -> new Customer(id, username, passwordHash, fullName, active);
        };
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public Status getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setActive(boolean active) { this.active = active; }
    public void setStatus(Status status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override public String toString() { return fullName + " (" + username + " / " + role + ")"; }
}

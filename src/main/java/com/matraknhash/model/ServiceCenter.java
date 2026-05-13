package com.matraknhash.model;

/**
 * Service center = an external auto-repair workshop that lists services
 * (oil change, brake job, ...) for customers to book. Signs up via the
 * public Signup screen and lands in PENDING_APPROVAL like sellers do.
 */
public class ServiceCenter extends User {
    public ServiceCenter(int id, String username, String passwordHash, String fullName, boolean active) {
        super(id, username, passwordHash, fullName, Role.SERVICE_CENTER, active);
    }
    @Override public String permissions() {
        return "publish-service-offers,handle-service-requests";
    }
}

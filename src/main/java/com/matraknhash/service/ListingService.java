package com.matraknhash.service;

import com.matraknhash.dao.PartDao;
import com.matraknhash.dao.SupplierDao;
import com.matraknhash.model.Part;
import com.matraknhash.model.Supplier;

import java.util.List;

/**
 * Listing approval pipeline (M3).
 *
 * <pre>
 *   Seller submits a listing
 *      │
 *      ├── supplier is trusted ───────────────────────────────▶ LIVE
 *      │                                                        (instant)
 *      └── otherwise ─────▶ PENDING_EMPLOYEE
 *                              │
 *                              ├── employee approves ─▶ PENDING_ADMIN
 *                              │                          │
 *                              │                          ├─ admin approves ──▶ LIVE
 *                              │                          └─ admin rejects  ──▶ REJECTED
 *                              └── employee rejects ──▶ REJECTED
 * </pre>
 */
public class ListingService {

    private final PartDao partDao;
    private final SupplierDao supplierDao;

    public ListingService(PartDao partDao, SupplierDao supplierDao) {
        this.partDao = partDao;
        this.supplierDao = supplierDao;
    }

    /**
     * Seller creates a new listing. Trusted-supplier listings go straight to LIVE,
     * otherwise they enter the employee review queue.
     */
    public Part submit(Part p, int sellerId) {
        p.setSellerId(sellerId);
        if (isTrustedSupplier(p.getSupplierId())) {
            p.setListingStatus(Part.ListingStatus.LIVE);
            p.setListingReason("Auto-approved: trusted supplier");
        } else {
            p.setListingStatus(Part.ListingStatus.PENDING_EMPLOYEE);
        }
        return partDao.insert(p);
    }

    public boolean employeeApprove(int partId, int employeeId) {
        return partDao.findById(partId).map(p -> {
            if (p.getListingStatus() != Part.ListingStatus.PENDING_EMPLOYEE) return false;
            p.setListingStatus(Part.ListingStatus.PENDING_ADMIN);
            p.setEmployeeReviewerId(employeeId);
            p.setListingReason(null);
            return partDao.update(p);
        }).orElse(false);
    }

    public boolean employeeReject(int partId, int employeeId, String reason) {
        return partDao.findById(partId).map(p -> {
            if (p.getListingStatus() != Part.ListingStatus.PENDING_EMPLOYEE) return false;
            p.setListingStatus(Part.ListingStatus.REJECTED);
            p.setEmployeeReviewerId(employeeId);
            p.setListingReason(reason == null ? "" : reason);
            return partDao.update(p);
        }).orElse(false);
    }

    public boolean adminApprove(int partId, int adminId) {
        return partDao.findById(partId).map(p -> {
            if (p.getListingStatus() != Part.ListingStatus.PENDING_ADMIN) return false;
            p.setListingStatus(Part.ListingStatus.LIVE);
            p.setAdminReviewerId(adminId);
            p.setListingReason(null);
            return partDao.update(p);
        }).orElse(false);
    }

    public boolean adminReject(int partId, int adminId, String reason) {
        return partDao.findById(partId).map(p -> {
            if (p.getListingStatus() != Part.ListingStatus.PENDING_ADMIN) return false;
            p.setListingStatus(Part.ListingStatus.REJECTED);
            p.setAdminReviewerId(adminId);
            p.setListingReason(reason == null ? "" : reason);
            return partDao.update(p);
        }).orElse(false);
    }

    public List<Part> liveCatalog()       { return partDao.findLive(); }
    public List<Part> bySeller(int sid)   { return partDao.findBySeller(sid); }
    public List<Part> pendingEmployee()   { return partDao.findByListingStatus(Part.ListingStatus.PENDING_EMPLOYEE); }
    public List<Part> pendingAdmin()      { return partDao.findByListingStatus(Part.ListingStatus.PENDING_ADMIN); }

    private boolean isTrustedSupplier(Integer supplierId) {
        if (supplierId == null) return false;
        return supplierDao.findById(supplierId).map(Supplier::isTrusted).orElse(false);
    }
}

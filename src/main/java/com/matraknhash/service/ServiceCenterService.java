package com.matraknhash.service;

import com.matraknhash.dao.ServiceCenterDao;
import com.matraknhash.model.ServiceOffer;
import com.matraknhash.model.ServiceRequest;

import java.util.List;

/**
 * Workflow facade for the service-centres feature. Centres publish offers,
 * an admin approves them, and customers request approved offers.
 */
public class ServiceCenterService {

    private final ServiceCenterDao dao;
    public ServiceCenterService(ServiceCenterDao dao) { this.dao = dao; }

    // --- offers -----------------------------------------------------------
    public ServiceOffer submitOffer(int centerId, String title, String description, double price) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title required");
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
        return dao.createOffer(new ServiceOffer(centerId, title.trim(),
                description == null ? "" : description.trim(), price));
    }

    public List<ServiceOffer> myOffers(int centerId)         { return dao.offersByCenter(centerId); }
    public List<ServiceOffer> liveOffers()                   { return dao.liveOffers(); }
    public List<ServiceOffer> pendingOffers()                { return dao.offersByStatus(ServiceOffer.Status.PENDING_ADMIN); }
    public boolean approveOffer(int id)                      { return dao.updateOfferStatus(id, ServiceOffer.Status.LIVE, null); }
    public boolean rejectOffer(int id, String reason)        { return dao.updateOfferStatus(id, ServiceOffer.Status.REJECTED, reason); }
    public boolean updateOffer(ServiceOffer o)               { return dao.updateOffer(o); }
    public boolean deleteOffer(int id)                       { return dao.deleteOffer(id); }

    public double totalRevenueLast30Days(int centerId)       { return dao.totalRevenueSince(centerId, java.time.LocalDateTime.now().minusDays(30)); }
    public int countActiveBookings(int centerId)             { return dao.countActiveBookings(centerId); }
    public int countOffers(int centerId)                     { return dao.countOffers(centerId); }
    public java.util.Map<String, Double> dailyRevenue(int centerId, int days) { return dao.dailyRevenue(centerId, days); }
    public java.util.List<Object[]> topCentersByBookings(int limit) { return dao.topCentersByBookings(limit); }

    // --- requests ---------------------------------------------------------
    public ServiceRequest requestService(int customerId, int offerId, String vehicleNote) {
        return dao.createRequest(new ServiceRequest(customerId, offerId,
                vehicleNote == null ? "" : vehicleNote.trim()));
    }

    public List<ServiceRequest> incoming(int centerId)        { return dao.requestsForCenter(centerId); }
    public List<ServiceRequest> myRequests(int customerId)    { return dao.requestsByCustomer(customerId); }
    public boolean acceptRequest(int id)                      { return dao.updateRequestStatus(id, ServiceRequest.Status.ACCEPTED); }
    public boolean completeRequest(int id)                    { return dao.updateRequestStatus(id, ServiceRequest.Status.COMPLETED); }
    public boolean rejectRequest(int id)                      { return dao.updateRequestStatus(id, ServiceRequest.Status.REJECTED); }
}

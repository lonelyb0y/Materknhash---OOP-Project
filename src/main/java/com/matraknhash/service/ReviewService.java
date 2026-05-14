package com.matraknhash.service;

import com.matraknhash.dao.ReviewDao;
import com.matraknhash.model.Review;

import java.util.List;

/** Service facade for seller reviews. */
public class ReviewService {

    private final ReviewDao dao;
    public ReviewService(ReviewDao dao) { this.dao = dao; }

    public Review submit(int saleId, int customerId, int sellerId, int rating, String comment) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be 1-5");
        if (dao.findBySale(saleId) != null) throw new IllegalStateException("This order has already been reviewed");
        return dao.create(new Review(saleId, customerId, sellerId, rating,
                comment == null ? "" : comment.trim()));
    }

    public Review findBySale(int saleId)               { return dao.findBySale(saleId); }
    public List<Review> reviewsForSeller(int sellerId)  { return dao.findBySeller(sellerId); }
    public double averageRating(int sellerId)           { return dao.averageRating(sellerId); }
    public int countBySeller(int sellerId)              { return dao.countBySeller(sellerId); }
}

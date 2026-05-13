package com.matraknhash.model;

import java.time.LocalDateTime;

/**
 * In-app notification (the "bell" inbox). Cheap pub-sub: every state change
 * in the marketplace (order placed, listing approved, return requested, ...)
 * writes a row to {@code notifications} for the user who needs to know.
 * Plain POJO; the UI reloads via {@code NotificationDao.findUnread(userId)}.
 */
public class Notification {

    /** Stable identifiers for every kind of event the UI knows how to render. */
    public static final class Kind {
        public static final String ORDER_PLACED       = "ORDER_PLACED";
        public static final String ORDER_SELLER_ACK   = "ORDER_SELLER_ACK";
        public static final String ORDER_APPROVED     = "ORDER_APPROVED";
        public static final String ORDER_REJECTED     = "ORDER_REJECTED";
        public static final String LISTING_SUBMITTED  = "LISTING_SUBMITTED";
        public static final String LISTING_EMP_OK     = "LISTING_EMP_OK";
        public static final String LISTING_LIVE       = "LISTING_LIVE";
        public static final String LISTING_REJECTED   = "LISTING_REJECTED";
        public static final String RETURN_REQUESTED   = "RETURN_REQUESTED";
        public static final String RETURN_SELLER_ACK  = "RETURN_SELLER_ACK";
        public static final String RETURN_APPROVED    = "RETURN_APPROVED";
        public static final String SELLER_APPROVED    = "SELLER_APPROVED";
        public static final String SELLER_REJECTED    = "SELLER_REJECTED";
        private Kind() {}
    }

    private long    id;
    private int     userId;
    private String  kind;
    private String  body;
    private String  linkTarget;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(int userId, String kind, String body, String linkTarget) {
        this.userId = userId;
        this.kind = kind;
        this.body = body;
        this.linkTarget = linkTarget;
    }

    public long    getId()         { return id; }
    public int     getUserId()     { return userId; }
    public String  getKind()       { return kind; }
    public String  getBody()       { return body; }
    public String  getLinkTarget() { return linkTarget; }
    public boolean isRead()        { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(long id)              { this.id = id; }
    public void setUserId(int userId)       { this.userId = userId; }
    public void setKind(String kind)        { this.kind = kind; }
    public void setBody(String body)        { this.body = body; }
    public void setLinkTarget(String s)     { this.linkTarget = s; }
    public void setRead(boolean read)       { this.read = read; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}

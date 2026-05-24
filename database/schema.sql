-- Metrkansh ERP - MySQL / TiDB schema
-- Charset utf8mb4 so Arabic text and emoji-safe.

CREATE TABLE IF NOT EXISTS users (
    id            INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    active        TINYINT      NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN','EMPLOYEE','SELLER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS suppliers (
    id         INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    phone      VARCHAR(40),
    email      VARCHAR(120),
    address    VARCHAR(200),
    trusted    TINYINT      NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS parts (
    id          INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sku         VARCHAR(40)   NOT NULL UNIQUE,
    name        VARCHAR(160)  NOT NULL,
    category    VARCHAR(60),
    car_make    VARCHAR(60),
    car_model   VARCHAR(60),
    cost_price  DECIMAL(12,2) NOT NULL DEFAULT 0,
    sell_price  DECIMAL(12,2) NOT NULL DEFAULT 0,
    quantity    INT           NOT NULL DEFAULT 0,
    min_qty     INT           NOT NULL DEFAULT 5,
    supplier_id INT           NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_parts_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sales (
    id            INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seller_id     INT           NOT NULL,
    total         DECIMAL(12,2) NOT NULL,
    status        VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    approver_id   INT           NULL,
    approved_at   DATETIME      NULL,
    reject_reason VARCHAR(255)  NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sales_seller   FOREIGN KEY (seller_id)   REFERENCES users(id),
    CONSTRAINT fk_sales_approver FOREIGN KEY (approver_id) REFERENCES users(id),
    CONSTRAINT chk_sales_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- For databases created BEFORE the approval workflow existed, add the new columns.
-- These ALTERs are tolerated as no-ops on fresh installs (DatabaseBootstrap ignores
-- "duplicate column" errors).
ALTER TABLE sales ADD COLUMN status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING';
ALTER TABLE sales ADD COLUMN approver_id   INT          NULL;
ALTER TABLE sales ADD COLUMN approved_at   DATETIME     NULL;
ALTER TABLE sales ADD COLUMN reject_reason VARCHAR(255) NULL;
-- Treat any pre-existing rows as already-approved (so historical reports keep working).
UPDATE sales SET status='APPROVED' WHERE status IS NULL OR status='';

CREATE TABLE IF NOT EXISTS sale_items (
    id         INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sale_id    INT           NOT NULL,
    part_id    INT           NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    subtotal   DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_si_sale FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    CONSTRAINT fk_si_part FOREIGN KEY (part_id) REFERENCES parts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS purchases (
    id            INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    supplier_id   INT           NOT NULL,
    supplier_name VARCHAR(120)  NOT NULL,
    user_id       INT           NOT NULL,
    total         DECIMAL(12,2) NOT NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pur_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_pur_user     FOREIGN KEY (user_id)     REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS purchase_items (
    id          INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT           NOT NULL,
    part_id     INT           NOT NULL,
    quantity    INT           NOT NULL,
    unit_cost   DECIMAL(12,2) NOT NULL,
    subtotal    DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_pi_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE,
    CONSTRAINT fk_pi_part     FOREIGN KEY (part_id)     REFERENCES parts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_parts_sku        ON parts(sku);
CREATE INDEX idx_parts_name       ON parts(name);
CREATE INDEX idx_parts_qty        ON parts(quantity);
CREATE INDEX idx_sales_created    ON sales(created_at);
CREATE INDEX idx_sales_status     ON sales(status);
CREATE INDEX idx_sales_status_ts  ON sales(status, created_at);
CREATE INDEX idx_sale_items_sale  ON sale_items(sale_id);
CREATE INDEX idx_sale_items_part  ON sale_items(part_id);

-- =====================================================================
-- MARKETPLACE MIGRATION (M1)
-- Pivot the app from internal POS to a multi-tenant marketplace:
--   * users gains a CUSTOMER role + a status (PENDING_APPROVAL / ACTIVE / SUSPENDED)
--   * parts becomes "listings" owned by a seller, with their own approval pipeline
--   * sales becomes "orders" that can be PLACED by customers, SELLER_ACKed,
--     APPROVED by admin, then optionally RETURN_REQUESTED -> RETURNED.
-- Every ALTER below is idempotent (DatabaseBootstrap tolerates duplicate /
-- already-exists / not-found errors) so this block re-runs cleanly.
-- =====================================================================

-- The role check stops us from inserting CUSTOMER; widen the column instead.
ALTER TABLE users DROP CHECK chk_users_role;

-- Suppliers gain a "trusted" flag. Listings sourced from a trusted supplier
-- skip the employee/admin review and go straight to LIVE (logic added in M3);
-- right now this is just the storage + an admin-visible badge.
ALTER TABLE suppliers ADD COLUMN trusted TINYINT NOT NULL DEFAULT 0;
-- Out of the box we vouch for the five household-name brands seeded in seed.sql.
UPDATE suppliers SET trusted = 1
  WHERE name IN ('Bosch Egypt','Denso Distributors','NGK Spark Plugs Egypt','Mahle Filters','Brembo Brakes');

ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
-- Anyone in the table when we migrate is treated as fully active.
UPDATE users SET status='ACTIVE' WHERE status IS NULL OR status='';

-- Parts become listings -- they have an owner (seller) and their own approval state.
ALTER TABLE parts ADD COLUMN image_url             VARCHAR(255) NULL;
ALTER TABLE parts ADD COLUMN seller_id            INT          NULL;
ALTER TABLE parts ADD COLUMN listing_status       VARCHAR(20)  NOT NULL DEFAULT 'LIVE';
ALTER TABLE parts ADD COLUMN listing_reason       VARCHAR(255) NULL;
ALTER TABLE parts ADD COLUMN employee_reviewer_id INT          NULL;
ALTER TABLE parts ADD COLUMN admin_reviewer_id    INT          NULL;
ALTER TABLE parts ADD CONSTRAINT fk_parts_seller          FOREIGN KEY (seller_id)            REFERENCES users(id);
ALTER TABLE parts ADD CONSTRAINT fk_parts_employee_review FOREIGN KEY (employee_reviewer_id) REFERENCES users(id);
ALTER TABLE parts ADD CONSTRAINT fk_parts_admin_review    FOREIGN KEY (admin_reviewer_id)    REFERENCES users(id);
-- All seed parts get owned by the default seller (id=3) and stay LIVE.
UPDATE parts SET seller_id = 3      WHERE seller_id IS NULL;
UPDATE parts SET listing_status='LIVE' WHERE listing_status IS NULL OR listing_status='';

-- Sales become orders. Drop the narrow status check so we can store the
-- expanded state machine, and add the columns the new flow needs.
ALTER TABLE sales DROP CHECK chk_sales_status;
ALTER TABLE sales ADD COLUMN buyer_id            INT          NULL;
ALTER TABLE sales ADD COLUMN seller_ack_at       DATETIME     NULL;
ALTER TABLE sales ADD COLUMN return_reason       VARCHAR(255) NULL;
ALTER TABLE sales ADD COLUMN return_requested_at DATETIME     NULL;
ALTER TABLE sales ADD COLUMN return_approved_at  DATETIME     NULL;
ALTER TABLE sales ADD CONSTRAINT fk_sales_buyer FOREIGN KEY (buyer_id) REFERENCES users(id);

-- In-app notification inbox for sellers/customers/admins.
CREATE TABLE IF NOT EXISTS notifications (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    kind        VARCHAR(40)  NOT NULL,                 -- e.g. ORDER_PLACED, LISTING_APPROVED
    body        VARCHAR(500) NOT NULL,
    link_target VARCHAR(120) NULL,                     -- e.g. "sale:42" or "listing:7"
    is_read     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_parts_seller   ON parts(seller_id);
CREATE INDEX idx_parts_listing  ON parts(listing_status);
CREATE INDEX idx_sales_buyer    ON sales(buyer_id);
CREATE INDEX idx_notif_unread   ON notifications(user_id, is_read, created_at);

-- =====================================================================
-- SERVICE-CENTERS MIGRATION (M5)
-- Workshops/centres can sign up (role=SERVICE_CENTER), publish service
-- offers (oil change, brake job, ...), and customers can book those
-- services. Admin reviews offers before they go live.
-- =====================================================================
CREATE TABLE IF NOT EXISTS service_offers (
    id          INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    center_id   INT           NOT NULL,
    title       VARCHAR(160)  NOT NULL,
    description VARCHAR(500)  NULL,
    price       DECIMAL(12,2) NOT NULL DEFAULT 0,
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING_ADMIN',
    reason      VARCHAR(255)  NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_so_center FOREIGN KEY (center_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS service_requests (
    id           INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id  INT          NOT NULL,
    offer_id     INT          NOT NULL,
    vehicle_note VARCHAR(255) NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sr_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_sr_offer    FOREIGN KEY (offer_id)    REFERENCES service_offers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_so_center   ON service_offers(center_id);
CREATE INDEX idx_so_status   ON service_offers(status);
CREATE INDEX idx_sr_customer ON service_requests(customer_id);
CREATE INDEX idx_sr_offer    ON service_requests(offer_id);
CREATE INDEX idx_sr_status   ON service_requests(status);

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

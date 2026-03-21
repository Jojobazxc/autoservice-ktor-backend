CREATE TABLE clients
(
    id                BIGSERIAL PRIMARY KEY,
    full_name         VARCHAR(255) NOT NULL,
    phone             VARCHAR(20)  NOT NULL UNIQUE,
    email             VARCHAR(255),
    address           VARCHAR(255),
    registration_date TIMESTAMP    NOT NULL,
    status            VARCHAR(20)  NOT NULL
);

CREATE TABLE cars
(
    id           BIGSERIAL PRIMARY KEY,
    client_id    BIGINT       NOT NULL,
    brand        VARCHAR(100) NOT NULL,
    model        VARCHAR(100) NOT NULL,
    year         INTEGER,
    plate_number VARCHAR(20)  NOT NULL UNIQUE,
    vin          VARCHAR(50) UNIQUE,
    mileage      INTEGER,
    CONSTRAINT fk_cars_client
        FOREIGN KEY (client_id)
            REFERENCES clients (id)
            ON DELETE CASCADE,
    CONSTRAINT chk_cars_year
        CHECK (year IS NULL OR year BETWEEN 1900 AND 2100
) ,
    CONSTRAINT chk_cars_mileage
        CHECK (mileage IS NULL OR mileage >= 0)
);

CREATE TABLE masters
(
    id                BIGSERIAL PRIMARY KEY,
    full_name         VARCHAR(255) NOT NULL,
    specialization    VARCHAR(255),
    experience_years  INTEGER,
    phone             VARCHAR(20),
    email             VARCHAR(255),
    employment_status VARCHAR(20)  NOT NULL,
    CONSTRAINT chk_masters_experience_years
        CHECK (experience_years IS NULL OR experience_years >= 0)
);

CREATE TABLE services
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)   NOT NULL UNIQUE,
    description TEXT,
    base_price  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    norm_hours  NUMERIC(5, 2),
    CONSTRAINT chk_services_base_price
        CHECK (base_price >= 0),
    CONSTRAINT chk_services_norm_hours
        CHECK (norm_hours IS NULL OR norm_hours >= 0)
);

CREATE TABLE parts
(
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255)   NOT NULL,
    article        VARCHAR(100)   NOT NULL UNIQUE,
    price          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    unit           VARCHAR(50)    NOT NULL DEFAULT 'pcs',
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT chk_parts_price
        CHECK (price >= 0),
    CONSTRAINT chk_parts_stock_quantity
        CHECK (stock_quantity >= 0)
);

CREATE TABLE orders
(
    id                    BIGSERIAL PRIMARY KEY,
    client_id             BIGINT         NOT NULL,
    car_id                BIGINT         NOT NULL,
    master_id             BIGINT,
    description           TEXT,
    comment               TEXT,
    status                VARCHAR(20)    NOT NULL,
    created_at            TIMESTAMP      NOT NULL,
    planned_completion_at TIMESTAMP,
    completed_at          TIMESTAMP,
    total_amount          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_orders_client
        FOREIGN KEY (client_id)
            REFERENCES clients (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_orders_car
        FOREIGN KEY (car_id)
            REFERENCES cars (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_orders_master
        FOREIGN KEY (master_id)
            REFERENCES masters (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_orders_total_amount
        CHECK (total_amount >= 0)
);

CREATE TABLE order_services
(
    order_id       BIGINT         NOT NULL,
    service_id     BIGINT         NOT NULL,
    quantity       INTEGER        NOT NULL DEFAULT 1,
    price_at_order NUMERIC(10, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (order_id, service_id),
    CONSTRAINT fk_order_services_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_order_services_service
        FOREIGN KEY (service_id)
            REFERENCES services (id)
            ON DELETE RESTRICT,
    CONSTRAINT chk_order_services_quantity
        CHECK (quantity >= 1),
    CONSTRAINT chk_order_services_price_at_order
        CHECK (price_at_order >= 0)
);

CREATE TABLE order_parts
(
    order_id       BIGINT         NOT NULL,
    part_id        BIGINT         NOT NULL,
    quantity       INTEGER        NOT NULL DEFAULT 1,
    price_at_order NUMERIC(10, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (order_id, part_id),
    CONSTRAINT fk_order_parts_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_order_parts_part
        FOREIGN KEY (part_id)
            REFERENCES parts (id)
            ON DELETE RESTRICT,
    CONSTRAINT chk_order_parts_quantity
        CHECK (quantity >= 1),
    CONSTRAINT chk_order_parts_price_at_order
        CHECK (price_at_order >= 0)
);

CREATE TABLE payments
(
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT         NOT NULL,
    amount         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(20)    NOT NULL,
    payment_status VARCHAR(20)    NOT NULL,
    paid_at        TIMESTAMP,
    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE CASCADE,
    CONSTRAINT chk_payments_amount
        CHECK (amount >= 0)
);

CREATE INDEX idx_cars_client_id ON cars (client_id);
CREATE INDEX idx_orders_client_id ON orders (client_id);
CREATE INDEX idx_orders_car_id ON orders (car_id);
CREATE INDEX idx_orders_master_id ON orders (master_id);
CREATE INDEX idx_payments_order_id ON payments (order_id);
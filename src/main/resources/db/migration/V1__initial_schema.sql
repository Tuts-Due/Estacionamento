CREATE TABLE IF NOT EXISTS garage (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sector      VARCHAR(10)  NOT NULL UNIQUE,
    base_price  DOUBLE       NOT NULL,
    max_capacity INT         NOT NULL
);

CREATE TABLE IF NOT EXISTS spot (
    -- ID vem do simulador, não é auto-gerado
    id           BIGINT       PRIMARY KEY,
    sector       VARCHAR(10)  NOT NULL,
    lat          DOUBLE,
    lng          DOUBLE,
    is_occupied  BOOLEAN      NOT NULL DEFAULT FALSE,
    license_plate VARCHAR(20),
    INDEX idx_spot_sector_occupied (sector, is_occupied),
    INDEX idx_spot_lat_lng (lat, lng)
);

CREATE TABLE IF NOT EXISTS vehicle_event (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_plate VARCHAR(20)  NOT NULL,
    event_type    VARCHAR(20)  NOT NULL,
    entry_time    DATETIME,
    exit_time     DATETIME,
    lat           DOUBLE,
    lng           DOUBLE,
    INDEX idx_event_license (license_plate)
);

CREATE TABLE IF NOT EXISTS parking_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_plate VARCHAR(20)  NOT NULL,
    sector        VARCHAR(10)  NOT NULL,
    entry_time    DATETIME     NOT NULL,
    exit_time     DATETIME,
    spot_id       BIGINT,
    -- Preço por hora calculado no momento da entrada (com desconto/acréscimo dinâmico)
    price_per_hour DOUBLE      NOT NULL,
    -- Valor total cobrado — preenchido apenas na saída
    total_amount  DOUBLE,
    FOREIGN KEY (spot_id) REFERENCES spot(id),
    INDEX idx_record_license_exit (license_plate, exit_time),
    INDEX idx_record_sector_exit (sector, exit_time)
);

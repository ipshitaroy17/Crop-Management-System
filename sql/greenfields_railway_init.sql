-- GreenFields production schema for a NEW, EMPTY MySQL database.
-- Select the database named by MYSQLDATABASE before running this file.
-- This file creates schema only: it contains no DROP statements and no seed users.
-- It is intentionally not idempotent; if a table already exists, stop and inspect.

CREATE TABLE users (
    user_id    INT            NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)    NOT NULL,
    password   VARCHAR(100)   NOT NULL,
    full_name  VARCHAR(100)   NOT NULL,
    role       ENUM('admin','viewer') NOT NULL DEFAULT 'viewer',
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crops (
    crop_id              INT           NOT NULL AUTO_INCREMENT,
    crop_name            VARCHAR(100)  NOT NULL,
    crop_type            VARCHAR(50)   NOT NULL,
    variety              VARCHAR(100),
    description          TEXT,
    growth_duration_days INT,
    status               ENUM('active','inactive') NOT NULL DEFAULT 'active',

    CONSTRAINT pk_crops PRIMARY KEY (crop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seasons (
    season_id             INT            NOT NULL AUTO_INCREMENT,
    crop_id               INT            NOT NULL,
    season_name           VARCHAR(100)   NOT NULL,
    field_location        VARCHAR(150),
    area_acres            DECIMAL(8,2),
    planting_date         DATE,
    expected_harvest_date DATE,
    season_status         ENUM('planned','active','completed','failed') NOT NULL DEFAULT 'planned',
    notes                 TEXT,

    CONSTRAINT pk_seasons PRIMARY KEY (season_id),
    CONSTRAINT fk_season_crop
        FOREIGN KEY (crop_id) REFERENCES crops(crop_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fertilizer_applications (
    fertilizer_id    INT           NOT NULL AUTO_INCREMENT,
    season_id        INT           NOT NULL,
    fertilizer_name  VARCHAR(100)  NOT NULL,
    fertilizer_type  ENUM('chemical','organic','bio') NOT NULL,
    quantity_kg      DECIMAL(8,2)  NOT NULL,
    application_date DATE          NOT NULL,
    applied_by       VARCHAR(100),
    notes            TEXT,

    CONSTRAINT pk_fertilizer PRIMARY KEY (fertilizer_id),
    CONSTRAINT fk_fert_season
        FOREIGN KEY (season_id) REFERENCES seasons(season_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE irrigation_schedules (
    irrigation_id       INT            NOT NULL AUTO_INCREMENT,
    season_id           INT            NOT NULL,
    scheduled_date      DATE           NOT NULL,
    actual_date         DATE,
    method              ENUM('drip','sprinkler','flood','manual') NOT NULL,
    water_volume_litres DECIMAL(10,2),
    status              ENUM('scheduled','completed','skipped') NOT NULL DEFAULT 'scheduled',
    notes               TEXT,

    CONSTRAINT pk_irrigation PRIMARY KEY (irrigation_id),
    CONSTRAINT fk_irr_season
        FOREIGN KEY (season_id) REFERENCES seasons(season_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE harvest_records (
    harvest_id        INT            NOT NULL AUTO_INCREMENT,
    season_id         INT            NOT NULL,
    harvest_date      DATE           NOT NULL,
    expected_yield_kg DECIMAL(10,2)  NOT NULL,
    actual_yield_kg   DECIMAL(10,2),
    quality_grade     ENUM('A','B','C','reject'),
    remarks           TEXT,
    recorded_by       VARCHAR(100),

    CONSTRAINT pk_harvest PRIMARY KEY (harvest_id),
    CONSTRAINT fk_harvest_season
        FOREIGN KEY (season_id) REFERENCES seasons(season_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

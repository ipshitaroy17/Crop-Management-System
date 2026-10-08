-- ============================================================
-- Greenfields Agri Farm — Crop Monitoring System
-- Database Initialization Script
-- Phase 1 | greenfields_db.sql
-- ============================================================
-- Run this file in MySQL Workbench or via:
--   mysql -u root -p < greenfields_db.sql
-- ============================================================

-- ─── Step 1: Create & select the database ───────────────────

DROP DATABASE IF EXISTS greenfields_db;
CREATE DATABASE greenfields_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE greenfields_db;

-- ─── Step 2: Drop tables in reverse FK order (safe re-run) ──

DROP TABLE IF EXISTS harvest_records;
DROP TABLE IF EXISTS irrigation_schedules;
DROP TABLE IF EXISTS fertilizer_applications;
DROP TABLE IF EXISTS seasons;
DROP TABLE IF EXISTS crops;
DROP TABLE IF EXISTS users;

-- ────────────────────────────────────────────────────────────
-- TABLE: users
-- Purpose: Login credentials for session-based authentication
-- ────────────────────────────────────────────────────────────
CREATE TABLE users (
    user_id    INT            NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)    NOT NULL,
    password   VARCHAR(100)   NOT NULL,
    full_name  VARCHAR(100)   NOT NULL,
    role       ENUM('admin','viewer') NOT NULL DEFAULT 'viewer',
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_username UNIQUE (username)
);

-- ────────────────────────────────────────────────────────────
-- TABLE: crops
-- Purpose: Master list of crops grown on the farm
-- ────────────────────────────────────────────────────────────
CREATE TABLE crops (
    crop_id              INT           NOT NULL AUTO_INCREMENT,
    crop_name            VARCHAR(100)  NOT NULL,
    crop_type            VARCHAR(50)   NOT NULL,
    variety              VARCHAR(100),
    description          TEXT,
    growth_duration_days INT,
    status               ENUM('active','inactive') NOT NULL DEFAULT 'active',

    CONSTRAINT pk_crops PRIMARY KEY (crop_id)
);

-- ────────────────────────────────────────────────────────────
-- TABLE: seasons
-- Purpose: A planting season/block for a specific crop
-- ────────────────────────────────────────────────────────────
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
);

-- ────────────────────────────────────────────────────────────
-- TABLE: fertilizer_applications
-- Purpose: Fertilizer events applied during a season
-- ────────────────────────────────────────────────────────────
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
);

-- ────────────────────────────────────────────────────────────
-- TABLE: irrigation_schedules
-- Purpose: Planned and actual watering events per season
-- ────────────────────────────────────────────────────────────
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
);

-- ────────────────────────────────────────────────────────────
-- TABLE: harvest_records
-- Purpose: Harvest event with expected vs actual yield
-- Both yield columns live here — single-table comparison query
-- ────────────────────────────────────────────────────────────
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
);


-- ============================================================
-- DEMO DATA
-- ============================================================

-- ─── Users ──────────────────────────────────────────────────

INSERT INTO users (username, password, full_name, role) VALUES
    ('admin',  'admin123',  'Farm Administrator', 'admin'),
    ('ravi',   'ravi123',   'Ravi Kumar',          'viewer'),
    ('priya',  'priya123',  'Priya Nair',           'viewer');


-- ─── Crops ──────────────────────────────────────────────────

INSERT INTO crops (crop_name, crop_type, variety, description, growth_duration_days, status) VALUES
    (
        'Paddy',
        'Grain',
        'IR-64',
        'High-yielding semi-dwarf rice variety. Suitable for irrigated lowland conditions. Resistant to common pests.',
        120,
        'active'
    ),
    (
        'Maize',
        'Grain',
        'Sweet Corn Hybrid',
        'Hybrid sweet corn variety with high sugar content. Grows well in well-drained loamy soil.',
        90,
        'active'
    ),
    (
        'Tomato',
        'Vegetable',
        'Roma VF',
        'Determinate variety with firm, meaty fruit. Ideal for processing and fresh market. Good disease tolerance.',
        75,
        'active'
    );


-- ─── Seasons ────────────────────────────────────────────────
-- crop_id 1 = Paddy | crop_id 2 = Maize | crop_id 3 = Tomato

INSERT INTO seasons
    (crop_id, season_name, field_location, area_acres, planting_date, expected_harvest_date, season_status, notes)
VALUES
    (
        1, 'Kharif 2025', 'Block A – North Field', 5.00,
        '2025-06-15', '2025-10-13', 'completed',
        'Main rainy-season paddy crop. Transplanted seedlings. Field bunded and levelled before planting.'
    ),
    (
        2, 'Rabi 2025', 'Block B – East Field', 3.00,
        '2025-11-01', '2026-01-30', 'active',
        'Post-monsoon maize crop using stored soil moisture. Supplementary irrigation from borewell.'
    ),
    (
        3, 'Summer 2025', 'Greenhouse 1 – South Wing', 1.00,
        '2025-03-01', '2025-05-15', 'completed',
        'Protected cultivation under poly-house. Drip irrigation with fertigation programme.'
    );


-- ─── Fertilizer Applications ────────────────────────────────

-- Paddy – Kharif 2025 (season_id = 1)
INSERT INTO fertilizer_applications
    (season_id, fertilizer_name, fertilizer_type, quantity_kg, application_date, applied_by, notes)
VALUES
    (1, 'Urea',          'chemical', 50.00, '2025-06-20', 'Ravi Kumar', 'Basal dose applied 5 days after transplanting.'),
    (1, 'DAP',           'chemical', 30.00, '2025-06-20', 'Ravi Kumar', 'Basal phosphorus application.'),
    (1, 'Urea',          'chemical', 25.00, '2025-07-20', 'Ravi Kumar', 'First top-dressing at tillering stage.'),
    (1, 'Potash (MOP)',  'chemical', 20.00, '2025-08-10', 'Ravi Kumar', 'Applied at panicle initiation stage.'),
    (1, 'Vermicompost',  'organic',  80.00, '2025-06-15', 'Priya Nair', 'Soil amendment applied before transplanting.');

-- Maize – Rabi 2025 (season_id = 2)
INSERT INTO fertilizer_applications
    (season_id, fertilizer_name, fertilizer_type, quantity_kg, application_date, applied_by, notes)
VALUES
    (2, 'NPK 10-26-26',  'chemical', 40.00, '2025-11-05', 'Ravi Kumar', 'Basal dose at sowing.'),
    (2, 'Urea',          'chemical', 35.00, '2025-11-25', 'Ravi Kumar', 'Top-dressing at knee-high stage.'),
    (2, 'Zinc Sulphate', 'chemical', 10.00, '2025-11-05', 'Priya Nair', 'Micronutrient application for grain filling.'),
    (2, 'Compost',       'organic',  60.00, '2025-10-28', 'Priya Nair', 'Pre-sowing organic enrichment.');

-- Tomato – Summer 2025 (season_id = 3)
INSERT INTO fertilizer_applications
    (season_id, fertilizer_name, fertilizer_type, quantity_kg, application_date, applied_by, notes)
VALUES
    (3, 'NPK 19-19-19',    'chemical', 15.00, '2025-03-05', 'Priya Nair', 'Starter fertigation through drip.'),
    (3, 'Calcium Nitrate', 'chemical', 10.00, '2025-03-20', 'Priya Nair', 'To prevent blossom-end rot.'),
    (3, 'NPK 0-52-34',     'chemical', 12.00, '2025-04-10', 'Priya Nair', 'Bloom booster at flowering stage.'),
    (3, 'Panchagavya',     'bio',       5.00, '2025-03-15', 'Priya Nair', 'Bio-stimulant foliar spray.');


-- ─── Irrigation Schedules ───────────────────────────────────

-- Paddy – Kharif 2025 (season_id = 1)
INSERT INTO irrigation_schedules
    (season_id, scheduled_date, actual_date, method, water_volume_litres, status, notes)
VALUES
    (1, '2025-06-16', '2025-06-16', 'flood', 25000.00, 'completed', 'Initial flooding for puddling.'),
    (1, '2025-07-01', '2025-07-01', 'flood', 18000.00, 'completed', 'Maintain 3-5 cm standing water.'),
    (1, '2025-07-15', '2025-07-15', 'flood', 18000.00, 'completed', 'Mid-tillering water top-up.'),
    (1, '2025-08-01', '2025-08-01', 'flood', 15000.00, 'completed', 'Panicle initiation – field kept moist.'),
    (1, '2025-08-20', '2025-08-22', 'flood', 12000.00, 'completed', 'Delayed 2 days due to rain forecast.'),
    (1, '2025-09-10', '2025-09-10', 'flood', 10000.00, 'completed', 'Grain filling stage irrigation.'),
    (1, '2025-09-25', NULL,         'flood',  8000.00, 'skipped',   'Field drained for harvest preparation.');

-- Maize – Rabi 2025 (season_id = 2)
INSERT INTO irrigation_schedules
    (season_id, scheduled_date, actual_date, method, water_volume_litres, status, notes)
VALUES
    (2, '2025-11-02', '2025-11-02', 'sprinkler', 8000.00, 'completed', 'Pre-sowing irrigation.'),
    (2, '2025-11-15', '2025-11-15', 'sprinkler', 6000.00, 'completed', 'Germination support.'),
    (2, '2025-12-01', '2025-12-01', 'sprinkler', 7000.00, 'completed', 'Vegetative growth stage.'),
    (2, '2025-12-20', '2025-12-21', 'sprinkler', 7500.00, 'completed', 'Tasselling – critical water period.'),
    (2, '2026-01-05', NULL,         'sprinkler', 6000.00, 'scheduled', 'Grain filling – upcoming.'),
    (2, '2026-01-20', NULL,         'sprinkler', 5000.00, 'scheduled', 'Final irrigation before harvest.');

-- Tomato – Summer 2025 (season_id = 3)
INSERT INTO irrigation_schedules
    (season_id, scheduled_date, actual_date, method, water_volume_litres, status, notes)
VALUES
    (3, '2025-03-02', '2025-03-02', 'drip', 1200.00, 'completed', 'Transplanting day irrigation.'),
    (3, '2025-03-05', '2025-03-05', 'drip', 1000.00, 'completed', 'Establishment phase – daily drip.'),
    (3, '2025-03-12', '2025-03-12', 'drip', 1100.00, 'completed', 'Vegetative growth.'),
    (3, '2025-03-20', '2025-03-20', 'drip', 1300.00, 'completed', 'Pre-flowering boost.'),
    (3, '2025-04-01', '2025-04-01', 'drip', 1500.00, 'completed', 'Flowering – increased water demand.'),
    (3, '2025-04-15', '2025-04-15', 'drip', 1600.00, 'completed', 'Fruit set stage.'),
    (3, '2025-05-01', '2025-05-01', 'drip', 1400.00, 'completed', 'Fruit sizing stage.'),
    (3, '2025-05-10', '2025-05-10', 'drip', 1000.00, 'completed', 'Reduced before harvest to concentrate sugars.');


-- ─── Harvest Records ────────────────────────────────────────

-- Paddy – Kharif 2025 (completed, actual yield recorded)
INSERT INTO harvest_records
    (season_id, harvest_date, expected_yield_kg, actual_yield_kg, quality_grade, remarks, recorded_by)
VALUES
    (
        1, '2025-10-10', 5000.00, 4750.00, 'A',
        'Slight yield reduction due to minor leaf blast incidence in September. Overall good quality grain.',
        'Ravi Kumar'
    );

-- Tomato – Summer 2025 (completed, exceeded expectation)
INSERT INTO harvest_records
    (season_id, harvest_date, expected_yield_kg, actual_yield_kg, quality_grade, remarks, recorded_by)
VALUES
    (
        3, '2025-05-12', 2400.00, 2580.00, 'A',
        'Yield exceeded expectation due to optimised drip-fertigation and pest-free greenhouse environment.',
        'Priya Nair'
    );

-- Maize – Rabi 2025 (active season – actual yield pending)
INSERT INTO harvest_records
    (season_id, harvest_date, expected_yield_kg, actual_yield_kg, quality_grade, remarks, recorded_by)
VALUES
    (
        2, '2026-01-28', 3600.00, NULL, NULL,
        'Harvest not yet completed. Expected yield based on stand count and ear size field assessment.',
        'Ravi Kumar'
    );


-- ============================================================
-- VERIFICATION QUERIES (uncomment to test after running)
-- ============================================================

-- Row counts per table
-- SELECT 'users'                   AS tbl, COUNT(*) AS rows FROM users
-- UNION ALL
-- SELECT 'crops',                           COUNT(*)        FROM crops
-- UNION ALL
-- SELECT 'seasons',                         COUNT(*)        FROM seasons
-- UNION ALL
-- SELECT 'fertilizer_applications',         COUNT(*)        FROM fertilizer_applications
-- UNION ALL
-- SELECT 'irrigation_schedules',            COUNT(*)        FROM irrigation_schedules
-- UNION ALL
-- SELECT 'harvest_records',                 COUNT(*)        FROM harvest_records;

-- Expected vs Actual Yield report
-- SELECT
--     c.crop_name,
--     s.season_name,
--     h.expected_yield_kg,
--     h.actual_yield_kg,
--     CASE
--         WHEN h.actual_yield_kg IS NULL THEN 'Pending'
--         ELSE CONCAT(ROUND((h.actual_yield_kg / h.expected_yield_kg) * 100, 1), '%')
--     END AS achievement
-- FROM harvest_records h
-- JOIN seasons s ON s.season_id = h.season_id
-- JOIN crops   c ON c.crop_id   = s.crop_id;

-- ============================================================
-- END OF SCRIPT
-- ============================================================

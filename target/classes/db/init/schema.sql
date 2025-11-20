-- ================================================
-- Spring Boot OTP - Database Schema and Initial Data
-- ================================================
-- This script creates all necessary tables and initializes the database
-- with admin and user roles, authorities, and sample users.
--
-- NOTE: IDs are generated using Snowflake algorithm (Twitter's distributed
-- unique ID generation). Initial data uses pre-generated Snowflake-style IDs
-- to maintain consistency. New entities get IDs from the application.
-- Database AUTO_INCREMENT has been removed from ID columns.
-- ================================================

-- ================================================
-- 1. CREATE ROLE TABLE
-- ================================================
CREATE TABLE IF NOT EXISTS role (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    last_modified_date DATETIME,
    last_modified_by VARCHAR(50),
    INDEX idx_role_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 2. CREATE AUTHORITY TABLE
-- ================================================
CREATE TABLE IF NOT EXISTS authority (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    last_modified_date DATETIME,
    last_modified_by VARCHAR(50),
    INDEX idx_authority_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 3. CREATE USER TABLE
-- ================================================
CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT NOT NULL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_password_reset_date DATETIME NULL,
    is_otp_required BOOLEAN DEFAULT false,
    role_id BIGINT,
    authority_id BIGINT,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    last_modified_date DATETIME,
    last_modified_by VARCHAR(50),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_user_authority FOREIGN KEY (authority_id) REFERENCES authority(id),
    INDEX idx_user_username (username),
    INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 4. CREATE OTP AUDIT ENTRIES TABLE
-- ================================================
CREATE TABLE IF NOT EXISTS otp_audit_entries (
    id BIGINT NOT NULL PRIMARY KEY,
    issued_on DATE NOT NULL,
    expires_on DATE NOT NULL,
    partner_expiry VARCHAR(255),
    username VARCHAR(50) NOT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    last_modified_date DATETIME,
    last_modified_by VARCHAR(50),
    INDEX idx_otp_username (username),
    INDEX idx_otp_issued_on (issued_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 4.5. CREATE REFRESH TOKENS TABLE
-- ================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at DATETIME NULL,
    replaced_by_token VARCHAR(500) NULL,
    INDEX idx_refresh_token_user (user_id),
    INDEX idx_refresh_token_expires (expires_at),
    INDEX idx_refresh_token_token (token),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 5. INSERT ROLES
-- ================================================
INSERT IGNORE INTO role (id, name, description, created_date, created_by) VALUES
(1988213221802115073, 'ROLE_ADMIN', 'Administrator role with full access to all features', CURRENT_TIMESTAMP, 'system'),
(1988213221802115074, 'ROLE_USER', 'Standard user role with limited access to features', CURRENT_TIMESTAMP, 'system');

-- ================================================
-- 6. INSERT AUTHORITIES
-- ================================================
INSERT IGNORE INTO authority (id, name, description, created_date, created_by) VALUES
(1988213221802115075, 'ADMIN', 'Administrator authority', CURRENT_TIMESTAMP, 'system'),
(1988213221802115076, 'USER', 'User authority', CURRENT_TIMESTAMP, 'system');

-- ================================================
-- 7. INSERT SAMPLE ADMIN USER
-- ================================================
INSERT IGNORE INTO user_account (
    id, username, password, first_name, last_name, email, enabled, status,
    last_password_reset_date, is_otp_required, role_id, authority_id,
    created_date, created_by
) SELECT
    1988213221802115077,
    'admin',
    '$2y$10$/CRi5UMb21iS5oQ98e8uIObrwQgu2ovKaWDw2xNd0i052.FhYMc6W',
    'System',
    'Administrator',
    'frederickraghul@gmail.com',
    true,
    'ACTIVE',
    NULL,
    false,
    (SELECT id FROM role WHERE name = 'ROLE_ADMIN' LIMIT 1),
    (SELECT id FROM authority WHERE name = 'ADMIN' LIMIT 1),
    CURRENT_TIMESTAMP,
    'system'
WHERE NOT EXISTS (SELECT 1 FROM user_account WHERE username = 'admin');

-- ================================================
-- 8. INSERT SAMPLE USER
-- ================================================
INSERT IGNORE INTO user_account (
    id, username, password, first_name, last_name, email, enabled, status,
    last_password_reset_date, is_otp_required, role_id, authority_id,
    created_date, created_by
) SELECT
    1988213221802115078,
    'rahulvijay',
    '$2y$10$qi.33noj4UCq..k4DYNgGewalolmmG1pKOGOyanWopSvNzsuYuNpS',
    'Rahul',
    'Vijay',
    'frederickraghul@gmail.com',
    true,
    'ACTIVE',
    NULL,
    false,
    (SELECT id FROM role WHERE name = 'ROLE_USER' LIMIT 1),
    (SELECT id FROM authority WHERE name = 'USER' LIMIT 1),
    CURRENT_TIMESTAMP,
    'system'
WHERE NOT EXISTS (SELECT 1 FROM user_account WHERE username = 'rahulvijay');

-- ================================================
-- 9. INSERT SAMPLE USER (ROLE_USER)
-- ================================================
INSERT IGNORE INTO user_account (
    id, username, password, first_name, last_name, email, enabled, status,
    last_password_reset_date, is_otp_required, role_id, authority_id,
    created_date, created_by
) SELECT
    1988213221802115079,
    'rameez',
    '$2y$10$qi.33noj4UCq..k4DYNgGewalolmmG1pKOGOyanWopSvNzsuYuNpS',
    'Test',
    'User',
    'frederickraghul@gmail.com',
    true,
    'ACTIVE',
    NULL,
    false,
    (SELECT id FROM role WHERE name = 'ROLE_USER' LIMIT 1),
    (SELECT id FROM authority WHERE name = 'USER' LIMIT 1),
    CURRENT_TIMESTAMP,
    'system'
WHERE NOT EXISTS (SELECT 1 FROM user_account WHERE username = 'rameez');
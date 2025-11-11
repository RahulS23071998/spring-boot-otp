-- ================================================
-- Spring Boot OTP - User Query Examples
-- ================================================
-- This file contains useful SQL queries for managing and querying users
-- in the Spring Boot OTP application database.
-- ================================================

-- ================================================
-- 1. GET ALL USERS WITH THEIR ROLES AND AUTHORITIES
-- ================================================
SELECT
    u.id,
    u.username,
    u.first_name,
    u.last_name,
    u.email,
    u.enabled,
    u.status,
    u.is_otp_required,
    u.last_password_reset_date,
    r.name as role_name,
    a.name as authority_name,
    u.created_date,
    u.created_by
FROM user u
LEFT JOIN role r ON u.role_id = r.id
LEFT JOIN authority a ON u.authority_id = a.id
ORDER BY u.id;

-- ================================================
-- 2. GET ACTIVE USERS ONLY
-- ================================================
SELECT
    u.id,
    u.username,
    u.first_name,
    u.last_name,
    u.email,
    r.name as role_name,
    a.name as authority_name
FROM user u
LEFT JOIN role r ON u.role_id = r.id
LEFT JOIN authority a ON u.authority_id = a.id
WHERE u.enabled = true
  AND u.status = 'ACTIVE'
ORDER BY u.username;

-- ================================================
-- 3. GET ADMIN USERS
-- ================================================
SELECT
    u.id,
    u.username,
    u.first_name,
    u.last_name,
    u.email,
    u.enabled,
    u.status
FROM user u
LEFT JOIN role r ON u.role_id = r.id
WHERE r.name = 'ROLE_ADMIN'
  AND u.enabled = true
ORDER BY u.username;

-- ================================================
-- 4. GET USERS WITH OTP REQUIRED
-- ================================================
SELECT
    u.id,
    u.username,
    u.first_name,
    u.last_name,
    u.email,
    u.is_otp_required
FROM user u
WHERE u.is_otp_required = true
ORDER BY u.username;

-- ================================================
-- 5. COUNT USERS BY ROLE
-- ================================================
SELECT
    r.name as role_name,
    COUNT(u.id) as user_count
FROM role r
LEFT JOIN user u ON r.id = u.role_id
GROUP BY r.id, r.name
ORDER BY r.name;

-- ================================================
-- 6. COUNT USERS BY STATUS
-- ================================================
SELECT
    status,
    COUNT(*) as user_count
FROM user
GROUP BY status
ORDER BY status;

-- ================================================
-- 7. FIND USER BY USERNAME
-- ================================================
SELECT
    u.id,
    u.username,
    u.first_name,
    u.last_name,
    u.email,
    u.enabled,
    u.status,
    u.is_otp_required,
    r.name as role_name,
    a.name as authority_name
FROM user u
LEFT JOIN role r ON u.role_id = r.id
LEFT JOIN authority a ON u.authority_id = a.id
WHERE u.username = 'admin';  -- Replace with desired username

-- ================================================
-- 8. FIND USER BY EMAIL
-- ================================================
SELECT
    u.id,
    u.username,
    u.first_name,
    u.last_name,
    u.email,
    u.enabled,
    u.status,
    r.name as role_name
FROM user u
LEFT JOIN role r ON u.role_id = r.id
WHERE u.email = 'admin@example.com';  -- Replace with desired email

-- ================================================
-- 9. GET USERS WHOSE PASSWORD WAS RESET RECENTLY
-- ================================================
SELECT
    u.id,
    u.username,
    u.first_name,
    u.last_name,
    u.email,
    u.last_password_reset_date
FROM user u
WHERE u.last_password_reset_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY u.last_password_reset_date DESC;

-- ================================================
-- 10. GET OTP AUDIT ENTRIES FOR A SPECIFIC USER
-- ================================================
SELECT
    oae.id,
    oae.issued_on,
    oae.expires_on,
    oae.partner_expiry,
    oae.username,
    oae.created_date
FROM otp_audit_entries oae
WHERE oae.username = 'admin'  -- Replace with desired username
ORDER BY oae.created_date DESC
LIMIT 10;

-- ================================================
-- 11. GET RECENT OTP AUDIT ENTRIES
-- ================================================
SELECT
    oae.id,
    oae.issued_on,
    oae.expires_on,
    oae.username,
    oae.created_date
FROM otp_audit_entries oae
WHERE oae.created_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY oae.created_date DESC;

-- ================================================
-- 12. COUNT OTP ENTRIES BY USER
-- ================================================
SELECT
    username,
    COUNT(*) as otp_count
FROM otp_audit_entries
GROUP BY username
ORDER BY otp_count DESC;

-- ================================================
-- 13. DISABLE A USER (UPDATE STATUS)
-- ================================================
-- UPDATE user SET enabled = false, status = 'INACTIVE' WHERE id = 1;
-- Replace 1 with the actual user ID

-- ================================================
-- 14. ENABLE OTP FOR A USER
-- ================================================
-- UPDATE user SET is_otp_required = true WHERE id = 1;
-- Replace 1 with the actual user ID

-- ================================================
-- 15. CHANGE USER ROLE
-- ================================================
-- UPDATE user SET role_id = (SELECT id FROM role WHERE name = 'ROLE_USER') WHERE id = 1;
-- Replace 1 with the actual user ID and 'ROLE_USER' with desired role

-- ================================================
-- End of User Query Examples
-- ================================================
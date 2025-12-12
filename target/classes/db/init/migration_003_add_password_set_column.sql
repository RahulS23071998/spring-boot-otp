-- ================================================
-- Migration: Add Password Set Column
-- Purpose: Track whether OAuth users have set their password
-- ================================================

-- Add password_set column to user_account table
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS password_set BOOLEAN DEFAULT true AFTER email_verified;

-- Set password_set to false for existing Google OAuth users (who need to set password)
UPDATE user_account SET password_set = false WHERE auth_type = 'GOOGLE_OAUTH';

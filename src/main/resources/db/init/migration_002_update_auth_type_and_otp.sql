-- ================================================
-- Migration: Update AuthType and OTP Requirements
-- Purpose: Rename USERNAME_PASSWORD to WEB_SIGNUP and set OTP as required by default
-- ================================================

-- Add email_verified column if it doesn't exist
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT false AFTER google_id;

-- Update auth_type default value from USERNAME_PASSWORD to WEB_SIGNUP
ALTER TABLE user_account MODIFY COLUMN auth_type VARCHAR(20) DEFAULT 'WEB_SIGNUP';

-- Update existing users with USERNAME_PASSWORD to WEB_SIGNUP
UPDATE user_account SET auth_type = 'WEB_SIGNUP' WHERE auth_type = 'USERNAME_PASSWORD' OR auth_type IS NULL;

-- Update is_otp_required default to true (for new web signup users)
-- Note: Existing users keep their current OTP requirement setting
ALTER TABLE user_account MODIFY COLUMN is_otp_required BOOLEAN DEFAULT true;

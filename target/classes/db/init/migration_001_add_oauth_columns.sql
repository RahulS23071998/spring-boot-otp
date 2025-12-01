-- ================================================
-- Migration: Add OAuth Authentication Support
-- Purpose: Add columns for Google OAuth authentication
-- ================================================

-- Add auth_type column to user_account table
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS auth_type VARCHAR(20) DEFAULT 'USERNAME_PASSWORD' AFTER is_otp_required;

-- Add google_id column to user_account table (for storing Google's unique ID)
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) NULL UNIQUE AFTER auth_type;

-- Create index on google_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_user_google_id ON user_account(google_id);

-- Update existing users to have USERNAME_PASSWORD auth type
UPDATE user_account SET auth_type = 'USERNAME_PASSWORD' WHERE auth_type IS NULL OR auth_type = 'USERNAME_PASSWORD';

-- Fix ip_address column type from inet to varchar for flexibility
-- This allows null values and string IP addresses without strict inet validation

ALTER TABLE auth_audit_log
ALTER COLUMN ip_address TYPE VARCHAR(45);

-- Add comment
COMMENT ON COLUMN auth_audit_log.ip_address IS 'IP address of the client (IPv4 or IPv6), nullable';

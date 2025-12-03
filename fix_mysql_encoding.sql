-- ================================================
-- FIX MYSQL CHARACTER ENCODING FOR DEVANAGARI
-- Run this if you see ??? instead of Hindi/Bhojpuri text
-- ================================================

-- Step 1: Drop existing database if it has wrong encoding
DROP DATABASE IF EXISTS bhojpuri_billa;

-- Step 2: Create database with correct UTF-8MB4 encoding
CREATE DATABASE bhojpuri_billa
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Step 3: Use the database
USE bhojpuri_billa;

-- Step 4: Create translations table with UTF-8MB4
CREATE TABLE translations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  audio_file_path VARCHAR(500) NOT NULL,
  audio_file_size BIGINT,
  english_text TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  bhojpuri_text TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  tts_file_path VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_created_at (created_at),
  INDEX idx_audio_path (audio_file_path(255))
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- Step 5: Create usage_stats table
CREATE TABLE usage_stats (
  id INT AUTO_INCREMENT PRIMARY KEY,
  date DATE NOT NULL,
  total_recordings INT DEFAULT 0,
  total_translations INT DEFAULT 0,
  total_audio_size BIGINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY unique_date (date)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- Step 6: Set session character set
SET NAMES 'utf8mb4';
SET CHARACTER SET utf8mb4;
SET character_set_connection=utf8mb4;

-- Step 7: Insert test data with Devanagari
INSERT INTO translations 
  (audio_file_path, audio_file_size, english_text, bhojpuri_text, tts_file_path) 
VALUES 
  ('test_audio.wav', 12345, 'Hello, how are you?', 'नमस्कार, कैसे बानी?', 'test_tts.mp3');

-- Step 8: Verify data is stored correctly
SELECT 
  id,
  english_text,
  bhojpuri_text,
  HEX(bhojpuri_text) as hex_check
FROM translations;

-- ================================================
-- EXPECTED OUTPUT:
-- You should see:
--   english_text: Hello, how are you?
--   bhojpuri_text: नमस्कार, कैसे बानी?
-- 
-- If you see ??? then check:
-- 1. MySQL client charset: SHOW VARIABLES LIKE 'character%';
-- 2. Terminal/console encoding
-- 3. MySQL Workbench connection settings
-- ================================================

-- Check current character set settings
SHOW VARIABLES LIKE 'character%';
SHOW VARIABLES LIKE 'collation%';

-- Verify table encoding
SELECT 
  table_name,
  table_collation 
FROM information_schema.tables 
WHERE table_schema = 'bhojpuri_billa';

SELECT 
  table_name,
  column_name,
  character_set_name,
  collation_name
FROM information_schema.columns
WHERE table_schema = 'bhojpuri_billa'
  AND table_name = 'translations';

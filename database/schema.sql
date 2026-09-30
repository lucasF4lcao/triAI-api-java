CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  email_verified_at TIMESTAMP NULL,
  password VARCHAR(255) NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  remember_token VARCHAR(100) NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS personal_access_tokens (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tokenable_type VARCHAR(255) NOT NULL,
  tokenable_id BIGINT UNSIGNED NOT NULL,
  name TEXT NOT NULL,
  token VARCHAR(64) NOT NULL UNIQUE,
  abilities TEXT NULL,
  last_used_at TIMESTAMP NULL,
  expires_at TIMESTAMP NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  INDEX personal_access_tokens_tokenable_idx (tokenable_type, tokenable_id),
  INDEX personal_access_tokens_expires_at_idx (expires_at)
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS triage_analyses (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  entry_type VARCHAR(255) NOT NULL,
  predicted_color VARCHAR(255) NOT NULL,
  confidence DECIMAL(5,4) NOT NULL,
  execution_status VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vital_signs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  triage_analysis_id BIGINT UNSIGNED NOT NULL,
  age SMALLINT UNSIGNED NULL,
  heart_rate SMALLINT UNSIGNED NULL,
  systolic_pressure SMALLINT UNSIGNED NULL,
  diastolic_pressure SMALLINT UNSIGNED NULL,
  oxygen_saturation TINYINT UNSIGNED NULL,
  temperature DECIMAL(4,1) NULL,
  respiratory_rate SMALLINT UNSIGNED NULL,
  pain TINYINT UNSIGNED NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  FOREIGN KEY (triage_analysis_id) REFERENCES triage_analyses (id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS changes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  triage_analysis_id BIGINT UNSIGNED NOT NULL,
  predicted_color VARCHAR(255) NOT NULL,
  final_color VARCHAR(255) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  FOREIGN KEY (triage_analysis_id) REFERENCES triage_analyses (id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4;

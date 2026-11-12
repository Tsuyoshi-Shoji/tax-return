CREATE TABLE evidence_drive_connection (
    id BIGINT NOT NULL AUTO_INCREMENT,
    app_user_id BIGINT NOT NULL,
    encrypted_refresh_token VARCHAR(2048) NOT NULL,
    folder_id VARCHAR(200) NOT NULL,
    granted_scope VARCHAR(500) NOT NULL,
    connected_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_evidence_drive_connection_user UNIQUE (app_user_id),
    CONSTRAINT fk_evidence_drive_connection_user FOREIGN KEY (app_user_id) REFERENCES app_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE evidence_file (
    id BIGINT NOT NULL AUTO_INCREMENT,
    retention_record_id BIGINT NOT NULL,
    storage_provider VARCHAR(20) NOT NULL,
    external_file_id VARCHAR(200) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    uploaded_at DATETIME NOT NULL,
    verification_status VARCHAR(20) NOT NULL,
    last_verified_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_evidence_file_record UNIQUE (retention_record_id),
    CONSTRAINT chk_evidence_file_provider CHECK (storage_provider IN ('GOOGLE_DRIVE')),
    CONSTRAINT chk_evidence_file_mime CHECK (mime_type IN ('application/pdf', 'image/jpeg', 'image/png')),
    CONSTRAINT chk_evidence_file_size CHECK (file_size > 0 AND file_size <= 10485760),
    CONSTRAINT chk_evidence_file_status CHECK (verification_status IN ('UPLOADED', 'OK', 'MISSING', 'TRASHED', 'MISMATCH')),
    CONSTRAINT fk_evidence_file_retention_record FOREIGN KEY (retention_record_id) REFERENCES retention_record (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

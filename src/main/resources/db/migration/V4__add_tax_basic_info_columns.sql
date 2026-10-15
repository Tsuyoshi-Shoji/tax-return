ALTER TABLE app_user
    ADD COLUMN phone VARCHAR(30) NULL,
    ADD COLUMN address VARCHAR(255) NULL;

ALTER TABLE business_profile
    ADD COLUMN industry VARCHAR(100) NULL;


ALTER TABLE fixed_asset
    ADD COLUMN life_basis VARCHAR(30) NOT NULL DEFAULT 'STATUTORY',
    ADD COLUMN statutory_useful_life INT NULL,
    ADD COLUMN used_elapsed_months INT NULL,
    ADD COLUMN used_requirements_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN private_use_conversion BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN rate_transition_confirmed BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE fixed_asset SET statutory_useful_life = useful_life WHERE useful_life >= 2;

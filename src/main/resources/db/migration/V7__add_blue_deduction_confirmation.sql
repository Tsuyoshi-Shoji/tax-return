ALTER TABLE settlement_adjustment
    ADD COLUMN blue_deduction_requirements_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN blue_deduction_65_method VARCHAR(30) NOT NULL DEFAULT 'UNCONFIRMED';

UPDATE settlement_adjustment
SET blue_deduction_65_method = 'NONE'
WHERE blue_deduction <> 65;

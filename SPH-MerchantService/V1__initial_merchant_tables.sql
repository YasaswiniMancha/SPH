CREATE TABLE merchants (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           merchant_code VARCHAR(50) NOT NULL UNIQUE,
                           business_name VARCHAR(100) NOT NULL,
                           business_description TEXT NOT NULL,
                           business_email VARCHAR(100) NOT NULL UNIQUE,
                           business_phone VARCHAR(20) NOT NULL,
                           business_address VARCHAR(255) NOT NULL,
                           business_city VARCHAR(50) NOT NULL,
                           business_state VARCHAR(50) NOT NULL,
                           business_zip_code VARCHAR(10) NOT NULL,
                           contact_person_name VARCHAR(100) NOT NULL,
                           contact_person_email VARCHAR(100) NOT NULL,
                           contact_person_phone VARCHAR(20) NOT NULL,
                           status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                           daily_transaction_limit NUMERIC(19, 2) NOT NULL DEFAULT 100000,
                           monthly_transaction_limit NUMERIC(19, 2) NOT NULL DEFAULT 5000000,
                           is_active BOOLEAN NOT NULL DEFAULT true,
                           tax_id VARCHAR(50),
                           registration_number VARCHAR(50),
                           bank_account_number VARCHAR(100),
                           bank_name VARCHAR(100),
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           verified_at TIMESTAMP,
                           rejected_reason TEXT
);

CREATE INDEX idx_merchant_code ON merchants(merchant_code);
CREATE INDEX idx_business_email ON merchants(business_email);
CREATE INDEX idx_status ON merchants(status);
CREATE INDEX idx_is_active ON merchants(is_active);
CREATE INDEX idx_created_at ON merchants(created_at DESC);
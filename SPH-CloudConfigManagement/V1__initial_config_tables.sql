CREATE TABLE configurations (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                service_name VARCHAR(100) NOT NULL,
                                environment VARCHAR(50) NOT NULL,
                                config_key VARCHAR(255) NOT NULL,
                                config_value TEXT,
                                description TEXT,
                                s3_object_key VARCHAR(500),
                                version BIGINT DEFAULT 1,
                                is_encrypted BOOLEAN DEFAULT false,
                                is_active BOOLEAN DEFAULT true,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT uk_config_unique UNIQUE(service_name, environment, config_key)
);

CREATE INDEX idx_service_env ON configurations(service_name, environment);
CREATE INDEX idx_config_key ON configurations(config_key);
CREATE INDEX idx_is_active ON configurations(is_active);

CREATE TABLE configuration_audits (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      config_id UUID NOT NULL REFERENCES configurations(id) ON DELETE CASCADE,
                                      action VARCHAR(50) NOT NULL,
                                      previous_value TEXT,
                                      new_value TEXT,
                                      modified_by VARCHAR(100) NOT NULL,
                                      modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      change_reason TEXT
);

CREATE INDEX idx_config_audit ON configuration_audits(config_id);
CREATE INDEX idx_modified_at ON configuration_audits(modified_at);
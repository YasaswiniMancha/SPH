CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               recipient_id VARCHAR(255) NOT NULL,
                               recipient_email VARCHAR(255) NOT NULL,
                               notification_type VARCHAR(50) NOT NULL,
                               subject VARCHAR(255) NOT NULL,
                               message TEXT NOT NULL,
                               html_content TEXT,
                               status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                               failure_reason TEXT,
                               retry_count INTEGER DEFAULT 0,
                               max_retries INTEGER DEFAULT 3,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               sent_at TIMESTAMP,
                               is_active BOOLEAN DEFAULT true,
                               version BIGINT DEFAULT 0
);

CREATE INDEX idx_recipient_id ON notifications(recipient_id);
CREATE INDEX idx_status ON notifications(status);
CREATE INDEX idx_created_at ON notifications(created_at DESC);
CREATE INDEX idx_is_active ON notifications(is_active);
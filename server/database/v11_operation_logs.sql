USE yunshang_market;

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id VARCHAR(20) NOT NULL,
    operator_name VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id VARCHAR(40) NOT NULL,
    target_name VARCHAR(150) NOT NULL,
    detail VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_operation_log_operator
        FOREIGN KEY (operator_id) REFERENCES sys_user (id),
    INDEX idx_operation_log_created (created_at),
    INDEX idx_operation_log_operator (operator_id),
    INDEX idx_operation_log_target (target_type, target_id)
) ENGINE = InnoDB;

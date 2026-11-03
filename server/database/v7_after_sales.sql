USE yunshang_market;

CREATE TABLE IF NOT EXISTS after_sale_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(40) NOT NULL UNIQUE,
    user_id VARCHAR(20) NOT NULL,
    merchant_id VARCHAR(20) NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    reason VARCHAR(300) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT '待商家处理',
    merchant_reply VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME,
    CONSTRAINT fk_after_sale_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_after_sale_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_after_sale_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
    CONSTRAINT chk_after_sale_type CHECK (request_type IN ('仅退款', '退货退款')),
    CONSTRAINT chk_after_sale_status
        CHECK (status IN ('待商家处理', '已同意', '已拒绝')),
    INDEX idx_after_sale_user (user_id, created_at),
    INDEX idx_after_sale_merchant (merchant_id, created_at),
    INDEX idx_after_sale_status (status, created_at)
) ENGINE = InnoDB;

INSERT IGNORE INTO after_sale_request
    (order_id, user_id, merchant_id, request_type, reason, description, status, created_at)
VALUES
    ('YS20261018001', 'u1', 'm1', '仅退款', '商品与描述不符',
     '收到的商品颜色与页面展示存在差异。', '待商家处理', '2026-10-30 10:20:00');

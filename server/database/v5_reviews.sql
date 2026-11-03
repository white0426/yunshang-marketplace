USE yunshang_market;

CREATE TABLE IF NOT EXISTS review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id VARCHAR(20) NOT NULL,
    product_id VARCHAR(40),
    order_id VARCHAR(40),
    review_type VARCHAR(20) NOT NULL,
    rating TINYINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    verified_purchase TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_review_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
    CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_review_type CHECK (review_type IN ('PRODUCT', 'MERCHANT')),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    INDEX idx_review_product (product_id, created_at),
    INDEX idx_review_merchant (merchant_id, created_at),
    INDEX idx_review_user (user_id)
) ENGINE = InnoDB;

INSERT INTO review
    (user_id, merchant_id, product_id, order_id, review_type, rating, content, verified_purchase, created_at)
SELECT * FROM (
    SELECT 'u2', 'm1', 'p1', NULL, 'PRODUCT', 5,
           '坐感很舒服，木色和家里的风格很搭，包装也很仔细。', 0, '2026-10-26 19:20:00' UNION ALL
    SELECT 'u1', 'm3', 'p4', 'YS20261023002', 'PRODUCT', 5,
           '跑起来回弹明显，通勤穿也不会觉得累。', 1, '2026-10-27 10:15:00' UNION ALL
    SELECT 'u2', 'm2', 'p2', NULL, 'PRODUCT', 5,
           '降噪效果和续航都符合预期，连接手机很快。', 0, '2026-10-25 21:08:00' UNION ALL
    SELECT 'u1', 'm1', NULL, 'YS20261018001', 'MERCHANT', 5,
           '商品描述准确，客服回复及时，整体购物体验很好。', 1, '2026-10-19 09:30:00' UNION ALL
    SELECT 'u2', 'm2', NULL, 'YS20261027003', 'MERCHANT', 5,
           '发货速度快，包装完整，有需要还会继续购买。', 1, '2026-10-28 14:05:00' UNION ALL
    SELECT 'u2', 'm1', NULL, 'YS20261027004', 'MERCHANT', 4,
           '商品选择比较丰富，希望后续能增加更多配件。', 1, '2026-10-29 16:40:00'
) seed_reviews
WHERE NOT EXISTS (SELECT 1 FROM review);

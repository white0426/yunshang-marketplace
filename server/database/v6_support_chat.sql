USE yunshang_market;

CREATE TABLE IF NOT EXISTS support_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    merchant_id VARCHAR(20) NOT NULL,
    last_message_at DATETIME,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_conversation_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_support_conversation_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
    UNIQUE KEY uk_support_conversation (user_id, merchant_id),
    INDEX idx_support_conversation_merchant (merchant_id, last_message_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS support_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id VARCHAR(20) NOT NULL,
    sender_role VARCHAR(20) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_message_conversation
        FOREIGN KEY (conversation_id) REFERENCES support_conversation (id) ON DELETE CASCADE,
    CONSTRAINT fk_support_message_sender FOREIGN KEY (sender_id) REFERENCES sys_user (id),
    CONSTRAINT chk_support_sender_role CHECK (sender_role IN ('USER', 'MERCHANT')),
    INDEX idx_support_message_conversation (conversation_id, created_at)
) ENGINE = InnoDB;

INSERT INTO support_conversation (user_id, merchant_id, last_message_at)
SELECT 'u1', 'm1', '2026-10-31 21:45:00'
WHERE EXISTS (SELECT 1 FROM sys_user WHERE id = 'u1')
  AND EXISTS (SELECT 1 FROM merchant WHERE id = 'm1')
ON DUPLICATE KEY UPDATE last_message_at = VALUES(last_message_at);

SET @demo_conversation_id = (
    SELECT id FROM support_conversation
    WHERE user_id = 'u1' AND merchant_id = 'm1'
    LIMIT 1
);

INSERT INTO support_message
    (conversation_id, sender_id, sender_role, content, created_at)
SELECT @demo_conversation_id, 'u1', 'USER',
       '你好，请问这款商品今天下单后多久可以发货？', '2026-10-31 21:42:00'
WHERE @demo_conversation_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM support_message
      WHERE conversation_id = @demo_conversation_id
  );

INSERT INTO support_message
    (conversation_id, sender_id, sender_role, content, created_at)
SELECT @demo_conversation_id, 'mu1', 'MERCHANT',
       '你好，现货商品通常会在 48 小时内安排发货。', '2026-10-31 21:45:00'
WHERE @demo_conversation_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM support_message
      WHERE conversation_id = @demo_conversation_id
        AND sender_role = 'MERCHANT'
  );

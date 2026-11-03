USE yunshang_market;

CREATE TABLE IF NOT EXISTS user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(30) NOT NULL,
    region VARCHAR(120) NOT NULL,
    detail VARCHAR(240) NOT NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    INDEX idx_address_user (user_id),
    INDEX idx_address_default (user_id, is_default)
) ENGINE = InnoDB;

INSERT INTO user_address
    (user_id, receiver_name, receiver_phone, region, detail, is_default)
SELECT 'u1', '林知夏', '137 3901 8827', '上海市 浦东新区', '示例路 18 号',
       NOT EXISTS (SELECT 1 FROM user_address WHERE user_id = 'u1')
WHERE EXISTS (SELECT 1 FROM sys_user WHERE id = 'u1');

UPDATE user_address
SET is_default = 1
WHERE user_id = 'u1'
  AND NOT EXISTS (
      SELECT 1 FROM (
          SELECT id FROM user_address WHERE user_id = 'u1' AND is_default = 1
      ) existing_default
  )
ORDER BY id
LIMIT 1;

CREATE DATABASE IF NOT EXISTS yunshang_market
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE yunshang_market;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS merchant_daily_stat;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS after_sale_request;
DROP TABLE IF EXISTS support_message;
DROP TABLE IF EXISTS support_conversation;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS user_address;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS merchant;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE merchant (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_name VARCHAR(50) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(30),
    location VARCHAR(50),
    description VARCHAR(500),
    banner VARCHAR(255),
    accent VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    review_note VARCHAR(500),
    reviewed_at DATETIME,
    rating DECIMAL(2, 1) NOT NULL DEFAULT 5.0,
    joined_at DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_merchant_status (status)
) ENGINE = InnoDB;
ALTER TABLE merchant ADD CONSTRAINT chk_merchant_status
    CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'DISABLED'));

CREATE TABLE sys_user (
    id VARCHAR(20) PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    phone VARCHAR(30),
    city VARCHAR(50),
    role VARCHAR(20) NOT NULL,
    member_level VARCHAR(30),
    avatar VARCHAR(10),
    merchant_id VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    token_version INT NOT NULL DEFAULT 0,
    joined_at DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
    INDEX idx_user_role (role),
    INDEX idx_user_status (status),
    INDEX idx_user_merchant (merchant_id)
) ENGINE = InnoDB;
ALTER TABLE sys_user
    ADD CONSTRAINT chk_user_role CHECK (role IN ('USER', 'MERCHANT', 'ADMIN')),
    ADD CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'DISABLED'));

CREATE TABLE operation_log (
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

CREATE TABLE support_conversation (
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

CREATE TABLE support_message (
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

CREATE TABLE user_address (
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

CREATE TABLE product (
    id VARCHAR(40) PRIMARY KEY,
    merchant_id VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(30) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    original_price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    sold INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    image VARCHAR(255),
    description VARCHAR(600),
    tags VARCHAR(255),
    created_at DATE NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
    INDEX idx_product_merchant (merchant_id),
    INDEX idx_product_category (category),
    INDEX idx_product_status (status)
) ENGINE = InnoDB;
ALTER TABLE product ADD CONSTRAINT chk_product_status CHECK (status IN ('ONLINE', 'OFFLINE'));

CREATE TABLE sys_role (
    id VARCHAR(20) PRIMARY KEY,
    role_code VARCHAR(30) NOT NULL UNIQUE,
    role_name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE = InnoDB;
CREATE TABLE sys_menu (
    id VARCHAR(30) PRIMARY KEY,
    permission_code VARCHAR(60) NOT NULL UNIQUE,
    menu_name VARCHAR(60) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE = InnoDB;

CREATE TABLE sys_user_role (
    user_id VARCHAR(20) NOT NULL,
    role_id VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE sys_role_menu (
    role_id VARCHAR(20) NOT NULL,
    menu_id VARCHAR(30) NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    FOREIGN KEY (menu_id) REFERENCES sys_menu (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE cart_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    product_id VARCHAR(40) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product (id),
    UNIQUE KEY uk_cart_user_product (user_id, product_id),
    INDEX idx_cart_user (user_id)
) ENGINE = InnoDB;

CREATE TABLE orders (
    id VARCHAR(40) PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL,
    merchant_id VARCHAR(20) NOT NULL,
    total DECIMAL(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_no VARCHAR(40),
    payment_expires_at DATETIME,
    paid_at DATETIME,
    shipping_company VARCHAR(80),
    tracking_no VARCHAR(80),
    shipped_at DATETIME,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(30) NOT NULL,
    receiver_address VARCHAR(300) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_order_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
    INDEX idx_order_user (user_id),
    INDEX idx_order_merchant (merchant_id),
    INDEX idx_order_status (status),
    INDEX idx_order_payment_no (payment_no),
    INDEX idx_order_created (created_at)
) ENGINE = InnoDB;
ALTER TABLE orders ADD CONSTRAINT chk_order_status CHECK (status IN ('待支付', '待发货', '待收货', '已完成', '已取消'));
ALTER TABLE orders ADD CONSTRAINT chk_order_tracking_no
    CHECK (tracking_no IS NULL OR CHAR_LENGTH(tracking_no) = 24);

CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(40) NOT NULL,
    product_id VARCHAR(40) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    INDEX idx_order_item_order (order_id)
) ENGINE = InnoDB;

CREATE TABLE review (
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

CREATE TABLE after_sale_request (
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

CREATE TABLE merchant_daily_stat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id VARCHAR(20) NOT NULL,
    stat_date DATE NOT NULL,
    sales_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    order_count INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_stat_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
    UNIQUE KEY uk_merchant_stat_date (merchant_id, stat_date)
) ENGINE = InnoDB;

INSERT INTO merchant
    (id, name, owner_name, email, phone, location, description, banner, accent, status, rating, joined_at)
VALUES
    ('m1', '山岚生活', '顾清', 'merchant@demo.com', '138 6608 2104', '杭州',
     '把自然的质感带进日常，专注家居、香氛与生活器物。', '/images/chair.jpg', '#56715f', 'ACTIVE', 4.9, '2024-03-18'),
    ('m2', '北岸数码', '程野', 'beian@demo.com', '139 8802 1456', '上海',
     '精选影像、音频与移动设备，让科技更接近日常。', '/images/headphones.jpg', '#315d7a', 'ACTIVE', 4.8, '2024-06-09'),
    ('m3', '轻野运动', '唐屿', 'qingye@demo.com', '136 4201 8870', '成都',
     '城市与户外之间的运动装备，轻量、耐用、不设边界。', '/images/sneaker.jpg', '#a44d3f', 'ACTIVE', 4.7, '2024-08-22');

INSERT INTO sys_user
    (id, email, password_hash, display_name, phone, city, role, member_level, avatar, merchant_id, status, joined_at)
VALUES
    ('a1', 'admin@demo.com', '$2b$10$g.6IvYHAK4DDZkXhMbbeEORcW/UrJzleY16iT5RWoQYNBeVZRNJC2',
     '平台运营中心', NULL, '上海', 'ADMIN', NULL, '管', NULL, 'ACTIVE', '2024-01-01'),
    ('u1', 'user@demo.com', '$2b$10$g.6IvYHAK4DDZkXhMbbeEORcW/UrJzleY16iT5RWoQYNBeVZRNJC2',
     '林知夏', '137 3901 8827', '上海', 'USER', '银卡会员', '林', NULL, 'ACTIVE', '2025-01-16'),
    ('u2', 'zhou@demo.com', '$2b$10$g.6IvYHAK4DDZkXhMbbeEORcW/UrJzleY16iT5RWoQYNBeVZRNJC2',
     '周予安', '158 2918 6602', '杭州', 'USER', '金卡会员', '周', NULL, 'ACTIVE', '2024-10-03'),
    ('u3', 'su@demo.com', '$2b$10$g.6IvYHAK4DDZkXhMbbeEORcW/UrJzleY16iT5RWoQYNBeVZRNJC2',
     '苏棠', '186 7204 1188', '成都', 'USER', '普通会员', '苏', NULL, 'DISABLED', '2025-06-11'),
    ('mu1', 'merchant@demo.com', '$2b$10$g.6IvYHAK4DDZkXhMbbeEORcW/UrJzleY16iT5RWoQYNBeVZRNJC2',
     '顾清', '138 6608 2104', '杭州', 'MERCHANT', NULL, '顾', 'm1', 'ACTIVE', '2024-03-18'),
    ('mu2', 'beian@demo.com', '$2b$10$g.6IvYHAK4DDZkXhMbbeEORcW/UrJzleY16iT5RWoQYNBeVZRNJC2',
     '程野', '139 8802 1456', '上海', 'MERCHANT', NULL, '程', 'm2', 'ACTIVE', '2024-06-09'),
    ('mu3', 'qingye@demo.com', '$2b$10$g.6IvYHAK4DDZkXhMbbeEORcW/UrJzleY16iT5RWoQYNBeVZRNJC2',
     '唐屿', '136 4201 8870', '成都', 'MERCHANT', NULL, '唐', 'm3', 'ACTIVE', '2024-08-22');

INSERT INTO product
    (id, merchant_id, name, category, price, original_price, stock, sold, status, image, description, tags, created_at)
VALUES
    ('p1', 'm2', 'AeroPods Pro 降噪耳机', '数码', 899.00, 1099.00, 128, 486, 'ONLINE',
     '/images/headphones.jpg', '自适应主动降噪，空间音频与 30 小时综合续航。', '主动降噪,空间音频', '2026-09-08'),
    ('p2', 'm2', 'Lumen X100 微单相机', '数码', 4299.00, 4699.00, 24, 138, 'ONLINE',
     '/images/camera.jpg', '轻量机身搭配高解析传感器，适合旅行与街头拍摄。', '4K 视频,轻量机身', '2026-08-21'),
    ('p3', 'm1', '折木休闲单椅', '家居', 1299.00, 1599.00, 36, 212, 'ONLINE',
     '/images/chair.jpg', '实木框架与柔软织物结合，为客厅留出一处安静角落。', '实木框架,手工织物', '2026-07-30'),
    ('p4', 'm3', 'Trace 城市跑鞋', '运动', 699.00, 899.00, 82, 638, 'ONLINE',
     '/images/sneaker.jpg', '回弹中底与透气鞋面，覆盖通勤、慢跑与日常训练。', '缓震回弹,透气网面', '2026-09-01'),
    ('p5', 'm3', 'Orbit 运动腕表', '运动', 1599.00, 1899.00, 45, 176, 'ONLINE',
     '/images/watch.jpg', '全天候运动记录与睡眠分析，续航最长可达 14 天。', '14 天续航,多运动模式', '2026-08-12'),
    ('p6', 'm1', '晨露修护精华', '美妆', 328.00, 398.00, 93, 816, 'ONLINE',
     '/images/skincare.jpg', '轻盈水感质地，适合换季期的日常保湿与屏障修护。', '温和修护,水感质地', '2026-09-14'),
    ('p7', 'm2', 'Nova 轻旗舰手机', '数码', 3299.00, 3699.00, 57, 352, 'ONLINE',
     '/images/phone.jpg', '高刷屏、长续航与旗舰影像系统，兼顾性能与握持手感。', '120Hz 高刷,长续航', '2026-06-25'),
    ('p8', 'm1', '落日氛围灯', '家居', 459.00, 529.00, 66, 294, 'OFFLINE',
     '/images/lamp.jpg', '无级调光与暖色光晕，为阅读和独处时间增加柔和层次。', '无级调光,暖色光', '2026-08-03');

INSERT INTO cart_item (user_id, product_id, quantity)
VALUES
    ('u1', 'p1', 1),
    ('u1', 'p6', 1);

INSERT INTO orders
    (id, user_id, merchant_id, total, status, receiver_name, receiver_phone, receiver_address, created_at)
VALUES
    ('YS20261018001', 'u1', 'm1', 1299.00, '已完成', '林知夏', '137 3901 8827', '上海市浦东新区示例路 18 号', '2026-10-18 14:26:00'),
    ('YS20261023002', 'u1', 'm3', 699.00, '待收货', '林知夏', '137 3901 8827', '上海市浦东新区示例路 18 号', '2026-10-23 09:42:00'),
    ('YS20261027003', 'u2', 'm2', 4299.00, '待发货', '周予安', '158 2918 6602', '杭州市西湖区示例街 26 号', '2026-10-27 19:08:00'),
    ('YS20261027004', 'u2', 'm1', 1115.00, '待发货', '周予安', '158 2918 6602', '杭州市西湖区示例街 26 号', '2026-10-27 21:16:00');

INSERT INTO order_item (order_id, product_id, quantity, price)
VALUES
    ('YS20261018001', 'p3', 1, 1299.00),
    ('YS20261023002', 'p4', 1, 699.00),
    ('YS20261027003', 'p2', 1, 4299.00),
    ('YS20261027004', 'p6', 2, 328.00),
    ('YS20261027004', 'p8', 1, 459.00);

INSERT INTO merchant_daily_stat (merchant_id, stat_date, sales_amount, order_count)
VALUES
    ('m1', '2026-10-23', 6800.00, 42),
    ('m1', '2026-10-24', 7600.00, 48),
    ('m1', '2026-10-25', 7200.00, 46),
    ('m1', '2026-10-26', 9100.00, 59),
    ('m1', '2026-10-27', 8800.00, 57),
    ('m1', '2026-10-28', 10400.00, 67),
    ('m1', '2026-10-29', 11600.00, 73),
    ('m2', '2026-10-23', 12600.00, 61),
    ('m2', '2026-10-24', 11800.00, 57),
    ('m2', '2026-10-25', 14300.00, 72),
    ('m2', '2026-10-26', 15200.00, 76),
    ('m2', '2026-10-27', 13800.00, 70),
    ('m2', '2026-10-28', 16900.00, 84),
    ('m2', '2026-10-29', 18200.00, 91),
    ('m3', '2026-10-23', 9200.00, 38),
    ('m3', '2026-10-24', 10100.00, 44),
    ('m3', '2026-10-25', 9600.00, 41),
    ('m3', '2026-10-26', 11200.00, 51),
    ('m3', '2026-10-27', 12500.00, 56),
    ('m3', '2026-10-28', 13400.00, 62),
    ('m3', '2026-10-29', 14800.00, 68);

INSERT IGNORE INTO merchant
    (id, name, owner_name, email, phone, location, description, banner, accent, status, rating, joined_at)
VALUES
    ('m4', '墨屿书房', '沈砚', 'moyu@demo.com', '137 2210 6608', '南京',
     '纸、墨与桌面器物，给阅读和书写的日常留出秩序。', '/images/books.jpg', '#6e5c4a', 'ACTIVE', 4.8, '2024-09-12'),
    ('m5', '南枝美研', '林栀', 'nanzhi@demo.com', '135 8806 2107', '广州',
     '从色彩到香气，寻找适合东方日常的轻盈美妆。', '/images/perfume.jpg', '#9b5f63', 'ACTIVE', 4.6, '2024-11-06'),
    ('m6', '佳禾厨房', '许禾', 'jiahe@demo.com', '136 5108 9924', '苏州',
     '耐用的锅具与餐厨器物，让每一次下厨更顺手。', '/images/cutting-board.jpg', '#927247', 'ACTIVE', 4.8, '2025-01-18');

INSERT IGNORE INTO sys_user
    (id, email, password_hash, display_name, phone, city, role, member_level, avatar, merchant_id, status, joined_at)
VALUES
    ('mu4', 'moyu@demo.com', '$2b$10$g.6IvYHAK4DDZkXhMbbeEORcW/UrJzleY16iT5RWoQYNBeVZRNJC2',
     '沈砚', '137 2210 6608', '南京', 'MERCHANT', NULL, '沈', 'm4', 'ACTIVE', '2024-09-12'),
    ('mu5', 'nanzhi@demo.com', '$2b$10$g.6IvYHAK4DDZkXhMbbeEORcW/UrJzleY16iT5RWoQYNBeVZRNJC2',
     '林栀', '135 8806 2107', '广州', 'MERCHANT', NULL, '林', 'm5', 'ACTIVE', '2024-11-06'),
    ('mu6', 'jiahe@demo.com', '$2b$10$g.6IvYHAK4DDZkXhMbbeEORcW/UrJzleY16iT5RWoQYNBeVZRNJC2',
     '许禾', '136 5108 9924', '苏州', 'MERCHANT', NULL, '许', 'm6', 'ACTIVE', '2025-01-18');

INSERT IGNORE INTO product
    (id, merchant_id, name, category, price, original_price, stock, sold, status, image, description, tags, created_at)
VALUES
    ('p9', 'm4', '山茶纸感手账本', '文具', 89.00, 109.00, 180, 342, 'ONLINE',
     '/images/notebook.jpg', '细腻米白纸张与平摊装订，适合日记、摘录和项目记录。', '100g 纸,平摊装订', '2026-10-02'),
    ('p10', 'm4', '一页之间成长书单', '图书', 168.00, 208.00, 76, 196, 'ONLINE',
     '/images/books.jpg', '围绕思考、写作与职业成长选出的五本入门读物。', '精选书单,成长阅读', '2026-09-22'),
    ('p11', 'm5', '暮色轻盈唇釉', '美妆', 219.00, 259.00, 88, 367, 'ONLINE',
     '/images/perfume.jpg', '轻薄显色不易拔干，适合通勤和日常妆容。', '轻薄显色,日常通勤', '2026-10-08'),
    ('p12', 'm5', '雪松琥珀香薰蜡烛', '家居', 168.00, 198.00, 124, 452, 'ONLINE',
     '/images/candle.jpg', '木质琥珀与柔和雪松气息，燃烧时间约 42 小时。', '木质香调,42 小时', '2026-09-18'),
    ('p13', 'm6', '赤陶珐琅炖锅', '厨具', 499.00, 599.00, 39, 188, 'ONLINE',
     '/images/dutch-oven.jpg', '铸铁锅身与珐琅内壁，适合炖煮、焖饭和烤箱料理。', '铸铁锅身,均匀蓄热', '2026-10-11'),
    ('p14', 'm6', '晨光便携榨汁杯', '厨具', 239.00, 289.00, 68, 264, 'ONLINE',
     '/images/blender.jpg', '随行杯设计搭配便携搅拌结构，适合早餐和办公室使用。', '便携随行,快速搅拌', '2026-09-27');

INSERT IGNORE INTO merchant_daily_stat (merchant_id, stat_date, sales_amount, order_count)
VALUES
    ('m4', '2026-10-23', 4200.00, 26),
    ('m4', '2026-10-24', 5100.00, 31),
    ('m4', '2026-10-25', 4800.00, 29),
    ('m4', '2026-10-26', 6300.00, 38),
    ('m4', '2026-10-27', 5900.00, 35),
    ('m4', '2026-10-28', 7100.00, 42),
    ('m4', '2026-10-29', 7600.00, 45),
    ('m5', '2026-10-23', 7600.00, 44),
    ('m5', '2026-10-24', 8200.00, 48),
    ('m5', '2026-10-25', 7900.00, 46),
    ('m5', '2026-10-26', 9100.00, 53),
    ('m5', '2026-10-27', 9800.00, 58),
    ('m5', '2026-10-28', 10200.00, 61),
    ('m5', '2026-10-29', 11100.00, 65),
    ('m6', '2026-10-23', 5300.00, 32),
    ('m6', '2026-10-24', 5800.00, 35),
    ('m6', '2026-10-25', 6200.00, 37),
    ('m6', '2026-10-26', 7200.00, 43),
    ('m6', '2026-10-27', 7600.00, 45),
    ('m6', '2026-10-28', 8100.00, 49),
    ('m6', '2026-10-29', 8900.00, 53);

INSERT IGNORE INTO sys_role (id, role_code, role_name, status)
VALUES
    ('r_admin', 'ADMIN', '超级管理员', 'ACTIVE'),
    ('r_merchant', 'MERCHANT', '商家', 'ACTIVE'),
    ('r_user', 'USER', '消费者', 'ACTIVE');

INSERT IGNORE INTO sys_menu (id, permission_code, menu_name, status)
VALUES
    ('mn_dashboard', 'dashboard:view', '数据看板', 'ACTIVE'),
    ('mn_product_view', 'product:view', '查看商品', 'ACTIVE'),
    ('mn_product_manage', 'product:manage', '商品管理', 'ACTIVE'),
    ('mn_order_view', 'order:view', '查看订单', 'ACTIVE'),
    ('mn_order_manage', 'order:manage', '订单处理', 'ACTIVE'),
    ('mn_user_manage', 'user:manage', '用户管理', 'ACTIVE'),
    ('mn_merchant_manage', 'merchant:manage', '商家管理', 'ACTIVE'),
    ('mn_cart_manage', 'cart:manage', '购物车', 'ACTIVE'),
    ('mn_profile_manage', 'profile:manage', '个人资料', 'ACTIVE');

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT id, 'r_admin' FROM sys_user WHERE role = 'ADMIN'
UNION ALL
SELECT id, 'r_merchant' FROM sys_user WHERE role = 'MERCHANT'
UNION ALL
SELECT id, 'r_user' FROM sys_user WHERE role = 'USER';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 'r_admin', id FROM sys_menu
UNION ALL
SELECT 'r_merchant', id FROM sys_menu
WHERE permission_code IN ('dashboard:view', 'product:manage', 'order:manage', 'profile:manage')
UNION ALL
SELECT 'r_user', id FROM sys_menu
WHERE permission_code IN ('dashboard:view', 'product:view', 'order:view', 'cart:manage', 'profile:manage');

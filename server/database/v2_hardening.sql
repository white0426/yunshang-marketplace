USE yunshang_market;

ALTER TABLE sys_user ADD COLUMN token_version INT NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN receiver_name VARCHAR(50) NOT NULL DEFAULT '演示用户' AFTER status,
    ADD COLUMN receiver_phone VARCHAR(30) NOT NULL DEFAULT '未填写' AFTER receiver_name,
    ADD COLUMN receiver_address VARCHAR(300) NOT NULL DEFAULT '演示地址' AFTER receiver_phone;

UPDATE orders o
JOIN sys_user u ON u.id = o.user_id
SET o.receiver_name = u.display_name,
    o.receiver_phone = COALESCE(u.phone, '未填写'),
    o.receiver_address = CONCAT(COALESCE(u.city, ''), '演示地址');

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

INSERT INTO sys_role VALUES
    ('r_admin', 'ADMIN', '超级管理员', 'ACTIVE'),
    ('r_merchant', 'MERCHANT', '商家', 'ACTIVE'),
    ('r_user', 'USER', '消费者', 'ACTIVE');

INSERT INTO sys_menu VALUES
    ('mn_dashboard', 'dashboard:view', '数据看板', 'ACTIVE'),
    ('mn_product_view', 'product:view', '查看商品', 'ACTIVE'),
    ('mn_product_manage', 'product:manage', '商品管理', 'ACTIVE'),
    ('mn_order_view', 'order:view', '查看订单', 'ACTIVE'),
    ('mn_order_manage', 'order:manage', '订单处理', 'ACTIVE'),
    ('mn_user_manage', 'user:manage', '用户管理', 'ACTIVE'),
    ('mn_merchant_manage', 'merchant:manage', '商家管理', 'ACTIVE'),
    ('mn_cart_manage', 'cart:manage', '购物车', 'ACTIVE'),
    ('mn_profile_manage', 'profile:manage', '个人资料', 'ACTIVE');

INSERT INTO sys_user_role (user_id, role_id)
SELECT id, 'r_admin' FROM sys_user WHERE role = 'ADMIN'
UNION ALL SELECT id, 'r_merchant' FROM sys_user WHERE role = 'MERCHANT'
UNION ALL SELECT id, 'r_user' FROM sys_user WHERE role = 'USER';

INSERT INTO sys_role_menu (role_id, menu_id) SELECT 'r_admin', id FROM sys_menu
UNION ALL SELECT 'r_merchant', id FROM sys_menu
WHERE permission_code IN ('dashboard:view', 'product:manage', 'order:manage', 'profile:manage')
UNION ALL SELECT 'r_user', id FROM sys_menu
WHERE permission_code IN ('dashboard:view', 'product:view', 'order:view', 'cart:manage', 'profile:manage');

ALTER TABLE product
    ADD CONSTRAINT chk_product_stock CHECK (stock >= 0),
    ADD CONSTRAINT chk_product_price CHECK (price > 0);

ALTER TABLE cart_item
    ADD CONSTRAINT chk_cart_quantity CHECK (quantity > 0);

USE yunshang_market;

ALTER TABLE merchant
    ADD COLUMN review_note VARCHAR(500) NULL AFTER status,
    ADD COLUMN reviewed_at DATETIME NULL AFTER review_note;

ALTER TABLE merchant DROP CHECK chk_merchant_status;
ALTER TABLE merchant
    ADD CONSTRAINT chk_merchant_status
    CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'DISABLED'));

INSERT IGNORE INTO merchant
    (id, name, owner_name, email, phone, location, description, banner, accent,
     status, review_note, reviewed_at, rating, joined_at)
VALUES
    ('m8', '竹间日用', '沈青禾', 'bamboo@demo.com', '139 5521 7088', '苏州',
     '专注自然材质与日常器物，主营家居、香氛和生活用品。',
     '/images/chair.jpg', '#6c7e60', 'PENDING', NULL, NULL, 5.0, '2026-11-02');

INSERT IGNORE INTO sys_user
    (id, email, password_hash, display_name, phone, city, role, member_level,
     avatar, merchant_id, status, joined_at)
VALUES
    ('mu8', 'bamboo@demo.com',
     '$2b$10$g.6IvYHAK4DDZkXhMbbeEORcW/UrJzleY16iT5RWoQYNBeVZRNJC2',
     '沈青禾', '139 5521 7088', '苏州', 'MERCHANT', NULL, '沈', 'm8', 'ACTIVE',
     '2026-11-02');

INSERT IGNORE INTO sys_user_role (user_id, role_id)
VALUES ('mu8', 'r_merchant');

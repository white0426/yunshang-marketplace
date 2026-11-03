USE yunshang_market;

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

USE yunshang_market;

UPDATE product
SET description = CASE id
    WHEN 'p_m1_tb_001' THEN '高效净化室内空气，适合新居除味、宠物家庭和日常空气管理。'
    WHEN 'p_m1_tb_002' THEN '高蓬松白鹅绒填充，全棉面料柔软亲肤，适合寒冷季节保暖。'
    WHEN 'p_m1_tb_003' THEN '乳胶与独立弹簧组合，兼顾承托、透气和静音睡眠体验。'
    WHEN 'p_m1_tb_004' THEN '大尺寸顶喷配合恒温控制，带来稳定舒适的淋浴体验。'
    WHEN 'p_m1_tb_005' THEN '宽幅庭院遮阳设计，适合露台、花园和户外休闲空间。'
    WHEN 'p_m1_tb_006' THEN '包裹感单人休闲椅，可摇动放松，适合客厅与阅读角。'
    WHEN 'p_m1_tb_007' THEN '桑蚕长丝填充，轻盈贴身，二合一结构适合四季使用。'
    WHEN 'p_m1_tb_008' THEN '蜂巢遮光结构，免打孔安装，兼顾隔热与卧室隐私。'
    WHEN 'p_m1_tb_009' THEN '全光谱照明设计，适合客厅和卧室，支持智能控制。'
    WHEN 'p_m1_tb_010' THEN '原木风橡木床体，带储物结构，兼顾卧室收纳与舒适睡眠。'
    WHEN 'p_m1_tb_011' THEN '轻盈面霜试用装，适合体验抗老紧致与日常修护。'
    WHEN 'p_m1_tb_012' THEN '防油防水面料，可擦手设计，适合日常厨房烹饪。'
    ELSE description
END
WHERE id LIKE 'p_m1_tb_%';

UPDATE product
SET description = CASE id
    WHEN 'p_tb_001' THEN '绒皮拼接的哈灵顿夹克，立领剪裁利落，适合秋冬叠穿。'
    WHEN 'p_tb_002' THEN '高克重针织面料，版型宽松，适合日常通勤与休闲搭配。'
    WHEN 'p_tb_003' THEN '复古学院风 V 领马甲，层次感强，适合叠穿。'
    WHEN 'p_tb_004' THEN '复古条纹长袖，色彩温暖，适合秋季日常穿搭。'
    WHEN 'p_tb_005' THEN '毛呢面料挺括保暖，复古轮廓适合秋冬外套搭配。'
    WHEN 'p_tb_006' THEN '棒球挂饰德训鞋，复古运动风格，适合日常休闲。'
    WHEN 'p_tb_007' THEN '日系连帽针织开衫，柔软舒适，适合换季叠穿。'
    WHEN 'p_tb_008' THEN '圆领短款小开衫，版型简洁，适合日常通勤。'
    WHEN 'p_tb_009' THEN '高性能游戏主机，适合大型游戏与高强度多任务使用。'
    WHEN 'p_tb_010' THEN '家庭 KTV 音响套装，适合客厅娱乐与聚会使用。'
    WHEN 'p_tb_011' THEN '高刷新率电竞显示器，画面流畅，适合竞技游戏。'
    WHEN 'p_tb_012' THEN '头戴式主动降噪耳机，音质清晰，适合通勤和长时间聆听。'
    WHEN 'p_tb_013' THEN '无线头戴耳机，轻量设计，适合日常音乐与移动使用。'
    WHEN 'p_tb_014' THEN '大光圈人像镜头，背景虚化自然，适合人像与街拍。'
    WHEN 'p_tb_015' THEN '高效空气净化器，适合新居除味和家庭空气管理。'
    WHEN 'p_tb_016' THEN '高蓬松白鹅绒冬被，柔软保暖，适合寒冷季节。'
    WHEN 'p_tb_017' THEN '天然乳胶与弹簧组合，提供稳定承托和舒适睡感。'
    WHEN 'p_tb_018' THEN '恒温淋浴花洒套装，大顶喷设计，提升日常沐浴体验。'
    WHEN 'p_tb_019' THEN '庭院遮阳伞，适合露台、花园和户外休闲区域。'
    WHEN 'p_tb_020' THEN '太空舱造型单人休闲椅，包裹感强，适合客厅放松。'
    WHEN 'p_tb_021' THEN '14K 金尖钢笔，书写顺滑，适合日常记录与收藏。'
    WHEN 'p_tb_022' THEN '世纪系列金尖钢笔，笔触细腻，适合书写与练字。'
    WHEN 'p_tb_023' THEN '词典笔学习扫描工具，适合查词、翻译和日常学习。'
    WHEN 'p_tb_024' THEN '英语复读机，支持跟读与听说练习，适合语言学习。'
    WHEN 'p_tb_025' THEN '初中物理实验器材套装，适合课堂演示与家庭实验。'
    WHEN 'p_tb_026' THEN '家用折叠跑步机，收纳方便，适合日常有氧训练。'
    WHEN 'p_tb_027' THEN '轻量冲锋衣，防风耐磨，适合户外徒步与通勤。'
    WHEN 'p_tb_028' THEN '防水徒步鞋，抓地稳定，适合户外行走与轻装登山。'
    WHEN 'p_tb_029' THEN '自动帐篷，搭建便捷，适合露营与户外短途出行。'
    WHEN 'p_tb_030' THEN '户外登山背包，分区合理，适合徒步和短途旅行。'
    WHEN 'p_tb_031' THEN '精华粉底液，妆感自然，适合日常通勤底妆。'
    WHEN 'p_tb_032' THEN '轻薄粉底液旅行装，便携易用，适合补妆与出行。'
    WHEN 'p_tb_033' THEN '水奶油质地粉底液，服帖滋润，适合干性与混合肤质。'
    WHEN 'p_tb_034' THEN '关于习惯养成的实用指南，适合日常自我提升。'
    WHEN 'p_tb_035' THEN '探讨快速与慢速思考的经典读物，适合认知与决策学习。'
    WHEN 'p_tb_036' THEN '专注提升深度工作能力的方法书，适合效率与自我管理。'
    WHEN 'p_tb_037' THEN '不粘涂层炒锅，受热均匀，适合家庭日常烹饪。'
    WHEN 'p_tb_038' THEN '硅胶厨具七件套，耐高温易清洁，满足日常厨房需求。'
    WHEN 'p_tb_039' THEN '实木砧板，纹理自然，适合切配与日常料理。'
    ELSE description
END
WHERE id LIKE 'p_tb_%';

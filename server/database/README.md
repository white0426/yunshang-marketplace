# MySQL 8.0 数据库

- 地址：`127.0.0.1:3307`
- 数据库：`yunshang_market`
- 项目专用用户：`yunshang_app`
- 本地开发密码：通过 `DB_PASSWORD` 环境变量配置

完整表结构和演示数据位于 `schema.sql`。重新执行该文件会删除现有业务表并恢复初始演示数据，因此不要在生产环境直接运行。

当前初始数据包含 6 家商家、14 件商品。`add_more_merchants.sql` 记录了新增墨屿书房、南枝美研和佳禾厨房三家商家的增量脚本。

主要数据表：

- `merchant`：商家
- `sys_user`：消费者、商家账号和超级管理员
- `product`：商品
- `cart_item`：购物车
- `user_address`：消费者收货地址簿
- `review`：商品评价与店铺评价
- `after_sale_request`：订单售后申请与商家处理结果
- `support_conversation`：用户与商家的客服会话
- `support_message`：客服聊天消息
- `orders`：订单主表
- `order_item`：订单明细
- `merchant_daily_stat`：商家每日经营统计

支付流程升级脚本为 `v3_payments.sql`，为订单增加支付单号、二维码有效期和支付时间。
收货地址脚本为 `v4_user_addresses.sql`，新增地址簿并为演示消费者补充默认地址。
评价脚本为 `v5_reviews.sql`，新增商品/店铺评价表和演示评价。
客服脚本为 `v6_support_chat.sql`，新增用户与商家双向聊天表和演示会话。
售后脚本为 `v7_after_sales.sql`，新增售后申请表和演示售后记录。
物流脚本为 `v8_order_logistics.sql`，为订单增加物流公司、运单号和发货时间。
运单号规范脚本为 `v9_tracking_codes.sql`，统一 24 位单号格式并增加长度校验。
商家审核脚本为 `v10_merchant_review.sql`，增加待审核/驳回状态、审核说明和审核时间。
操作审计脚本为 `v11_operation_logs.sql`，记录管理员关键写操作。
商品简介脚本为 `v12_product_descriptions.sql`，统一去除采集说明和来源网址。
`import-m1-taobao.sql` 记录了为山岚生活（m1）导入的 12 件淘宝公开推荐商品及来源链接。

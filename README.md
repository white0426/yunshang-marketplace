# 云商集多商家电商平台

当前版本已经接入本机 MySQL 8.0 和 Spring Boot 后端，前端不再使用 `localStorage` 保存商品、用户、购物车或订单。浏览器只保存 JWT 登录令牌。

## 运行环境

- 前端：Vue 3、Vite、ECharts
- 后端：Spring Boot 3、Spring Security、JWT、MyBatis-Plus
- 数据库：MySQL 8.0，`127.0.0.1:3307`
- Java：17，请自行配置 `JAVA_HOME`

## 启动

当前开发环境已经启动前后端。重新启动时，在项目根目录运行：

```powershell
.\start-all.ps1
```

访问：

- 登录页：`http://127.0.0.1:4173/login`
- 后端健康检查：`http://127.0.0.1:8080/api/health`

启动脚本会自动生成局域网访问地址。手机与电脑处于同一网络时，可通过该地址访问商城并扫描支付二维码；如无法访问，请允许 Node.js 通过 Windows 防火墙。

## 数据库

- 数据库：`yunshang_market`
- 项目用户：`yunshang_app`
- 本地开发密码：请通过 `DB_PASSWORD` 环境变量配置
- 建表与演示数据：`server/database/schema.sql`
- 增量安全与 RBAC：`server/database/v2_hardening.sql`
- 数据库说明：`server/database/README.md`

数据库表包括商家、账号、商品、购物车、订单、订单明细和商家每日经营统计。

当前初始数据包含 6 家商家和 14 件商品，新增商家为墨屿书房、南枝美研和佳禾厨房。

## 演示账号

点击登录页的演示账号会自动填入邮箱和密码 `123456`。

- 消费者：林知夏，`user@demo.com`
- 商家：山岚生活，`merchant@demo.com`
- 超级管理员：平台运营中心，`admin@demo.com`

登录成功后，后端根据 JWT 中的角色返回不同数据：

- 消费者只能访问商城、自己的购物车、个人资料和订单。
- 商家只能访问自己绑定的店铺、商品和订单。
- 超级管理员可以访问全部商家、商品、用户和平台订单。

## 已实现

消费者端：

- 游客可浏览多商家商品首页、分类搜索、商品和店铺详情
- 商品与店铺评价、评分汇总及已完成订单的已购评价
- 用户与商家双向客服会话，消息写入 MySQL 并定时同步
- 跨商家购物车、数量修改、数据库下单
- 收货地址新增、编辑、删除、默认地址和购物车选择
- 加购或购买时登录，登录后恢复页面并自动继续原操作
- 扫码支付二维码、支付状态轮询和支付完成页
- 待支付订单重新生成二维码
- 用户售后申请、商家同意/拒绝与管理员售后记录
- 商家发货填写物流公司与运单号，用户订单详情展示物流时间线
- 发货自动生成 24 位物流单号，前两位标识物流公司
- 商家入驻待审核、管理员通过/驳回，通过后才可登录后台
- 管理员操作日志，记录操作人、动作、对象、变更详情和时间
- 商品卡片使用简洁卖点简介，不展示采集来源或网址
- 消费者账户注册与商家入驻，注册后自动进入对应工作台
- 个人中心、订单记录、独立订单详情、再次购买、个人资料编辑

商家端：

- 销售额与订单趋势、热销商品、订单状态概览
- 商品新建、价格修改、上下架、库存和销量
- 本店订单状态处理、店铺资料编辑

超级管理员端：

- 平台经营概览、商家表现和全平台订单
- 商家启停用、商品改价与上下架、用户账号启停用

## 开发命令

后端构建：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f server\pom.xml package -DskipTests
```

前端构建：

```powershell
pnpm build
```

后端测试：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f server\pom.xml test
```

运行前请配置 `DB_USERNAME` 和 `DB_PASSWORD`。`start-all.ps1` 不再保存真实密码，并会在未提供 `JWT_SECRET` 时自动生成随机密钥。

PowerShell 示例：

```powershell
$env:DB_USERNAME = 'yunshang_app'
$env:DB_PASSWORD = '你的本地数据库密码'
.\start-all.ps1
```

验收材料：

- `docs/system-design.md`
- `docs/test-report.md`

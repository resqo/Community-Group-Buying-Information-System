# 社区团购管理信息系统

<p align="center">
  <strong>Community Group-Buy Management Information System</strong>
</p>

<p align="center">
  基于 Spring Boot + Vue 3 的前后端分离社区团购电商平台，支持多角色协作、拼团交易、智能数据分析与 AI 经营建议。
</p>

---

## 📖 目录

- [项目简介](#项目简介)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [功能模块](#功能模块)
- [数据库设计](#数据库设计)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [界面预览](#界面预览)
- [API 接口](#api-接口)
- [AI 智能特性](#ai-智能特性)
- [参考文献](#参考文献)

---

## 项目简介

社区团购管理信息系统是一个面向社区团购场景的全流程管理平台。系统以社区为单位，连接**普通用户、团长、商家、系统管理员**四种角色，提供从商品浏览、拼团下单、支付结算到自提核销的完整业务流程，并集成 **ECharts 可视化大屏**和 **Ollama 本地大模型**，为商家和管理员提供数据驱动的智能经营建议。

### 核心角色

| 角色 | 说明 | 主要功能 |
|---|---|---|
| 🧑 **普通用户 (USER)** | 社区居民消费者 | 浏览商品、参与拼团/单买、下单支付、查看订单、申请退款 |
| 🏠 **团长 (LEADER)** | 社区自提点负责人 | 管理自提点、核销取件码、查看社区团购进度 |
| 🏪 **商家 (MERCHANT)** | 商品供应商 | 商品上架/下架、查看销售数据大屏、接收 AI 经营建议 |
| ⚙️ **管理员 (ADMIN)** | 平台运营者 | 商品审核、用户管理、平台数据监控大屏、发送通知、运营建议 |

---

## 系统架构

```
┌──────────────────────────────────────────────────────────┐
│                    用户浏览器 (Browser)                    │
├──────────────────────────────────────────────────────────┤
│   Vue 3 前端 (Vite + Element Plus + ECharts + Axios)     │
│   ┌──────────┬──────────┬──────────┬──────────┐          │
│   │  用户端   │  团长端   │  商家端   │  管理端   │          │
│   │ UserHome │LeaderHome│MerchantHome│AdminHome │          │
│   └──────────┴──────────┴──────────┴──────────┘          │
└──────────────────────┬───────────────────────────────────┘
                       │ HTTP / JSON (RESTful API)
┌──────────────────────┴───────────────────────────────────┐
│         Spring Boot 后端 (Java 17 + Maven)                │
│   ┌──────────┬──────────┬──────────┬──────────┐          │
│   │Controller│ Service  │  Mapper  │  Entity  │          │
│   │  REST    │ Business │  MyBatis │   POJO   │          │
│   └──────────┴──────────┴──────────┴──────────┘          │
│   ┌──────────────────────────────────────────┐           │
│   │   AI Agent: OllamaService (qwen2.5:0.5b) │           │
│   └──────────────────────────────────────────┘           │
└──────────────────────┬───────────────────────────────────┘
                       │ JDBC
┌──────────────────────┴───────────────────────────────────┐
│              MySQL 数据库 (10 张核心表)                     │
└──────────────────────────────────────────────────────────┘
```

---

## 技术栈

### 前端

| 技术 | 版本 | 说明 |
|---|---|---|
| **Vue 3** | ^3.5 | 渐进式 JavaScript 框架，Composition API |
| **Vue Router** | ^5.0 | 官方路由管理器 |
| **Element Plus** | ^2.14 | 企业级 UI 组件库 |
| **ECharts** | ^5.6 | 数据可视化图表库 |
| **Axios** | ^1.16 | HTTP 客户端 |
| **Vite** | ^8.0 | 前端构建工具 |

### 后端

| 技术 | 版本 | 说明 |
|---|---|---|
| **Spring Boot** | 4.0.6 | Java 企业级开发框架 |
| **MyBatis** | 4.0.1 | 持久层 ORM 框架 |
| **MySQL** | 8.0+ | 关系型数据库 |
| **JWT** | — | 无状态用户认证 |
| **Lombok** | — | 简化 Java Bean 代码 |
| **Maven** | — | 项目构建与依赖管理 |
| **Java** | 17 | 编程语言 |

### AI / 数据分析

| 技术 | 说明 |
|---|---|
| **Ollama** | 本地大模型运行时 |
| **qwen2.5:0.5b** | 轻量级中文大语言模型，提供经营建议 |
| **ECharts** | 销售数据可视化大屏 |

---

## 功能模块

### 1. 用户认证与权限管理

- 统一登录页面，用户选择角色（USER / LEADER / MERCHANT / ADMIN）登录
- JWT Token 无状态认证
- 角色级别的页面路由隔离
- 账号状态管理（启用/禁用）

### 2. 商品管理

- 商品分类管理（新鲜果蔬、肉禽蛋奶、粮油副食、日用百货）
- 商家商品 CRUD（上架/下架）
- 管理员商品审核（通过/驳回）
- 商品支持三种价格：原价、拼团价、单买价

### 3. 拼团交易

- 商家发起拼团活动（设置成团人数、活动时间、免拼规则）
- 用户可开团（成为团长）或参团
- 支持免拼功能（团长可使用免拼次数直接成团）
- 拼团状态追踪（进行中 / 成功 / 失败）
- 活动到期自动处理

### 4. 订单与支付

- 下单类型：拼团购买 / 单独购买
- 订单状态流转：待支付 → 已支付 → 已发货 → 已完成
- 支付记录管理
- 取件码生成与团长核销
- 退款售后申请与审核

### 5. 自提点管理

- 团长管理自提点信息（地址、电话、营业时间）
- 用户下单选择自提点
- 取件码核销确认

### 6. 通知系统

- 管理员发送平台通知
- 通知类型分类（系统通知、促销活动、审核结果等）
- 已读/未读状态追踪

### 7. 📊 数据可视化大屏

#### 商家端
- **商品销量统计图** — 柱状图展示各商品销售数量，指导商家针对性进货
- **商品销售额统计图** — 展示各商品销售总额，辅助定价与促销决策
- **动态实时更新** — 用户购买行为变化时图表同步刷新
- **AI 经营建议** — 基于销售数据自动生成 3 条经营建议

#### 管理员端
- **全平台销售额总览** — 总交易额、订单数、付费用户数
- **商品销量排行图** — 全局热销商品 TOP 榜
- **商品销售额排行图** — 全局销售额排行
- **用户消费排行图** — 高价值用户识别
- **AI 运营建议** — 基于全平台数据生成促销活动、用户激励等运营建议

### 8. 🤖 AI 智能推荐

- **个性化商品推荐** — 基于用户历史购买行为推荐感兴趣商品
- **Ollama 本地大模型集成** — 调用 `qwen2.5:0.5b` 模型，保护数据隐私
- 商家经营建议：结合销量、库存、销售额数据
- 管理员运营建议：结合全平台交易数据

### 9. 用户免拼次数管理

- 管理员可为用户设置/调整免拼次数
- 用户可使用免拼次数直接成团，无需等待其他团员

---

## 数据库设计

系统共包含 **10 张核心表**，采用 InnoDB 引擎，UTF8MB4 字符集：

| 序号 | 表名 | 说明 | 核心字段 |
|---|---|---|---|
| 1 | `user` | 用户表 | user_id, username, password, role, free_group_count |
| 2 | `category` | 商品分类表 | category_id, category_name, parent_id, sort |
| 3 | `product` | 商品表 | product_id, merchant_id, category_id, 价格字段, status, audit_status |
| 4 | `group_activity` | 拼团活动表 | activity_id, product_id, group_size, 时间范围, free_group 配置 |
| 5 | `group_instance` | 拼团实例表 | group_id, activity_id, leader_user_id, current_count, status |
| 6 | `orders` | 订单表 | order_id, order_no, user_id, merchant_id, 金额字段, 状态字段 |
| 7 | `payment` | 支付记录表 | payment_id, order_id, pay_no, pay_method, pay_status |
| 8 | `pickup_point` | 自提点表 | pickup_point_id, leader_id, point_name, address, business_hours |
| 9 | `refund` | 退款售后表 | refund_id, order_id, refund_reason, refund_status |
| 10 | `notice` | 通知表 | notice_id, user_id, title, content, notice_type, read_status |

### ER 关系图

```
user ──< product ──< group_activity ──< group_instance ──< orders
  │                     │                                        │
  │                     │                                        ├── payment
  │                     │                                        └── refund
  ├──< pickup_point
  └──< notice
```

---

## 项目结构

```
community-group-buy/
├── README.md                          # 项目说明文档
├── docs/                              # 项目文档
│   ├── 社区团购管理信息系统_前后端服务实现文档.md
│   └── 参考文献.md
├── database/                          # 数据库脚本
│   └── community_group_buy.session.sql
├── backend/                           # 后端项目
│   └── community-group-buy-backend/
│       ├── pom.xml                    # Maven 配置
│       └── src/main/java/com/example/community_group_buy_backend/
│           ├── CommunityGroupBuyBackendApplication.java
│           ├── common/                # 通用类 (Result, Code, ExceptionHandler)
│           ├── config/                # 配置类 (CORS)
│           ├── controller/            # 控制器层
│           │   ├── AuthController.java
│           │   ├── UserController.java
│           │   ├── ProductController.java
│           │   ├── GroupBuyController.java
│           │   ├── OrderController.java
│           │   ├── PickupPointController.java
│           │   ├── AnalyticsController.java
│           │   ├── RecommendationController.java
│           │   └── HealthController.java
│           ├── service/               # 服务层接口
│           │   ├── AuthService.java
│           │   ├── UserService.java
│           │   ├── ProductService.java
│           │   ├── GroupBuyService.java
│           │   ├── OrderService.java
│           │   ├── PickupPointService.java
│           │   ├── AnalyticsService.java
│           │   └── RecommendationService.java
│           ├── service/impl/          # 服务层实现
│           │   ├── AuthServiceImpl.java
│           │   ├── UserServiceImpl.java
│           │   ├── ProductServiceImpl.java
│           │   ├── GroupBuyServiceImpl.java
│           │   ├── OrderServiceImpl.java
│           │   ├── PickupPointServiceImpl.java
│           │   ├── AnalyticsServiceImpl.java
│           │   ├── RecommendationServiceImpl.java
│           │   └── OllamaAgentService.java  # AI Agent 服务
│           ├── mapper/                # MyBatis Mapper 层
│           │   ├── UserMapper.java
│           │   ├── ProductMapper.java
│           │   ├── GroupBuyMapper.java
│           │   ├── OrderMapper.java
│           │   └── PickupPointMapper.java
│           ├── entity/                # 数据库实体
│           │   ├── User.java
│           │   ├── Product.java
│           │   ├── GroupBuy.java
│           │   ├── Order.java
│           │   ├── OrderItem.java
│           │   └── PickupPoint.java
│           ├── dto/                   # 数据传输对象
│           │   ├── LoginDTO.java
│           │   ├── RegisterDTO.java
│           │   ├── ProductDTO.java
│           │   └── OrderDTO.java
│           ├── vo/                    # 视图对象
│           │   ├── LoginVO.java
│           │   ├── UserVO.java
│           │   ├── ProductVO.java
│           │   ├── OrderVO.java
│           │   └── RecommendationVO.java
│           └── utils/                 # 工具类
│               └── JwtUtil.java
└── frontend/                          # 前端项目
    └── community-group-buy-frontend/
        ├── package.json               # Node.js 配置
        ├── vite.config.js             # Vite 构建配置
        ├── index.html
        └── src/
            ├── App.vue                # 根组件
            ├── style.css              # 全局样式
            ├── api/                   # API 请求层
            │   └── request.js         # Axios 封装
            ├── router/                # 路由配置
            │   └── index.js
            ├── components/            # 公共组件
            │   ├── HelloWorld.vue
            │   └── OperationToast.vue
            └── views/                 # 页面视图
                ├── login/
                │   └── LoginView.vue           # 统一登录页
                ├── user/
                │   └── UserHome.vue            # 用户端首页
                ├── leader/
                │   └── LeaderHome.vue          # 团长端首页
                ├── merchant/
                │   └── MerchantHome.vue        # 商家端首页 (含可视化大屏)
                └── admin/
                    └── AdminHome.vue           # 管理员端首页 (含全平台监控大屏)
```

---

## 快速开始

### 环境要求

| 依赖 | 版本要求 |
|---|---|
| JDK | 17+ |
| Maven | 3.6+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Ollama | (可选，AI 功能需要) |

### 1. 克隆项目

```bash
git clone <your-repo-url>
cd community-group-buy
```

### 2. 初始化数据库

在 MySQL 中执行数据库初始化脚本：

```bash
mysql -u root -p < database/community_group_buy.session.sql
```

该脚本会创建数据库 `isdemo`，建立 10 张核心表，并插入以下初始测试账号：

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin_1` | `123456` | 管理员 (ADMIN) |
| `leader_1` | `123456` | 团长 (LEADER) |
| `merchant_1` | `123456` | 商家 (MERCHANT) |
| `merchant_2` | `123456` | 商家 (MERCHANT) |
| `merchant_3` | `123456` | 商家 (MERCHANT) |
| `merchant_4` | `123456` | 商家 (MERCHANT) |
| `user_1` | `123456` | 普通用户 (USER) |
| `user_2` | `123456` | 普通用户 (USER) |
| `user_3` | `123456` | 普通用户 (USER) |

### 3. 配置后端

修改 `backend/community-group-buy-backend/src/main/resources/application.yml`（或 `application.properties`）中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/isdemo?useUnicode=true&characterEncoding=utf8mb4
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 4. 启动后端

```bash
cd backend/community-group-buy-backend
mvn spring-boot:run
```

后端服务默认运行在 `http://localhost:8080`。

### 5. 启动前端

```bash
cd frontend/community-group-buy-frontend
npm install
npm run dev
```

前端开发服务器默认运行在 `http://localhost:5173`。

### 6. (可选) 启用 AI 功能

如需使用 AI 经营建议功能，请安装并启动 [Ollama](https://ollama.com/)，然后拉取模型：

```bash
ollama pull qwen2.5:0.5b
```

---

## API 接口

### 认证模块

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/register` | 用户注册 |

### 用户模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/users/{id}` | 获取用户信息 |
| PUT | `/api/users/{id}` | 更新用户信息 |
| GET | `/api/admin/users` | 管理员获取用户列表 |
| PUT | `/api/admin/users/{id}/free-group-count` | 设置用户免拼次数 |

### 商品模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/products` | 商品列表（支持分类筛选） |
| GET | `/api/products/{id}` | 商品详情 |
| POST | `/api/merchant/products` | 商家上架商品 |
| PUT | `/api/merchant/products/{id}` | 商家更新商品 |
| PUT | `/api/admin/products/{id}/audit` | 管理员审核商品 |

### 拼团模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/group-buys` | 拼团活动列表 |
| POST | `/api/merchant/group-buys` | 商家创建拼团活动 |
| POST | `/api/group-buys/{activityId}/join` | 用户参团 |
| POST | `/api/group-buys/{activityId}/start` | 用户开团 |

### 订单模块

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/orders` | 创建订单 |
| GET | `/api/orders/{id}` | 订单详情 |
| GET | `/api/user/orders` | 用户订单列表 |
| GET | `/api/merchant/orders` | 商家订单列表 |
| PUT | `/api/orders/{id}/pay` | 订单支付 |
| PUT | `/api/orders/{id}/pickup` | 团长核销取件 |

### 数据分析模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/merchant/analytics/sales?merchantId={id}` | 商家销售数据 |
| GET | `/api/merchant/analytics/suggestions?merchantId={id}` | 商家 AI 经营建议 |
| GET | `/api/admin/analytics/overview` | 管理员平台总览 |
| GET | `/api/admin/analytics/suggestions` | 管理员 AI 运营建议 |

### 推荐模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/recommendations?userId={id}` | 个性化商品推荐 |

---

## AI 智能特性

系统集成了 **Ollama 本地大模型**，无需联网即可为商家和管理员提供智能决策支持：

```
┌─────────────────────────────────────┐
│          OllamaAgentService         │
├─────────────────────────────────────┤
│  Model: qwen2.5:0.5b                │
│  Endpoint: http://localhost:11434   │
│  Timeout:  15s                       │
├─────────────────────────────────────┤
│  merchantSuggestions()              │
│    → 输入：销量 + 库存 + 销售额      │
│    → 输出：3条经营建议               │
│                                     │
│  adminSuggestions()                 │
│    → 输入：总交易额 + 订单数 +       │
│            商品排行 + 用户排行       │
│    → 输出：3条运营建议               │
└─────────────────────────────────────┘
```

**建议示例：**
- 🏪 商家端：*"农家土鸡蛋销量最高，建议加大进货量并搭配面包促销"*
- ⚙️ 管理员端：*"平台付费用户增长平缓，建议推出新用户首单立减活动"*

---

## 参考文献

本项目在设计与实现过程中参考了以下文献资料：

1. 孙秀杰, 关胜, 邵欣欣, 等. 信息系统分析与设计实训教程[M]. 大连: 东软电子出版社, 2013.
2. 傅铅生. 信息系统分析与设计[M]. 2版. 北京: 国防工业出版社, 2009.
3. 段鹏松, 曹仰杰. 轻量级Java Web整合开发: Spring+Spring Boot+MyBatis[M]. 2版. 北京: 清华大学出版社, 2020.
4. 李冬海, 靳宗信, 姜维, 等. 轻量级Java EE Web框架技术——Spring MVC+Spring+MyBatis+Spring Boot[M]. 北京: 清华大学出版社, 2022.
5. 肖宏启, 杨丰嘉, 柳均. MySQL数据库设计与应用[M]. 北京: 清华大学出版社, 2021.
6. 夏永康, 左向山. 新零售社区团购信息管理系统设计[J]. 软件, 2024, 45(9): 127-129.
7. 唐双林. 基于Vue和SpringBoot架构的智能推荐农产品团购销售系统[D]. 重庆: 重庆三峡学院, 2023.
8. 舒玲利, 周永毅, 秦勇. Game-theoretic approach on strategizing cooperation model in community group-buying[J]. Information & Management, 2026, 63(3): 1043-1058.

> 完整参考文献请参阅 [docs/参考文献.md](docs/参考文献.md)

---

## 许可证

本项目仅用于学术教育目的。

---

<p align="center">
  <sub>Made with ❤️ for Information Systems Analysis & Design Course Project</sub>
</p>

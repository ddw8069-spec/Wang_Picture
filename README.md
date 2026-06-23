# 智能协同云图库

基于 Vue 3 + Spring Boot + COS + WebSocket 的企业级智能协同云图库平台，支持图片上传、检索、团队协作和实时编辑。

## 功能

- **公开图库** — 上传、检索、浏览公开图片资源
- **私有空间** — 个人图片的批量管理、编辑和分析
- **团队空间** — 创建团队，邀请成员，共享图片资源
- **实时协同编辑** — WebSocket 多端通信，多人实时同步编辑图片
- **图片审核** — 管理员审核图片内容，支持批量操作
- **AI 扩图** — 接入阿里云 AI 大模型，实现图片智能扩展
- **空间分析** — 数据可视化展示空间使用情况

## 技术栈

### 后端

- Java 11 + Spring Boot 2.7
- MySQL + MyBatis-Plus
- Redis + Caffeine 多级缓存
- ShardingSphere 分库分表
- Sa-Token 权限控制
- WebSocket 双向通信
- Disruptor 无锁队列
- COS 对象存储（腾讯云）
- DDD 领域驱动设计
- 阿里云 AI 大模型

### 前端

- Vue 3 + Vite
- Ant Design Vue
- TypeScript
- Pinia 状态管理
- ESLint + Prettier

## 项目结构

```
├── yu-picture-backend       # 标准 Spring Boot 架构
├── yu-picture-backend-ddd   # DDD 领域驱动设计架构
├── yu-picture-frontend      # Vue 3 前端
└── README.md
```

两个后端模块功能等价，`-ddd` 模块采用 DDD 四层架构（接口层、应用层、领域层、基础设施层）。

## 快速启动

### 环境要求

- JDK 11+
- MySQL 8.0+
- Redis 6.0+
- Node.js 16+
- 腾讯云 COS（对象存储）账号

### 后端

1. 导入 `sql/create_table.sql` 到 MySQL
2. 配置 `application.yml` 中的数据库、Redis 和 COS 连接信息
3. 运行 `YuPictureBackendApplication`

```bash
cd yu-picture-backend
mvn spring-boot:run
```

### 前端

```bash
cd yu-picture-frontend
npm install
npm run dev
```

## 架构亮点

| 能力 | 方案 |
|---|---|
| 缓存 | Redis + Caffeine 二级缓存，随机 TTL 防雪崩 |
| 分表 | ShardingSphere 按 spaceId 动态分表 |
| 并发 | Disruptor RingBuffer 无锁队列处理 WebSocket 消息 |
| 实时协作 | WebSocket + 排他编辑锁，操作广播同步 |
| 权限 | RBAC 模型 + Sa-Token 空间级权限校验 |
| 成本优化 | COS 上传自动 WebP 压缩 + 缩略图生成，定时冷数据沉降 |
| 运维 | 定时清理孤儿文件/过期数据，Redis 分布式锁防重复执行 |

## License

MIT

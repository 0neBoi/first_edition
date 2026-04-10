# 部署与环境变量说明

## 通义千问 API Key（必看）

**不要将 API Key 提交到 Git 仓库。** 本项目通过以下方式注入密钥（优先级从高到低）：

1. **环境变量 `DASHSCOPE_API_KEY`**（推荐用于服务器、CI、Docker）  
   - Linux / macOS: `export DASHSCOPE_API_KEY=你的密钥`  
   - Windows (PowerShell): `$env:DASHSCOPE_API_KEY="你的密钥"`  
   - Spring Boot 会将 `DASHSCOPE_API_KEY` 映射到配置项 `dashscope.api-key`（见 `application.yml`）。

2. **本地文件 `application-local.yml`（仅开发机）**  
   - 复制 `backend/src/main/resources/application-local.yml.example` 为 `application-local.yml`。  
   - 编辑其中的 `dashscope.api-key`。  
   - `application-local.yml` 已在 `.gitignore` 中忽略，不会被提交。

若未配置密钥，需要调用 AI 的接口（问答、抽知识点、出题）会报错；其余 CRUD、打卡、练习记录等不依赖 DashScope 的功能仍可正常使用。

## 数据库

1. 初始化表结构：执行 `docs/sql/schema.sql`。  
2. 若已有旧库，补充练习表：执行 `docs/sql/alter-question-attempt.sql`。

## 后端

```bash
cd backend
# 设置数据库连接：编辑 src/main/resources/application.yml 或使用环境变量覆盖 spring.datasource.*
export DASHSCOPE_API_KEY=你的密钥   # Linux/macOS
mvn spring-boot:run
```

默认上下文路径为 `/api`，端口 `8080`。

## 前端 Web

```bash
cd web
npm install
# 开发时代理到后端，或设置 VITE_API_BASE
npm run dev
```

生产构建：`npm run build`，将 `dist` 部署到静态服务器，并配置反向代理将 `/api` 指到后端。

## 安全建议

- 生产环境修改 `jwt.secret` 与数据库密码，并通过环境变量或外部配置注入。  
- 定期轮换 DashScope API Key，撤销泄露的密钥。

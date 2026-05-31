# 大学生学习辅助 / 校园工具

基于 **Spring Boot + Vue + 微信小程序 + MySQL**，Web 端以「校园工具」为主界面，整合 **学习资料与 AI**、**校园信息**、**广场交流**、**小游戏**、**笔记 / 待办 / 打卡 / 练习闭环** 等。AI 能力通过 **阿里云百炼（通义千问，DashScope HTTP API）** 调用，**不再使用 Ollama / Spring AI**。

## 功能概览

**学习与资料**

- 注册 / 登录（JWT），资料上传（TXT / PDF / DOCX）、知识要点提炼、模拟出题、AI 问答  
- 笔记、待办、学习打卡、练习与错题本、复习清单、学习报告  

**校园与社区**

- 课程表、校园地图、校园公告；交流广场 / 闲置（帖子、评论、私信等，需对应 SQL）  
- 管理员：公告管理、用户管理（管理员账号见配置说明）  

**休闲**

- 小恐龙跑酷、飞机大战（排行榜需对应 SQL）  

**小程序**

- 资料列表与详情、提炼 / 出题、AI 问答等；复杂能力以 Web 为主  

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2、Spring Security、JWT、MyBatis-Plus、MySQL |
| Web | Vue 3、Vite、Element Plus |
| 学生端 | 微信小程序 |
| AI | 阿里云 DashScope（`dashscope-sdk-java`，默认模型可在 `application.yml` 的 `dashscope.model` 调整） |

## 快速开始

### 1. 数据库  898989

新建库并执行基础脚本后，按功能补充迁移（**新库建议按顺序全部执行**，与 `docs/DEPLOYMENT.md` 一致）：

```bash
mysql -u root -p < docs/sql/schema.sql
mysql -u root -p study_helper < docs/sql/alter-question-attempt.sql
mysql -u root -p study_helper < docs/sql/alter-question-answer.sql
mysql -u root -p study_helper < docs/sql/migration_campus_announcement.sql
mysql -u root -p study_helper < docs/sql/migration_plaza.sql
mysql -u root -p study_helper < docs/sql/migration_games_all.sql
```

`migration_games_all.sql` 已包含小游戏排行榜表；若你曾单独执行过 `migration_dino_leaderboard.sql` / `migration_plane_leaderboard.sql`，请勿重复建表。

### 2. 通义千问（DashScope）API Key

1. 在阿里云百炼控制台创建 API Key。  
2. **不要**把真实 Key 写进仓库。任选其一：  
   - 环境变量：`DASHSCOPE_API_KEY`  
   - 本地：复制 `backend/src/main/resources/application-local.yml.example` 为 `application-local.yml`，填写 `dashscope.api-key`（该文件已被 `.gitignore` 忽略）  

详见 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)。

### 3. 后端

```bash
cd backend
# 修改 application.yml 中的数据库连接（或使用环境变量覆盖）
mvn spring-boot:run
```

服务地址：`http://localhost:8080/api`（上下文路径为 `/api`）。

**本机 / 局域网（后端）**

| 场景 | 命令 | 说明 |
|------|------|------|
| 仅本机 | `mvn spring-boot:run` | 默认 `127.0.0.1:8080` |
| 手机 / 同 WiFi | `mvn spring-boot:run -Dspring-boot.run.profiles=lan` | `0.0.0.0:8080` |

Windows 可使用 `backend/run-dev-local.bat` 或 `backend/run-dev-lan.bat`。IDE 在 **Active profiles** 填 `lan` 与第二行等价。

### 4. Web 前端

```bash
cd web
npm install
npm run dev
```

浏览器访问：`http://localhost:5173`。开发环境下 **推荐不设置** `VITE_API_BASE`，请求走相对路径 **`/api`**，由 Vite 代理到本机 `8080`（见 `web/vite.config.js` 与 `web/.env.development` 注释）。

**本机 / 局域网（前端）**

| 场景 | 命令 | 说明 |
|------|------|------|
| 仅本机 | `npm run dev` | 默认 `localhost:5173` |
| 局域网访问 | `npm run dev:lan` | `0.0.0.0:5173`，手机访问 `http://电脑IP:5173` |
| 构建预览 | `npm run preview` / `npm run preview:lan` | |

手机访问时 **勿** 将 `VITE_API_BASE` 设为 `localhost`（请求会发到手机本机）。`npm run dev:lan` 使用 `mode=lan`，不读取 `.env.development`，局域网所需变量请放在 `.env.lan` 或 `.env`。

### 5. 微信小程序

1. 用微信开发者工具打开 `miniprogram` 目录。  
2. 「详情 → 本地设置」勾选「不校验合法域名」。  
3. 在 `miniprogram/app.js` 中设置 `globalData.baseUrl`（如 `http://你的IP:8080/api`）。  

## 接口说明

接口较多且带鉴权，完整路径均以 `/api` 为前缀（与 `server.servlet.context-path` 一致）。常用示例：

| 路径（相对 `/api`） | 方法 | 说明 |
|---------------------|------|------|
| `/auth/register`、`/auth/login`、`/auth/me` | POST/GET | 注册、登录、当前用户 |
| `/material/upload`、`/material/list`、`/material/{id}` | POST/GET | 资料上传与查询 |
| `/knowledge/list`、`/knowledge/extract` | GET/POST | 知识要点 |
| `/question/list`、`/question/generate` | GET/POST | 题目 |
| `/qwen/ask` | POST | AI 问答 |

更多模块见 `backend/.../controller/` 下各 `*Controller.java`。

## 目录结构

```
study-helper/
├── backend/          # Spring Boot 后端
├── web/              # Vue 前端（校园工具 + 学习）
├── miniprogram/      # 微信小程序
├── docs/             # 说明文档与 SQL
│   ├── sql/
│   ├── DEPLOYMENT.md
│   └── 如何运行.md
├── PROJECT_STRUCTURE.md
└── README.md
```

## 其它说明

- 上传文件保存在 `file.upload-dir`（默认 `./uploads`）。  
- 提炼要点、生成题目、AI 问答依赖 DashScope；未配置 Key 时这些接口会失败，其它功能多数仍可用。  
- 内容过长时可能会截断后再送模型，以控制耗时与 token。  
- 更细的逐步操作见 [docs/如何运行.md](docs/如何运行.md)。

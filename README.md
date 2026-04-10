# 大学生学习辅助工具

基于 **Spring Boot + Vue + 微信小程序 + MySQL**，结合 **Qwen（Ollama 本地模型）**，实现：导入文件 → 整理知识要点 → 模拟出题 → AI 问答。

## 功能概览

- **资料导入**：上传 TXT / PDF / DOCX，自动解析为纯文本
- **知识要点**：基于 Qwen 从资料中提炼知识要点，便于复习
- **模拟出题**：根据资料内容生成单选、多选、填空、简答等练习题
- **AI 问答**：针对学习内容向 Qwen 提问

## 技术栈

| 模块     | 技术 |
|----------|------|
| 后端     | Spring Boot 3.2、MyBatis-Plus、MySQL、Spring AI (Ollama) |
| Web 管理端 | Vue 3、Vite、Element Plus |
| 学生端   | 微信小程序 |
| AI       | Qwen（Ollama 本地，如 qwen2.5:0.5b / qwen3:0.8b） |

## 快速开始

### 1. 数据库

创建库并执行脚本：

```bash
mysql -u root -p < docs/sql/schema.sql
```

或手动创建库 `study_helper` 后，在 MySQL 客户端中执行 `docs/sql/schema.sql`。

### 2. Ollama 与 Qwen

安装 [Ollama](https://ollama.com/)，并拉取模型（二选一）：

```bash
ollama pull qwen2.5:0.5b
# 或
ollama pull qwen3:0.8b
```

若使用 qwen3:0.8b，在 `backend/src/main/resources/application.yml` 中修改：

```yaml
spring.ai.ollama.chat.options.model: qwen3:0.8b
```

### 3. 后端

```bash
cd backend
# 修改 application.yml 中的数据库账号密码
mvn spring-boot:run
```

服务地址：`http://localhost:8080/api`。

**本机 / 局域网 一键切换（后端）**

| 场景 | 命令 | 说明 |
|------|------|------|
| 仅本机可连后端 | `mvn spring-boot:run` | 默认监听 `127.0.0.1:8080` |
| 手机 / 同 WiFi 可连后端 | `mvn spring-boot:run -Dspring-boot.run.profiles=lan` | 启用 profile `lan`，监听 `0.0.0.0:8080` |

Windows 也可双击 `backend/run-dev-local.bat` 或 `backend/run-dev-lan.bat`。  
IDE 运行时在 **Active profiles** 中填 `lan` 即与上表第二行等价。  
配置来自 `application-lan.yml`（仅覆盖 `server.address`）。

### 4. Web 管理端

```bash
cd web
npm install
npm run dev
```

浏览器访问：`http://localhost:5173`。可在此上传资料、查看资料详情、提炼知识要点、生成题目、使用 AI 问答。

**本机 / 局域网 一键切换（前端）**

| 场景 | 命令 | 说明 |
|------|------|------|
| 仅本机可打开页面 | `npm run dev` | Vite 只监听 `localhost:5173` |
| 手机 / 同 WiFi 可打开页面 | `npm run dev:lan` | Vite 监听 `0.0.0.0:5173`，用手机访问 `http://电脑IP:5173` |
| 构建后本机预览 | `npm run preview` | |
| 构建后局域网预览 | `npm run preview:lan` | |

**手机或同局域网设备访问（热点 / WiFi）**

1. **前后端都切到局域网模式**：后端用带 `profiles=lan` 的命令（或 `run-dev-lan.bat`），前端执行 `npm run dev:lan`。
2. 电脑与手机在同一网络（手机开热点给电脑连，或都连同一路由器）。
3. 在电脑上查 IPv4：Windows 可在 PowerShell 执行 `ipconfig`，看「无线局域网适配器」或「以太网」下的 IPv4（如 `192.168.x.x`）。
4. 手机浏览器打开：`http://上述IP:5173`（不要用 localhost）。
5. 若打不开，在 Windows「防火墙」中为 **Node.js**、**Java**（或你的 JDK）勾选「专用/公用网络」允许，或临时关闭防火墙做测试。
6. 不要设置 `VITE_API_BASE` 为 `localhost`（接口应走相对路径 `/api`，由 Vite 代理到本机 8080）。
7. **`npm run dev:lan` 使用 Vite `mode=lan`，不会读取 `.env.development`**。若你在 `.env.development` 里配置了 `VITE_*` 变量，局域网开发时请把相同项写到 `.env.lan`（或 `.env`）中。

### 5. 微信小程序

1. 用微信开发者工具打开 `miniprogram` 目录。
2. 在「详情 → 本地设置」中勾选「不校验合法域名」。
3. 若后端非本机，修改 `miniprogram/app.js` 中 `globalData.baseUrl` 为你的后端地址（如 `http://你的IP:8080/api`）。

小程序可：查看资料列表、进入资料详情（提炼要点、生成题目）、AI 问答。资料上传建议在 Web 端完成。

## 接口说明

| 接口 | 方法 | 说明 |
|------|------|------|
| `/material/upload` | POST | 上传资料（file, userId 可选） |
| `/material/list` | GET | 资料列表（userId 可选） |
| `/material/{id}` | GET | 资料详情 |
| `/knowledge/list` | GET | 某资料的知识要点（materialId） |
| `/knowledge/extract` | POST | 根据资料 ID 提炼要点（materialId, userId 可选） |
| `/question/list` | GET | 某资料的题目列表（materialId） |
| `/question/generate` | POST | 根据资料生成题目（materialId, count, userId 可选） |
| `/qwen/ask` | POST | AI 问答（question） |

## 目录结构

```
study-helper/
├── backend/          # Spring Boot 后端
├── web/              # Vue 管理端
├── miniprogram/      # 微信小程序
├── docs/sql/         # 数据库脚本
├── PROJECT_STRUCTURE.md
└── README.md
```

## 说明

- 上传文件会保存在后端配置的 `file.upload-dir`（默认 `./uploads`）。
- 提炼要点与生成题目会调用本地 Ollama，请确保 Ollama 已启动且模型已拉取。
- 内容过长时，会截断前约 5000 字再交给模型，以控制 token 与响应时间。

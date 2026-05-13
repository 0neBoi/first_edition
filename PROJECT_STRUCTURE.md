# 大学生学习辅助 / 校园工具 — 项目结构

## 技术栈

- **后端**：Spring Boot 3.x、Spring Security、JWT、MyBatis-Plus、MySQL  
- **AI**：阿里云 DashScope（通义千问，`dashscope-sdk-java`），密钥通过 `DASHSCOPE_API_KEY` 或 `application-local.yml` 注入  
- **Web**：Vue 3、Vite、Element Plus  
- **小程序**：微信小程序（资料与 AI 相关能力为主）  
- **数据库**：MySQL 8.x（推荐）

## 目录说明

```
study-helper/
├── backend/              # Spring Boot：鉴权、资料、AI、校园、广场、练习统计等
├── web/                  # Vue 单页：路由与页面在 src/views、src/router
├── miniprogram/          # 微信小程序
├── docs/
│   ├── sql/              # schema 与增量迁移脚本
│   ├── DEPLOYMENT.md     # 部署与环境变量
│   └── 如何运行.md       # 本地运行步骤
├── README.md
└── PROJECT_STRUCTURE.md
```

## 功能模块（与当前代码对应）

- **账号**：学生注册 / 登录、JWT；管理员角色用于后台公告与用户管理  
- **学习闭环**：资料解析、知识要点、出题、练习与错题、复习清单、学习报告、笔记、待办、打卡  
- **校园**：课程表、地图、公告（含管理端发布）  
- **社区**：广场帖子（交流 / 闲置）、评论、点赞收藏、私信等（依赖 `migration_plaza.sql`）  
- **小游戏**：小恐龙、飞机大战及排行榜（依赖 `migration_games_all.sql` 或等价脚本）  

课程维度的「课程 → 章节 → 资料」若未在界面中完全展开，以实际路由与接口为准；扩展时可继续迭代表结构与页面。

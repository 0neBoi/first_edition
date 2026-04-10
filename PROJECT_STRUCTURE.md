# 大学生学习辅助工具 - 项目结构

## 技术栈
- **后端**: Spring Boot 3.x + MySQL + MyBatis-Plus
- **AI**: Qwen3.5:0.8b（Ollama 本地 / 或阿里云 API 二选一）
- **Web 管理端**: Vue 3 + Vite + Element Plus
- **学生端**: 微信小程序
- **数据库**: MySQL 8.x

## 目录说明
```
study-helper/
├── backend/          # Spring Boot 后端
├── web/              # Vue 管理/Web 前端
├── miniprogram/      # 微信小程序
├── docs/             # 文档与 SQL 脚本
└── README.md
```

## 功能模块（初版）
- 用户：学生注册/登录（含微信登录）
- 学习：笔记、待办、打卡、AI 问答（Qwen）
- 数据：成绩/课程管理（可选扩展）

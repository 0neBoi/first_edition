-- 大学生学习辅助工具 - 数据库初始化脚本
-- MySQL 8.x

CREATE DATABASE IF NOT EXISTS study_helper
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE study_helper;

-- 用户表（学生）
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) DEFAULT NULL,
  password VARCHAR(128) DEFAULT NULL,
  nickname VARCHAR(64) DEFAULT NULL,
  avatar VARCHAR(255) DEFAULT NULL,
  openid VARCHAR(64) DEFAULT NULL COMMENT '微信 openid',
  phone VARCHAR(20) DEFAULT NULL,
  school VARCHAR(128) DEFAULT NULL,
  major VARCHAR(128) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 学习笔记
CREATE TABLE IF NOT EXISTS study_note (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT DEFAULT NULL,
  title VARCHAR(200) DEFAULT NULL,
  content TEXT,
  tags VARCHAR(255) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 学习待办
CREATE TABLE IF NOT EXISTS study_todo (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT DEFAULT NULL,
  title VARCHAR(200) DEFAULT NULL,
  content TEXT,
  status TINYINT DEFAULT 0 COMMENT '0待办 1完成',
  due_date DATE DEFAULT NULL,
  priority TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 学习打卡
CREATE TABLE IF NOT EXISTS study_clock (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT DEFAULT NULL,
  clock_date DATE DEFAULT NULL,
  minutes INT DEFAULT 0,
  remark VARCHAR(255) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_clock_date (clock_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 导入的学习资料（上传文件元信息 + 解析出的文本）
CREATE TABLE IF NOT EXISTS study_material (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT DEFAULT NULL COMMENT '可为空，未登录也可导入',
  title VARCHAR(200) NOT NULL COMMENT '资料标题，默认取文件名',
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(512) NOT NULL COMMENT '相对存储路径',
  file_type VARCHAR(32) DEFAULT NULL COMMENT 'txt/pdf/docx',
  file_size BIGINT DEFAULT 0,
  content_text LONGTEXT COMMENT '解析出的纯文本，供 AI 提取知识点与出题',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 从资料中提取的知识要点
CREATE TABLE IF NOT EXISTS study_knowledge_point (
  id BIGINT NOT NULL AUTO_INCREMENT,
  material_id BIGINT NOT NULL,
  user_id BIGINT DEFAULT NULL,
  title VARCHAR(300) NOT NULL COMMENT '要点标题',
  content TEXT COMMENT '要点内容',
  sort_order INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_material_id (material_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 根据资料/知识点生成的题目
CREATE TABLE IF NOT EXISTS study_question (
  id BIGINT NOT NULL AUTO_INCREMENT,
  material_id BIGINT NOT NULL,
  user_id BIGINT DEFAULT NULL,
  type VARCHAR(20) NOT NULL DEFAULT 'single' COMMENT 'single=单选 multiple=多选 fill=填空 essay=简答',
  question_text TEXT NOT NULL,
  options_json TEXT COMMENT '选择题选项 JSON，如 [{"key":"A","value":"选项A"},...]',
  answer TEXT COMMENT '参考答案',
  analysis TEXT COMMENT '解析',
  sort_order INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_material_id (material_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 练习作答记录（错题本、掌握度、复习推荐）
CREATE TABLE IF NOT EXISTS question_attempt (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  is_correct TINYINT NOT NULL DEFAULT 0 COMMENT '1正确 0错误',
  user_answer TEXT,
  time_cost_ms INT DEFAULT NULL,
  attempt_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_question (user_id, question_id),
  KEY idx_user_time (user_id, attempt_time),
  KEY idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 校园公告（管理员发布，公告页公开读取）
CREATE TABLE IF NOT EXISTS campus_announcement (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(500) NOT NULL,
  content TEXT NOT NULL,
  notice_type VARCHAR(32) DEFAULT 'teaching',
  publisher VARCHAR(128) DEFAULT '教务处',
  pinned TINYINT DEFAULT 0,
  publish_date DATE DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_pinned_date (pinned, publish_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

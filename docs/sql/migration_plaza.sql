-- 交流广场：帖子、评论、点赞收藏、私信、闲置交易
USE study_helper;

CREATE TABLE IF NOT EXISTS plaza_post (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category VARCHAR(32) NOT NULL COMMENT 'DISCUSSION 交流 / MARKETPLACE 闲置',
  title VARCHAR(200) DEFAULT NULL,
  content TEXT NOT NULL,
  images_json VARCHAR(4000) DEFAULT NULL COMMENT 'JSON 数组，相对路径 plaza/xxx.jpg',
  price_cent BIGINT DEFAULT NULL COMMENT '闲置标价（分），交流帖为空',
  trade_status VARCHAR(16) DEFAULT 'NA' COMMENT 'NA ON_SALE SOLD RESERVED',
  like_count INT DEFAULT 0,
  comment_count INT DEFAULT 0,
  favorite_count INT DEFAULT 0,
  share_count INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_category_time (category, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS plaza_comment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT NULL COMMENT '回复某条评论',
  reply_to_user_id BIGINT DEFAULT NULL COMMENT '被回复者',
  content TEXT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_post_id (post_id),
  KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS plaza_post_like (
  id BIGINT NOT NULL AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_post_user (post_id, user_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS plaza_post_favorite (
  id BIGINT NOT NULL AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_post_user (post_id, user_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS plaza_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  from_user_id BIGINT NOT NULL,
  to_user_id BIGINT NOT NULL,
  content TEXT,
  image_path VARCHAR(512) DEFAULT NULL,
  read_flag TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_pair (from_user_id, to_user_id, create_time),
  KEY idx_to_read (to_user_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

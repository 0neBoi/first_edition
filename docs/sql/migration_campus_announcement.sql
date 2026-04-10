-- 已有库增量：校园公告表（在 study_helper 中执行）
USE study_helper;

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

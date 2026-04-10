-- 小恐龙跑酷：用户最佳分（排行榜）
USE study_helper;

CREATE TABLE IF NOT EXISTS dino_best (
  user_id BIGINT NOT NULL PRIMARY KEY COMMENT 'sys_user.id',
  score INT NOT NULL COMMENT '历史最高分',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_score (score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

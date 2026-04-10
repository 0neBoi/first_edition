-- 小游戏排行榜表（小恐龙 + 飞机大战）一次执行
USE study_helper;

CREATE TABLE IF NOT EXISTS dino_best (
  user_id BIGINT NOT NULL PRIMARY KEY COMMENT 'sys_user.id',
  score INT NOT NULL COMMENT '历史最高分',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_score (score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS plane_best (
  user_id BIGINT NOT NULL PRIMARY KEY COMMENT 'sys_user.id',
  score INT NOT NULL COMMENT '历史最高分',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_score (score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

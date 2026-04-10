-- 练习作答记录（用于错题本、掌握度与复习推荐）
USE study_helper;

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

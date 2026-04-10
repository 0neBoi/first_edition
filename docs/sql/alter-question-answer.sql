-- 修复 study_question.answer 长度不足导致「Data too long for column answer」
-- 若已建表且报错，在 MySQL 中执行本脚本即可
USE study_helper;
ALTER TABLE study_question MODIFY COLUMN answer TEXT COMMENT '参考答案';

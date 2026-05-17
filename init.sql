-- Docker 啟動 MySQL 容器時自動執行此檔案

CREATE DATABASE IF NOT EXISTS quiz_1141121
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE quiz_1141121;

-- 建立 quiz 資料表
CREATE TABLE IF NOT EXISTS `quiz` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `is_published` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

-- MySQL dump 10.16  Distrib 10.1.26-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: admin_dev
-- ------------------------------------------------------
-- Server version	10.1.26-MariaDB-0+deb9u1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = '+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

--
-- Table structure for table `apicases`


DROP TABLE IF EXISTS `apicases`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `apicases`
(
    `id`            bigint(20) unsigned    NOT NULL AUTO_INCREMENT COMMENT 'Id',
    `apiid`  bigint(20) unsigned  NOT NULL COMMENT 'apiid',
    `apiname`  varchar(64) CHARACTER SET utf8 COLLATE utf8_bin COMMENT 'API',
    `deployunitid`  bigint(20) unsigned  NOT NULL COMMENT '微服务id',
    `deployunitname`  varchar(64) CHARACTER SET utf8 COLLATE utf8_bin COMMENT '微服务',
    `casename`  varchar(64) CHARACTER SET utf8 COLLATE utf8_bin COMMENT '用例名',
    `casejmxname`  varchar(64) CHARACTER SET utf8 COLLATE utf8_bin COMMENT '用例jmx名，和jmx文件名对齐',
    `casetype`  varchar(10) CHARACTER SET utf8 COLLATE utf8_bin COMMENT '类型，功能，性能',
    `threadnum`  bigint(20) unsigned  NOT NULL COMMENT '线程数',
    `loops`  bigint(20) unsigned  NOT NULL COMMENT '循环数',
    `casecontent`       varchar(64) CHARACTER SET utf8 COLLATE utf8_bin COMMENT '用例内容，以英文逗号分开，提供jar获取自定义期望结果A：1的值，入参为冒号左边的内容',
    `expect`     varchar(500) CHARACTER SET utf8 COLLATE utf8_bin COMMENT '期望值',
    `middleparam`     varchar(200) CHARACTER SET utf8 COLLATE utf8_bin COMMENT '中间变量',
    `level`  varchar(10) CHARACTER SET utf8 COLLATE utf8_bin COMMENT '优先级',
    `memo`          varchar(200) CHARACTER SET utf8 COLLATE utf8_bin COMMENT '备注',
    `create_time`   datetime DEFAULT NOW() COMMENT '创建时间',
    `lastmodify_time`    datetime DEFAULT NOW() COMMENT '上一次修改时间',
    `creator`    varchar(10) CHARACTER SET utf8 COLLATE utf8_bin COMMENT '创建者',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 4
  DEFAULT CHARSET = utf8mb4 COMMENT ='api用例表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `apicases`
--

LOCK TABLES `apicases` WRITE;
/*!40000 ALTER TABLE `apicases`
    DISABLE KEYS */;
INSERT INTO `apicases`
VALUES (1,1, 'getnamebyuserid',1,'accountservice','登录成功','loginsucess', '描述用例是做什么的','功能', 'name:"aaa",pass:"bbb"','低','备注',
        '2019-07-01 00:00:00', '2019-07-01 00:00:00','admin');
/*!40000 ALTER TABLE `apicases`
    ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;

-- Dump completed on 2018-02-16 19:24:53

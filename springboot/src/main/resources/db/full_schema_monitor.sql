-- =============================================================================
-- 独居老人智能监护系统：建表脚本（本地 MySQL 首次初始化用）
-- 数据库：community
-- 执行前先建库：CREATE DATABASE IF NOT EXISTS community DEFAULT CHARSET utf8mb4;
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. 用户表（社区工作人员 / 老人账号）
--    角色：admin / community / elder / child（child 禁止登录）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id`          INT          NOT NULL AUTO_INCREMENT,
    `username`    VARCHAR(64)  NOT NULL              COMMENT '登录用户名',
    `password`    VARCHAR(255) NOT NULL             COMMENT '密码（明文存储，课设演示用）',
    `nickname`    VARCHAR(64)  NULL                  COMMENT '昵称',
    `role`        VARCHAR(32)  NOT NULL DEFAULT 'community' COMMENT 'admin/community/elder/child',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- -----------------------------------------------------------------------------
-- 2. 老人档案表
--    与 users 通过 elder_user_id 关联（elder 角色账号 → 老人档案）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `elder_profiles`;
CREATE TABLE `elder_profiles` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `elder_user_id`     BIGINT       NULL                COMMENT '关联 users.id（elder 角色账号）',
    `elder_username`    VARCHAR(64)  NULL                COMMENT '关联 users.username',
    `real_name`         VARCHAR(64)  NOT NULL            COMMENT '真实姓名',
    `gender`            VARCHAR(8)   NULL                COMMENT '男/女',
    `age`               INT          NULL                COMMENT '年龄',
    `room`              VARCHAR(128) NULL                COMMENT '房间号（如 1号楼201）',
    `address`           VARCHAR(255) NULL                COMMENT '住址',
    `phone`             VARCHAR(20)  NULL                COMMENT '联系电话',
    `emergency_contact`  VARCHAR(64)  NULL                COMMENT '紧急联系人',
    `emergency_phone`   VARCHAR(20)  NULL                COMMENT '紧急联系电话',
    `monitor_device_id`  VARCHAR(64)  NULL                COMMENT '监测设备编号',
    `status`            VARCHAR(32)  NULL DEFAULT 'active' COMMENT '状态：active/inactive',
    `notes`             TEXT         NULL                COMMENT '备注',
    `nursing_level`     VARCHAR(16)  NULL                COMMENT '护理等级：LEVEL_1~4',
    `risk_level`        VARCHAR(16)  NULL                COMMENT '风险等级：HIGH/MEDIUM/LOW',
    `health_score`      INT          NULL                COMMENT '健康评分 0-100',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_elder_user` (`elder_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人档案';

-- -----------------------------------------------------------------------------
-- 3. 健康数据表（心率/呼吸/睡眠等体征，模拟设备采集）
--    elderly_id 为非持久字段（@TableField(exist=false)），通过 device_id 间接关联
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `health_history`;
CREATE TABLE `health_history` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT,
    `device_id`      VARCHAR(64)  NULL                COMMENT '设备编号',
    `health_score`   INT      NULL                COMMENT '健康综合评分',
    `heart_rate`     DOUBLE   NULL                COMMENT '心率',
    `breathing_rate`  DOUBLE   NULL                COMMENT '呼吸频率',
    `sleep_status`   VARCHAR(32) NULL              COMMENT '睡眠状态',
    `sleep_score`    DOUBLE   NULL                COMMENT '睡眠评分',
    `motion_index`   DOUBLE   NULL                COMMENT '体动指数',
    `on_bed_status`  VARCHAR(32) NULL              COMMENT '在床状态：ON_BED/OFF_BED',
    `posture_status`  VARCHAR(32) NULL              COMMENT '体态',
    `device_online`  INT      NULL                COMMENT '设备在线 1/0',
    `report_time`    VARCHAR(32) NULL              COMMENT '上报时间（字符串格式）',
    `recorded_at`    BIGINT   NULL                COMMENT '采集时间戳（毫秒）',
    PRIMARY KEY (`id`),
    KEY `idx_health_device` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康监测数据';

-- -----------------------------------------------------------------------------
-- 4. 环境数据表（温湿度/空气质量等，模拟设备采集）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `environment_history`;
CREATE TABLE `environment_history` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT,
    `device_id`     VARCHAR(64)  NULL,
    `temperature`   DECIMAL(5,1) NULL             COMMENT '温度℃',
    `humidity`      DECIMAL(5,1) NULL             COMMENT '湿度%',
    `air_quality`   DECIMAL(5,1) NULL             COMMENT '空气质量指数',
    `illumination`  DECIMAL(8,2) NULL             COMMENT '光照度 lux',
    `device_online` INT      NULL                COMMENT '设备在线 1/0',
    `report_time`   DATETIME     NULL             COMMENT '上报时间',
    `recorded_at`   BIGINT   NULL                COMMENT '采集时间戳（毫秒）',
    PRIMARY KEY (`id`),
    KEY `idx_env_device` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环境监测数据';

-- -----------------------------------------------------------------------------
-- 5. 预警事件表
--    elder_name/room 为冗余字段（事件发生时快照），不依赖外键
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `alerts`;
CREATE TABLE `alerts` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT,
    `elder_name`    VARCHAR(64)  NULL                COMMENT '老人姓名（快照）',
    `room`          VARCHAR(128) NULL                COMMENT '房间号（快照）',
    `type`          VARCHAR(32)  NOT NULL            COMMENT '事件类型：FALL/EMERGENCY/SMOKE/PRESSURE',
    `description`   TEXT         NULL                COMMENT '事件描述',
    `time`          VARCHAR(32)  NULL                COMMENT '发生时间（字符串格式）',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'NEW' COMMENT 'NEW/PROCESSING/CLOSED',
    `timeline_json` TEXT         NULL                COMMENT '处理时间线（JSON）',
    `notes`         TEXT         NULL                COMMENT '处理备注',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_alerts_type` (`type`),
    KEY `idx_alerts_status` (`status`),
    KEY `idx_alerts_elder_name` (`elder_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警事件';

-- -----------------------------------------------------------------------------
-- 6. 公告表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `announcements`;
CREATE TABLE `announcements` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT,
    `title`        VARCHAR(128) NOT NULL,
    `content`      TEXT     NULL,
    `priority`     INT      NULL DEFAULT 0          COMMENT '优先级 0-3',
    `publish_type` INT      NULL DEFAULT 0          COMMENT '0=即时 1=预约',
    `published_at` DATETIME NULL                   COMMENT '实际发布时间',
    `scheduled_at`  DATETIME NULL                   COMMENT '预约发布时间',
    `end_time`     DATETIME NULL                   COMMENT '结束/到期时间',
    `status`       INT      NULL DEFAULT 1          COMMENT '0=待发布 1=已发布 2=已撤下 3=已过期',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告通知';

-- =============================================================================
-- 初始化数据
-- =============================================================================

-- 用户：admin / community / elder 三种角色
INSERT INTO `users` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '123456', '系统管理员', 'admin'),
('community', '123456', '社区工作人员', 'community'),
('elder', '123456', '老人账号', 'elder');

-- 老人档案：8 位老人，覆盖不同楼栋和护理等级
INSERT INTO `elder_profiles`
(`elder_user_id`, `elder_username`, `real_name`, `gender`, `age`, `room`, `address`, `phone`,
 `emergency_contact`, `emergency_phone`, `monitor_device_id`, `status`, `notes`,
 `nursing_level`, `risk_level`, `health_score`)
VALUES
(3, 'elder', '陈守田', '男', 82, '1号楼101', '1号楼101室', '13800000001', '陈小明', '13900000001', 'DEV_001', 'active', '高血压史，需重点关注', 'LEVEL_2', 'HIGH', 45),
(NULL, NULL, '王秀兰', '女', 78, '1号楼203', '1号楼203室', '13800000002', '王大伟', '13900000002', 'DEV_002', 'active', '糖尿病，定期服药', 'LEVEL_3', 'MEDIUM', 62),
(NULL, NULL, '李建国', '男', 75, '2号楼301', '2号楼301室', '13800000003', '李小花', '13900000003', 'DEV_003', 'active', NULL, 'LEVEL_4', 'LOW', 85),
(NULL, NULL, '张桂芳', '女', 80, '2号楼102', '2号楼102室', '13800000004', '张志强', '13900000004', 'DEV_004', 'active', '关节炎，行动不便', 'LEVEL_3', 'MEDIUM', 58),
(NULL, NULL, '刘德海', '男', 85, '3号楼201', '3号楼201室', '13800000005', '刘美玲', '13900000005', 'DEV_005', 'active', '冠心病，安装心脏起搏器', 'LEVEL_1', 'HIGH', 38),
(NULL, NULL, '赵淑珍', '女', 72, '3号楼302', '3号楼302室', '13800000006', '赵建国', '13900000006', 'DEV_006', 'active', NULL, 'LEVEL_4', 'LOW', 90),
(NULL, NULL, '孙耀祖', '男', 79, '1号楼305', '1号楼305室', '13800000007', '孙丽', '13900000007', 'DEV_007', 'active', '轻度认知障碍', 'LEVEL_3', 'MEDIUM', 55),
(NULL, NULL, '周佩兰', '女', 83, '2号楼401', '2号楼401室', '13800000008', '周伟', '13900000008', 'DEV_008', 'active', '骨质疏松，跌倒风险高', 'LEVEL_2', 'HIGH', 42);

-- 公告：2 条已发布
INSERT INTO `announcements` (`title`, `content`, `priority`, `publish_type`, `published_at`, `status`, `end_time`)
VALUES
('冬季安全用电提醒', '近期气温骤降，请各位老人注意用电安全，不要在卧室使用大功率电器，电热毯使用前请检查线路。', 2, 0, NOW(), 1, DATE_ADD(NOW(), INTERVAL 30 DAY)),
('社区体检通知', '本周三上午9点在社区服务中心进行免费健康体检，请各位老人空腹前往。', 1, 0, NOW(), 1, DATE_ADD(NOW(), INTERVAL 7 DAY));

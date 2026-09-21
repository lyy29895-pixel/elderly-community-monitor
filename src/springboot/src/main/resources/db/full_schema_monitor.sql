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

-- 健康监测演示数据：今天多个时间点，覆盖高风险（DEV_001/005/008）与低风险（DEV_003/006）设备
-- report_time 与代码一致使用 ISO 格式（yyyy-MM-ddTHH:mm:ss），recorded_at 为北京时间毫秒时间戳
INSERT INTO `health_history`
(`device_id`, `health_score`, `heart_rate`, `breathing_rate`, `sleep_status`, `sleep_score`,
 `motion_index`, `on_bed_status`, `posture_status`, `device_online`, `report_time`, `recorded_at`)
VALUES
-- DEV_001 陈守田（高风险）：清晨离床 + 心率异常，午后恢复
('DEV_001', 45, 96, 24, 'AWAKE',       55, 8.5,  'OFF_BED', 'STANDING', 1, CONCAT(CURDATE(), 'T06:10:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 06:10:00')) * 1000)),
('DEV_001', 42, 102, 26, 'AWAKE',      50, 12.0, 'OFF_BED', 'WALKING',  1, CONCAT(CURDATE(), 'T09:30:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 09:30:00')) * 1000)),
('DEV_001', 48, 88,  22, 'LIGHT_SLEEP', 60, 3.2, 'ON_BED',  'LYING',    1, CONCAT(CURDATE(), 'T14:00:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 14:00:00')) * 1000)),
('DEV_001', 50, 84,  21, 'AWAKE',       65, 2.1, 'ON_BED',  'SITTING',  1, CONCAT(CURDATE(), 'T18:30:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 18:30:00')) * 1000)),
-- DEV_002 王秀兰（中风险）
('DEV_002', 62, 76, 19, 'LIGHT_SLEEP', 72, 1.8, 'ON_BED', 'LYING', 1, CONCAT(CURDATE(), 'T08:00:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 08:00:00')) * 1000)),
('DEV_002', 65, 80, 20, 'AWAKE',       70, 4.5, 'ON_BED', 'SITTING', 1, CONCAT(CURDATE(), 'T15:20:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 15:20:00')) * 1000)),
-- DEV_003 李建国（低风险，指标平稳）
('DEV_003', 85, 70, 17, 'DEEP_SLEEP', 90, 0.8, 'ON_BED', 'LYING', 1, CONCAT(CURDATE(), 'T07:00:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 07:00:00')) * 1000)),
('DEV_003', 88, 72, 18, 'AWAKE',       88, 3.6, 'OFF_BED', 'WALKING', 1, CONCAT(CURDATE(), 'T16:00:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 16:00:00')) * 1000)),
-- DEV_005 刘德海（高风险，起搏器）
('DEV_005', 38, 58, 16, 'LIGHT_SLEEP', 48, 0.5, 'ON_BED', 'LYING', 1, CONCAT(CURDATE(), 'T05:40:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 05:40:00')) * 1000)),
('DEV_005', 35, 55, 15, 'AWAKE',       45, 1.2, 'ON_BED', 'LYING', 1, CONCAT(CURDATE(), 'T11:00:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 11:00:00')) * 1000)),
('DEV_005', 40, 62, 18, 'AWAKE',       52, 2.0, 'ON_BED', 'SITTING', 1, CONCAT(CURDATE(), 'T19:10:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 19:10:00')) * 1000)),
-- DEV_006 赵淑珍（低风险）
('DEV_006', 90, 74, 18, 'DEEP_SLEEP', 92, 0.6, 'ON_BED', 'LYING', 1, CONCAT(CURDATE(), 'T07:30:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 07:30:00')) * 1000)),
('DEV_006', 92, 78, 19, 'AWAKE',       90, 5.0, 'OFF_BED', 'WALKING', 1, CONCAT(CURDATE(), 'T17:00:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 17:00:00')) * 1000)),
-- DEV_008 周佩兰（高风险，跌倒风险高）
('DEV_008', 42, 92, 23, 'AWAKE',       50, 9.8, 'OFF_BED', 'WALKING', 1, CONCAT(CURDATE(), 'T08:45:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 08:45:00')) * 1000)),
('DEV_008', 45, 86, 21, 'LIGHT_SLEEP', 58, 2.4, 'ON_BED', 'LYING', 1, CONCAT(CURDATE(), 'T13:30:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE(), ' 13:30:00')) * 1000)),
-- 离线设备样例：DEV_007 最后一次上报后离线
('DEV_007', 55, 82, 20, 'AWAKE', 60, 3.0, 'ON_BED', 'SITTING', 0, CONCAT(CURDATE() - INTERVAL 1 DAY, 'T20:00:00'), ROUND(UNIX_TIMESTAMP(CONCAT(CURDATE() - INTERVAL 1 DAY, ' 20:00:00')) * 1000));

-- 环境监测演示数据：温湿度/空气质量/光照，含一条离线记录
INSERT INTO `environment_history`
(`device_id`, `temperature`, `humidity`, `air_quality`, `illumination`, `device_online`, `report_time`, `recorded_at`)
VALUES
('DEV_001', 22.5, 55.0, 62.0, 320.50, 1, NOW() - INTERVAL 240 MINUTE, ROUND(UNIX_TIMESTAMP(NOW() - INTERVAL 240 MINUTE) * 1000)),
('DEV_001', 23.1, 53.5, 58.0, 450.00, 1, NOW() - INTERVAL 180 MINUTE, ROUND(UNIX_TIMESTAMP(NOW() - INTERVAL 180 MINUTE) * 1000)),
('DEV_001', 24.0, 52.0, 71.5, 380.20, 1, NOW() - INTERVAL 60 MINUTE,  ROUND(UNIX_TIMESTAMP(NOW() - INTERVAL 60 MINUTE) * 1000)),
('DEV_002', 21.8, 60.5, 45.0, 210.00, 1, NOW() - INTERVAL 120 MINUTE, ROUND(UNIX_TIMESTAMP(NOW() - INTERVAL 120 MINUTE) * 1000)),
('DEV_003', 23.5, 48.0, 35.0, 620.80, 1, NOW() - INTERVAL 90 MINUTE,  ROUND(UNIX_TIMESTAMP(NOW() - INTERVAL 90 MINUTE) * 1000)),
('DEV_004', 26.8, 65.5, 88.0, 150.00, 1, NOW() - INTERVAL 45 MINUTE,  ROUND(UNIX_TIMESTAMP(NOW() - INTERVAL 45 MINUTE) * 1000)),
('DEV_005', 20.5, 70.0, 95.5, 80.00,  1, NOW() - INTERVAL 30 MINUTE,  ROUND(UNIX_TIMESTAMP(NOW() - INTERVAL 30 MINUTE) * 1000)),
('DEV_006', 23.0, 50.0, 30.0, 700.00, 1, NOW() - INTERVAL 20 MINUTE,  ROUND(UNIX_TIMESTAMP(NOW() - INTERVAL 20 MINUTE) * 1000)),
('DEV_007', 18.2, 75.0, 92.0, 5.50,   0, NOW() - INTERVAL 600 MINUTE, ROUND(UNIX_TIMESTAMP(NOW() - INTERVAL 600 MINUTE) * 1000)),
('DEV_008', 22.0, 58.0, 66.0, 260.00, 1, NOW() - INTERVAL 15 MINUTE,  ROUND(UNIX_TIMESTAMP(NOW() - INTERVAL 15 MINUTE) * 1000));

-- 预警事件演示数据：覆盖摔倒/求助/情绪/烟雾四类，状态含未处理/处理中/已解决
-- elder_name/room 为事件发生时快照；timeline_json 为处理时间线
INSERT INTO `alerts`
(`elder_name`, `room`, `type`, `description`, `time`, `status`, `timeline_json`, `notes`, `created_at`)
VALUES
('周佩兰', '2号楼401', 'FALL', '监测到疑似跌倒：卫生间区域体动指数突增后长时间静止', CONCAT(CURDATE(), ' 07:52'), 'NEW', NULL, NULL, NOW() - INTERVAL 200 MINUTE),
('陈守田', '1号楼101', 'EMERGENCY', '老人主动按下床头一键求助按钮', CONCAT(CURDATE(), ' 09:31'), 'PROCESSING',
 '[{"time":"2026-09-21 09:31","action":"触发求助","detail":"床头按钮按下"},{"time":"2026-09-21 09:35","action":"社区接单","detail":"工作人员张伟开始处理"}]',
 '已电话联系家属，正在上门查看', NOW() - INTERVAL 150 MINUTE),
('刘德海', '3号楼201', 'SMOKE', '烟雾传感器浓度超标，疑似厨房油烟/火情', CONCAT(CURDATE(), ' 11:05'), 'PROCESSING',
 '[{"time":"2026-09-21 11:05","action":"烟雾告警","detail":"浓度 95.5 超阈值"},{"time":"2026-09-21 11:08","action":"通知住户","detail":"电话提醒老人检查厨房"}]',
 '初步判断为炒菜油烟，持续观察中', NOW() - INTERVAL 120 MINUTE),
('张桂芳', '2号楼102', 'PRESSURE', 'AI 情绪分析：连续 3 日语音互动情绪低落', CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 18:40'), 'CLOSED',
 '[{"time":"2026-09-20 18:40","action":"情绪预警","detail":"情绪评分连续偏低"},{"time":"2026-09-20 19:10","action":"上门探访","detail":"社区志愿者陪同聊天 40 分钟"},{"time":"2026-09-20 21:00","action":"事件关闭","detail":"情绪明显好转"}]',
 '志愿者已安排每周两次定期探访', NOW() - INTERVAL 1500 MINUTE),
('孙耀祖', '1号楼305', 'FALL', '夜间起床徘徊时间异常偏长，疑似跌倒后自行爬起', CONCAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), ' 03:15'), 'CLOSED',
 '[{"time":"2026-09-19 03:15","action":"夜间异常","detail":"徘徊 25 分钟未回床"},{"time":"2026-09-19 08:30","action":"核实","detail":"老人自述起身找水喝，无受伤"},{"time":"2026-09-19 09:00","action":"事件关闭","detail":"确认为误报，建议加装夜灯"}]',
 '家属已确认安全；已为房间加装感应夜灯', NOW() - INTERVAL 3000 MINUTE),
('王秀兰', '1号楼203', 'PRESSURE', 'AI 情绪分析：今日午后情绪评分低于阈值', CONCAT(CURDATE(), ' 14:25'), 'NEW', NULL, NULL, NOW() - INTERVAL 60 MINUTE);

package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.HealthData;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 健康数据服务类
 * 负责基于老人ID生成稳定的差异化模拟健康画像与健康历史时序数据，并提供分页查询能力；
 * 画像与采样逻辑在卡片、详情曲线、风险评估等场景统一复用，保证各处数据口径一致。
 * */
@Service
public class HealthDataService {

    @Resource
    private ElderlyAccessService elderlyAccessService;

    /**
     * 生成指定老人最近一段时间的模拟健康数据列表。
     * 以老人ID对应的稳定画像基线为基础，按30分钟固定间隔在基线上叠加小幅确定性扰动，
     * 采样点数随时间跨度自适应（不足时取10个，最多50个）。
     *
     * @param elderlyId 老人ID，为null时按ID=1处理
     * @param minutes   距当前时间的分钟数，用于确定数据时间范围与采样点数
     * @return 模拟健康数据列表，记录时间由近及远排列
     */
    public List<HealthData> generateMockHealthDataList(Long elderlyId, int minutes) {
        List<HealthData> mockData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("+8"));
        LocalDateTime startTime = now.minusMinutes(minutes);
        
        long totalMinutes = Duration.between(startTime, now).toMinutes();
        int intervalMinutes = 30;
        int points = (int) (totalMinutes / intervalMinutes);
        if (points == 0) points = 10;
        if (points > 50) points = 50;
        
        // 使用统一的差异化画像，风险评估时用这些基线值再加轻微时序扰动
        Object[] profile = buildMockHealthProfile(elderlyId);
        int baseHeart   = (int) profile[1];
        int baseBreath  = (int) profile[2];
        int baseSleep   = (int) profile[3];
        int baseMotion  = (int) profile[4];
        String sleepStatus = mockSleepStatus(profile);
        String onBedStatus  = mockOnBedStatus(profile);

        // 风险层级决定了波动幅度（高危：波动大；低危：波动小）
        int tier = (int) profile[0];
        int heartJitter  = (tier == 2) ? 10 : (tier == 1 ? 6 : 4);
        int breathJitter = (tier == 2) ? 4  : (tier == 1 ? 3 : 2);
        int sleepJitter  = (tier == 2) ? 10 : (tier == 1 ? 7 : 4);
        long seed = Math.abs((elderlyId == null ? 1L : elderlyId) * 2654435761L);
        
        for (int i = 0; i < points; i++) {
            HealthData data = new HealthData();
            data.setElderlyId(elderlyId);
            data.setDeviceId("mock_device_" + elderlyId);
            
            // 每个时间点在画像基线上加小幅随机扰动，模拟真实波动
            long rnd = (seed + i * 2654435761L) >>> 8;
            double hr = baseHeart  + ((int)(rnd % (heartJitter * 2 + 1)) - heartJitter);
            double br = baseBreath + ((int)((rnd >> 8) % (breathJitter * 2 + 1)) - breathJitter);
            double ss = baseSleep  + ((int)((rnd >> 16) % (sleepJitter * 2 + 1)) - sleepJitter);
            double mi = baseMotion + ((int)((rnd >> 4) % 11)) - 5;
            if (hr < 30) hr = 30;
            if (br < 6)  br = 6;
            if (ss < 20) ss = 20;
            if (ss > 100) ss = 100;
            if (mi < 0)  mi = 0;

            data.setHeartRate(hr);
            data.setBreathingRate(br);
            data.setSleepScore(ss);
            data.setMotionIndex(mi);
            data.setSleepStatus(sleepStatus);
            data.setOnBedStatus(onBedStatus);

            LocalDateTime recordTime = now.minusMinutes(i * intervalMinutes);
            data.setRecordedAt(recordTime.atZone(ZoneId.of("+8")).toInstant().toEpochMilli());
            data.setReportTime(recordTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            mockData.add(data);
        }
        
        return mockData;
    }

    /**
     * 分页查询老人健康数据：先校验当前用户对该老人的访问权限，再返回模拟历史数据。
     *
     * @param elderlyId 老人ID
     * @param pageNum   页码，从1开始
     * @param pageSize  每页条数
     * @param start     查询起始时间，为null时默认最近24小时
     * @param end       查询截止时间，为null时默认当前时间
     * @return 健康数据分页结果，记录已拆分为前端指标行
     */
    public Page<HealthData> page(Long elderlyId, int pageNum, int pageSize,
                                 LocalDateTime start, LocalDateTime end) {
        elderlyAccessService.assertCanAccessElderly(elderlyId);
        return generateMockHealthHistory(elderlyId, pageNum, pageSize, start, end);
    }

    /**
     * 基于老人ID生成统一的差异化健康画像（所有模拟数据生成都调用此方法，保证卡片/详情/风险评估数据一致）
     * 
     * 返回 int/long 混合的对象数组，索引含义：
     *  [0] riskTier (0=LOW低危, 1=MEDIUM中危, 2=HIGH高危)
     *  [1] heartRate (心率)
     *  [2] breathingRate (呼吸)
     *  [3] sleepScore  (睡眠评分 0-100)
     *  [4] motionIndex (体动次数)
     *  [5] sleepStatusIdx  0=深睡,1=浅睡,2=清醒（仅保留3种状态）
     *  [6] onBed (1=ON_BED, 0=OFF_BED)
     *  [7] healthScore (健康评分 0-100)
     *  
     *  护理等级与ID哈希分布（对应 determineNursingLevel 硬规则）：
     *   - 自理（四级）：healthScore >= 80   → 30%  (tierIdx 0-29)
     *   - 一般护理（三级）：60~79           → 30%  (tierIdx 30-59)
     *   - 重点护理（二级）：40~59           → 30%  (tierIdx 60-89)
     *   - 特别护理（一级）：< 40            → 10%  (tierIdx 90-99)
     *  各指标基线严格对齐 calculateHealthScoreByRules 评分标准，确保 AI 评估结果落在目标护理等级区间。
     *
     * @param elderlyIdRaw 老人ID原始值，为null时按ID=1处理
     * @return 画像对象数组，各索引含义见上述说明
     */
    public static Object[] buildMockHealthProfile(Long elderlyIdRaw) {
        long elderlyId = elderlyIdRaw == null ? 1L : elderlyIdRaw;
        long seed = Math.abs(elderlyId * 2654435761L ^ 0x5bd1e995L);
        int tierIdx = (int) (seed % 100); // 0-99

        int riskTier;
        int heartMin, heartMax;
        int breathMin, breathMax;
        int sleepMin, sleepMax;
        int motionMin, motionMax;
        int scoreMin, scoreMax;
        int sleepMinIdx, sleepMaxIdx;

        if (tierIdx < 30) {
            // =====================================================
            // 护理等级：四级 / 自理 (healthScore >= 80)
            // 评分组成：心率25 + 呼吸25 + 体动20 + 睡眠评分15 + 睡眠状态15 = 100
            // =====================================================
            riskTier = 0;
            heartMin = 68;    heartMax = 88;     // 51-100 → 心率 25分
            breathMin = 10;   breathMax = 13;    // 9-14  → 呼吸 25分
            sleepMin = 82;    sleepMax = 95;     // >=70  → 睡眠评分 15分
            motionMin = 18;   motionMax = 28;    // 15-35 → 体动 20分
            scoreMin = 82;    scoreMax = 93;     // >= 80 → 自理
            sleepMinIdx = 0;  sleepMaxIdx = 2;   // 深睡/浅睡/清醒 → 3种状态
        } else if (tierIdx < 60) {
            // =====================================================
            // 护理等级：三级 / 一般护理 (healthScore = 60 ~ 79)
            // 评分组成：心率15 + 呼吸15 + 体动15 + 睡眠评分10 + 睡眠状态10 = 65
            // =====================================================
            riskTier = 1;
            // 心率偏快/偏慢 → 41-50 或 101-110 → 15分
            if (tierIdx % 2 == 0) {
                heartMin = 102; heartMax = 109;
            } else {
                heartMin = 43;  heartMax = 49;
            }
            breathMin = 16;   breathMax = 19;    // 15-20   → 呼吸 15分
            sleepMin = 55;    sleepMax = 66;     // 50-69   → 睡眠评分 10分
            motionMin = 38;   motionMax = 46;    // 36-50   → 体动 15分
            scoreMin = 63;    scoreMax = 76;     // 60-79   → 一般护理
            sleepMinIdx = 1;  sleepMaxIdx = 2;   // 浅睡/清醒 → 3种里取中后两档
        } else if (tierIdx < 90) {
            // =====================================================
            // 护理等级：二级 / 重点护理 (healthScore = 40 ~ 59)
            // 评分组成：心率5 + 呼吸25 + 体动10 + 睡眠评分4 + 睡眠状态5 = 49
            // =====================================================
            riskTier = 1;
            // 心率明显异常 → <=40 或 111-129 → 5分
            if (tierIdx % 2 == 0) {
                heartMin = 116; heartMax = 126;
            } else {
                heartMin = 33;  heartMax = 40;
            }
            breathMin = 10;   breathMax = 13;    // 9-14    → 呼吸 25分（仅此项拉满，总分不至于太低）
            sleepMin = 33;    sleepMax = 47;     // < 50    → 睡眠评分 4分
            motionMin = 53;   motionMax = 65;    // 51-70   → 体动 10分
            scoreMin = 44;    scoreMax = 56;     // 40-59   → 重点护理
            sleepMinIdx = 1;  sleepMaxIdx = 2;   // 浅睡/清醒 → 3种里取中后两档
        } else {
            // =====================================================
            // 护理等级：一级 / 特别护理 (healthScore < 40)
            // 评分组成：心率5 + 呼吸5 + 体动5 + 睡眠评分4 + 睡眠状态5 = 24
            // =====================================================
            riskTier = 2;
            // 心率高危 → <=40 或 111-129 → 5分
            if (tierIdx % 2 == 0) {
                heartMin = 119; heartMax = 130;
            } else {
                heartMin = 28;  heartMax = 38;
            }
            // 呼吸高危 → <9 或 21-29 → 5分
            if ((tierIdx / 5) % 2 == 0) {
                breathMin = 22; breathMax = 28;
            } else {
                breathMin = 6;  breathMax = 8;
            }
            sleepMin = 22;    sleepMax = 40;     // < 50    → 睡眠评分 4分
            motionMin = 75;   motionMax = 88;    // > 70    → 体动 5分
            scoreMin = 14;    scoreMax = 36;     // < 40    → 特别护理
            sleepMinIdx = 2;  sleepMaxIdx = 2;   // 清醒 → 取第3种状态
        }

        // 每个老人取不同的偏移量，保证数据有差异
        long s1 = (seed * 1103515245L + 12345L) >>> 16;
        long s2 = (seed * 214013L + 2531011L) >>> 16;
        long s3 = (seed * 134775813L + 1L) >>> 16;
        long s4 = (seed * 69069L + 1L) >>> 16;
        long s5 = (seed * 741103597L) >>> 16;
        long s6 = (seed * 35714285L + 7L) >>> 16;

        int heartRate  = heartMin + (int)(s1 % (heartMax - heartMin + 1));
        int breathRate = breathMin + (int)(s2 % (breathMax - breathMin + 1));
        int sleepScore = sleepMin + (int)(s3 % (sleepMax - sleepMin + 1));
        int motionIdx  = motionMin + (int)(s4 % (motionMax - motionMin + 1));
        int healthScore= scoreMin + (int)(s5 % (scoreMax - scoreMin + 1));
        int sleepStatusIdx = sleepMinIdx + (int)(s6 % (sleepMaxIdx - sleepMinIdx + 1));

        // 在床/离床：深睡/浅睡时一定在床；清醒时有25%概率离床
        int onBed;
        if (sleepStatusIdx < 2) {
            onBed = 1;
        } else {
            onBed = (s1 % 4 == 0) ? 0 : 1;
        }

        return new Object[]{riskTier, heartRate, breathRate, sleepScore, motionIdx, sleepStatusIdx, onBed, healthScore};
    }

    /**
     * 由画像数组获取睡眠状态中文（仅3种：深睡/浅睡/清醒）。
     *
     * @param profile 健康画像数组，取索引[5]作为睡眠状态下标
     * @return 睡眠状态中文名称，下标越界时收敛到合法范围
     */
    public static String mockSleepStatus(Object[] profile) {
        int idx = (int) profile[5];
        String[] sleeps = {"深睡", "浅睡", "清醒"};
        if (idx < 0) idx = 0;
        if (idx >= sleeps.length) idx = sleeps.length - 1;
        return sleeps[idx];
    }
    /**
     * 由画像数组获取在床状态。
     *
     * @param profile 健康画像数组，取索引[6]判断在床或离床
     * @return 在床状态字符串，值为1返回"ON_BED"，否则返回"OFF_BED"
     */
    public static String mockOnBedStatus(Object[] profile) {
        return ((int) profile[6] == 1) ? "ON_BED" : "OFF_BED";
    }

    /**
     * 将一条健康记录拆成前端"健康数据"表格需要的指标行
     * （metricType/metricValue/unit/recordTime），仅输出有值的指标。
     */
    private static List<HealthData> transformToMetrics(HealthData h) {
        List<HealthData> rows = new ArrayList<>();
        if (h == null) return rows;
        addNumericMetricRow(rows, h, "HEART_RATE", h.getHeartRate(), "次/分");
        addNumericMetricRow(rows, h, "BREATHING_RATE", h.getBreathingRate(), "次/分");
        addNumericMetricRow(rows, h, "MOTION_INDEX", h.getMotionIndex(), "次");
        addNumericMetricRow(rows, h, "SLEEP_SCORE", h.getSleepScore(), "分");
        addTextMetricRow(rows, h, "SLEEP_STATUS", h.getSleepStatus());
        addTextMetricRow(rows, h, "ON_BED_STATUS", h.getOnBedStatus());
        return rows;
    }

    /**
     * 向指标行列表追加一条数值型指标行，值为null或小于等于0时跳过，数值按整数形式输出。
     */
    private static void addNumericMetricRow(List<HealthData> rows, HealthData source, String metricType, Double value, String unit) {
        if (value == null || value <= 0) return;
        HealthData row = newMetricRow(source, metricType, unit);
        row.setMetricValue(String.valueOf(value.intValue()));
        rows.add(row);
    }

    /**
     * 向指标行列表追加一条文本型指标行，值为null或空字符串时跳过。
     */
    private static void addTextMetricRow(List<HealthData> rows, HealthData source, String metricType, String value) {
        if (value == null || value.isEmpty()) return;
        HealthData row = newMetricRow(source, metricType, null);
        row.setMetricValue(value);
        rows.add(row);
    }

    /**
     * 以源记录为模板构造一条空白指标行，复制老人ID、设备ID及上报时间、记录时间戳。
     */
    private static HealthData newMetricRow(HealthData source, String metricType, String unit) {
        HealthData row = new HealthData();
        row.setMetricType(metricType);
        row.setUnit(unit);
        row.setElderlyId(source.getElderlyId());
        row.setDeviceId(source.getDeviceId());
        row.setReportTime(source.getReportTime());
        row.setRecordedAt(source.getRecordedAt());
        return row;
    }

    /**
     * 生成模拟健康历史数据，完成指标行拆分并执行内存分页。
     * 采样间隔随时间跨度自适应（1/5/15/60分钟）；点数不足时在序列前方补充镜像记录，
     * 最后将每条记录拆成多个指标行，并按页码、页大小截取当前页。
     */
    private Page<HealthData> generateMockHealthHistory(Long elderlyId, int pageNum, int pageSize, LocalDateTime start, LocalDateTime end) {
        List<HealthData> mockData = new ArrayList<>();
        LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("+8"));
        
        LocalDateTime startTime = start != null ? start : now.minusHours(24);
        LocalDateTime endTime = end != null ? end : now;
        
        long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
        
        int intervalMinutes;
        if (minutes <= 30) {
            intervalMinutes = 1;
        } else if (minutes <= 120) {
            intervalMinutes = 5;
        } else if (minutes <= 720) {
            intervalMinutes = 15;
        } else {
            intervalMinutes = 60;
        }
        
        int points = (int) (minutes / intervalMinutes);
        if (points == 0) points = 10;
        if (points > pageSize) points = pageSize;

        // 复用 generateMockHealthDataList 的同一套逻辑，保证曲线和风险评估数据一致
        List<HealthData> source = generateMockHealthDataList(elderlyId,
                (int) Math.max(minutes, (long) points * intervalMinutes));
        // 截取需要的条数（generateMockHealthDataList 默认间隔30分钟，这里按目标点数适当截取或扩展）
        if (source.size() > points) {
            source = source.subList(0, points);
        }
        mockData.addAll(source);
        // 如果点数还不够（间隔更小的场景），则在前面补充镜像点
        while (mockData.size() < points) {
            LocalDateTime recordTime = endTime.minusMinutes(mockData.size() * (long) intervalMinutes);
            HealthData first = mockData.isEmpty() ? null : mockData.get(0);
            HealthData copy = new HealthData();
            copy.setElderlyId(elderlyId);
            copy.setDeviceId("mock_device_" + elderlyId);
            if (first != null) {
                copy.setHeartRate(first.getHeartRate());
                copy.setBreathingRate(first.getBreathingRate());
                copy.setSleepScore(first.getSleepScore());
                copy.setMotionIndex(first.getMotionIndex());
                copy.setSleepStatus(first.getSleepStatus());
                copy.setOnBedStatus(first.getOnBedStatus());
            } else {
                Object[] profile = buildMockHealthProfile(elderlyId);
                copy.setHeartRate(Double.valueOf((int) profile[1]));
                copy.setBreathingRate(Double.valueOf((int) profile[2]));
                copy.setSleepScore(Double.valueOf((int) profile[3]));
                copy.setMotionIndex(Double.valueOf((int) profile[4]));
                copy.setSleepStatus(mockSleepStatus(profile));
                copy.setOnBedStatus(mockOnBedStatus(profile));
            }
            copy.setRecordedAt(recordTime.atZone(java.time.ZoneId.of("+8")).toInstant().toEpochMilli());
            copy.setReportTime(recordTime.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            mockData.add(0, copy);
        }
        
        List<HealthData> transformed = new ArrayList<>();
        for (HealthData h : mockData) {
            transformed.addAll(transformToMetrics(h));
        }

        Page<HealthData> page = new Page<>(pageNum, pageSize);
        page.setTotal(transformed.size());
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, transformed.size());
        page.setRecords(fromIndex < transformed.size() ? new ArrayList<>(transformed.subList(fromIndex, toIndex)) : new ArrayList<>());

        return page;
    }
}

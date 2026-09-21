package com.example.demo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.auth.AuthContext;
import com.example.demo.dto.AssessAllVO;
import com.example.demo.dto.CommunityOverviewVO;
import com.example.demo.dto.CommunityOverviewVO.ElderlyRiskRow;
import com.example.demo.entity.AlertEvent;
import com.example.demo.entity.Elderly;
import com.example.demo.enums.UserRole;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.AlertEventMapper;
import com.example.demo.mapper.ElderlyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 辖区概览与一键评估服务。
 * <p>聚合全部老人档案与告警事件，统计待处理/紧急未关闭事件、性别年龄分布、设备绑定与
 * 事件解决情况，并结合模拟生命体征画像计算每位老人的健康评分、风险等级与护理等级，
 * 同时提供社区工作人员/管理员的全员一键评估能力。</p>
 * */
@Service
public class CommunityOverviewService {

    /** 待处理事件状态口径：只算还没人接手的 NEW / PENDING / 未处理 */
    private static final List<String> NEW_ONLY = Arrays.asList("NEW", "PENDING", "未处理");
    /** 紧急未关闭事件状态口径：待处理口径 + 处理中（PROCESSING / IN_PROGRESS / 处理中） */
    private static final List<String> NEW_OR_PROCESSING = Arrays.asList(
            "NEW", "PENDING", "未处理",
            "PROCESSING", "IN_PROGRESS", "处理中"
    );

    /** 已解决事件状态口径：RESOLVED / CLOSED / ACK / FALSE_ALARM 及对应中文同义词 */
    private static final List<String> RESOLVED_VALUES = Arrays.asList(
            "RESOLVED", "已解决",
            "CLOSED", "已关闭",
            "ACK", "已确认",
            "FALSE_ALARM", "误报"
    );

    // ===== 健康评分：各项满分权重与区间得分 =====
    private static final int HEART_SCORE_NORMAL = 25;
    private static final int HEART_SCORE_MILD = 15;
    private static final int HEART_SCORE_SEVERE = 5;
    private static final int HEART_SCORE_CRITICAL = 0;
    private static final int HEART_SCORE_FALLBACK = 12;

    private static final int BREATH_SCORE_NORMAL = 25;
    private static final int BREATH_SCORE_MILD = 15;
    private static final int BREATH_SCORE_SEVERE = 5;
    private static final int BREATH_SCORE_CRITICAL = 0;
    private static final int BREATH_SCORE_FALLBACK = 12;

    private static final int MOTION_SCORE_NORMAL = 20;
    private static final int MOTION_SCORE_MILD = 15;
    private static final int MOTION_SCORE_MODERATE = 10;
    private static final int MOTION_SCORE_LOW = 5;
    private static final int MOTION_SCORE_FALLBACK = 10;

    private static final int SLEEP_SCORE_GOOD = 15;
    private static final int SLEEP_SCORE_FAIR = 10;
    private static final int SLEEP_SCORE_POOR = 4;
    private static final int SLEEP_SCORE_FALLBACK = 7;

    private static final int SLEEP_STATUS_GOOD = 15;
    private static final int SLEEP_STATUS_FAIR = 10;
    private static final int SLEEP_STATUS_AWAKE = 7;
    private static final int SLEEP_STATUS_POOR = 5;
    private static final int SLEEP_STATUS_FALLBACK = 7;

    // ===== 心率区间边界（次/分钟）=====
    private static final int HEART_NORMAL_LOW = 51;
    private static final int HEART_NORMAL_HIGH = 100;
    private static final int HEART_MILD_LOW1 = 41;
    private static final int HEART_MILD_HIGH1 = 50;
    private static final int HEART_MILD_LOW2 = 101;
    private static final int HEART_MILD_HIGH2 = 110;
    private static final int HEART_SEVERE_HIGH1 = 40;
    private static final int HEART_SEVERE_LOW2 = 111;
    private static final int HEART_SEVERE_HIGH2 = 129;
    private static final int HEART_CRITICAL = 130;

    // ===== 呼吸区间边界（次/分钟）=====
    private static final int BREATH_NORMAL_LOW = 9;
    private static final int BREATH_NORMAL_HIGH = 14;
    private static final int BREATH_MILD_LOW = 15;
    private static final int BREATH_MILD_HIGH = 20;
    private static final int BREATH_SEVERE_LOW = 21;
    private static final int BREATH_SEVERE_HIGH = 29;
    private static final int BREATH_CRITICAL = 30;

    // ===== 活动指数区间边界 =====
    private static final int MOTION_NORMAL_LOW = 15;
    private static final int MOTION_NORMAL_HIGH = 35;
    private static final int MOTION_MILD_LOW1 = 8;
    private static final int MOTION_MILD_HIGH1 = 15;
    private static final int MOTION_MILD_LOW2 = 35;
    private static final int MOTION_MILD_HIGH2 = 50;
    private static final int MOTION_MODERATE_LOW1 = 3;
    private static final int MOTION_MODERATE_HIGH1 = 8;
    private static final int MOTION_MODERATE_LOW2 = 50;
    private static final int MOTION_MODERATE_HIGH2 = 70;

    // ===== 睡眠评分边界 =====
    private static final int SLEEP_GOOD_THRESHOLD = 70;
    private static final int SLEEP_FAIR_THRESHOLD = 50;

    // ===== 护理等级阈值（按健康分）=====
    private static final int NURSING_LEVEL_4_THRESHOLD = 80;
    private static final int NURSING_LEVEL_3_THRESHOLD = 60;
    private static final int NURSING_LEVEL_2_THRESHOLD = 40;

    // ===== 风险与统计阈值 =====
    /** 紧急事件严重等级阈值：severity ≥ 3 视为紧急（如摔倒/烟雾/一键求助等3级事件） */
    private static final int URGENT_SEVERITY = 3;
    /** 有未关闭事件但 severity 全为空/0 时的兜底等级，避免这类事件被当成"无风险" */
    private static final int DEFAULT_FALLBACK_SEVERITY = 2;
    private static final int AGE_UNDER_70 = 70;
    private static final int AGE_OVER_80 = 80;
    private static final int HEALTH_SCORE_LOW = 60;
    private static final int HEALTH_SCORE_MEDIUM = 80;
    /** "近 N 天新增事件"的统计窗口天数 */
    private static final int RECENT_DAYS = 7;

    /** 判断状态是否属于"待处理"口径（仅 NEW/PENDING/未处理），status 为 null 时返回 false */
    // 待处理事件：只算 NEW / PENDING / 未处理（管理员一眼知道还没动手的）
    private static boolean isPendingOnly(String status) {
        if (status == null) return false;
        if (NEW_ONLY.contains(status)) return true;
        String s = status.trim().toUpperCase();
        return "NEW".equals(s) || "PENDING".equals(s) || status.contains("未处理");
    }

    /** 判断3级紧急事件是否尚未关闭（NEW/PENDING/未处理 + PROCESSING/IN_PROGRESS/处理中） */
    // 紧急未关闭：3级事件 + 还没关（NEW/PENDING/未处理 + PROCESSING/IN_PROGRESS/处理中）
    private static boolean isEmergencyOpen(String status) {
        if (status == null) return false;
        if (NEW_OR_PROCESSING.contains(status)) return true;
        String s = status.trim().toUpperCase();
        return "NEW".equals(s) || "PENDING".equals(s)
                || "PROCESSING".equals(s) || "IN_PROGRESS".equals(s)
                || status.contains("未处理") || status.contains("处理中");
    }

    /** 判断事件是否属于已解决口径（RESOLVED/CLOSED/ACK/FALSE_ALARM 及其中文同义词） */
    // 事件总数统计用：已解决（RESOLVED/已解决 + CLOSED/已关闭 + ACK/已确认 + FALSE_ALARM/误报）
    private static boolean isResolvedStatus(String status) {
        if (status == null) return false;
        if (RESOLVED_VALUES.contains(status)) return true;
        String s = status.trim().toUpperCase();
        return "已解决".equals(status) || "已关闭".equals(status) || "误报".equals(status) || "已确认".equals(status)
                || "RESOLVED".equals(s) || "CLOSED".equals(s) || "ACK".equals(s) || "FALSE_ALARM".equals(s);
    }

    /** 严重等级空值兜底：null 按 0 处理 */
    // severity null 安全兜底
    private static int safeSeverity(Integer severity) {
        return severity == null ? 0 : severity;
    }

    @Resource
    private ElderlyMapper elderlyMapper;
    @Resource
    private AlertEventMapper alertEventMapper;

    /**
     * 生成辖区概览数据。
     * <p>仅社区工作人员或管理员可访问；聚合老人总数、待处理与紧急未关闭告警数、近7天新增
     * 事件、性别/年龄/设备绑定/事件解决统计，以及按风险等级、未关闭事件数排序的逐人风险行。</p>
     *
     * @return 辖区概览视图对象
     * @throws BusinessException 当前登录角色无权查看时抛出（403）
     */
    public CommunityOverviewVO overview() {
        String role = UserRole.normalize(AuthContext.getRole());
        if (!UserRole.isCommunityOrAdmin(role)) {
            throw new BusinessException("403", "仅社区工作人员或管理员可查看辖区概览");
        }

        // 不再根据 communityId 过滤老人列表，直接查询全部
        List<Elderly> elders = elderlyMapper.selectList(
                Wrappers.<Elderly>lambdaQuery().orderByDesc(Elderly::getUpdatedAt)
        );
        CommunityOverviewVO vo = new CommunityOverviewVO();
        vo.setCommunityId(null);
        vo.setElderlyTotal(elders.size());
        if (elders.isEmpty()) {
            vo.setPendingAlertTotal(0);
            vo.setCriticalPendingTotal(0);
            vo.setNewAlertsLast7Days(0);
            return vo;
        }
        List<AlertEvent> scopedAlerts = loadScopedAlerts(elders);

        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_DAYS);
        vo.setNewAlertsLast7Days((int) scopedAlerts.stream()
                .filter(a -> a.getOccurredAt() != null && !a.getOccurredAt().isBefore(since))
                .count());
        // 按用户确认的口径：
        // 待处理事件    → isPendingOnly(status)  → 只算 NEW/未处理
        // 紧急未关闭    → severity≥3（摔倒/烟雾/一键求助） + isEmergencyOpen(status)
        //  → 即3级紧急 + NEW/未处理/PROCESSING/处理中
        List<AlertEvent> pending = scopedAlerts.stream()
                .filter(a -> isPendingOnly(a.getStatus()))
                .collect(Collectors.toList());
        vo.setPendingAlertTotal(pending.size());
        vo.setCriticalPendingTotal((int) scopedAlerts.stream()
                .filter(a -> safeSeverity(a.getSeverity()) >= URGENT_SEVERITY && isEmergencyOpen(a.getStatus()))
                .count());
        // 性别统计
        vo.setMaleCount((int) elders.stream().filter(e -> "男".equals(e.getGender()) || "M".equalsIgnoreCase(e.getGender())).count());
        vo.setFemaleCount((int) elders.stream().filter(e -> "女".equals(e.getGender()) || "F".equalsIgnoreCase(e.getGender())).count());
        // 年龄分布
        vo.setAgeUnder70((int) elders.stream().filter(e -> e.getAge() != null && e.getAge() < AGE_UNDER_70).count());
        vo.setAge70to80((int) elders.stream().filter(e -> e.getAge() != null && e.getAge() >= AGE_UNDER_70 && e.getAge() < AGE_OVER_80).count());
        vo.setAgeOver80((int) elders.stream().filter(e -> e.getAge() != null && e.getAge() >= AGE_OVER_80).count());
        // 设备绑定状态
        vo.setDeviceBoundCount((int) elders.stream().filter(e -> e.getMonitorDeviceId() != null && !e.getMonitorDeviceId().isEmpty()).count());
        vo.setDeviceUnboundCount((int) elders.stream().filter(e -> e.getMonitorDeviceId() == null || e.getMonitorDeviceId().isEmpty()).count());
        // 事件统计：已解决/未解决与 isResolvedStatus 对称，避免同义词口径不一致
        vo.setAlertEventTotal(scopedAlerts.size());
        long resolved = scopedAlerts.stream()
                .filter(a -> isResolvedStatus(a.getStatus()))
                .count();
        vo.setAlertResolvedCount((int) resolved);
        vo.setAlertUnresolvedCount((int) (scopedAlerts.size() - resolved));
        // loadScopedAlerts 已按姓名兜底回填 elderlyId，直接分组
        Map<Long, List<AlertEvent>> openByElderly = pending.stream()
                .filter(a -> a.getElderlyId() != null)
                .collect(Collectors.groupingBy(AlertEvent::getElderlyId));
        // 逐人风险行组装：每个老人一行，含基础信息、未关闭事件、体征画像与评估结论
        List<ElderlyRiskRow> rows = new ArrayList<>();
        for (Elderly e : elders) {
            ElderlyRiskRow row = new ElderlyRiskRow();
            row.setElderlyId(e.getId());
            row.setName(e.getRealName());
            row.setGender(e.getGender());
            row.setAge(e.getAge());
            row.setRoom(e.getRoom());
            row.setPhone(e.getPhone());
            row.setAddress(e.getAddress());
            // 该老人名下的未关闭事件：数量 + 最高严重等级，是风险等级的核心输入
            List<AlertEvent> open = openByElderly.getOrDefault(e.getId(), new ArrayList<>());
            row.setOpenAlertCount(open.size());
            // 取未关闭事件中的最高严重等级；事件存在但 severity 全为空/0 时，用默认等级兜底
            int rawMaxSev = open.stream()
                    .map(AlertEvent::getSeverity)
                    .filter(s -> s != null)
                    .mapToInt(Integer::intValue)
                    .max().orElse(0);
            int effectiveSev = open.isEmpty() ? 0 : (rawMaxSev > 0 ? rawMaxSev : DEFAULT_FALLBACK_SEVERITY);
            row.setMaxOpenSeverity(open.isEmpty() ? null : effectiveSev);
            // 健康数据：统一由模拟采集器生成差异化画像
            fillMockHealth(row, e.getId());
            // 环境数据：统一由模拟采集器生成
            fillMockEnv(row, e.getId());
            // 评估结论：风险等级看"未关闭事件 + 三项体征"，健康评分看"五项体征加权"
            row.setRiskLevel(computeRiskLevel(row.getOpenAlertCount(), effectiveSev, row.getLastHealthScore(), row.getHeartRate(), row.getBreathingRate(), row.getSleepScore()));
            row.setHealthScore(computeHealthScore(row.getHeartRate(), row.getBreathingRate(), null, row.getSleepScore(), row.getSleepStatus()));
            // 风险提示语：档案已登记风险因素则直接拆分使用，否则按未关闭事件与健康分自动生成
            row.setHints(e.getRiskFactors() != null && !e.getRiskFactors().isEmpty() ? 
            Arrays.asList(e.getRiskFactors().split("\\s*,\\s*")) : buildHints(open, row.getLastHealthScore()));
            rows.add(row);
        }
        // 排序：HIGH > MEDIUM > LOW；同级内未关闭事件多的在前；最后按老人ID保证顺序稳定
        rows.sort(Comparator.comparingInt((ElderlyRiskRow r) -> riskSortKey(r.getRiskLevel()))
                .thenComparingInt(r -> -r.getOpenAlertCount())
                .thenComparing(ElderlyRiskRow::getElderlyId));
        vo.setRiskList(rows);
        return vo;
    }

    /**
     * 全量查询事件并归属到当前老人范围、按事件ID去重：
     * 规则1：elderlyId 命中档案；规则2：elderName 命中档案 realName/name 时归属并回填 elderlyId。
     * 查询失败时返回空列表，保证评估/概览不被事件表问题拖垮。
     */
    private List<AlertEvent> loadScopedAlerts(List<Elderly> elders) {
        // 当前范围内老人ID集合 + "姓名 → 老人ID"映射（事件只带姓名时的兜底归属依据）
        Set<Long> eidSet = elders.stream().map(Elderly::getId).collect(Collectors.toSet());
        Map<String, Long> nameToId = new HashMap<>();
        for (Elderly e : elders) {
            if (e.getRealName() != null && !e.getRealName().trim().isEmpty()) {
                nameToId.putIfAbsent(e.getRealName().trim(), e.getId());
            }
            if (e.getName() != null && !e.getName().trim().isEmpty()) {
                nameToId.putIfAbsent(e.getName().trim(), e.getId());
            }
        }
        // 全量查询事件（按创建时间倒序）；查询失败时降级为空列表，保证概览/评估不被事件表异常拖垮
        List<AlertEvent> allAlerts;
        try {
            allAlerts = alertEventMapper.selectList(
                    Wrappers.<AlertEvent>lambdaQuery().orderByDesc(AlertEvent::getCreatedAt));
        } catch (Exception ex) {
            allAlerts = new ArrayList<>();
        }
        // 逐条判断事件是否属于当前范围，并用 dedupMap 按事件ID去重（同一事件只保留一条）
        Map<Long, AlertEvent> dedupMap = new HashMap<>();
        for (AlertEvent a : allAlerts) {
            if (a == null || a.getId() == null) continue;
            Long elderId = a.getElderlyId();
            String elderName = a.getElderName();
            // 归属规则1：事件自带 elderlyId 且在当前老人范围内
            boolean belongToScope = elderId != null && eidSet.contains(elderId);
            // 归属规则2：ID没命中时，用事件上的老人姓名查映射表兜底，并回填 elderlyId
            if (!belongToScope && elderName != null && !elderName.trim().isEmpty()) {
                Long mappedId = nameToId.get(elderName.trim());
                if (mappedId != null && eidSet.contains(mappedId)) {
                    belongToScope = true;
                    if (elderId == null) a.setElderlyId(mappedId);
                }
            }
            if (!belongToScope) continue;
            // 最终兜底：仍缺 elderlyId 时用姓名再补一次，保证下游按老人分组可用
            if (a.getElderlyId() == null && elderName != null) {
                Long mappedId = nameToId.get(elderName.trim());
                if (mappedId != null) a.setElderlyId(mappedId);
            }
            dedupMap.putIfAbsent(a.getId(), a);
        }
        return new ArrayList<>(dedupMap.values());
    }

    /**
     * 一键评估：对全部老人基于最新监测画像与未处理事件重新计算
     * 健康评分、风险等级，并按健康分硬规则确定护理等级，结果保存到老人档案。
     *
     * @return 全员评估结果汇总（评估时间、各风险等级人数与逐人评估明细）
     */
    @Transactional(rollbackFor = Exception.class)
    public AssessAllVO assessAll() {
        String role = UserRole.normalize(AuthContext.getRole());
        if (!UserRole.isCommunityOrAdmin(role)) {
            throw new BusinessException("403", "仅社区工作人员或管理员可执行评估");
        }

        List<Elderly> elders = elderlyMapper.selectList(
                Wrappers.<Elderly>lambdaQuery().orderByAsc(Elderly::getId));

        AssessAllVO vo = new AssessAllVO();
        LocalDateTime now = LocalDateTime.now();
        vo.setEvaluatedAt(now);
        vo.setTotal(elders.size());
        if (elders.isEmpty()) {
            return vo;
        }

        // 只取"待处理"口径的事件并按老人ID分组，作为评估的告警维度输入
        Map<Long, List<AlertEvent>> openByElderly = loadScopedAlerts(elders).stream()
                .filter(a -> isPendingOnly(a.getStatus()))
                .filter(a -> a.getElderlyId() != null)
                .collect(Collectors.groupingBy(AlertEvent::getElderlyId));

        // 逐人评估：体征画像 + 未关闭事件 → 健康分/风险等级/护理等级 → 写回档案
        for (Elderly e : elders) {
            List<AlertEvent> open = openByElderly.getOrDefault(e.getId(), new ArrayList<>());
            int rawMaxSev = open.stream()
                    .map(AlertEvent::getSeverity)
                    .filter(s -> s != null)
                    .mapToInt(Integer::intValue)
                    .max().orElse(0);
            int effectiveSev = open.isEmpty() ? 0 : (rawMaxSev > 0 ? rawMaxSev : DEFAULT_FALLBACK_SEVERITY);

            // 与辖区概览同一套模拟画像 + 评分/风险算法，保证两处结论一致
            ElderlyRiskRow probe = new ElderlyRiskRow();
            fillMockHealth(probe, e.getId());
            int healthScore = computeHealthScore(probe.getHeartRate(), probe.getBreathingRate(),
                    null, probe.getSleepScore(), probe.getSleepStatus());
            String riskLevel = computeRiskLevel(open.size(), effectiveSev, probe.getLastHealthScore(),
                    probe.getHeartRate(), probe.getBreathingRate(), probe.getSleepScore());
            String nursingLevel = determineNursingLevel(healthScore);

            // 只更新评估相关字段，其余列保持不变
            Elderly update = new Elderly();
            update.setId(e.getId());
            update.setHealthScore(healthScore);
            update.setRiskLevel(riskLevel);
            update.setNursingLevel(nursingLevel);
            update.setUpdatedAt(now);
            elderlyMapper.updateById(update);

            AssessAllVO.AssessItem item = new AssessAllVO.AssessItem();
            item.setElderlyId(e.getId());
            item.setName(e.getRealName());
            item.setRoom(e.getRoom());
            item.setHeartRate(probe.getHeartRate());
            item.setBreathingRate(probe.getBreathingRate());
            item.setSleepScore(probe.getSleepScore());
            item.setSleepStatus(probe.getSleepStatus());
            item.setOpenAlertCount(open.size());
            item.setRiskLevel(riskLevel);
            item.setHealthScore(healthScore);
            item.setNursingLevel(nursingLevel);
            vo.getItems().add(item);
        }

        // 明细排序规则与概览列表一致；随后统计各风险等级人数供前端汇总展示
        vo.getItems().sort(Comparator.comparingInt((AssessAllVO.AssessItem i) -> riskSortKey(i.getRiskLevel()))
                .thenComparingInt(i -> -i.getOpenAlertCount())
                .thenComparing(AssessAllVO.AssessItem::getElderlyId));
        for (AssessAllVO.AssessItem item : vo.getItems()) {
            if ("HIGH".equals(item.getRiskLevel())) {
                vo.setHighCount(vo.getHighCount() + 1);
            } else if ("MEDIUM".equals(item.getRiskLevel())) {
                vo.setMediumCount(vo.getMediumCount() + 1);
            } else {
                vo.setLowCount(vo.getLowCount() + 1);
            }
        }
        return vo;
    }

    /**
     * 护理等级只由健康分决定（硬规则）：
     * ≥80 四级自理；60~79 三级一般护理；40~59 二级重点护理；&lt;40 一级特别护理。
     */
    private static String determineNursingLevel(int healthScore) {
        if (healthScore >= NURSING_LEVEL_4_THRESHOLD) return "LEVEL_4";
        if (healthScore >= NURSING_LEVEL_3_THRESHOLD) return "LEVEL_3";
        if (healthScore >= NURSING_LEVEL_2_THRESHOLD) return "LEVEL_2";
        return "LEVEL_1";
    }
    /** 风险等级排序键：HIGH 最靠前（0），MEDIUM 次之（1），LOW 及其他最后（2） */
    private static int riskSortKey(String level) {
        if ("HIGH".equals(level)) {
            return 0;
        }
        if ("MEDIUM".equals(level)) {
            return 1;
        }
        return 2;
    }
    /**
     * 计算风险等级：存在未关闭且最高严重等级≥3 的事件直接 HIGH；否则将心率、呼吸、睡眠评分
     * 按区间分别打分，任一项达高风险为 HIGH，任一异常为 MEDIUM；仍有未关闭事件为 MEDIUM，其余 LOW。
     */
    private static String computeRiskLevel(int openCount, int maxSeverity, Integer healthScore, Integer heartRate, Integer breathingRate, Double sleepScore) {
        // 第一优先级：有未关闭事件且最高严重等级 ≥3 → 直接判定高危
        boolean urgent = openCount > 0 && maxSeverity >= URGENT_SEVERITY;
        if (urgent) {
            return "HIGH";
        }
        // 心率风险分：0=正常(51~100) 1=轻度(41~50/101~110) 2=严重(≤40/111~129) 3=危急(≥130)；缺失按轻度处理
        int heartRiskScore = 0;
        if (heartRate != null) {
            if (heartRate >= HEART_NORMAL_LOW && heartRate <= HEART_NORMAL_HIGH) {
                heartRiskScore = 0;
            } else if ((heartRate >= HEART_MILD_LOW1 && heartRate <= HEART_MILD_HIGH1) || (heartRate >= HEART_MILD_LOW2 && heartRate <= HEART_MILD_HIGH2)) {
                heartRiskScore = 1;
            } else if (heartRate <= HEART_SEVERE_HIGH1 || (heartRate >= HEART_SEVERE_LOW2 && heartRate <= HEART_SEVERE_HIGH2)) {
                heartRiskScore = 2;
            } else if (heartRate >= HEART_CRITICAL) {
                heartRiskScore = 3;
            }
        } else {
            heartRiskScore = 1;
        }
        // 呼吸风险分：0=正常(9~14) 1=轻度(15~20) 2=严重(<9或21~29) 3=危急(≥30)；缺失按轻度处理
        int breathRiskScore = 0;
        if (breathingRate != null) {
            if (breathingRate >= BREATH_NORMAL_LOW && breathingRate <= BREATH_NORMAL_HIGH) {
                breathRiskScore = 0;
            } else if (breathingRate >= BREATH_MILD_LOW && breathingRate <= BREATH_MILD_HIGH) {
                breathRiskScore = 1;
            } else if (breathingRate < BREATH_NORMAL_LOW || (breathingRate >= BREATH_SEVERE_LOW && breathingRate <= BREATH_SEVERE_HIGH)) {
                breathRiskScore = 2;
            } else if (breathingRate >= BREATH_CRITICAL) {
                breathRiskScore = 3;
            }
        } else {
            breathRiskScore = 1;
        }
        // 睡眠风险分：0=良好(≥70) 1=一般(50~69) 2=差(<50)；缺失按一般处理
        int sleepRiskLevel = 0;
        if (sleepScore != null) {
            if (sleepScore >= SLEEP_GOOD_THRESHOLD) {
                sleepRiskLevel = 0;
            } else if (sleepScore >= SLEEP_FAIR_THRESHOLD) {
                sleepRiskLevel = 1;
            } else {
                sleepRiskLevel = 2;
            }
        } else {
            sleepRiskLevel = 1;
        }
        // 汇总判定：任一项严重异常 → HIGH；任一轻度异常 → MEDIUM；
        // 体征全正常但仍有未关闭事件 → MEDIUM（提醒还有事没处理完）；其余 → LOW
        if (heartRiskScore >= 2 || breathRiskScore >= 2 || sleepRiskLevel == 2) {
            return "HIGH";
        } else if (heartRiskScore == 1 || breathRiskScore == 1 || sleepRiskLevel == 1) {
            return "MEDIUM";
        } else if (openCount > 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * 计算 0~100 健康评分：心率25 + 呼吸25 + 活动指数20 + 睡眠评分15 + 睡眠状态15，
     * 各项按生理区间给分，数据缺失时取中性兜底分，总和四舍五入取整。
     */
    private static int computeHealthScore(Integer heartRate, Integer breathingRate, Integer motionIndex, Double sleepScore, String sleepStatus) {
        // 心率得分：正常25 / 轻度15 / 严重5 / 危急0；数据缺失取兜底12
        double heartScore = 0;
        // 呼吸得分：正常25 / 轻度15 / 严重5 / 危急0；数据缺失取兜底12
        double breathScore = 0;
        // 活动指数得分：适中20 / 轻度偏离15 / 中度偏离10 / 严重偏离5；数据缺失取兜底10
        double motionScore = 0;
        // 睡眠评分得分：好(≥70)15 / 一般(50~69)10 / 差(<50)4；数据缺失取兜底7
        double sleepScoreVal = 0;
        // 睡眠状态得分：深睡/良好15 / 浅睡10 / 清醒7 / 差5；数据缺失取兜底7
        double sleepStatusScore = 0;
        if (heartRate != null) {
            if (heartRate >= HEART_NORMAL_LOW && heartRate <= HEART_NORMAL_HIGH) {
                heartScore = HEART_SCORE_NORMAL;
            } else if ((heartRate >= HEART_MILD_LOW1 && heartRate <= HEART_MILD_HIGH1) || (heartRate >= HEART_MILD_LOW2 && heartRate <= HEART_MILD_HIGH2)) {
                heartScore = HEART_SCORE_MILD;
            } else if (heartRate <= HEART_SEVERE_HIGH1 || (heartRate >= HEART_SEVERE_LOW2 && heartRate <= HEART_SEVERE_HIGH2)) {
                heartScore = HEART_SCORE_SEVERE;
            } else {
                heartScore = HEART_SCORE_CRITICAL;
            }
        } else {
            heartScore = HEART_SCORE_FALLBACK;
        }
        if (breathingRate != null) {
            if (breathingRate >= BREATH_NORMAL_LOW && breathingRate <= BREATH_NORMAL_HIGH) {
                breathScore = BREATH_SCORE_NORMAL;
            } else if (breathingRate >= BREATH_MILD_LOW && breathingRate <= BREATH_MILD_HIGH) {
                breathScore = BREATH_SCORE_MILD;
            } else if (breathingRate < BREATH_NORMAL_LOW || (breathingRate >= BREATH_SEVERE_LOW && breathingRate <= BREATH_SEVERE_HIGH)) {
                breathScore = BREATH_SCORE_SEVERE;
            } else {
                breathScore = BREATH_SCORE_CRITICAL;
            }
        } else {
            breathScore = BREATH_SCORE_FALLBACK;
        }
        if (motionIndex != null) {
            if (motionIndex >= MOTION_NORMAL_LOW && motionIndex <= MOTION_NORMAL_HIGH) {
                motionScore = MOTION_SCORE_NORMAL;
            } else if ((motionIndex >= MOTION_MILD_LOW1 && motionIndex < MOTION_MILD_HIGH1) || (motionIndex > MOTION_MILD_LOW2 && motionIndex <= MOTION_MILD_HIGH2)) {
                motionScore = MOTION_SCORE_MILD;
            } else if ((motionIndex >= MOTION_MODERATE_LOW1 && motionIndex < MOTION_MODERATE_HIGH1) || 
            (motionIndex > MOTION_MODERATE_LOW2 && motionIndex <= MOTION_MODERATE_HIGH2)) {
                motionScore = MOTION_SCORE_MODERATE;
            } else {
                motionScore = MOTION_SCORE_LOW;
            }
        } else {
            motionScore = MOTION_SCORE_FALLBACK;
        }
        if (sleepScore != null) {
            if (sleepScore >= SLEEP_GOOD_THRESHOLD) {
                sleepScoreVal = SLEEP_SCORE_GOOD;
            } else if (sleepScore >= SLEEP_FAIR_THRESHOLD) {
                sleepScoreVal = SLEEP_SCORE_FAIR;
            } else {
                sleepScoreVal = SLEEP_SCORE_POOR;
            }
        } else {
            sleepScoreVal = SLEEP_SCORE_FALLBACK;
        }
        if (sleepStatus != null && !sleepStatus.isEmpty()) {
            String s = sleepStatus.toLowerCase();
            // 仅3种枚举：深睡(15分)/浅睡(10分)/清醒(7分中性）
            if (s.contains("良好") || s.contains("好") || s.contains("good") || s.contains("正常") || s.contains("normal") || s.contains("深睡")) {
                sleepStatusScore = SLEEP_STATUS_GOOD;
            } else if (s.contains("浅") || s.contains("一般") || s.contains("light")) {
                sleepStatusScore = SLEEP_STATUS_FAIR;
            } else if (s.contains("深睡不足") || s.contains("失眠") || s.contains("差") || s.contains("bad") || s.contains("异常")) {
                sleepStatusScore = SLEEP_STATUS_POOR;
            } else if (s.contains("清醒") || s.contains("wake") || s.contains("awake")) {
                sleepStatusScore = SLEEP_STATUS_AWAKE;
            } else {
                sleepStatusScore = SLEEP_STATUS_FAIR;
            }
        } else {
            sleepStatusScore = SLEEP_STATUS_FALLBACK;
        }
        // 五项加权求和后四舍五入取整，得到 0~100 的健康评分
        return (int) Math.round(heartScore + breathScore + motionScore + sleepScoreVal + sleepStatusScore);
    }

    /** 依据未关闭事件的紧急程度与近期健康评分生成风险提示语，无突出风险时返回"暂无突出风险" */
    private static List<String> buildHints(List<AlertEvent> open, Integer healthScore) {
        List<String> hints = new ArrayList<>();
        if (!open.isEmpty()) {
            long urgent = open.stream()
                    .filter(a -> a.getSeverity() != null && a.getSeverity() >= URGENT_SEVERITY)
                    .count();
            if (urgent > 0) {
                hints.add("存在未关闭的紧急/高等级告警");
            } else {
                hints.add("存在未关闭的异常事件");
            }
        }
        if (healthScore != null && healthScore < HEALTH_SCORE_LOW) {
            hints.add("近期健康评分偏低");
        } else if (healthScore != null && healthScore < HEALTH_SCORE_MEDIUM) {
            hints.add("健康评分有下降空间");
        }
        if (hints.isEmpty()) {
            hints.add("暂无突出风险");
        }
        return hints;
    }

    /** 无真实健康数据时填充模拟数据 */
    private static void fillMockHealth(ElderlyRiskRow row, Long elderlyId) {
        // 使用 HealthDataService 统一的差异化画像方法，保证卡片/详情/风险评估数据一致
        // 画像数组索引含义：[1]=心率 [2]=呼吸 [3]=睡眠评分 [5]=睡眠状态 [6]=在床 [7]=健康评分
        Object[] profile = HealthDataService.buildMockHealthProfile(elderlyId);
        if (row.getHeartRate() == null) {
            row.setHeartRate((int) profile[1]);
        }
        if (row.getBreathingRate() == null) {
            row.setBreathingRate((int) profile[2]);
        }
        String sleepStatus = HealthDataService.mockSleepStatus(profile);
        if (row.getSleepStatus() == null || row.getSleepStatus().isEmpty() || "null".equals(row.getSleepStatus())) {
            row.setSleepStatus(sleepStatus);
        }
        if (row.getOnBedStatus() == null || row.getOnBedStatus().isEmpty() || "null".equals(row.getOnBedStatus())) {
            row.setOnBedStatus(HealthDataService.mockOnBedStatus(profile));
        }
        if (row.getLastHealthScore() == null) {
            row.setLastHealthScore((int) profile[7]);
        }
        if (row.getSleepScore() == null) {
            row.setSleepScore(Double.valueOf((int) profile[3]));
        }
    }

    /** 无真实环境数据时填充模拟数据（基于老人ID生成稳定的差异化值） */
    private static void fillMockEnv(ElderlyRiskRow row, Long elderlyId) {
        long id = elderlyId == null ? 1L : elderlyId;
        // 用老人ID做线性同余种子：同一老人每次生成的温湿度稳定不变，不同老人之间有差异
        long seed = Math.abs(id * 1103515245L + 12345L);
        if (row.getTemperature() == null) {
            // 温度：22~30℃，每人有稳定的±4℃偏移
            double t = 24 + ((int)(seed % 800) - 200) / 100.0;
            row.setTemperature(Math.round(t * 10.0) / 10.0);
        }
        if (row.getHumidity() == null) {
            // 湿度：40~75%，每人有稳定的±15%偏移
            double h = 55 + ((int)((seed >> 16) % 3000) - 1500) / 100.0;
            h = Math.max(35, Math.min(80, h));
            row.setHumidity(Math.round(h * 10.0) / 10.0);
        }
        if (row.getEnvTime() == null) {
            int minsAgo = (int) ((seed >> 24) % 55);
            row.setEnvTime(java.time.LocalDateTime.now(java.time.ZoneId.of("+8"))
                    .minusMinutes(minsAgo).toString());
        }
    }
}

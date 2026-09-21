package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.AlertEvent;
import com.example.demo.entity.Elderly;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.AlertEventMapper;
import com.example.demo.mapper.ElderlyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

/**
 * 异常告警事件服务。
 * <p>提供事件标题解析与类型归类、多条件分页查询（状态同义词归一、严重等级/事件类型/关键词
 * 内存二次过滤、时间范围筛选）、单个与批量状态流转、已解决事件清理及单人事件统计等能力。</p>
 * */
@Service
public class AlertEventService {

    private static final Logger log = LoggerFactory.getLogger(AlertEventService.class);

    @Resource
    private AlertEventMapper alertEventMapper;
    @Resource
    private ElderlyMapper elderlyMapper;

    private static final Map<String, String> CATEGORY_TITLES = new HashMap<>();
    static {
        CATEGORY_TITLES.put("FALL", "摔倒报警");
        CATEGORY_TITLES.put("EMERGENCY", "一键求助");
        CATEGORY_TITLES.put("PRESSURE", "情绪低落");
        CATEGORY_TITLES.put("SMOKE", "烟雾报警");
    }

    /**
     * 将事件类型解析为简洁中文标题（摔倒报警、一键求助、情绪低落、烟雾报警、心率波动异常、
     * 环境参数异常等），无法归类时回退为原始类型或"其他"。
     *
     * @param type 事件原始类型
     * @return 中文事件标题
     */
    public static String resolveEventTitle(String type) {
        String cat = resolveEventTypeCategory(type);
        String title = CATEGORY_TITLES.get(cat);
        if (title != null) return title;
        if (type == null) return "其他";
        String t = type.toUpperCase();
        if (t.contains("HEART") || type.contains("心率")) return "心率波动异常";
        if (t.contains("ENV") || type.contains("环境") || type.contains("温度") || type.contains("湿度")) return "环境参数异常";
        return type.trim().isEmpty() ? "其他" : type.trim();
    }

    /**
     * 为单个事件回填展示用中文标题。
     *
     * @param e 告警事件，为 null 时直接返回
     */
    public void enrichAlertForDisplay(AlertEvent e) {
        if (e == null) return;
        e.setTitle(resolveEventTitle(e.getType()));
    }

    /**
     * 批量回填展示用中文标题。
     *
     * @param list 告警事件列表，为 null 或空列表时直接返回
     */
    public void enrichForDisplay(List<AlertEvent> list) {
        if (list == null || list.isEmpty()) return;
        for (AlertEvent e : list) enrichAlertForDisplay(e);
    }

    // ===================================================================================

    private static final Map<String, List<String>> STATUS_MAPPING = new HashMap<>();

    static {
        List<String> openPending = Arrays.asList("NEW", "PENDING", "未处理");
        for (String k : openPending) STATUS_MAPPING.put(k, openPending);
        List<String> openProcessing = Arrays.asList("PROCESSING", "IN_PROGRESS", "处理中");
        for (String k : openProcessing) STATUS_MAPPING.put(k, openProcessing);
        List<String> closedResolved = Arrays.asList("RESOLVED", "CLOSED", "ACK", "已解决", "已关闭", "已确认");
        for (String k : closedResolved) STATUS_MAPPING.put(k, closedResolved);
        List<String> closedFalseAlarm = Arrays.asList("FALSE_ALARM", "误报");
        for (String k : closedFalseAlarm) STATUS_MAPPING.put(k, closedFalseAlarm);
    }

    /**
     * 将各种中英文事件类型（含别名）归一到 FALL/EMERGENCY/PRESSURE/SMOKE/OTHER 五个大类。
     *
     * @param type 事件原始类型
     * @return 事件大类编码
     */
    public static String resolveEventTypeCategory(String type) {
        if (type == null) return "OTHER";
        String t = type.toUpperCase();
        if (t.contains("FALL") || type.contains("摔倒") || type.contains("跌倒")) return "FALL";
        if (t.contains("SOS") || t.contains("EMERGENCY") || t.contains("CALL_HELP") || t.contains("BUTTON_PRESS")
                || type.contains("一键求助") || type.contains("紧急")) return "EMERGENCY";
        if (t.contains("PRESSURE") || t.contains("EMOTION") || t.contains("MOOD") || t.contains("DEPRESS")
                || type.contains("压力") || type.contains("床压") || type.contains("情绪")) return "PRESSURE";
        if (t.contains("SMOKE") || t.contains("FIRE") || t.contains("GAS")
                || type.contains("烟雾") || type.contains("火灾") || type.contains("燃气")) return "SMOKE";
        return "OTHER";
    }

    /**
     * 仅按老人与状态条件分页查询事件（其余筛选条件不过滤）。
     *
     * @param pageNum   页码，从 1 开始
     * @param pageSize  每页条数
     * @param elderlyId 老人ID，可空
     * @param status    事件状态，可空（自动匹配同义词组）
     * @return 事件分页结果
     */
    public Page<AlertEvent> page(int pageNum, int pageSize, Long elderlyId, String status) {
        return page(pageNum, pageSize, elderlyId, status, null, null, null, null, null, null);
    }

    /**
     * 多条件分页查询事件：数据库先按状态同义词组与时间范围过滤，再在内存依次做事件类型归类、
     * 严重等级（≥3紧急/=2一般/≤1提示）、关键词（姓名/描述/备注）三次过滤，按发生时间倒序后
     * 手动分页，并为当前页记录回填中文标题。
     *
     * @param pageNum    页码，从 1 开始
     * @param pageSize   每页条数
     * @param elderlyId  老人ID，可空
     * @param status     事件状态，可空
     * @param severity   严重等级，可空（1/2/3）
     * @param eventType  事件大类编码，可空
     * @param keyword    关键词，可空（匹配姓名、描述、备注）
     * @param timeRange  快捷时间范围 today/week/month，可空
     * @param startTime  自定义开始时间，可空（与 endTime 同时存在时优先）
     * @param endTime    自定义结束时间，可空
     * @return 事件分页结果
     */
    public Page<AlertEvent> page(int pageNum, int pageSize, Long elderlyId, String status,
                                  String severity, String eventType, String keyword,
                                  String timeRange, String startTime, String endTime) {
        LambdaQueryWrapper<AlertEvent> q = Wrappers.<AlertEvent>lambdaQuery()
                .orderByDesc(AlertEvent::getCreatedAt);

        if (status != null && !status.isEmpty()) {
            List<String> statusValues = STATUS_MAPPING.get(status);
            if (statusValues != null && !statusValues.isEmpty()) {
                q.in(AlertEvent::getStatus, statusValues);
            } else {
                q.eq(AlertEvent::getStatus, status);
            }
        }

        // 时间范围筛选
        if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            q.ge(AlertEvent::getCreatedAt, LocalDateTime.parse(startTime.replace(" ", "T")))
             .le(AlertEvent::getCreatedAt, LocalDateTime.parse(endTime.replace(" ", "T")));
        } else if (timeRange != null && !timeRange.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime rangeStart;
            switch (timeRange) {
                case "today":
                    rangeStart = now.toLocalDate().atStartOfDay();
                    break;
                case "week":
                    rangeStart = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1).atStartOfDay();
                    break;
                case "month":
                    rangeStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                    break;
                default:
                    rangeStart = null;
            }
            if (rangeStart != null) {
                q.ge(AlertEvent::getCreatedAt, rangeStart);
            }
        }

        // severity 是虚拟字段、OTHER 类型判定、keyword 匹配均需在内存二次过滤
        final int needSev = (severity != null && !severity.isEmpty()) ? Integer.parseInt(severity) : -1;
        final String needTypeCat = (eventType != null && !eventType.isEmpty()) ? eventType.toUpperCase() : null;
        final String keywordLower = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim().toLowerCase() : null;
        List<AlertEvent> allRows = alertEventMapper.selectList(q);

        // 第1关：事件类型内存过滤
        List<AlertEvent> filtered = allRows;
        if (needTypeCat != null) {
            filtered = filtered.stream().filter(e -> needTypeCat.equals(resolveEventTypeCategory(e.getType()))).collect(Collectors.toList());
        }
        // 第2关：严重等级内存过滤
        if (needSev >= 1) {
            filtered = filtered.stream().filter(e -> {
                Integer s = e.getSeverity();
                if (s == null) return false;
                if (needSev >= 3) return s >= 3;
                if (needSev == 2) return s == 2;
                return s <= 1;
            }).collect(Collectors.toList());
        }
        // 第3关：关键词内存过滤
        if (keywordLower != null) {
            final String kw = keywordLower;
            filtered = filtered.stream().filter(e -> {
                String n = e.getElderName() == null ? "" : e.getElderName().toLowerCase();
                String d = e.getDescription() == null ? "" : e.getDescription().toLowerCase();
                String r = e.getRemark() == null ? (e.getNotes() == null ? "" : e.getNotes().toLowerCase()) : e.getRemark().toLowerCase();
                return n.contains(kw) || d.contains(kw) || r.contains(kw);
            }).collect(Collectors.toList());
        }

        // 第4关：按发生时间倒序
        filtered = filtered.stream().sorted((a, b) -> {
            LocalDateTime ta = a.getOccurredAt();
            LocalDateTime tb = b.getOccurredAt();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        }).collect(Collectors.toList());

        // 手动分页
        int total = filtered.size();
        int start = Math.max(0, (pageNum - 1) * pageSize);
        int end = Math.min(start + pageSize, total);
        List<AlertEvent> pageRecords = (start < end) ? new ArrayList<>(filtered.subList(start, end)) : Collections.emptyList();

        // 对当前页设置简洁中文标题
        enrichForDisplay(pageRecords);

        Page<AlertEvent> result = new Page<>(pageNum, pageSize);
        result.setTotal(total);
        result.setRecords(pageRecords);
        return result;
    }

    private static final Map<String, String> STATUS_DISPLAY_TO_DB = new HashMap<>();

    static {
        STATUS_DISPLAY_TO_DB.put("NEW", "未处理");
        STATUS_DISPLAY_TO_DB.put("未处理", "未处理");
        STATUS_DISPLAY_TO_DB.put("ACK", "已解决");
        STATUS_DISPLAY_TO_DB.put("RESOLVED", "已解决");
        STATUS_DISPLAY_TO_DB.put("PROCESSING", "处理中");
        STATUS_DISPLAY_TO_DB.put("CLOSED", "已解决");
        STATUS_DISPLAY_TO_DB.put("FALSE_ALARM", "误报");
    }

    /**
     * 流转单个事件状态并记录处理备注。前端状态码会归一为库内中文状态（如 NEW→未处理）；
     * 当备注含摔倒/心率/环境等关键词但与事件本身类型不匹配时拦截本次更新；事件不存在抛出业务异常。
     *
     * @param id     事件ID
     * @param status 目标状态
     * @param remark 处理备注，可空
     * @throws BusinessException 事件不存在时抛出（404）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status, String remark) {
        AlertEvent existing = alertEventMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("404", "事件不存在");
        }

        if (remark != null) {
            // remark 关键词 -> 允许匹配的事件 type/title/description 关键词
            Map<String, String[]> typeGuards = new HashMap<>();
            typeGuards.put("摔倒",   new String[]{"FALL","摔"});
            typeGuards.put("心率",   new String[]{"HEART","心率","心跳"});
            typeGuards.put("心跳",   new String[]{"HEART","心率","心跳"});
            typeGuards.put("环境",   new String[]{"ENV","环境","温度","湿度"});
            typeGuards.put("温湿度", new String[]{"ENV","环境","温度","湿度"});

            for (Map.Entry<String, String[]> en : typeGuards.entrySet()) {
                String remarkKey = en.getKey();
                if (!remark.contains(remarkKey)) continue;
                boolean typeMatch = false;
                String hay = (existing.getType() == null ? "" : existing.getType())
                           + (existing.getTitle() == null ? "" : existing.getTitle())
                           + (existing.getDescription() == null ? "" : existing.getDescription());
                for (String tKey : en.getValue()) {
                    if (hay.toLowerCase().contains(tKey.toLowerCase())) {
                        typeMatch = true;
                        break;
                    }
                }
                if (!typeMatch) {
                    log.warn("[updateStatus拦截] 自动恢复 remark 与事件类型不匹配，已跳过更新. " +
                                    "id={} existingType={} existingTitle={} status={} remark={}",
                            existing.getId(), existing.getType(), existing.getTitle(), status, remark);
                    return;
                }
            }
        }

        String dbStatus = STATUS_DISPLAY_TO_DB.get(status);
        if (dbStatus != null) {
            existing.setStatus(dbStatus);
        } else {
            existing.setStatus(status);
        }
        if (remark != null) {
            existing.setRemark(remark);
        }
        alertEventMapper.updateById(existing);
    }

    /**
     * 物理删除所有已解决/已关闭/已确认状态的事件。
     *
     * @return 实际删除条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int clearResolved() {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AlertEvent> q =
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<AlertEvent>lambdaQuery()
                        .in(AlertEvent::getStatus, Arrays.asList("CLOSED", "ACK", "已解决", "已关闭", "已确认"));
        return alertEventMapper.delete(q);
    }

    /**
     * 批量流转事件状态：逐条查询更新，单条失败仅跳过，不影响其余记录。
     *
     * @param ids    事件ID列表
     * @param status 目标状态（自动归一为库内中文状态）
     * @param remark 处理备注，可空
     * @return 实际更新成功的条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStatus(List<Long> ids, String status, String remark) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        String dbStatus = STATUS_DISPLAY_TO_DB.get(status);
        String finalStatus = dbStatus != null ? dbStatus : status;

        int count = 0;
        for (Long id : ids) {
            try {
                AlertEvent existing = alertEventMapper.selectById(id);
                if (existing != null) {
                    existing.setStatus(finalStatus);
                    if (remark != null) {
                        existing.setRemark(remark);
                    }
                    alertEventMapper.updateById(existing);
                    count++;
                }
            } catch (Exception e) {
                // 继续处理其他记录
            }
        }
        return count;
    }

    /**
     * 获取某位老人的异常事件统计
     *
     * @param elderlyId 老人ID
     * @param days      统计时间窗口：1=今日、7=本周、30=本月，其余按近 N 天处理
     * @return 统计结果 Map：total 事件总数、byType 按事件大类分组计数、unresolved 未解决数
     */
    public Map<String, Object> getAlertStats(Long elderlyId, int days) {
        Map<String, Object> result = new HashMap<>();

        Elderly elderly = elderlyMapper.selectById(elderlyId);
        if (elderly == null || elderly.getRealName() == null) {
            result.put("total", 0);
            result.put("byType", new HashMap<>());
            result.put("unresolved", 0);
            return result;
        }

        String elderName = elderly.getRealName();

        LocalDateTime startTime;
        LocalDateTime now = LocalDateTime.now();
        if (days == 1) {
            startTime = now.toLocalDate().atStartOfDay();
        } else if (days == 7) {
            startTime = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1).atStartOfDay();
        } else if (days == 30) {
            startTime = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        } else {
            startTime = now.minusDays(days);
        }

        LambdaQueryWrapper<AlertEvent> q = Wrappers.<AlertEvent>lambdaQuery()
                .eq(AlertEvent::getElderName, elderName)
                .ge(AlertEvent::getCreatedAt, startTime)
                .orderByDesc(AlertEvent::getCreatedAt);

        List<AlertEvent> allAlerts = alertEventMapper.selectList(q);

        result.put("total", allAlerts.size());

        Map<String, Long> byType = allAlerts.stream()
                .collect(Collectors.groupingBy(
                    a -> resolveEventTypeCategory(a.getType()),
                    Collectors.counting()
                ));
        result.put("byType", byType);

        long unresolved = allAlerts.stream()
                .filter(a -> "未处理".equals(a.getStatus()) || "NEW".equals(a.getStatus()) || "PROCESSING".equals(a.getStatus()) || "处理中".equals(a.getStatus()))
                .count();
        result.put("unresolved", unresolved);

        return result;
    }

}

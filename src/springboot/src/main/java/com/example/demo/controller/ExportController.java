package com.example.demo.controller;

import com.example.demo.entity.AlertEvent;
import com.example.demo.entity.Elderly;
import com.example.demo.mapper.AlertEventMapper;
import com.example.demo.mapper.ElderlyMapper;
import com.example.demo.service.AlertEventService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel 报表导出接口。
 * <p>以 HTML 表格片段配合 .xls 扩展名输出两类报表：异常事件报表（完全复用
 * AlertEventService.page 的筛选与标题回填逻辑）和楼栋健康报表（楼栋归属判定、
 * 关键词过滤及风险/护理等级统计，口径与前端保持一致）。</p>
 * */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Resource
    private AlertEventMapper alertEventMapper;

    @Resource
    private ElderlyMapper elderlyMapper;

    @Resource
    private AlertEventService alertEventService;

    /**
     * 导出异常事件 Excel 报表。筛选逻辑与 AlertEventService.page 完全一致，
     * 报表抬头列出已应用的筛选条件与总条数，文件名含"全部/筛选结果"与时间戳。
     *
     * @param status    处理状态，可空
     * @param severity  严重等级，可空
     * @param eventType 事件大类，可空
     * @param keyword   关键词（姓名/描述/备注），可空
     * @param timeRange 快捷时间范围 today/week/month，可空
     * @param startTime 自定义开始时间，可空
     * @param endTime   自定义结束时间，可空
     * @param response  Servlet 响应，用于写出 .xls 文件流与下载头
     * @throws Exception 文件名编码或响应写出失败时抛出
     */
    @GetMapping("/alerts")
    public void exportAlerts(@RequestParam(required = false) String status,
                             @RequestParam(required = false) String severity,
                             @RequestParam(required = false) String eventType,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String timeRange,
                             @RequestParam(required = false) String startTime,
                             @RequestParam(required = false) String endTime,
                             HttpServletResponse response) throws Exception {
        final boolean hasFilter = (status != null && !status.isEmpty())
                || (severity != null && !severity.isEmpty())
                || (eventType != null && !eventType.isEmpty())
                || (keyword != null && !keyword.isEmpty())
                || (timeRange != null && !timeRange.isEmpty())
                || (startTime != null && !startTime.isEmpty())
                || (endTime != null && !endTime.isEmpty());
        String fileLabel = hasFilter ? "异常事件报表_筛选结果" : "异常事件报表_全部";
        String fileName = fileLabel + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xls";
        response.setContentType("application/vnd.ms-excel; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));

        // 【关键】完全复用 AlertEventService.page() 的筛选逻辑，保证与页面显示的条数/内容 100% 一致
        // page() 内部已包含：STATUS_MAPPING 同义词、EVENT_TYPE_ALIASES 同义词+OTHER内存分类、
        //                   severity虚拟字段内存二次过滤、关键词模糊匹配、时间范围(today/week/month/custom)
        // page() 返回前已 enrichForDisplay(records)，和前端展示完全一致
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AlertEvent> p =
                alertEventService.page(1, 200000, null, status, severity, eventType, keyword, timeRange, startTime, endTime);
        List<AlertEvent> list = p.getRecords();
        if (list == null) list = java.util.Collections.emptyList();
        // 幂等兜底再 enrich 一次
        alertEventService.enrichForDisplay(list);
        final long total = p.getTotal();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset='UTF-8'><style>");
        sb.append("table{border-collapse:collapse;width:100%;font-size:11px;font-family:'Microsoft YaHei',Arial,sans-serif}");
        sb.append("th{background:#2b5797;color:#fff;padding:5px 6px;text-align:center;border:1px solid #1a3a6e;font-weight:bold}");
        sb.append("td{padding:3px 6px;border:1px solid #ccc;text-align:center}");
        sb.append(".row-alt td{background:#f0f4fa}");
        sb.append(".desc{text-align:left;word-break:break-all}");
        sb.append(".meta{margin:6px 0 10px;color:#555;font-size:12px}");
        sb.append("</style></head><body>");
        sb.append("<h2 style='color:#2b5797;margin:10px 0;font-size:16px'>").append(esc(fileLabel.replace("_", " "))).append("</h2>");
        sb.append("<div class='meta'>导出时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (hasFilter) {
            sb.append(" · 应用筛选条件：");
            boolean first = true;
            first = appendCond(sb, first, "处理状态", statusLabel(status));
            first = appendCond(sb, first, "严重等级", severityLabel(severity));
            first = appendCond(sb, first, "事件类型", eventTypeLabelExport(eventType));
            first = appendCond(sb, first, "关键词", keyword);
            first = appendCond(sb, first, "时间范围", timeRangeLabel(timeRange, startTime, endTime));
        }
        sb.append(" · 共 ").append(total).append(" 条</div>");
        sb.append("<table>");
        sb.append("<colgroup>");
        sb.append("<col width='60'><col width='90'><col width='100'><col width='400'><col width='200'><col width='70'><col width='300'>");
        sb.append("</colgroup>");
        sb.append("<tr>");
        sb.append("<th>ID</th><th>老人姓名</th><th>事件类型</th><th>描述</th><th>发生时间</th><th>状态</th><th>备注</th>");
        sb.append("</tr>");

        int alt = 0;
        for (AlertEvent e : list) {
            sb.append("<tr").append(alt++ % 2 == 0 ? "" : " class='row-alt'").append(">");
            sb.append("<td>").append(esc(e.getId())).append("</td>");
            // enrich 后直接取原始字段（已回填清洗）
            sb.append("<td>").append(esc(e.getElderName())).append("</td>");
            sb.append("<td>").append(esc(eventTypeLabel(e.getType()))).append("</td>");
            sb.append("<td class='desc'>").append(esc(e.getDescription())).append("</td>");
            sb.append("<td>").append(esc(e.getOccurredAt() != null ? e.getOccurredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "")).append("</td>");
            sb.append("<td>").append(esc(statusLabel(e.getStatus()))).append("</td>");
            sb.append("<td class='desc'>").append(esc(e.getRemark())).append("</td>");
            sb.append("</tr>\n");
        }

        sb.append("</table></body></html>");
        response.getWriter().write(sb.toString());
    }

    /**
     * 导出楼栋健康 Excel 报表：先给风险等级概览（总人数与高/中/低危人数），再列老人明细；
     * 楼栋归属与关键词过滤口径与前端 filterElders/filterReportList 完全一致。
     *
     * @param building 楼栋标识（"X号楼"或旧格式"X栋"），可空表示全部楼栋
     * @param keyword  姓名/房间/ID 模糊关键词，可空
     * @param response Servlet 响应，用于写出 .xls 文件流与下载头
     * @throws Exception 文件名编码或响应写出失败时抛出
     */
    @GetMapping("/building-report")
    public void exportBuildingReport(@RequestParam(required = false) String building,
                                     @RequestParam(required = false) String keyword,
                                     HttpServletResponse response) throws Exception {
        final String safeBuilding = building == null ? "" : building.trim();
        final String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        final String label = safeBuilding.isEmpty() ? "全部楼栋" : safeBuilding;
        final boolean hasFilter = !safeBuilding.isEmpty() || !safeKeyword.isEmpty();
        String fileLabel = hasFilter ? (label + "楼栋健康报表_筛选结果") : (label + "楼栋健康报表");
        String fileName = fileLabel + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xls";
        response.setContentType("application/vnd.ms-excel; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));

        List<Elderly> allElders = elderlyMapper.selectList(null);

        // 【关键1】与前端 filterElders() 完全一致的楼栋归属判断
        java.util.function.Predicate<Elderly> matchBuilding = e -> {
            if (safeBuilding.isEmpty()) return true;
            String room = (e.getRoom() != null && !e.getRoom().isEmpty()) ? e.getRoom()
                    : (e.getAddress() != null ? e.getAddress() : "");
            if (safeBuilding.contains("号楼")) {
                return room.startsWith(safeBuilding);
            }
            // 旧格式："A栋" → "A-"
            String prefix = safeBuilding.replace("栋", "");
            return room.startsWith(prefix + "-");
        };
        // 【关键2】与前端 filterReportList() 完全一致的关键词模糊（姓名/房间/ID）
        java.util.function.Predicate<Elderly> matchKeyword = e -> {
            if (safeKeyword.isEmpty()) return true;
            String name = (e.getRealName() != null ? e.getRealName() : (e.getName() != null ? e.getName() : "")).toLowerCase();
            String room = (e.getRoom() != null ? e.getRoom() : "").toLowerCase();
            String id = String.valueOf(e.getId() != null ? e.getId() : "");
            return name.contains(safeKeyword) || room.contains(safeKeyword) || id.contains(safeKeyword);
        };
        List<Elderly> elders = allElders.stream()
                .filter(matchBuilding)
                .filter(matchKeyword)
                .collect(java.util.stream.Collectors.toList());

        long total = elders.size();
        long highRisk = elders.stream().filter(e -> "高危".equals(e.getRiskLevel()) || "HIGH".equalsIgnoreCase(e.getRiskLevel())).count();
        long mediumRisk = elders.stream().filter(e -> "中危".equals(e.getRiskLevel()) || "MEDIUM".equalsIgnoreCase(e.getRiskLevel())).count();
        long lowRisk = elders.stream().filter(e -> "低危".equals(e.getRiskLevel()) || "LOW".equalsIgnoreCase(e.getRiskLevel())).count();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset='UTF-8'><style>");
        sb.append("table{border-collapse:collapse;width:100%;font-size:11px;font-family:'Microsoft YaHei',Arial,sans-serif;margin-bottom:20px}");
        sb.append("th{background:#2b5797;color:#fff;padding:5px 6px;text-align:center;border:1px solid #1a3a6e;font-weight:bold}");
        sb.append("td{padding:3px 6px;border:1px solid #ccc;text-align:center}");
        sb.append(".row-alt td{background:#f0f4fa}");
        sb.append("h2{color:#2b5797;margin:10px 0;font-size:16px}");
        sb.append("</style></head><body>");

        sb.append("<h2>").append(esc(label)).append(" 楼栋健康报表 - 概览</h2>");
        sb.append("<table><colgroup><col width='200'><col width='120'></colgroup>");
        sb.append("<tr><th>指标</th><th>数值</th></tr>");
        sb.append("<tr><td>总人数</td><td>").append(total).append("</td></tr>");
        sb.append("<tr class='row-alt'><td>高危人数</td><td>").append(highRisk).append("</td></tr>");
        sb.append("<tr><td>中危人数</td><td>").append(mediumRisk).append("</td></tr>");
        sb.append("<tr class='row-alt'><td>低危人数</td><td>").append(lowRisk).append("</td></tr>");
        sb.append("</table>");

        sb.append("<h2>老人明细</h2>");
        sb.append("<table>");
        sb.append("<colgroup>");
        sb.append("<col width='50'><col width='80'><col width='50'><col width='50'><col width='80'><col width='150'><col width='80'><col width='80'>");
        sb.append("</colgroup>");
        sb.append("<tr>");
        sb.append("<th>ID</th><th>姓名</th><th>年龄</th><th>性别</th><th>房间</th><th>护理等级</th><th>风险等级</th><th>健康评分</th>");
        sb.append("</tr>");

        int alt = 0;
        for (Elderly e : elders) {
            sb.append("<tr").append(alt++ % 2 == 0 ? "" : " class='row-alt'").append(">");
            sb.append("<td>").append(esc(e.getId())).append("</td>");
            sb.append("<td>").append(esc(e.getRealName())).append("</td>");
            sb.append("<td>").append(e.getAge() != null ? e.getAge() : "").append("</td>");
            sb.append("<td>").append(esc(e.getGender() != null ? ("0".equals(e.getGender()) ? "女" : "男") : "")).append("</td>");
            sb.append("<td>").append(esc(e.getRoom())).append("</td>");
            sb.append("<td>").append(esc(nursingLevelLabel(e.getNursingLevel()))).append("</td>");
            sb.append("<td>").append(esc(riskLevelLabel(e.getRiskLevel()))).append("</td>");
            sb.append("<td>").append(e.getHealthScore() != null ? e.getHealthScore() : "").append("</td>");
            sb.append("</tr>\n");
        }

        sb.append("</table></body></html>");
        response.getWriter().write(sb.toString());
    }

    /** 对报表字段做 HTML 转义（&amp; &lt; &gt;），防止内容破坏表格结构，null 返回空串 */
    private String esc(Object val) {
        if (val == null) return "";
        return val.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 将事件类型映射为报表展示中文名，兼容中英文历史数据，无法识别时回退后端大类归类 */
    private String eventTypeLabel(String type) {
        if (type == null) return "";
        String safe = type.trim();
        // 中文兜底（历史数据存中文的情况）
        if (safe.contains("摔倒") || safe.contains("跌倒") || safe.contains("FALL")) return "摔倒报警";
        if (safe.contains("一键") || safe.contains("求助") || safe.contains("SOS") || safe.contains("紧急") || safe.contains("EMERGENCY") || safe.contains("CALL") || safe.contains("BUTTON")) return "一键求助";
        if (safe.contains("情绪") || safe.contains("压力") || safe.contains("床压") || safe.contains("PRESSURE") || safe.contains("低")) return "情绪低落";
        if (safe.contains("烟雾") || safe.contains("燃气") || safe.contains("火灾") || safe.contains("SMOKE") || safe.contains("GAS") || safe.contains("FIRE")) return "烟雾报警";
        if (safe.contains("心率") || safe.contains("HEART") || safe.contains("心跳")) return "心率异常";
        if (safe.contains("环境") || safe.contains("温湿") || safe.contains("温度") || safe.contains("湿度") || safe.contains("ENV")) return "环境异常";
        if (safe.contains("离床") || safe.contains("LEAVE") || safe.contains("BED")) return "离床告警";
        // 复用后端分类兜底
        String cat = com.example.demo.service.AlertEventService.resolveEventTypeCategory(safe);
        switch (cat) {
            case "FALL": return "摔倒报警";
            case "EMERGENCY": return "一键求助";
            case "PRESSURE": return "情绪低落";
            case "SMOKE": return "烟雾报警";
            default: return "其他告警";
        }
    }

    /** 护理等级编码转中文：LEVEL_1 一级特护、LEVEL_2 二级重点、LEVEL_3 三级一般、LEVEL_4 四级自理 */
    private String nursingLevelLabel(String level) {
        if (level == null) return "";
        switch (level) {
            case "LEVEL_1": return "一级护理（特护）";
            case "LEVEL_2": return "二级护理（重点护理）";
            case "LEVEL_3": return "三级护理（一般护理）";
            case "LEVEL_4": return "四级护理（自理）";
            default: return level;
        }
    }

    /** 风险等级编码转中文（高危/中危/低危），null 显示"未评估"，未知值原样返回 */
    private String riskLevelLabel(String level) {
        if (level == null) return "未评估";
        String upper = level.toUpperCase();
        switch (upper) {
            case "HIGH": return "高危";
            case "MEDIUM": return "中危";
            case "LOW": return "低危";
            default: return level;
        }
    }

    // ============================================================
    // 导出报表辅助：条件标签拼接 + 各筛选维度的中文 label
    // ============================================================
    /** 向报表抬头追加一个非空筛选条件（label=value），空值跳过；返回更新后的"是否仍是首个条件"标志 */
    private boolean appendCond(StringBuilder sb, boolean first, String label, String value) {
        if (value == null || value.isEmpty()) return first;
        if (!first) sb.append("，");
        sb.append(label).append("=").append(value);
        return false;
    }

    /** 将各种中英文状态同义词归一为报表展示名：未处理/处理中/已解决/误报/已确认，无法识别时原样返回 */
    private String statusLabel(String status) {
        if (status == null || status.isEmpty()) return "";
        String s = status.trim();
        // 中文历史数据兜底
        if (s.contains("未处理") || s.equalsIgnoreCase("NEW") || s.equalsIgnoreCase("PENDING") || s.equalsIgnoreCase("OPEN") || s.equalsIgnoreCase("TODO")) return "未处理";
        if (s.contains("处理中") || s.contains("跟进") || s.equalsIgnoreCase("PROCESSING") || s.equalsIgnoreCase("IN_PROGRESS") || s.equalsIgnoreCase("HANDLING") || s.equalsIgnoreCase("DOING")) return "处理中";
        if (s.contains("已解决") || s.contains("已关闭") || s.contains("完成") || s.equalsIgnoreCase("RESOLVED") || s.equalsIgnoreCase("CLOSED") || s.equalsIgnoreCase("DONE") || s.equalsIgnoreCase("FINISHED") || s.equalsIgnoreCase("SUCCESS")) return "已解决";
        if (s.contains("误报") || s.contains("忽略") || s.equalsIgnoreCase("FALSE_ALARM") || s.equalsIgnoreCase("IGNORED") || s.equalsIgnoreCase("INVALID")) return "误报";
        if (s.contains("已确认") || s.equalsIgnoreCase("ACK") || s.equalsIgnoreCase("ACKNOWLEDGED")) return "已确认";
        return s;
    }

    /** 严重等级编码转中文：3 紧急、2 一般、1 提示，其余原样返回 */
    private String severityLabel(String severity) {
        if (severity == null || severity.isEmpty()) return "";
        switch (severity) {
            case "3": return "紧急";
            case "2": return "一般";
            case "1": return "提示";
            default: return severity;
        }
    }

    /** 事件类型导出时展示的 label（筛选选项用，兼容 FALL/EMERGENCY/PRESSURE/SMOKE/OTHER/HEART/ENV 全类型） */
    private String eventTypeLabelExport(String eventType) {
        if (eventType == null || eventType.isEmpty()) return "";
        String safe = eventType.trim();
        if (safe.equalsIgnoreCase("FALL")) return "摔倒报警";
        if (safe.equalsIgnoreCase("EMERGENCY")) return "一键求助";
        if (safe.equalsIgnoreCase("PRESSURE")) return "情绪低落";
        if (safe.equalsIgnoreCase("SMOKE")) return "烟雾报警";
        if (safe.equalsIgnoreCase("OTHER")) return "其他";
        if (safe.contains("HEART")) return "心率异常";
        if (safe.contains("ENV")) return "环境异常";
        // 兜底复用通用映射
        return eventTypeLabel(safe);
    }

    /** 生成时间范围筛选的中文描述：优先展示自定义起止区间，其次 today/week/month 对应今日/本周/本月 */
    private String timeRangeLabel(String timeRange, String startTime, String endTime) {
        if ((timeRange == null || timeRange.isEmpty())
                && (startTime == null || startTime.isEmpty())
                && (endTime == null || endTime.isEmpty())) {
            return "";
        }
        if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            return "自定义(" + startTime + " 至 " + endTime + ")";
        }
        switch (timeRange == null ? "" : timeRange) {
            case "today": return "今日";
            case "week": return "本周";
            case "month": return "本月";
            default: return timeRange == null ? "" : timeRange;
        }
    }
}

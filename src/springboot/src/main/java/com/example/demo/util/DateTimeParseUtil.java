package com.example.demo.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 通用时间解析工具类
 * 双格式兼容（空格分隔 / 带 T 的 ISO 8601），同时支持毫秒时间戳转换（对应 recorded_at 列 bigint）
 * */
public final class DateTimeParseUtil {

    private static final ZoneOffset BEIJING_OFFSET = ZoneOffset.ofHours(8);

    /** 私有构造方法，禁止实例化该工具类 */
    private DateTimeParseUtil() {}

    // ============== LocalDateTime 解析：同时兼容 空格 / T 两种格式 ==============

    /**
     * 解析 LocalDateTime，同时兼容：
     *   1) 2026-07-07T12:34:56   （axios 默认 / ISO 格式）
     *   2) 2026-07-07 12:34:56   （空格格式 / SQL 原生 / Postman 测试）
     * 解析失败返回 null（不抛异常，Controller 层直接判断是否传了时间参数）
     *
     * @param timeStr 待解析的时间字符串，可为null或空串
     * @return 解析得到的 LocalDateTime；入参为空或解析失败时返回null
     */
    public static LocalDateTime parseLocalDateTimeFlexible(String timeStr) {
        if (timeStr == null) return null;
        String s = timeStr.trim();
        if (s.isEmpty()) return null;
        try {
            if (s.contains("T")) {
                return LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
            }
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    // ============== LocalDate 解析：yyyy-MM-dd（HealthReport 只传日期） ==============

    /**
     * 解析 LocalDate，仅接受 yyyy-MM-dd 标准格式（有无时间部分皆可，有则截掉时间）
     *
     * @param dateStr 待解析的日期或日期时间字符串，可为null或空串
     * @return 解析得到的 LocalDate；入参为空或解析失败时返回null
     */
    public static LocalDate parseLocalDateFlexible(String dateStr) {
        if (dateStr == null) return null;
        String s = dateStr.trim();
        if (s.isEmpty()) return null;
        try {
            // 1) 纯日期 2026-07-07
            if (s.length() == 10 && !s.contains("T") && !s.contains(" ")) {
                return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            // 2) 传了完整时间（带 T / 空格），先转 LocalDateTime 再截日期
            LocalDateTime ldt = parseLocalDateTimeFlexible(s);
            return ldt != null ? ldt.toLocalDate() : null;
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    // ============== 毫秒时间戳转换（对应表 recorded_at 列 bigint） ==============

    /**
     * LocalDateTime → 北京时间毫秒时间戳（查询 recorded_at 列用，永远用这个，别用 report_time 字符串比大小）
     *
     * @param ldt 待转换的本地时间，可为null
     * @return 东八区毫秒时间戳；入参为null时返回null
     */
    public static Long toEpochMilli(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.toInstant(BEIJING_OFFSET).toEpochMilli();
    }

    /**
     * null 安全：入参为 null 时返回 null，调用方不用写非空判断
     *
     * @param ldt 待转换的本地时间，可为null
     * @return 东八区毫秒时间戳；入参为null时返回null
     */
    public static Long toEpochMilliOrNull(LocalDateTime ldt) {
        return toEpochMilli(ldt);
    }

    /**
     * 反方向：recorded_at 毫秒时间戳 → LocalDateTime（展示用）
     *
     * @param milli 东八区基准的毫秒时间戳，可为null
     * @return 转换得到的本地时间；入参为null时返回null
     */
    public static LocalDateTime fromEpochMilli(Long milli) {
        if (milli == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(milli), BEIJING_OFFSET);
    }
}

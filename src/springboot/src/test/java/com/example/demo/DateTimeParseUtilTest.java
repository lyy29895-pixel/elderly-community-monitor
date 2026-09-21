package com.example.demo;

import com.example.demo.util.DateTimeParseUtil;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * 时间解析工具类单元测试
 * 对应模块：健康和环境数据模块（时间范围筛选功能）
 *
 * 测试 DateTimeParseUtil 的双格式兼容解析能力：
 *   1) ISO 格式（带T）：2026-07-07T12:34:56（axios 默认发送格式）
 *   2) 空格格式：2026-07-07 12:34:56（SQL 原生 / Postman 测试格式）
 * */
public class DateTimeParseUtilTest {

    /** 测试用 ISO 格式时间字符串 */
    private String isoTimeStr;
    /** 测试用空格格式时间字符串 */
    private String spaceTimeStr;

    /**
     * 测试前初始化：准备两组格式不同但表示同一时刻的时间字符串。
     */
    @Before
    public void setUp() {
        isoTimeStr = "2026-07-07T12:34:56";       // ISO 格式（带T）
        spaceTimeStr = "2026-07-07 12:34:56";      // 空格格式
    }

    /**
     * 测试1：解析 ISO 格式时间字符串（带T分隔符）。
     * 前端 axios 默认发送此格式，后端必须正确解析。
     */
    @Test
    public void testParseIsoFormat() {
        LocalDateTime result = DateTimeParseUtil.parseLocalDateTimeFlexible(isoTimeStr);
        assertNotNull("ISO格式时间解析结果不应为null", result);
        assertEquals("年份应匹配", 2026, result.getYear());
        assertEquals("月份应匹配", 7, result.getMonthValue());
        assertEquals("日期应匹配", 7, result.getDayOfMonth());
        assertEquals("小时应匹配", 12, result.getHour());
        assertEquals("分钟应匹配", 34, result.getMinute());
        assertEquals("秒应匹配", 56, result.getSecond());
    }

    /**
     * 测试2：解析空格格式时间字符串（SQL原生格式）。
     * Postman 测试或直接拼接 SQL 时使用此格式，后端必须兼容。
     */
    @Test
    public void testParseSpaceFormat() {
        LocalDateTime result = DateTimeParseUtil.parseLocalDateTimeFlexible(spaceTimeStr);
        assertNotNull("空格格式时间解析结果不应为null", result);
        assertEquals("年份应匹配", 2026, result.getYear());
        assertEquals("月份应匹配", 7, result.getMonthValue());
        assertEquals("小时应匹配", 12, result.getHour());
    }

    /**
     * 测试3：两种格式解析结果应表示同一时刻。
     * 这是兼容性设计的核心验证：同一时刻的不同格式表示，解析后必须相等。
     */
    @Test
    public void testBothFormatsYieldSameTime() {
        LocalDateTime fromIso = DateTimeParseUtil.parseLocalDateTimeFlexible(isoTimeStr);
        LocalDateTime fromSpace = DateTimeParseUtil.parseLocalDateTimeFlexible(spaceTimeStr);
        assertNotNull("ISO解析不应为null", fromIso);
        assertNotNull("空格解析不应为null", fromSpace);
        assertEquals("两种格式解析结果应相等", fromIso, fromSpace);
    }

    /**
     * 测试4：传入null应返回null（不抛异常）。
     * Controller 层 start/end 参数可选，为 null 时不应导致空指针。
     */
    @Test
    public void testParseNullReturnsNull() {
        LocalDateTime result = DateTimeParseUtil.parseLocalDateTimeFlexible(null);
        assertNull("null入参应返回null", result);
    }

    /**
     * 测试5：传入空字符串应返回null。
     */
    @Test
    public void testParseEmptyReturnsNull() {
        LocalDateTime result = DateTimeParseUtil.parseLocalDateTimeFlexible("");
        assertNull("空字符串入参应返回null", result);
    }

    /**
     * 测试6：传入非法格式字符串应返回null（不抛异常）。
     * 验证容错能力：前端传了格式错误的时间，后端不应崩溃。
     */
    @Test
    public void testParseInvalidReturnsNull() {
        LocalDateTime result = DateTimeParseUtil.parseLocalDateTimeFlexible("这不是时间");
        assertNull("非法格式入参应返回null", result);
    }

    /**
     * 测试7：LocalDateTime 转毫秒时间戳再转回，应保持一致。
     * 对应 recorded_at 列的写入与读取逻辑。
     */
    @Test
    public void testEpochMilliRoundTrip() {
        LocalDateTime original = LocalDateTime.of(2026, 7, 7, 12, 34, 56);
        Long milli = DateTimeParseUtil.toEpochMilli(original);
        assertNotNull("毫秒时间戳不应为null", milli);
        LocalDateTime converted = DateTimeParseUtil.fromEpochMilli(milli);
        assertEquals("往返转换后时间应一致", original, converted);
    }

    /**
     * 测试8：纯日期格式 yyyy-MM-dd 解析为 LocalDate。
     */
    @Test
    public void testParseLocalDate() {
        LocalDate result = DateTimeParseUtil.parseLocalDateFlexible("2026-07-07");
        assertNotNull("纯日期解析不应为null", result);
        assertEquals("年份应匹配", 2026, result.getYear());
        assertEquals("月份应匹配", 7, result.getMonthValue());
        assertEquals("日期应匹配", 7, result.getDayOfMonth());
    }
}

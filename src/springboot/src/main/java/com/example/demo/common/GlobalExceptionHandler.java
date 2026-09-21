package com.example.demo.common;

import com.example.demo.exception.BusinessException;
import org.apache.ibatis.exceptions.PersistenceException;
import org.mybatis.spring.MyBatisSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：集中捕获 Controller 层抛出的各类异常，
 * 统一转换为 {@link Result} 错误响应，避免向前端暴露原始堆栈。
 * */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String DB_HINT = "请确认 MySQL 已启动、spring.datasource.url 中的库名存在，并已执行 resources/db/elderly_monitor_schema.sql（含对 user 表补充 phone、community_id 的 ALTER）。";

    /**
     * 处理业务异常，按异常自带的响应码与信息返回。
     *
     * @param e 业务异常
     * @return 含业务异常码与信息的错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理 Spring 数据访问异常，记录日志并返回附带排障提示的 500 响应。
     *
     * @param e 数据访问异常
     * @return 含根因摘要与数据库排障提示的错误响应
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<?> handleDataAccess(DataAccessException e) {
        log.error("Database error", e);
        return Result.error("500", "数据库访问失败：" + DB_HINT + " 详情：" + shorten(rootCauseMessage(e), 280));
    }

    /**
     * MyBatis 抛出的 SQL/映射异常一般不继承 Spring 的 DataAccessException，需单独处理，否则前端只能看到「服务器内部错误」。
     *
     * @param e MyBatis 系统异常或持久化异常
     * @return 含根因摘要与数据库排障提示的错误响应
     */
    @ExceptionHandler({MyBatisSystemException.class, PersistenceException.class})
    public Result<?> handleMyBatis(RuntimeException e) {
        log.error("MyBatis / persistence error", e);
        return Result.error("500", "数据库或 SQL 异常：" + DB_HINT + " 详情：" + shorten(rootCauseMessage(e), 280));
    }

    /**
     * 处理请求体不可读异常（如 JSON 格式错误或缺失）。
     *
     * @param e 请求体解析异常
     * @return 提示使用 JSON 提交的 400 错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleBadBody(HttpMessageNotReadableException e) {
        log.warn("Bad request body: {}", e.getMessage());
        return Result.error("400", "请求体格式错误，请使用 JSON 提交");
    }

    /**
     * 兜底处理所有未被前面处理器捕获的异常。
     *
     * @param e 未分类异常
     * @return 含异常类型与根因摘要的 500 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleOther(Exception e) {
        log.error("Unhandled error", e);
        String detail = shorten(rootCauseMessage(e), 240);
        return Result.error("500", "服务器内部错误（" + e.getClass().getSimpleName() + "）：" + detail);
    }

    /**
     * 沿异常因果链查找最底层根因的消息文本。
     *
     * @param e 异常
     * @return 根因消息；根因无消息时回退为异常自身消息或占位提示
     */
    private static String rootCauseMessage(Throwable e) {
        Throwable c = e;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        String m = c.getMessage();
        if (m != null && !m.trim().isEmpty()) {
            return m;
        }
        m = e.getMessage();
        return m != null && !m.trim().isEmpty() ? m : "(无消息，请看后端控制台完整堆栈)";
    }

    /**
     * 压缩异常消息：合并空白字符并截断到指定长度。
     *
     * @param s   原始文本
     * @param max 最大保留字符数
     * @return 压缩截断后的文本；入参为 null 时返回空串
     */
    private static String shorten(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}

package com.example.demo.common;

import com.example.demo.exception.BusinessException;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 全局异常处理器单元测试：验证各类异常到统一错误响应的转换与根因提取。
 *
 * @author 甲
 */
public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** 业务异常：响应码与提示原样透传。 */
    @Test
    public void businessExceptionKeepsCodeAndMessage() {
        Result<?> res = handler.handleBusiness(new BusinessException("403", "需要社区工作人员权限"));
        assertEquals("403", res.getCode());
        assertEquals("需要社区工作人员权限", res.getMsg());

        Result<?> res2 = handler.handleBusiness(new BusinessException("参数有误"));
        assertEquals("400", res2.getCode());
        assertEquals("参数有误", res2.getMsg());
    }

    /** 请求体不可读异常：返回 400 与 JSON 提交提示。 */
    @Test
    public void badBodyReturns400() {
        Result<?> res = handler.handleBadBody(new HttpMessageNotReadableException("bad json"));
        assertEquals("400", res.getCode());
        assertTrue(res.getMsg().contains("JSON"));
    }

    /** 数据访问异常：返回 500 并附带数据库排障提示与根因摘要。 */
    @Test
    public void dataAccessReturns500WithHint() {
        Result<?> res = handler.handleDataAccess(
                new DataAccessResourceFailureException("could not connect to mysql"));
        assertEquals("500", res.getCode());
        assertTrue("应包含排障提示", res.getMsg().contains("数据库访问失败"));
        assertTrue("应包含根因信息", res.getMsg().contains("could not connect to mysql"));
    }

    /** MyBatis 持久化异常：返回 500 与 SQL 异常提示。 */
    @Test
    public void myBatisErrorReturns500() {
        Result<?> res = handler.handleMyBatis(new PersistenceException("sql syntax error"));
        assertEquals("500", res.getCode());
        assertTrue(res.getMsg().contains("数据库或 SQL 异常"));
        assertTrue(res.getMsg().contains("sql syntax error"));
    }

    /** 兜底异常：返回 500，消息含异常类型与根因。 */
    @Test
    public void otherExceptionReturns500WithRootCause() {
        RuntimeException e = new RuntimeException("外层消息",
                new IllegalStateException("真正的根因"));
        Result<?> res = handler.handleOther(e);
        assertEquals("500", res.getCode());
        assertTrue("应包含异常类型", res.getMsg().contains("RuntimeException"));
        assertTrue("应提取最底层根因消息", res.getMsg().contains("真正的根因"));
    }

    /** 超长根因消息会被截断，避免响应体过大。 */
    @Test
    public void longMessageIsShortened() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            big.append("错");
        }
        Result<?> res = handler.handleOther(new RuntimeException(big.toString()));
        assertTrue("消息应被截断到有限长度", res.getMsg().length() < 600);
    }
}

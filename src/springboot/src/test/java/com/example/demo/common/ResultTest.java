package com.example.demo.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 统一响应包装体单元测试：验证成功/失败构造与响应码语义。
 * */
public class ResultTest {

    /** 无数据成功响应：码 200、默认提示、数据为空。 */
    @Test
    public void successWithoutData() {
        Result<Void> res = Result.success();
        assertEquals("200", res.getCode());
        assertEquals("操作成功", res.getMsg());
        assertNull(res.getData());
    }

    /** 带数据成功响应：码 200 且数据原样返回。 */
    @Test
    public void successWithData() {
        Result<String> res = Result.success("payload");
        assertEquals("200", res.getCode());
        assertEquals("payload", res.getData());
    }

    /** 失败响应：默认码 500 与自定义提示。 */
    @Test
    public void errorWithDefaultCode() {
        Result<?> res = Result.error("参数有误");
        assertEquals("500", res.getCode());
        assertEquals("参数有误", res.getMsg());
    }

    /** 失败响应：支持自定义响应码。 */
    @Test
    public void errorWithCustomCode() {
        Result<?> res = Result.error("403", "无权限");
        assertEquals("403", res.getCode());
        assertEquals("无权限", res.getMsg());
    }

    /** setter 可独立赋值（Jackson 反序列化需要）。 */
    @Test
    public void settersWork() {
        Result<Integer> res = new Result<>();
        res.setCode("400");
        res.setMsg("bad");
        res.setData(7);
        assertEquals("400", res.getCode());
        assertEquals("bad", res.getMsg());
        assertEquals(Integer.valueOf(7), res.getData());
    }
}

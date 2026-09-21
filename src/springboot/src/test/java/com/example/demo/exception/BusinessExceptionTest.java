package com.example.demo.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 业务异常单元测试：验证默认响应码、自定义响应码与消息透传。
 * */
public class BusinessExceptionTest {

    /** 单参构造默认响应码为 400。 */
    @Test
    public void defaultCodeIs400() {
        BusinessException e = new BusinessException("姓名不能为空");
        assertEquals("400", e.getCode());
        assertEquals("姓名不能为空", e.getMessage());
    }

    /** 双参构造使用自定义响应码。 */
    @Test
    public void customCodeIsKept() {
        BusinessException e = new BusinessException("403", "需要社区工作人员权限");
        assertEquals("403", e.getCode());
        assertEquals("需要社区工作人员权限", e.getMessage());
    }

    /** 继承 RuntimeException，可携带 null 消息不抛错。 */
    @Test
    public void nullableMessage() {
        BusinessException e = new BusinessException((String) null);
        assertEquals("400", e.getCode());
        assertNull(e.getMessage());
    }
}

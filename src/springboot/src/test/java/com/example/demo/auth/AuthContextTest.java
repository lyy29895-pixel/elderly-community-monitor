package com.example.demo.auth;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 登录上下文单元测试：验证 ThreadLocal 绑定、读取与清理，防止线程复用串数据。
 * */
public class AuthContextTest {

    @After
    public void tearDown() {
        AuthContext.clear();
    }

    /** 绑定后可读取用户 ID 与角色。 */
    @Test
    public void setThenGet() {
        AuthContext.set(3, "community");
        assertEquals(Integer.valueOf(3), AuthContext.getUserId());
        assertEquals("community", AuthContext.getRole());
    }

    /** 清理后用户 ID 与角色均为 null。 */
    @Test
    public void clearRemovesAll() {
        AuthContext.set(3, "elder");
        AuthContext.clear();
        assertNull(AuthContext.getUserId());
        assertNull(AuthContext.getRole());
    }

    /** 未登录时读取返回 null 而非抛异常。 */
    @Test
    public void emptyContextReturnsNull() {
        assertNull(AuthContext.getUserId());
        assertNull(AuthContext.getRole());
    }

    /** 不同线程的上下文互相隔离。 */
    @Test
    public void contextIsThreadIsolated() throws InterruptedException {
        AuthContext.set(1, "admin");
        final Integer[] otherThreadId = new Integer[1];
        Thread t = new Thread(() -> {
            otherThreadId[0] = AuthContext.getUserId();
            AuthContext.set(2, "elder");
            AuthContext.clear();
        });
        t.start();
        t.join();
        assertNull("子线程未绑定时应为 null", otherThreadId[0]);
        assertEquals("主线程上下文不受子线程影响", Integer.valueOf(1), AuthContext.getUserId());
    }
}

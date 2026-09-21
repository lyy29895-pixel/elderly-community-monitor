package com.example.demo.auth;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 登录 Token 存储单元测试：验证保存、匹配、顶替与清除逻辑（进程内真实对象，无 Mock）。
 *
 * @author 甲
 */
public class TokenStoreServiceTest {

    private TokenStoreService store;

    @Before
    public void setUp() {
        store = new TokenStoreService();
    }

    /** 保存后的 Token 与用户 ID 匹配。 */
    @Test
    public void savedTokenMatches() {
        store.saveToken(1, "token-a");
        assertTrue(store.matches(1, "token-a"));
    }

    /** 换新登录顶替旧 Token 后，旧 Token 立即失效。 */
    @Test
    public void newTokenReplacesOldOne() {
        store.saveToken(1, "token-a");
        store.saveToken(1, "token-b");
        assertFalse("旧 Token 应被顶替失效", store.matches(1, "token-a"));
        assertTrue(store.matches(1, "token-b"));
    }

    /** 清除后 Token 不再匹配。 */
    @Test
    public void clearedTokenNoLongerMatches() {
        store.saveToken(1, "token-a");
        store.clear(1);
        assertFalse(store.matches(1, "token-a"));
    }

    /** 边界：token 为 null 或未知用户一律不匹配。 */
    @Test
    public void nullTokenOrUnknownUserNeverMatch() {
        assertFalse(store.matches(1, null));
        assertFalse(store.matches(99, "token-a"));
    }
}

package com.example.demo.enums;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 用户角色工具单元测试：验证角色归一化与各角色判定方法。
 * */
public class UserRoleTest {

    /** null 与空白归一化为 null。 */
    @Test
    public void normalizeNullAndBlank() {
        assertNull(UserRole.normalize(null));
        assertNull(UserRole.normalize("   "));
        assertNull(UserRole.normalize(""));
    }

    /** 大小写混合的历史角色值归一化为小写。 */
    @Test
    public void normalizeMixedCase() {
        assertEquals("community", UserRole.normalize("COMMUNITY"));
        assertEquals("community", UserRole.normalize("Community"));
        assertEquals("admin", UserRole.normalize("ADMIN"));
        assertEquals("elder", UserRole.normalize("Elder"));
        assertEquals("child", UserRole.normalize("CHILD"));
    }

    /** 未知角色转小写保留原值（不丢弃）。 */
    @Test
    public void normalizeUnknownKeepsLowercase() {
        assertEquals("family", UserRole.normalize("FAMILY"));
    }

    /** 各角色判定方法大小写不敏感。 */
    @Test
    public void roleChecksIgnoreCase() {
        assertTrue(UserRole.isAdmin("ADMIN"));
        assertTrue(UserRole.isCommunity("community"));
        assertTrue(UserRole.isElder("ELDER"));
        assertTrue(UserRole.isChild("Child"));

        assertFalse(UserRole.isAdmin("community"));
        assertFalse(UserRole.isCommunity("admin"));
        assertFalse(UserRole.isElder("child"));
        assertFalse(UserRole.isChild("elder"));
    }

    /** 管理员或社区判定：两者均通过，其他角色不通过。 */
    @Test
    public void communityOrAdminCheck() {
        assertTrue(UserRole.isCommunityOrAdmin("admin"));
        assertTrue(UserRole.isCommunityOrAdmin("COMMUNITY"));
        assertFalse(UserRole.isCommunityOrAdmin("elder"));
        assertFalse(UserRole.isCommunityOrAdmin("child"));
        assertFalse(UserRole.isCommunityOrAdmin(null));
    }
}

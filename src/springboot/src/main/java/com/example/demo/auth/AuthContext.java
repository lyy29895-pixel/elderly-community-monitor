package com.example.demo.auth;

/**
 * 当前请求登录上下文：基于 ThreadLocal 持有当前线程对应的用户 ID 与角色，
 * 供业务层在不传递参数的情况下获取当前登录人信息。
 *
 * @author 甲
 */
public final class AuthContext {
    private static final ThreadLocal<Integer> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    /** 工具类私有构造，禁止外部实例化。 */
    private AuthContext() {
    }

    /**
     * 绑定当前线程的登录用户信息。
     *
     * @param userId 用户 ID
     * @param role   归一化后的用户角色
     */
    public static void set(Integer userId, String role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    /**
     * 获取当前线程登录用户 ID。
     *
     * @return 用户 ID；未登录时为 null
     */
    public static Integer getUserId() {
        return USER_ID.get();
    }

    /**
     * 获取当前线程登录用户角色。
     *
     * @return 角色标识；未登录时为 null
     */
    public static String getRole() {
        return ROLE.get();
    }

    /**
     * 清除当前线程绑定的全部登录信息。
     */
    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}

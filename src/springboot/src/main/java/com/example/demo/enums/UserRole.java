package com.example.demo.enums;

/**
 * 全平台统一角色（Web 社区端、App 老人端 / 子女端共用 user 表，以 role 区分）。
 * 数据库存小写：elder、child、community；可选 admin 运维/后台。
 * */
public final class UserRole {

    /** 老人角色（App 老人端） */
    public static final String ELDER = "elder";
    /** 子女角色（App 子女端） */
    public static final String CHILD = "child";
    /** 社区角色（Web 社区端工作人员） */
    public static final String COMMUNITY = "community";
    /** 管理员角色（运维 / 后台） */
    public static final String ADMIN = "admin";

    /** 常量工具类私有构造，禁止外部实例化。 */
    private UserRole() {
    }

    /**
     * 转为规范小写值，并兼容历史大写 COMMUNITY / ADMIN / ELDER / CHILD。
     *
     * @param raw 原始角色值；可为 null
     * @return 归一化后的小写角色值；入参为 null 或空白时返回 null
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (COMMUNITY.equalsIgnoreCase(s)) {
            return COMMUNITY;
        }
        if (ADMIN.equalsIgnoreCase(s)) {
            return ADMIN;
        }
        if (ELDER.equalsIgnoreCase(s)) {
            return ELDER;
        }
        if (CHILD.equalsIgnoreCase(s)) {
            return CHILD;
        }
        return s.toLowerCase();
    }

    /**
     * 判断角色是否为管理员（大小写不敏感）。
     *
     * @param role 待判断的角色值
     * @return 是管理员返回 true
     */
    public static boolean isAdmin(String role) {
        return ADMIN.equalsIgnoreCase(role);
    }

    /**
     * 判断角色是否为社区工作人员（大小写不敏感）。
     *
     * @param role 待判断的角色值
     * @return 是社区角色返回 true
     */
    public static boolean isCommunity(String role) {
        return COMMUNITY.equalsIgnoreCase(role);
    }

    /**
     * 判断角色是否为老人（大小写不敏感）。
     *
     * @param role 待判断的角色值
     * @return 是老人角色返回 true
     */
    public static boolean isElder(String role) {
        return ELDER.equalsIgnoreCase(role);
    }

    /**
     * 判断角色是否为子女（大小写不敏感）。
     *
     * @param role 待判断的角色值
     * @return 是子女角色返回 true
     */
    public static boolean isChild(String role) {
        return CHILD.equalsIgnoreCase(role);
    }

    /**
     * 判断角色是否为社区工作人员或管理员（大小写不敏感）。
     *
     * @param role 待判断的角色值
     * @return 属于社区端管理类角色返回 true
     */
    public static boolean isCommunityOrAdmin(String role) {
        return isCommunity(role) || isAdmin(role);
    }
}

package com.example.demo.util;

/**
 * 身份证号规范化：去首尾空格、去中间空格，末位 x 转大写 X（18 位）。
 * */
public final class IdCardUtil {

    /** 工具类私有构造，禁止外部实例化。 */
    private IdCardUtil() {
    }

    /**
     * 规范化身份证号：去除全部空白字符，并将 18 位号码末位的 x 转为大写 X。
     *
     * @param raw 原始身份证号；可为 null
     * @return 规范化后的身份证号；入参为 null 或空白时返回空串
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().replaceAll("\\s+", "");
        if (s.isEmpty()) {
            return "";
        }
        if (s.length() == 18) {
            s = s.substring(0, 17) + s.substring(17).toUpperCase();
        }
        return s;
    }
}

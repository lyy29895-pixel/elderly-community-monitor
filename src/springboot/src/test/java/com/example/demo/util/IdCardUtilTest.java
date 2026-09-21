package com.example.demo.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 身份证号规范化单元测试：验证去空白、末位 x 转大写与空值兜底。
 *
 * @author 甲
 */
public class IdCardUtilTest {

    /** null 输入返回空串。 */
    @Test
    public void nullReturnsEmpty() {
        assertEquals("", IdCardUtil.normalize(null));
    }

    /** 空白串（纯空格/制表符）返回空串。 */
    @Test
    public void blankReturnsEmpty() {
        assertEquals("", IdCardUtil.normalize("   "));
        assertEquals("", IdCardUtil.normalize("\t \n"));
    }

    /** 去除首尾与中间空白。 */
    @Test
    public void removesAllWhitespace() {
        assertEquals("11010119900307891X",
                IdCardUtil.normalize("  110101 19900307\t891X "));
    }

    /** 18 位号码末位小写 x 转大写 X。 */
    @Test
    public void lowercaseXTurnsUppercase() {
        assertEquals("11010119900307891X", IdCardUtil.normalize("11010119900307891x"));
    }

    /** 非 18 位（如 15 位老号码）不做大小写转换。 */
    @Test
    public void non18DigitUnchanged() {
        assertEquals("110101900307891", IdCardUtil.normalize("110101900307891"));
    }

    /** 正常号码原样返回。 */
    @Test
    public void normalCardUnchanged() {
        assertEquals("11010119900307891X", IdCardUtil.normalize("11010119900307891X"));
    }
}

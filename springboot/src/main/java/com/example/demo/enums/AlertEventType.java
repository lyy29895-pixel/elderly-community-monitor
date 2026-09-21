package com.example.demo.enums;

/**
 * 告警事件类型状态机：对监护场景中产生的告警按来源进行大类划分。
 *
 * @author 丙
 */
public enum AlertEventType {
    /** 摔倒/跌倒报警 */
    FALL,
    /** 燃气泄漏报警 */
    GAS,
    /** 烟雾/火灾报警 */
    SMOKE,
    /** 久坐异常报警 */
    SEDENTARY,
    /** 其他类型事件 */
    OTHER
}

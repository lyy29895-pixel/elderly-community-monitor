package com.example.demo.enums;

/**
 * 告警事件处理状态状态机。
 * <p>典型流转：NEW（新建未处理）→ ACK（已确认）/ PROCESSING（处理中）→ CLOSED（已关闭）；
 * 经核实为误报的事件直接置为 FALSE_ALARM，不再进入正常处置流程。</p>
 *
 * @author 乙
 */
public enum AlertEventStatus {
    /** 新建：事件刚产生，尚未有人受理 */
    NEW,
    /** 已确认：工作人员已确认收到该事件 */
    ACK,
    /** 处理中：事件正在跟进处置 */
    PROCESSING,
    /** 已关闭：事件处置完成、已解决 */
    CLOSED,
    /** 误报：经核实为无效/误报告警 */
    FALSE_ALARM
}

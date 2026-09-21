package com.example.demo.dto;

import lombok.Data;

/**
 * 单个告警事件状态更新请求体。
 *
 * @author 乙
 */
@Data
public class AlertStatusUpdateRequest {
    /** 目标处理状态（NEW/PROCESSING/CLOSED 等，必填） */
    private String status;
    /** 处理备注（可选） */
    private String remark;
}

package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.dto.AlertStatusUpdateRequest;
import com.example.demo.entity.AlertEvent;
import com.example.demo.service.AlertEventService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 异常告警事件管理接口：事件多条件分页查询、单个/批量处理状态流转、
 * 已解决事件清理与单人事件统计。
 *
 * @author 乙
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertEventController {

    @Resource
    private AlertEventService alertEventService;

    /**
     * 分页查询异常事件列表，支持状态、严重等级、事件类型、关键词与时间范围组合筛选。
     *
     * @param pageNum    页码，默认 1
     * @param pageSize   每页条数，默认 10
     * @param elderlyId  老人ID，可空
     * @param status     处理状态，可空
     * @param severity   严重等级 1/2/3，可空
     * @param eventType  事件大类，可空
     * @param keyword    关键词（姓名/描述/备注），可空
     * @param timeRange  快捷时间范围 today/week/month，可空
     * @param startTime  自定义开始时间，可空
     * @param endTime    自定义结束时间，可空
     * @return 统一包装的事件分页结果
     */
    @GetMapping("/page")
    public Result<Page<AlertEvent>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long elderlyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(alertEventService.page(pageNum, pageSize, elderlyId, status, severity, eventType, keyword, timeRange, startTime, endTime));
    }

    /**
     * 更新单个事件的处理状态。
     *
     * @param id   事件ID
     * @param body 状态更新请求体，status 必填、remark 可选
     * @return status 为空时返回 400 错误，成功返回空成功体
     */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody AlertStatusUpdateRequest body) {
        if (body == null || body.getStatus() == null || body.getStatus().trim().isEmpty()) {
            return Result.error("400", "status 不能为空");
        }
        alertEventService.updateStatus(id, body.getStatus().trim(), body.getRemark());
        return Result.success();
    }

    /**
     * 批量更新事件处理状态。
     *
     * @param body 批量状态更新请求体，ids 与 status 必填、remark 可选
     * @return 参数缺失返回 400 错误；成功时 data 为实际更新条数
     */
    @PutMapping("/batch")
    public Result<?> batchUpdateStatus(@RequestBody BatchStatusUpdateRequest body) {
        if (body == null || body.getIds() == null || body.getIds().isEmpty()) {
            return Result.error("400", "ids 不能为空");
        }
        if (body.getStatus() == null || body.getStatus().trim().isEmpty()) {
            return Result.error("400", "status 不能为空");
        }
        int count = alertEventService.batchUpdateStatus(body.getIds(), body.getStatus().trim(), body.getRemark());
        return Result.success(count);
    }

    /**
     * 清理（物理删除）全部已解决事件。
     *
     * @return 统一包装的实际删除条数
     */
    @DeleteMapping("/resolved")
    public Result<Integer> clearResolved() {
        int count = alertEventService.clearResolved();
        return Result.success(count);
    }

    /**
     * 获取某位老人的异常事件统计（按类型分组+按状态分组）
     *
     * @param elderlyId 老人ID
     * @param days      统计时间窗口天数，默认 1
     * @return 统一包装的统计结果（总数、按类型计数、未解决数）
     */
    @GetMapping("/stats/{elderlyId}")
    public Result<Map<String, Object>> getAlertStats(@PathVariable Long elderlyId,
                                                      @RequestParam(defaultValue = "1") Integer days) {
        return Result.success(alertEventService.getAlertStats(elderlyId, days));
    }

    /**
     * 批量状态更新请求体。
     */
    public static class BatchStatusUpdateRequest {
        /** 待更新状态的事件ID列表 */
        private List<Long> ids;
        /** 目标处理状态 */
        private String status;
        /** 处理备注（可选） */
        private String remark;

        public List<Long> getIds() { return ids; }
        public void setIds(List<Long> ids) { this.ids = ids; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}

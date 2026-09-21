package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.Elderly;
import com.example.demo.service.ElderlyService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 老人档案与监护概览接口，接口前缀 /api/elderly；
 * 提供档案增删改查及模拟的环境、健康、设备状态批量概览数据。
 *
 * @author 甲
 */
@RestController
@RequestMapping("/api/elderly")
public class ElderlyController {

    @Resource
    private ElderlyService elderlyService;

    /**
     * 批量获取所有老人的概览数据（环境+健康+设备状态）
     * 数据由模拟采集器生成，1 分钟内保持稳定，避免页面频繁刷新时数值跳变
     *
     * @return 每位老人一条概览数据（温湿度、心率、呼吸、睡眠、在床与在线状态等）
     */
    @GetMapping("/batch-overview")
    public Result<List<Map<String, Object>>> batchOverview() {
        List<Elderly> elders = elderlyService.listForCurrentUser();
        if (elders == null || elders.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<Map<String, Object>> result = new ArrayList<>(elders.size());
        Random rnd = new Random(System.currentTimeMillis() / 60000L); // 1分钟内稳定随机

        for (Elderly e : elders) {
            Map<String, Object> row = new HashMap<>();
            row.put("elderlyId", e.getId());

            // --- 环境数据：温湿度（模拟） ---
            double baseT = 24.5 + (rnd.nextInt(30) - 15) * 0.1; // 23-26℃
            double baseH = 52.0 + (rnd.nextInt(40) - 20) * 0.3; // 46-58%
            row.put("temperature", BigDecimal.valueOf(Math.round(baseT * 10.0) / 10.0));
            row.put("humidity", BigDecimal.valueOf(Math.round(baseH * 10.0) / 10.0));
            row.put("recordTime", LocalDateTime.now(ZoneId.of("+8")).toString());

            // --- 健康数据：心率/呼吸/睡眠/在床（模拟） ---
            row.put("heartRate", (double) (68 + rnd.nextInt(12)));   // 68-79
            row.put("breathingRate", (double) (15 + rnd.nextInt(4))); // 15-18
            row.put("sleepScore", 85.0 + rnd.nextInt(10));            // 85-94
            row.put("sleepStatus", rnd.nextInt(5) == 0 ? "AWAKE" : "LIGHT_SLEEP");
            row.put("onBedStatus", rnd.nextInt(10) < 3 ? "OFF_BED" : "ON_BED");

            // --- 设备在线状态（模拟数据始终在线） ---
            row.put("online", true);

            result.add(row);
        }

        return Result.success(result);
    }

    /**
     * 查询当前登录用户可见的老人档案列表。
     *
     * @return 老人档案列表统一响应
     */
    @GetMapping("/list")
    public Result<List<Elderly>> list() {
        return Result.success(elderlyService.listForCurrentUser());
    }

    /**
     * 按 ID 查询单个老人档案（含访问权限校验）。
     *
     * @param id 老人档案 ID
     * @return 老人档案统一响应
     */
    @GetMapping("/{id}")
    public Result<Elderly> get(@PathVariable Long id) {
        return Result.success(elderlyService.getByIdForCurrentUser(id));
    }

    /**
     * 新增老人档案 + 自动创建对应老人账号
     *
     * @param elderly 请求体中的老人档案信息
     * @return 创建成功后含主键的老人档案
     */
    @PostMapping
    public Result<Elderly> create(@RequestBody Elderly elderly) {
        return Result.success(elderlyService.create(elderly));
    }

    /**
     * 修改老人档案。
     *
     * @param elderly 请求体中含主键与待更新字段的老人档案
     * @return 不携带数据的成功统一响应
     */
    @PutMapping
    public Result<?> update(@RequestBody Elderly elderly) {
        elderlyService.update(elderly);
        return Result.success();
    }

    /**
     * 删除指定老人档案。
     *
     * @param id 老人档案 ID
     * @return 不携带数据的成功统一响应
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        elderlyService.delete(id);
        return Result.success();
    }
}

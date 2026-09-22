package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.EnvironmentData;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 环境数据服务类
 * 负责生成老人居住环境的模拟温湿度时序数据：以老人ID派生稳定的常态基线，
 * 叠加长短周期波形与小幅随机扰动，并支持按时间范围分页查询。
 *
 * @author 乙
 */
@Service
public class EnvironmentDataService {

    @Resource
    private ElderlyAccessService elderlyAccessService;

    /**
     * 分页查询老人环境数据：先校验当前用户对该老人的访问权限，再按时间范围生成模拟温湿度记录。
     *
     * @param elderlyId 老人ID
     * @param pageNum   页码，从1开始
     * @param pageSize  每页条数
     * @param start     查询起始时间，为null时默认最近24小时
     * @param end       查询截止时间，为null时默认当前时间
     * @return 环境数据分页结果
     */
    public Page<EnvironmentData> page(Long elderlyId, int pageNum, int pageSize, LocalDateTime start, LocalDateTime end) {
        elderlyAccessService.assertCanAccessElderly(elderlyId);

        LocalDateTime nowLdt = LocalDateTime.now(java.time.ZoneId.of("+8"));
        LocalDateTime startTime = start != null ? start : nowLdt.minusHours(24);
        LocalDateTime endTime = end != null ? end : nowLdt;

        return generateMockEnvHistory(elderlyId, pageNum, pageSize, startTime, endTime);
    }

    /**
     * 生成模拟环境历史数据并封装为分页结果。
     * 时间跨度不超过5分钟时返回固定温度26.5℃、湿度55%（1分钟间隔）；
     * 更大跨度按1/5/15/30分钟自适应采样，基于老人ID派生稳定基线，
     * 再叠加长周期正弦、短周期余弦与小幅随机扰动，温度、湿度结果均保留一位小数并做上下限收敛。
     */
    private Page<EnvironmentData> generateMockEnvHistory(Long elderlyId, int pageNum, int pageSize, LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("+8"));
        LocalDateTime startTime = start != null ? start : now.minusHours(24);
        LocalDateTime endTime = end != null ? end : now;

        long minutes = java.time.Duration.between(startTime, endTime).toMinutes();

        // ≤5分钟：返回固定温度26.5℃、湿度55%（与修改前的旧行为完全一致），1分钟间隔
        if (minutes <= 5) {
            List<EnvironmentData> mockData = new ArrayList<>();
            long totalMins = minutes <= 0 ? 5 : minutes;
            int points = (int) totalMins;
            if (points < 10) points = 10;
            if (points > pageSize) points = pageSize;
            for (int i = 0; i < points; i++) {
                EnvironmentData data = new EnvironmentData();
                data.setElderlyId(elderlyId);
                data.setDeviceId("mock_device_" + elderlyId);
                data.setTemperature(java.math.BigDecimal.valueOf(26.5));
                data.setHumidity(java.math.BigDecimal.valueOf(55.0));
                LocalDateTime rt = endTime.minusMinutes(i);
                data.setReportTime(rt);
                data.setRecordedAt(rt.atZone(java.time.ZoneId.of("+8")).toInstant().toEpochMilli());
                mockData.add(data);
            }
            Page<EnvironmentData> page = new Page<>(pageNum, pageSize);
            page.setTotal(mockData.size());
            page.setRecords(mockData);
            return page;
        }

        int intervalMinutes;
        if (minutes <= 30) {
            intervalMinutes = 1;
        } else if (minutes <= 120) {
            intervalMinutes = 5;
        } else if (minutes <= 720) {
            intervalMinutes = 15;
        } else {
            intervalMinutes = 30;
        }

        int points = (int) (minutes / intervalMinutes);
        if (points < 12) points = 12;
        if (points > pageSize) points = pageSize;

        // 基于老人ID生成稳定的基线：每个老人的"常态温湿度"略有差异，避免千篇一律
        long seed = elderlyId * 49297L + 233280L;
        java.util.Random rnd = new java.util.Random(seed);
        double baseTemp = 25.5 + (Math.abs(seed) % 60) * 0.05;       // 25.5 ~ 28.5 ℃
        double baseHum = 48.0 + (Math.abs(seed >> 3) % 160) * 0.1; // 48.0 ~ 64.0 %
        baseTemp = Math.max(22.0, Math.min(30.0, baseTemp));
        baseHum = Math.max(42.0, Math.min(70.0, baseHum));

        List<EnvironmentData> mockData = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            EnvironmentData data = new EnvironmentData();
            data.setElderlyId(elderlyId);
            data.setDeviceId("mock_device_" + elderlyId);

            LocalDateTime recordTime = endTime.minusMinutes((long) i * intervalMinutes);
            // 温和的多频段波动：长周期(整段范围)正弦 + 短周期(相邻点)余弦 + 小幅随机扰动
            double phaseLong = (double) i / points * Math.PI * 2;          // 整个范围只走1个周期，变化很缓
            double phaseShort = (double) i / Math.max(4, points / 6) * Math.PI * 2; // 短周期起伏
            double jitterT = (rnd.nextDouble() - 0.5) * 0.6;
            double jitterH = (rnd.nextDouble() - 0.5) * 2.0;

            // 温度：整体波动控制在 ±1.8℃ 以内
            double t = baseTemp
                    + Math.sin(phaseLong) * 0.8
                    + Math.cos(phaseShort) * 0.6
                    + jitterT;
            t = Math.max(20.0, Math.min(33.0, t));

            // 湿度：整体波动控制在 ±6% 以内
            double h = baseHum
                    + Math.cos(phaseLong * 0.9) * 3.0
                    + Math.sin(phaseShort) * 1.8
                    + jitterH;
            h = Math.max(38.0, Math.min(80.0, h));

            data.setTemperature(java.math.BigDecimal.valueOf(Math.round(t * 10) / 10.0));
            data.setHumidity(java.math.BigDecimal.valueOf(Math.round(h * 10) / 10.0));
            data.setReportTime(recordTime);
            data.setRecordedAt(recordTime.atZone(java.time.ZoneId.of("+8")).toInstant().toEpochMilli());

            mockData.add(data);
        }

        Page<EnvironmentData> page = new Page<>(pageNum, pageSize);
        page.setTotal(points);
        page.setRecords(mockData);

        return page;
    }
}

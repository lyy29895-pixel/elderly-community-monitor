package com.example.demo.task;

import com.example.demo.service.AnnouncementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 公告定时任务：固定速率每分钟触发一次，驱动预约公告发布与到期公告过期处理。
 *
 * @author 乙
 */
@Component
public class AnnouncementScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementScheduledTask.class);

    @Resource
    private AnnouncementService announcementService;

    /**
     * 每分钟执行一次，处理预约发布和过期公告
     */
    @Scheduled(fixedRate = 60_000)
    public void processAnnouncements() {
        try {
            // 处理预约发布
            announcementService.processScheduledPublish();
            // 处理过期公告
            announcementService.processExpiredAnnouncements();
        } catch (Exception e) {
            log.error("公告定时任务执行失败", e);
        }
    }
}

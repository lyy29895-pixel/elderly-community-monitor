package com.example.demo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.auth.AuthContext;
import com.example.demo.entity.Announcement;
import com.example.demo.enums.UserRole;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.AnnouncementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 公告业务服务：提供公告分页查询、发布（即时/预约）、修改、删除、撤下，
 * 以及供定时任务调用的预约发布与到期过期处理。
 * */
@Service
public class AnnouncementService {

    @Resource
    private AnnouncementMapper announcementMapper;

    /** 公告状态常量 */
    /** 状态：待发布 */
    public static final int STATUS_PENDING = 0;    // 待发布
    /** 状态：已发布 */
    public static final int STATUS_PUBLISHED = 1;  // 已发布
    /** 状态：已撤下 */
    public static final int STATUS_RECALLED = 2;   // 已撤下
    /** 状态：已过期 */
    public static final int STATUS_EXPIRED = 3;    // 已过期

    /** 发布方式常量 */
    /** 发布方式：即时发布 */
    public static final int PUBLISH_IMMEDIATE = 0; // 即时发布
    /** 发布方式：预约发布 */
    public static final int PUBLISH_SCHEDULED = 1; // 预约发布

    /**
     * 分页查询公告，按优先级、发布时间、创建时间倒序排列；
     * 非平台四类已知角色返回空分页。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 公告分页结果
     */
    public Page<Announcement> page(int pageNum, int pageSize) {
        String role = UserRole.normalize(AuthContext.getRole());
        if (UserRole.isAdmin(role) || UserRole.isCommunity(role) || UserRole.isElder(role) || UserRole.isChild(role)) {
            return announcementMapper.selectPage(new Page<>(pageNum, pageSize),
                    Wrappers.<Announcement>lambdaQuery()
                            .orderByDesc(Announcement::getPriority)
                            .orderByDesc(Announcement::getPublishedAt, Announcement::getCreatedAt));
        }
        return new Page<>(pageNum, pageSize);
    }

    /**
     * 发布公告（仅社区/管理员）：预约发布且指定了预约时间时置为待发布，
     * 其余情况即时发布并记录发布时间；优先级为空时默认 0。
     *
     * @param announcement 待发布公告
     * @return 插入成功后含主键与状态的公告
     */
    @Transactional(rollbackFor = Exception.class)
    public Announcement publish(Announcement announcement) {
        String role = UserRole.normalize(AuthContext.getRole());
        if (!UserRole.isCommunityOrAdmin(role)) {
            throw new BusinessException("403", "仅社区或管理员可发布公告");
        }

        LocalDateTime now = LocalDateTime.now();

        // 判断发布方式
        if (Objects.equals(announcement.getPublishType(), PUBLISH_SCHEDULED) && announcement.getScheduledAt() != null) {
            // 预约发布
            announcement.setStatus(STATUS_PENDING);
            // 预约发布时 publishedAt 为空，等定时任务触发时再设置
        } else {
            // 即时发布
            announcement.setPublishType(PUBLISH_IMMEDIATE);
            announcement.setPublishedAt(now);
            announcement.setStatus(STATUS_PUBLISHED);

            // 如果设置了有效时长，计算结束时间
            if (announcement.getEndTime() != null) {
                // endTime 由前端传入
            }
        }

        if (announcement.getPriority() == null) {
            announcement.setPriority(0);
        }

        announcementMapper.insert(announcement);
        return announcement;
    }

    /**
     * 修改公告（仅社区/管理员），且仅待发布状态的公告允许修改。
     *
     * @param id           公告 ID
     * @param announcement 含待更新字段的公告内容
     * @return 更新后的最新公告
     */
    @Transactional(rollbackFor = Exception.class)
    public Announcement update(Long id, Announcement announcement) {
        String role = UserRole.normalize(AuthContext.getRole());
        if (!UserRole.isCommunityOrAdmin(role)) {
            throw new BusinessException("403", "仅社区或管理员可修改公告");
        }
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("404", "公告不存在");
        }
        // 只允许修改待发布状态的公告
        if (!Objects.equals(existing.getStatus(), STATUS_PENDING)) {
            throw new BusinessException("400", "仅待发布状态的公告可修改");
        }
        announcement.setId(id);
        announcementMapper.updateById(announcement);
        return announcementMapper.selectById(id);
    }

    /**
     * 删除公告（仅社区/管理员）。
     *
     * @param id 公告 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        String role = UserRole.normalize(AuthContext.getRole());
        if (!UserRole.isCommunityOrAdmin(role)) {
            throw new BusinessException("403", "仅社区或管理员可删除公告");
        }
        announcementMapper.deleteById(id);
    }

    /**
     * 撤下公告（手动撤下）
     *
     * @param id 公告 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void recall(Long id) {
        String role = UserRole.normalize(AuthContext.getRole());
        if (!UserRole.isCommunityOrAdmin(role)) {
            throw new BusinessException("403", "仅社区或管理员可撤下公告");
        }
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("404", "公告不存在");
        }
        existing.setStatus(STATUS_RECALLED);
        announcementMapper.updateById(existing);
    }

    // ========== 定时任务调用的方法 ==========

    /**
     * 处理预约发布的公告：将到期的待发布公告改为已发布
     */
    @Transactional(rollbackFor = Exception.class)
    public void processScheduledPublish() {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> pendingList = announcementMapper.selectList(
                Wrappers.<Announcement>lambdaQuery()
                        .eq(Announcement::getStatus, STATUS_PENDING)
                        .eq(Announcement::getPublishType, PUBLISH_SCHEDULED)
                        .le(Announcement::getScheduledAt, now));
        for (Announcement item : pendingList) {
            item.setStatus(STATUS_PUBLISHED);
            item.setPublishedAt(now);
            announcementMapper.updateById(item);
        }
    }

    /**
     * 处理过期的公告：将已发布且超过结束时间的公告改为已过期
     */
    @Transactional(rollbackFor = Exception.class)
    public void processExpiredAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> expiredList = announcementMapper.selectList(
                Wrappers.<Announcement>lambdaQuery()
                        .eq(Announcement::getStatus, STATUS_PUBLISHED)
                        .isNotNull(Announcement::getEndTime)
                        .le(Announcement::getEndTime, now));
        for (Announcement item : expiredList) {
            item.setStatus(STATUS_EXPIRED);
            announcementMapper.updateById(item);
        }
    }
}

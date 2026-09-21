package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Announcement;
import com.example.demo.service.AnnouncementService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 公告管理接口，接口前缀 /api/announcements；提供公告分页、发布、修改、删除与撤下。
 *
 * @author 甲
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Resource
    private AnnouncementService announcementService;

    /**
     * 分页查询公告列表。
     *
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @return 公告分页结果统一响应
     */
    @GetMapping("/page")
    public Result<Page<Announcement>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(announcementService.page(pageNum, pageSize));
    }

    /**
     * 发布公告（即时发布或预约发布）。
     *
     * @param announcement 请求体中的公告内容
     * @return 创建成功后含主键与状态的公告
     */
    @PostMapping
    public Result<Announcement> publish(@RequestBody Announcement announcement) {
        return Result.success(announcementService.publish(announcement));
    }

    /**
     * 修改待发布状态的公告。
     *
     * @param id           公告 ID
     * @param announcement 请求体中待更新的公告内容
     * @return 更新后的最新公告
     */
    @PutMapping("/{id}")
    public Result<Announcement> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        return Result.success(announcementService.update(id, announcement));
    }

    /**
     * 删除指定公告。
     *
     * @param id 公告 ID
     * @return 不携带数据的成功统一响应
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success();
    }

    /**
     * 手动撤下指定公告。
     *
     * @param id 公告 ID
     * @return 不携带数据的成功统一响应
     */
    @PutMapping("/{id}/recall")
    public Result<?> recall(@PathVariable Long id) {
        announcementService.recall(id);
        return Result.success();
    }
}

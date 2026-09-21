package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.AssessAllVO;
import com.example.demo.dto.CommunityOverviewVO;
import com.example.demo.service.CommunityOverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 辖区监控接口：提供社区工作人员/管理员的辖区概览查询与全员一键评估入口。
 * */
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    @Resource
    private CommunityOverviewService communityOverviewService;

    /**
     * 查询辖区概览：顶部汇总指标、整体统计数据与逐人风险列表。
     *
     * @return 统一包装的辖区概览数据
     */
    @GetMapping("/overview")
    public Result<CommunityOverviewVO> overview() {
        return Result.success(communityOverviewService.overview());
    }

    /**
     * 一键评估：对全部老人重新评估风险等级与健康评分，结果写入档案
     *
     * @return 统一包装的全员评估结果
     */
    @PostMapping("/assess-all")
    public Result<AssessAllVO> assessAll() {
        return Result.success(communityOverviewService.assessAll());
    }
}

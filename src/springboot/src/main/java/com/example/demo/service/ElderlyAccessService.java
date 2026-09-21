package com.example.demo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.auth.AuthContext;
import com.example.demo.entity.Elderly;
import com.example.demo.entity.User;
import com.example.demo.enums.UserRole;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.ElderlyMapper;
import com.example.demo.mapper.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 老人数据访问权限服务：按角色判定当前用户对老人档案的可见性，
 * 社区/管理员可访问全部，老人仅可访问与本人用户名关联的档案，子女无权访问。
 * */
@Service
public class ElderlyAccessService {

    @Resource
    private ElderlyMapper elderlyMapper;
    @Resource
    private UserMapper userMapper;

    /**
     * 断言当前登录用户可访问指定老人档案，校验不通过时直接抛出业务异常。
     *
     * @param elderlyId 老人档案 ID
     */
    public void assertCanAccessElderly(Long elderlyId) {
        if (elderlyId == null) {
            throw new BusinessException("404", "老人不存在");
        }
        Elderly elderly = elderlyMapper.selectById(elderlyId);
        if (elderly == null) {
            throw new BusinessException("404", "老人不存在");
        }
        Integer uid = AuthContext.getUserId();
        String role = UserRole.normalize(AuthContext.getRole());
        if (UserRole.isAdmin(role) || UserRole.isCommunity(role)) {
            return;
        }
        if (UserRole.isElder(role)) {
            User u = userMapper.selectById(uid);
            if (u != null && elderly.getElderUsername() != null 
                    && elderly.getElderUsername().equals(u.getUsername())) {
                return;
            }
            throw new BusinessException("403", "无权限访问该老人数据");
        }
        if (UserRole.isChild(role)) {
            throw new BusinessException("403", "无权限访问该老人数据");
        }
        throw new BusinessException("403", "无权限访问该老人数据");
    }

    /**
     * 判断指定用户是否有权访问某份老人档案（不抛异常的布尔判定版本）。
     *
     * @param userId    用户 ID
     * @param role      用户角色
     * @param elderlyId 老人档案 ID
     * @return 可访问返回 true；参数缺失、档案不存在或无权限返回 false
     */
    public boolean canUserAccessElderly(Integer userId, String role, Long elderlyId) {
        if (userId == null || elderlyId == null) {
            return false;
        }
        Elderly elderly = elderlyMapper.selectById(elderlyId);
        if (elderly == null) {
            return false;
        }
        String r = UserRole.normalize(role);
        if (UserRole.isAdmin(r) || UserRole.isCommunity(r)) {
            return true;
        }
        if (UserRole.isElder(r)) {
            User u = userMapper.selectById(userId);
            return u != null && elderly.getElderUsername() != null 
                    && elderly.getElderUsername().equals(u.getUsername());
        }
        if (UserRole.isChild(r)) {
            return false;
        }
        return false;
    }

    /**
     * 查询指定用户可访问的全部老人档案 ID：社区/管理员为全部档案，
     * 老人为与本人用户名关联的单份档案，子女及其他角色为空列表。
     *
     * @param userId 用户 ID
     * @param role   用户角色
     * @return 可访问的老人档案 ID 列表
     */
    public List<Long> getAccessibleElderlyIds(Integer userId, String role) {
        String r = UserRole.normalize(role);
        if (UserRole.isAdmin(r) || UserRole.isCommunity(r)) {
            return elderlyMapper.selectList(Wrappers.emptyWrapper())
                    .stream().map(Elderly::getId).collect(Collectors.toList());
        }
        if (UserRole.isElder(r)) {
            User u = userMapper.selectById(userId);
            if (u != null) {
                Elderly e = elderlyMapper.selectOne(
                        Wrappers.<Elderly>lambdaQuery().eq(Elderly::getElderUsername, u.getUsername()));
                if (e != null) {
                    return Collections.singletonList(e.getId());
                }
            }
            return Collections.emptyList();
        }
        if (UserRole.isChild(r)) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    /**
     * 断言当前登录用户为社区工作人员或管理员，否则抛出 403 业务异常。
     */
    public void requireCommunityStaff() {
        String role = UserRole.normalize(AuthContext.getRole());
        if (UserRole.isAdmin(role)) {
            return;
        }
        if (!UserRole.isCommunity(role)) {
            throw new BusinessException("403", "需要社区工作人员权限");
        }
    }
}

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
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 老人档案业务服务：按当前登录角色提供档案的查询、增删改，
 * 所有访问均通过 {@link ElderlyAccessService} 进行权限校验。
 *
 * @author 甲
 */
@Service
public class ElderlyService {

    @Resource
    private ElderlyMapper elderlyMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ElderlyAccessService elderlyAccessService;

    /**
     * 查询当前登录用户可见的老人档案列表：社区/管理员可见全部（按更新时间倒序），
     * 老人仅可见与自己用户名关联的档案，子女及其他角色返回空列表。
     *
     * @return 当前用户可见的老人档案列表；无可见数据时返回空列表
     */
    public List<Elderly> listForCurrentUser() {
        Integer uid = AuthContext.getUserId();
        String role = UserRole.normalize(AuthContext.getRole());
        
        List<Elderly> elders;
        
        if (UserRole.isCommunity(role) || UserRole.isAdmin(role)) {
            elders = elderlyMapper.selectList(Wrappers.<Elderly>lambdaQuery().orderByDesc(Elderly::getUpdatedAt));
        } else if (UserRole.isElder(role)) {
            User u = userMapper.selectById(uid);
            if (u == null) {
                return Collections.emptyList();
            }
            Elderly e = elderlyMapper.selectOne(
                    Wrappers.<Elderly>lambdaQuery().eq(Elderly::getElderUsername, u.getUsername()));
            elders = e == null ? Collections.emptyList() : Collections.singletonList(e);
        } else if (UserRole.isChild(role)) {
            return Collections.emptyList();
        } else {
            return Collections.emptyList();
        }

        return elders;
    }

    /**
     * 按 ID 查询老人档案，并先校验当前用户是否有访问权限。
     *
     * @param id 老人档案 ID
     * @return 老人档案；不存在时返回 null
     */
    public Elderly getByIdForCurrentUser(Long id) {
        elderlyAccessService.assertCanAccessElderly(id);
        return elderlyMapper.selectById(id);
    }

    /**
     * 按 ID 直接查询老人档案，不做权限校验（供服务内部调用）。
     *
     * @param id 老人档案 ID
     * @return 老人档案；不存在时返回 null
     */
    public Elderly getById(Long id) {
        return elderlyMapper.selectById(id);
    }

    /**
     * 新建老人档案（仅社区工作人员/管理员）。规范化姓名后回填真实姓名与登录用户名，
     * 未指定老人用户 ID 时按现有最大值顺延生成。
     *
     * @param elderly 待创建的老人档案信息
     * @return 插入成功后含主键的老人档案
     */
    @Transactional(rollbackFor = Exception.class)
    public Elderly create(Elderly elderly) {
        elderlyAccessService.requireCommunityStaff();
        
        String name = elderly.getName();
        if (name != null) {
            name = name.trim();
        } else {
            name = elderly.getRealName();
        }
        
        if (name == null || name.isEmpty()) {
            throw new BusinessException("姓名不能为空");
        }
        
        elderly.setRealName(name);
        elderly.setElderUsername(name);
        
        if (elderly.getElderUserId() == null) {
            Elderly last = elderlyMapper.selectOne(
                    Wrappers.<Elderly>lambdaQuery().orderByDesc(Elderly::getElderUserId).last("LIMIT 1"));
            long nextId = (last != null && last.getElderUserId() != null) ? last.getElderUserId() + 1 : 1L;
            elderly.setElderUserId(nextId);
        }
        
        elderlyMapper.insert(elderly);
        
        return elderly;
    }

    /**
     * 修改老人档案（仅社区工作人员/管理员，且需有该档案访问权）。
     * 传入 name 时同步更新真实姓名，其余字段按主键非空更新。
     *
     * @param elderly 含主键与待更新字段的老人档案
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Elderly elderly) {
        elderlyAccessService.assertCanAccessElderly(elderly.getId());
        elderlyAccessService.requireCommunityStaff();
        
        if (elderly.getName() != null && !elderly.getName().isEmpty()) {
            elderly.setRealName(elderly.getName());
        }
        
        elderlyMapper.updateById(elderly);
    }

    /**
     * 删除老人档案（仅社区工作人员/管理员），档案不存在时抛出业务异常。
     *
     * @param elderlyId 老人档案 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long elderlyId) {
        elderlyAccessService.assertCanAccessElderly(elderlyId);
        elderlyAccessService.requireCommunityStaff();

        Elderly elderly = elderlyMapper.selectById(elderlyId);
        if (elderly == null) {
            throw new BusinessException("404", "老人档案不存在");
        }

        elderlyMapper.deleteById(elderlyId);
    }
}

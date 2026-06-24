package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.dto.UserInfo;
import com.scjc.ndt.dto.UserRequest;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;
import com.scjc.ndt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final UserProjectRelMapper userProjectRelMapper;
    private final SysDeptMapper deptMapper;
    private final SysProjectMapper projectMapper;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> CAN_CREATE_SYSTEM = Set.of("COMPANY_ADMIN", "BU_ADMIN", "PROJECT_ADMIN", "TECHNICAL_LEADER", "PROJECT_MANAGER");
    private static final Set<String> CAN_CREATE_COMPANY = Set.of("BU_ADMIN");
    private static final Set<String> CAN_CREATE_BU = Set.of("PROJECT_ADMIN", "TECHNICAL_LEADER", "PROJECT_MANAGER");

    @Override
    public IPage<UserInfo> listUsers(Integer page, Integer size, String keyword) {
        Page<SysUser> p = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(keyword), SysUser::getRealName, keyword)
                .orderByDesc(SysUser::getCreateTime);
        return userMapper.selectPage(p, q).convert(this::toUserInfo);
    }

    @Override
    @Transactional
    public SysUser createUser(UserRequest request, Long creatorId) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }

        List<String> creatorRoles = getRoleCodes(creatorId);

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            List<SysRole> targetRoles = roleMapper.selectBatchIds(request.getRoleIds());
            for (SysRole r : targetRoles) {
                boolean allowed = false;
                if (creatorRoles.contains("SYSTEM_ADMIN") && CAN_CREATE_SYSTEM.contains(r.getRoleCode())) allowed = true;
                if (creatorRoles.contains("COMPANY_ADMIN") && CAN_CREATE_COMPANY.contains(r.getRoleCode())) allowed = true;
                if (creatorRoles.contains("BU_ADMIN") && CAN_CREATE_BU.contains(r.getRoleCode())) allowed = true;
                if (!allowed) throw new BusinessException(403, "无权创建角色: " + r.getRoleName());
            }
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setDeptId(request.getDeptId());
        user.setCreateBy(creatorId);
        userMapper.insert(user);

        if (request.getRoleIds() != null) {
            for (Long roleId : request.getRoleIds()) {
                UserRoleRel rel = new UserRoleRel();
                rel.setUserId(user.getId());
                rel.setRoleId(roleId);
                userRoleRelMapper.insert(rel);
            }
        }
        if (request.getProjectIds() != null) {
            for (Long projectId : request.getProjectIds()) {
                UserProjectRel rel = new UserProjectRel();
                rel.setUserId(user.getId());
                rel.setProjectId(projectId);
                userProjectRelMapper.insert(rel);
            }
        }
        return user;
    }

    @Override
    @Transactional
    public void updateUser(Long id, UserRequest request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if (StringUtils.hasText(request.getRealName())) user.setRealName(request.getRealName());
        if (StringUtils.hasText(request.getPhone())) user.setPhone(request.getPhone());
        if (StringUtils.hasText(request.getEmail())) user.setEmail(request.getEmail());
        if (request.getDeptId() != null) user.setDeptId(request.getDeptId());
        if (StringUtils.hasText(request.getPassword())) user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.updateById(user);

        if (request.getRoleIds() != null) {
            userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, id));
            request.getRoleIds().forEach(roleId -> {
                UserRoleRel rel = new UserRoleRel();
                rel.setUserId(id);
                rel.setRoleId(roleId);
                userRoleRelMapper.insert(rel);
            });
        }
        if (request.getProjectIds() != null) {
            userProjectRelMapper.delete(new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, id));
            request.getProjectIds().forEach(projectId -> {
                UserProjectRel rel = new UserProjectRel();
                rel.setUserId(id);
                rel.setProjectId(projectId);
                userProjectRelMapper.insert(rel);
            });
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId));
        if (roleIds != null) {
            roleIds.forEach(roleId -> {
                UserRoleRel rel = new UserRoleRel();
                rel.setUserId(userId);
                rel.setRoleId(roleId);
                userRoleRelMapper.insert(rel);
            });
        }
    }

    @Override
    @Transactional
    public void assignProjects(Long userId, List<Long> projectIds) {
        userProjectRelMapper.delete(new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId));
        if (projectIds != null) {
            projectIds.forEach(projectId -> {
                UserProjectRel rel = new UserProjectRel();
                rel.setUserId(userId);
                rel.setProjectId(projectId);
                userProjectRelMapper.insert(rel);
            });
        }
    }

    @Override
    public UserInfo getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        return toUserInfo(user);
    }

    private UserInfo toUserInfo(SysUser user) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setPhone(user.getPhone());
        info.setEmail(user.getEmail());
        info.setDeptId(user.getDeptId());
        info.setStatus(user.getStatus());
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) info.setDeptName(dept.getDeptName());
        }
        info.setRoles(getRoleCodes(user.getId()));
        List<UserProjectRel> projRels = userProjectRelMapper.selectList(
            new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, user.getId())
        );
        info.setProjectIds(projRels.stream().map(UserProjectRel::getProjectId).collect(Collectors.toList()));
        if (!projRels.isEmpty()) {
            List<Long> pids = projRels.stream().map(UserProjectRel::getProjectId).collect(Collectors.toList());
            info.setProjectNames(projectMapper.selectBatchIds(pids).stream()
                .map(SysProject::getProjectName).collect(Collectors.toList()));
        }
        return info;
    }

    private List<String> getRoleCodes(Long userId) {
        List<UserRoleRel> rels = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId)
        );
        if (rels.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = rels.stream().map(UserRoleRel::getRoleId).collect(Collectors.toList());
        return roleMapper.selectBatchIds(roleIds).stream().map(SysRole::getRoleCode).collect(Collectors.toList());
    }
}

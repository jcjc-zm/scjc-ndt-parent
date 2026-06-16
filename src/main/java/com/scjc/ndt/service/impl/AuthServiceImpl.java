package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.common.JwtUtils;
import com.scjc.ndt.dto.LoginRequest;
import com.scjc.ndt.dto.LoginResponse;
import com.scjc.ndt.dto.UserInfo;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;
import com.scjc.ndt.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper roleMapper;
    private final UserProjectRelMapper userProjectRelMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
        );
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账户已被禁用");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        UserInfo userInfo = buildUserInfo(user);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserInfo(userInfo);
        return response;
    }

    private UserInfo buildUserInfo(SysUser user) {
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

        List<UserRoleRel> rels = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, user.getId())
        );
        List<Long> roleIds = rels.stream().map(UserRoleRel::getRoleId).collect(Collectors.toList());
        List<String> roles = roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode).collect(Collectors.toList());
        info.setRoles(roles);

        List<UserProjectRel> projRels = userProjectRelMapper.selectList(
            new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, user.getId())
        );
        info.setProjectIds(projRels.stream().map(UserProjectRel::getProjectId).collect(Collectors.toList()));

        return info;
    }
}

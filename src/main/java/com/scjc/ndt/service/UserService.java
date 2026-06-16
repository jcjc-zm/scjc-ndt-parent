package com.scjc.ndt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.dto.UserInfo;
import com.scjc.ndt.dto.UserRequest;
import com.scjc.ndt.entity.SysUser;

import java.util.List;

public interface UserService {
    IPage<UserInfo> listUsers(Integer page, Integer size, String keyword);
    SysUser createUser(UserRequest request, Long creatorId);
    void updateUser(Long id, UserRequest request);
    void updateStatus(Long id, Integer status);
    void assignRoles(Long userId, List<Long> roleIds);
    void assignProjects(Long userId, List<Long> projectIds);
    UserInfo getById(Long id);
}

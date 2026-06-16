package com.scjc.ndt.service.impl;

import com.scjc.ndt.entity.SysRole;
import com.scjc.ndt.mapper.SysRoleMapper;
import com.scjc.ndt.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;

    @Override
    public List<SysRole> getAll() {
        return roleMapper.selectList(null);
    }
}

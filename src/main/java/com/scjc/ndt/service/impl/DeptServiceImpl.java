package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.entity.SysDept;
import com.scjc.ndt.mapper.SysDeptMapper;
import com.scjc.ndt.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper deptMapper;

    @Override
    public List<SysDept> getTree() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));
    }

    @Override
    public List<SysDept> getBuList() {
        return deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptType, "BU")
                .eq(SysDept::getStatus, 1)
                .orderByAsc(SysDept::getSort)
        );
    }

    @Override
    public SysDept create(SysDept dept) {
        deptMapper.insert(dept);
        return dept;
    }

    @Override
    public SysDept update(Long id, SysDept dept) {
        if (deptMapper.selectById(id) == null) throw new BusinessException("部门不存在");
        dept.setId(id);
        deptMapper.updateById(dept);
        return dept;
    }

    @Override
    public void delete(Long id) {
        if (deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id)) > 0) {
            throw new BusinessException("请先删除子部门");
        }
        deptMapper.deleteById(id);
    }
}

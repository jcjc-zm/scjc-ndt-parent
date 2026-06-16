package com.scjc.ndt.service;

import com.scjc.ndt.entity.SysDept;
import java.util.List;

public interface DeptService {
    List<SysDept> getTree();
    List<SysDept> getBuList();
    SysDept create(SysDept dept);
    SysDept update(Long id, SysDept dept);
    void delete(Long id);
}

package com.scjc.ndt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.dto.ProjectRequest;
import com.scjc.ndt.dto.TreeNode;
import com.scjc.ndt.entity.SysProject;

import java.util.List;

public interface ProjectService {
    IPage<SysProject> listProjects(Integer page, Integer size, String keyword, String projectType, String buName, Long userId);
    List<TreeNode> getTree(Long userId);
    SysProject create(ProjectRequest request, Long creatorId);
    SysProject update(Long id, ProjectRequest request);
    void updateStatus(Long id, String status);
    SysProject getById(Long id);
    List<SysProject> getByBuName(String buName);
}

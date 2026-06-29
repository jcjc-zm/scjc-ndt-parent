package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.dto.ProjectRequest;
import com.scjc.ndt.dto.TreeNode;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;

import java.util.Objects;
import com.scjc.ndt.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final SysProjectMapper projectMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper roleMapper;
    private final UserProjectRelMapper userProjectRelMapper;
    private final SysDeptMapper deptMapper;

    @Override
    public IPage<SysProject> listProjects(Integer page, Integer size, String keyword,
                                           String projectType, String buName, Long userId) {
        Page<SysProject> p = new Page<>(page, size);
        LambdaQueryWrapper<SysProject> q = new LambdaQueryWrapper<>();

        List<String> roles = getRoleCodes(userId);
        if (!roles.contains("SYSTEM_ADMIN") && !roles.contains("COMPANY_ADMIN")) {
            if (roles.contains("BU_ADMIN")) {
                List<SysProject> userProjects = userProjectRelMapper.selectList(
                    new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId)
                ).stream().map(r -> projectMapper.selectById(r.getProjectId()))
                  .filter(Objects::nonNull).collect(Collectors.toList());
                List<String> buNames = userProjects.stream()
                    .map(SysProject::getBuName).filter(Objects::nonNull).distinct()
                    .collect(Collectors.toList());
                // 空列表时用不可能匹配的值，防止误匹配 bu_name 为 NULL 或空串的项目
                q.in(SysProject::getBuName, buNames.isEmpty() ? List.of("__NONEXISTENT__") : buNames);
            } else {
                List<Long> projectIds = userProjectRelMapper.selectList(
                    new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId)
                ).stream().map(UserProjectRel::getProjectId).collect(Collectors.toList());
                q.in(SysProject::getId, projectIds.isEmpty() ? Arrays.asList(-1L) : projectIds);
            }
        }

        q.like(StringUtils.hasText(keyword), SysProject::getProjectName, keyword)
         .eq(StringUtils.hasText(projectType), SysProject::getProjectType, projectType)
         .eq(StringUtils.hasText(buName), SysProject::getBuName, buName)
         .orderByDesc(SysProject::getCreateTime);
        return projectMapper.selectPage(p, q);
    }

    @Override
    public SysProject create(ProjectRequest request, Long creatorId) {
        if (projectMapper.selectCount(
            new LambdaQueryWrapper<SysProject>().eq(SysProject::getProjectCode, request.getProjectCode())) > 0) {
            throw new BusinessException("项目编号已存在");
        }
        SysProject project = new SysProject();
        project.setProjectCode(request.getProjectCode());
        project.setProjectName(request.getProjectName());
        project.setUnitProjectName(request.getUnitProjectName());
        project.setParentId(request.getParentId());
        project.setBuName(request.getBuName());
        project.setProjectType(request.getProjectType());
        project.setConstructionUnit(request.getConstructionUnit());
        project.setDesignUnit(request.getDesignUnit());
        project.setSupervisionUnit(request.getSupervisionUnit());
        project.setContractNo(request.getContractNo());
        project.setContractAmount(request.getContractAmount());
        project.setProjectManager(request.getProjectManager());
        project.setProjectLocation(request.getProjectLocation());
        project.setProjectDescription(request.getProjectDescription());
        project.setStatus("PENDING");
        project.setCreateBy(creatorId);
        projectMapper.insert(project);
        return project;
    }

    @Override
    public SysProject update(Long id, ProjectRequest request, Long userId) {
        // 仅系统管理员可修改项目信息
        if (!isSystemAdmin(userId)) {
            throw new BusinessException(403, "仅系统管理员可修改项目信息");
        }
        SysProject project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException("项目不存在");
        if (StringUtils.hasText(request.getProjectName())) project.setProjectName(request.getProjectName());
        if (StringUtils.hasText(request.getUnitProjectName())) project.setUnitProjectName(request.getUnitProjectName());
        if (StringUtils.hasText(request.getConstructionUnit())) project.setConstructionUnit(request.getConstructionUnit());
        if (StringUtils.hasText(request.getDesignUnit())) project.setDesignUnit(request.getDesignUnit());
        if (StringUtils.hasText(request.getSupervisionUnit())) project.setSupervisionUnit(request.getSupervisionUnit());
        if (StringUtils.hasText(request.getContractNo())) project.setContractNo(request.getContractNo());
        if (request.getContractAmount() != null) project.setContractAmount(request.getContractAmount());
        if (StringUtils.hasText(request.getProjectManager())) project.setProjectManager(request.getProjectManager());
        if (StringUtils.hasText(request.getProjectLocation())) project.setProjectLocation(request.getProjectLocation());
        if (StringUtils.hasText(request.getProjectDescription())) project.setProjectDescription(request.getProjectDescription());
        projectMapper.updateById(project);
        return project;
    }

    @Override
    public void updateStatus(Long id, String status) {
        SysProject project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException("项目不存在");
        project.setStatus(status);
        projectMapper.updateById(project);
    }

    @Override
    public void delete(Long id) {
        SysProject project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException("项目不存在");
        projectMapper.deleteById(id);
    }

    @Override
    public SysProject getById(Long id) {
        SysProject project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException("项目不存在");
        return project;
    }

    @Override
    public List<SysProject> getByBuName(String buName) {
        return projectMapper.selectList(new LambdaQueryWrapper<SysProject>().eq(SysProject::getBuName, buName));
    }

    @Override
    public List<TreeNode> getTree(Long userId) {
        List<String> roles = getRoleCodes(userId);

        // 1. 查所有启用的部门
        List<SysDept> depts = deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, 1)
        );

        // 2. 找公司节点
        SysDept company = depts.stream()
            .filter(d -> "COMPANY".equals(d.getDeptType()))
            .findFirst().orElse(null);
        if (company == null) return List.of();

        // 3. 找出所有 BU 节点
        List<SysDept> buDepts = depts.stream()
            .filter(d -> "BU".equals(d.getDeptType()))
            .collect(Collectors.toList());

        // 4. 查所有未删除项目（权限过滤）
        LambdaQueryWrapper<SysProject> projQ = new LambdaQueryWrapper<>();
        if (!roles.contains("SYSTEM_ADMIN") && !roles.contains("COMPANY_ADMIN")) {
            if (roles.contains("BU_ADMIN")) {
                List<SysProject> userProjects = userProjectRelMapper.selectList(
                    new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId)
                ).stream().map(r -> projectMapper.selectById(r.getProjectId()))
                  .filter(Objects::nonNull).collect(Collectors.toList());
                List<String> buNames = userProjects.stream()
                    .map(SysProject::getBuName).filter(Objects::nonNull).distinct()
                    .collect(Collectors.toList());
                // 空列表时用不可能匹配的值，防止误匹配 bu_name 为 NULL 或空串的项目
                projQ.in(SysProject::getBuName, buNames.isEmpty() ? List.of("__NONEXISTENT__") : buNames);
            } else {
                List<Long> projectIds = userProjectRelMapper.selectList(
                    new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId)
                ).stream().map(UserProjectRel::getProjectId).collect(Collectors.toList());
                projQ.in(SysProject::getId, projectIds.isEmpty() ? Arrays.asList(-1L) : projectIds);
            }
        }
        List<SysProject> projects = projectMapper.selectList(projQ);

        // 5. 组装树
        TreeNode root = new TreeNode();
        root.setId("dept_" + company.getId());
        root.setLabel(company.getDeptName());
        root.setType("COMPANY");

        // 公司直属项目
        projects.stream()
            .filter(p -> "COMPANY_DIRECT".equals(p.getProjectType()))
            .forEach(p -> {
                TreeNode projNode = new TreeNode();
                projNode.setId("proj_" + p.getId());
                projNode.setLabel(p.getProjectName());
                projNode.setType("PROJECT");
                projNode.setProjectId(p.getId());
                projNode.setProjectCode(p.getProjectCode());
                projNode.setStatus(p.getStatus());
                root.getChildren().add(projNode);
            });

        // 各事业部及其项目
        for (SysDept bu : buDepts) {
            TreeNode buNode = new TreeNode();
            buNode.setId("dept_" + bu.getId());
            buNode.setLabel(bu.getDeptName());
            buNode.setType("BU");

            List<TreeNode> buProjects = projects.stream()
                .filter(p -> "BU_SUB".equals(p.getProjectType()) && bu.getBuName().equals(p.getBuName()))
                .map(p -> {
                    TreeNode projNode = new TreeNode();
                    projNode.setId("proj_" + p.getId());
                    projNode.setLabel(p.getProjectName());
                    projNode.setType("PROJECT");
                    projNode.setProjectId(p.getId());
                    projNode.setProjectCode(p.getProjectCode());
                    projNode.setStatus(p.getStatus());
                    return projNode;
                })
                .collect(Collectors.toList());
            buNode.setChildren(buProjects);

            root.getChildren().add(buNode);
        }

        return List.of(root);
    }

    private List<String> getRoleCodes(Long userId) {
        return userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId)
        ).stream().map(r -> roleMapper.selectById(r.getRoleId()).getRoleCode()).collect(Collectors.toList());
    }

    private boolean isSystemAdmin(Long userId) {
        return getRoleCodes(userId).contains("SYSTEM_ADMIN");
    }
}

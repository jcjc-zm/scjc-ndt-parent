package com.scjc.ndt.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.common.R;
import com.scjc.ndt.dto.ProjectRequest;
import com.scjc.ndt.dto.TreeNode;
import com.scjc.ndt.entity.SysProject;
import com.scjc.ndt.service.ProjectService;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/list")
    public R<IPage<SysProject>> list(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "20") Integer size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String projectType,
                                      @RequestParam(required = false) String buName,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(projectService.listProjects(page, size, keyword, projectType, buName, userId));
    }

    @GetMapping("/tree")
    public R<List<TreeNode>> tree(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(projectService.getTree(userId));
    }

    @GetMapping("/{id}")
    public R<SysProject> getById(@PathVariable Long id) {
        return R.ok(projectService.getById(id));
    }

    @PostMapping
    public R<SysProject> create(@Valid @RequestBody ProjectRequest req, HttpServletRequest request) {
        Long creatorId = (Long) request.getAttribute("userId");
        return R.ok(projectService.create(req, creatorId));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectRequest req) {
        projectService.update(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        projectService.updateStatus(id, status);
        return R.ok();
    }
}

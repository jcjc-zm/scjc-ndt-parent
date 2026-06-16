package com.scjc.ndt.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.common.R;
import com.scjc.ndt.dto.UserInfo;
import com.scjc.ndt.dto.UserRequest;
import com.scjc.ndt.entity.SysUser;
import com.scjc.ndt.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/list")
    public R<IPage<UserInfo>> list(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "20") Integer size,
                                    @RequestParam(required = false) String keyword) {
        return R.ok(userService.listUsers(page, size, keyword));
    }

    @GetMapping("/{id}")
    public R<UserInfo> getById(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PostMapping
    public R<SysUser> create(@Valid @RequestBody UserRequest req, HttpServletRequest request) {
        Long creatorId = (Long) request.getAttribute("userId");
        return R.ok(userService.createUser(req, creatorId));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        userService.updateUser(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return R.ok();
    }

    @PostMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return R.ok();
    }

    @PostMapping("/{id}/projects")
    public R<Void> assignProjects(@PathVariable Long id, @RequestBody List<Long> projectIds) {
        userService.assignProjects(id, projectIds);
        return R.ok();
    }
}

package com.scjc.ndt.controller;

import com.scjc.ndt.common.R;
import com.scjc.ndt.entity.SysRole;
import com.scjc.ndt.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/list")
    public R<List<SysRole>> list() {
        return R.ok(roleService.getAll());
    }
}

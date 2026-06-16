package com.scjc.ndt.controller;

import com.scjc.ndt.common.R;
import com.scjc.ndt.entity.SysDept;
import com.scjc.ndt.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    public R<List<SysDept>> tree() {
        return R.ok(deptService.getTree());
    }

    @GetMapping("/list")
    public R<List<SysDept>> list() {
        return R.ok(deptService.getBuList());
    }

    @PostMapping
    public R<SysDept> create(@RequestBody SysDept dept) {
        return R.ok(deptService.create(dept));
    }

    @PutMapping("/{id}")
    public R<SysDept> update(@PathVariable Long id, @RequestBody SysDept dept) {
        return R.ok(deptService.update(id, dept));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }
}

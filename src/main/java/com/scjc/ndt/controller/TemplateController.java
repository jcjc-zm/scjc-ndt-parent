package com.scjc.ndt.controller;

import com.scjc.ndt.common.R;
import com.scjc.ndt.entity.ReportTemplate;
import com.scjc.ndt.service.TemplateService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping("/list")
    public R<List<ReportTemplate>> list(@RequestParam(required = false) String methodType,
                                         @RequestParam(required = false) Long projectId) {
        return R.ok(templateService.listTemplates(methodType, projectId));
    }

    @GetMapping("/{id}")
    public R<ReportTemplate> getById(@PathVariable Long id) {
        return R.ok(templateService.getById(id));
    }

    @PostMapping
    public R<ReportTemplate> create(@RequestBody ReportTemplate template, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(templateService.create(template, userId));
    }

    @PutMapping("/{id}")
    public R<ReportTemplate> update(@PathVariable Long id, @RequestBody ReportTemplate template) {
        return R.ok(templateService.update(id, template));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return R.ok();
    }
}

package com.scjc.ndt.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.common.R;
import com.scjc.ndt.dto.InspectionRequest;
import com.scjc.ndt.entity.InspectionRecord;
import com.scjc.ndt.service.InspectionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @GetMapping("/list")
    public R<IPage<InspectionRecord>> list(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "20") Integer size,
                                            @RequestParam(required = false) Long projectId,
                                            @RequestParam(required = false) String method,
                                            @RequestParam(required = false) String level,
                                            @RequestParam(required = false) String conclusion,
                                            @RequestParam(required = false) String keyword,
                                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(inspectionService.list(page, size, projectId, method, level, conclusion, keyword, userId));
    }

    @GetMapping("/{id}")
    public R<InspectionRecord> getById(@PathVariable Long id) {
        return R.ok(inspectionService.getById(id));
    }

    @PostMapping
    public R<InspectionRecord> create(
            @Validated(InspectionRequest.OnCreate.class) @RequestBody InspectionRequest req,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(inspectionService.create(req, userId));
    }

    @PutMapping("/{id}")
    public R<InspectionRecord> update(@PathVariable Long id, @RequestBody InspectionRequest req) {
        return R.ok(inspectionService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        inspectionService.delete(id);
        return R.ok();
    }

    @PostMapping("/batch-import")
    public R<Integer> batchImport(@RequestParam Long projectId,
                                   @RequestBody List<InspectionRequest> records,
                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(inspectionService.batchImport(records, projectId, userId));
    }

    @GetMapping("/export")
    public R<List<InspectionRecord>> export(@RequestParam(required = false) Long projectId,
                                             @RequestParam(required = false) String method,
                                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(inspectionService.export(projectId, method, userId));
    }
}

package com.scjc.ndt.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.common.R;
import com.scjc.ndt.dto.ReportRequest;
import com.scjc.ndt.entity.ReportRecord;
import com.scjc.ndt.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/list")
    public R<IPage<ReportRecord>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "20") Integer size,
                                        @RequestParam(required = false) Long projectId,
                                        @RequestParam(required = false) String status,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(reportService.listReports(page, size, projectId, status, userId));
    }

    @GetMapping("/{id}")
    public R<ReportRecord> getById(@PathVariable Long id) {
        return R.ok(reportService.getById(id));
    }

    @PostMapping
    public R<ReportRecord> generate(@RequestBody ReportRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(reportService.generateReport(req, userId));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return R.ok();
    }
}

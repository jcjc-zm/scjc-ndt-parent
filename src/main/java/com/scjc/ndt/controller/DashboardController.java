package com.scjc.ndt.controller;

import com.scjc.ndt.common.R;
import com.scjc.ndt.dto.DashboardStats;
import com.scjc.ndt.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public R<DashboardStats> overview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(dashboardService.getOverview(userId));
    }

    @GetMapping("/project-distribution")
    public R<DashboardStats> projectDistribution(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(dashboardService.getOverview(userId));
    }

    @GetMapping("/weekly-workload")
    public R<DashboardStats> weeklyWorkload(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(dashboardService.getOverview(userId));
    }

    @GetMapping("/inspection-stats")
    public R<DashboardStats> inspectionStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(dashboardService.getOverview(userId));
    }

    @GetMapping("/my-todos")
    public R<DashboardStats> myTodos(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(dashboardService.getOverview(userId));
    }
}

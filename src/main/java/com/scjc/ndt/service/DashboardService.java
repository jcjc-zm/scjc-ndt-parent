package com.scjc.ndt.service;

import com.scjc.ndt.dto.DashboardStats;

public interface DashboardService {
    DashboardStats getOverview(Long userId);
}

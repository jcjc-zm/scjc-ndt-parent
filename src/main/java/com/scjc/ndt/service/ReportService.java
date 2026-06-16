package com.scjc.ndt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.dto.ReportRequest;
import com.scjc.ndt.entity.ReportRecord;

public interface ReportService {
    IPage<ReportRecord> listReports(Integer page, Integer size, Long projectId, String status, Long userId);
    ReportRecord generateReport(ReportRequest request, Long userId);
    ReportRecord getById(Long id);
    void delete(Long id);
}

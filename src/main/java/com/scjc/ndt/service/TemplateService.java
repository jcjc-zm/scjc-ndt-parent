package com.scjc.ndt.service;

import com.scjc.ndt.entity.ReportTemplate;
import java.util.List;

public interface TemplateService {
    List<ReportTemplate> listTemplates(String methodType, Long projectId);
    ReportTemplate getById(Long id);
    ReportTemplate create(ReportTemplate template, Long userId);
    ReportTemplate update(Long id, ReportTemplate template);
    void delete(Long id);
}

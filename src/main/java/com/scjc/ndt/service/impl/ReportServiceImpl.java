package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.dto.ReportRequest;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;
import com.scjc.ndt.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRecordMapper reportMapper;
    private final InspectionRecordMapper inspectionMapper;
    private final ReportTemplateMapper templateMapper;
    private final SignatureRecordMapper signatureMapper;
    private final UserProjectRelMapper userProjectRelMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static int reportSeq = 0;

    @Override
    public IPage<ReportRecord> listReports(Integer page, Integer size, Long projectId, String status, Long userId) {
        Page<ReportRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<ReportRecord> q = new LambdaQueryWrapper<>();
        q.eq(StringUtils.hasText(status), ReportRecord::getStatus, status)
         .orderByDesc(ReportRecord::getCreateTime);
        if (projectId != null) {
            q.apply("inspection_id IN (SELECT id FROM inspection_record WHERE project_id = {0})", projectId);
        }
        return reportMapper.selectPage(p, q);
    }

    @Override
    @Transactional
    @SneakyThrows
    public ReportRecord generateReport(ReportRequest request, Long userId) {
        InspectionRecord inspection = inspectionMapper.selectById(request.getInspectionId());
        if (inspection == null) throw new BusinessException("检测记录不存在");

        ReportTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null) throw new BusinessException("报告模板不存在");

        ReportRecord report = new ReportRecord();
        report.setInspectionId(request.getInspectionId());
        report.setTemplateId(request.getTemplateId());
        report.setReportNo(generateReportNo());
        report.setStatus("DRAFT");
        report.setCreateBy(userId);

        if (request.getImageSelections() != null) {
            report.setImageSelections(objectMapper.writeValueAsString(request.getImageSelections()));
        }

        reportMapper.insert(report);

        // Create pending signature records
        createSignatures(report.getId());

        return report;
    }

    private void createSignatures(Long reportId) {
        SignatureRecord sign1 = new SignatureRecord();
        sign1.setReportId(reportId);
        sign1.setSignatoryRole("TECHNICAL_LEADER");
        sign1.setSignOrder(1);
        sign1.setSignStatus("PENDING");
        signatureMapper.insert(sign1);

        SignatureRecord sign2 = new SignatureRecord();
        sign2.setReportId(reportId);
        sign2.setSignatoryRole("PROJECT_MANAGER");
        sign2.setSignOrder(2);
        sign2.setSignStatus("PENDING");
        signatureMapper.insert(sign2);
    }

    private synchronized String generateReportNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        reportSeq++;
        return "BG-" + date + "-" + String.format("%04d", reportSeq);
    }

    @Override
    public ReportRecord getById(Long id) {
        ReportRecord r = reportMapper.selectById(id);
        if (r == null) throw new BusinessException("报告不存在");
        return r;
    }

    @Override
    public void delete(Long id) {
        reportMapper.deleteById(id);
    }
}

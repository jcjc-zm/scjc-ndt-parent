package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.dto.SignRequest;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;
import com.scjc.ndt.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignatureServiceImpl implements SignatureService {

    private final SignatureRecordMapper signatureMapper;
    private final ReportRecordMapper reportMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public List<SignatureRecord> getPending(Long userId) {
        List<UserRoleRel> rels = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId)
        );
        List<String> roles;
        if (rels.isEmpty()) {
            roles = List.of();
        } else {
            roles = roleMapper.selectBatchIds(
                rels.stream().map(UserRoleRel::getRoleId).toList()
            ).stream().map(SysRole::getRoleCode).toList();
        }

        LambdaQueryWrapper<SignatureRecord> q = new LambdaQueryWrapper<>();
        q.eq(SignatureRecord::getSignStatus, "PENDING");

        if (!roles.contains("SYSTEM_ADMIN") && !roles.contains("COMPANY_ADMIN")) {
            if (roles.contains("TECHNICAL_LEADER")) q.eq(SignatureRecord::getSignatoryRole, "TECHNICAL_LEADER");
            else if (roles.contains("PROJECT_MANAGER")) q.eq(SignatureRecord::getSignatoryRole, "PROJECT_MANAGER");
        }
        q.orderByAsc(SignatureRecord::getSignOrder);
        return signatureMapper.selectList(q);
    }

    @Override
    @Transactional
    public void sign(Long reportId, SignRequest request, Long userId) {
        // Find the pending signature for this user's role
        List<UserRoleRel> rels = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId)
        );
        List<String> roles;
        if (rels.isEmpty()) {
            roles = List.of();
        } else {
            roles = roleMapper.selectBatchIds(
                rels.stream().map(UserRoleRel::getRoleId).toList()
            ).stream().map(SysRole::getRoleCode).toList();
        }

        SignatureRecord sig = signatureMapper.selectOne(
            new LambdaQueryWrapper<SignatureRecord>()
                .eq(SignatureRecord::getReportId, reportId)
                .eq(SignatureRecord::getSignStatus, "PENDING")
                .in(roles.contains("TECHNICAL_LEADER") || roles.contains("PROJECT_MANAGER"),
                    SignatureRecord::getSignatoryRole,
                    roles.contains("TECHNICAL_LEADER") ? "TECHNICAL_LEADER" : "PROJECT_MANAGER")
                .orderByAsc(SignatureRecord::getSignOrder)
                .last("LIMIT 1")
        );

        if (sig == null) throw new BusinessException("无待签字记录或您无权签字");

        sig.setSignatoryId(userId);
        sig.setSignStatus("SIGNED");
        sig.setSignTime(LocalDateTime.now());
        sig.setComment(request.getComment());
        sig.setSignatureImageUrl(request.getSignatureImageUrl());
        signatureMapper.updateById(sig);

        // Check if all signatures done
        long pending = signatureMapper.selectCount(
            new LambdaQueryWrapper<SignatureRecord>()
                .eq(SignatureRecord::getReportId, reportId)
                .eq(SignatureRecord::getSignStatus, "PENDING")
        );
        if (pending == 0) {
            ReportRecord report = reportMapper.selectById(reportId);
            if (report != null) {
                report.setStatus("SIGNED");
                reportMapper.updateById(report);
            }
        }
    }

    @Override
    public List<SignatureRecord> getHistory(Long reportId) {
        return signatureMapper.selectList(
            new LambdaQueryWrapper<SignatureRecord>()
                .eq(SignatureRecord::getReportId, reportId)
                .orderByAsc(SignatureRecord::getSignOrder)
        );
    }
}

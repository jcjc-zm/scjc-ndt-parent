package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scjc.ndt.dto.DashboardStats;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;
import com.scjc.ndt.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final InspectionRecordMapper inspectionMapper;
    private final SysProjectMapper projectMapper;
    private final SignatureRecordMapper signatureMapper;
    private final SysDeptMapper deptMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper roleMapper;
    private final UserProjectRelMapper userProjectRelMapper;

    @Override
    public DashboardStats getOverview(Long userId) {
        DashboardStats stats = new DashboardStats();

        List<Long> projectIds = getProjectIds(userId);
        LambdaQueryWrapper<InspectionRecord> iq = new LambdaQueryWrapper<>();
        if (projectIds != null) iq.in(InspectionRecord::getProjectId, projectIds);

        // Total inspections
        stats.setTotalInspections(inspectionMapper.selectCount(iq.clone()));

        // Qualified rate
        long total = inspectionMapper.selectCount(iq.clone());
        long qualified = inspectionMapper.selectCount(iq.clone().eq(InspectionRecord::getInspectionConclusion, "合格"));
        stats.setQualifiedRate(total > 0 ? Math.round(qualified * 1000.0 / total) / 10.0 : 100.0);

        // Pending
        stats.setPendingSignCount(signatureMapper.selectCount(
            new LambdaQueryWrapper<SignatureRecord>().eq(SignatureRecord::getSignStatus, "PENDING")));
        stats.setPendingCount(stats.getPendingSignCount());

        // Active projects
        stats.setActiveProjects(projectMapper.selectCount(
            new LambdaQueryWrapper<SysProject>().eq(SysProject::getStatus, "IN_PROGRESS")));

        // Weekly new
        stats.setWeeklyNew(inspectionMapper.selectCount(
            iq.clone().ge(InspectionRecord::getCreateTime,
                java.time.LocalDateTime.now().minusDays(7))));

        // Method distribution
        List<DashboardStats.MethodDistribution> methods = new ArrayList<>();
        String[] methodNames = {"RT", "UT", "PT", "MT", "AUT", "PA", "TOFD", "DR"};
        for (String m : methodNames) {
            long cnt = inspectionMapper.selectCount(iq.clone().eq(InspectionRecord::getInspectionMethod, m));
            if (cnt > 0) {
                DashboardStats.MethodDistribution md = new DashboardStats.MethodDistribution();
                md.setMethod(m);
                md.setCount(cnt);
                md.setPercentage(total > 0 ? Math.round(cnt * 1000.0 / total) / 10.0 : 0.0);
                methods.add(md);
            }
        }
        stats.setMethodDistribution(methods);

        // Weekly workload per BU
        List<SysDept> buList = deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeptType, "BU"));
        List<DashboardStats.WeeklyWorkload> workloads = new ArrayList<>();
        for (SysDept bu : buList) {
            DashboardStats.WeeklyWorkload wl = new DashboardStats.WeeklyWorkload();
            wl.setDeptName(bu.getDeptName());
            wl.setRt(countMethodForDept(bu.getBuName(), "RT"));
            wl.setUt(countMethodForDept(bu.getBuName(), "UT"));
            wl.setPt(countMethodForDept(bu.getBuName(), "PT"));
            wl.setMt(countMethodForDept(bu.getBuName(), "MT"));
            wl.setAut(countMethodForDept(bu.getBuName(), "AUT"));
            wl.setPa(countMethodForDept(bu.getBuName(), "PA"));
            wl.setTofd(countMethodForDept(bu.getBuName(), "TOFD"));
            wl.setDr(countMethodForDept(bu.getBuName(), "DR"));
            wl.setTotal(wl.getRt() + wl.getUt() + wl.getPt() + wl.getMt() + wl.getAut() + wl.getPa() + wl.getTofd() + wl.getDr());
            workloads.add(wl);
        }
        stats.setWeeklyWorkload(workloads);

        return stats;
    }

    private int countMethodForDept(String buName, String method) {
        Long count = inspectionMapper.selectCount(
            new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getBuDept, buName)
                .eq(InspectionRecord::getInspectionMethod, method)
                .ge(InspectionRecord::getCreateTime, java.time.LocalDateTime.now().minusDays(7))
        );
        return count != null ? count.intValue() : 0;
    }

    private List<Long> getProjectIds(Long userId) {
        List<String> roles = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId)
        ).stream().map(r -> roleMapper.selectById(r.getRoleId()).getRoleCode()).collect(Collectors.toList());
        if (roles.contains("SYSTEM_ADMIN") || roles.contains("COMPANY_ADMIN")) return null;
        return userProjectRelMapper.selectList(
            new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId)
        ).stream().map(UserProjectRel::getProjectId).collect(Collectors.toList());
    }
}

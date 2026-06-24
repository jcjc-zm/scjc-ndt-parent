package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scjc.ndt.dto.DashboardStats;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;
import com.scjc.ndt.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        if (projectIds != null && !projectIds.isEmpty()) iq.in(InspectionRecord::getProjectId, projectIds);

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

        // 30-day trend — daily inspection counts
        List<InspectionRecord> recentRecords = inspectionMapper.selectList(
            iq.clone().ge(InspectionRecord::getCreateTime, LocalDateTime.now().minusDays(30))
                .select(InspectionRecord::getCreateTime));
        Map<LocalDate, Long> dailyCounts = recentRecords.stream()
            .collect(Collectors.groupingBy(r -> r.getCreateTime().toLocalDate(), Collectors.counting()));
        List<Integer> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            trend.add(dailyCounts.getOrDefault(today.minusDays(i), 0L).intValue());
        }
        stats.setTrend(trend);

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

        // Weekly workload per BU — 通过项目归属关系匹配，不依赖 bu_dept 字符串
        List<SysDept> buList = deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeptType, "BU"));
        List<DashboardStats.WeeklyWorkload> workloads = new ArrayList<>();
        for (SysDept bu : buList) {
            // 找到该 BU 下的所有项目 ID
            List<Long> buProjectIds = projectMapper.selectList(
                new LambdaQueryWrapper<SysProject>().eq(SysProject::getBuName, bu.getBuName())
            ).stream().map(SysProject::getId).collect(Collectors.toList());

            DashboardStats.WeeklyWorkload wl = new DashboardStats.WeeklyWorkload();
            wl.setDeptName(bu.getDeptName());
            int rt = 0, ut = 0, pt = 0, mt = 0, aut = 0, pa = 0, tofd = 0, dr = 0;
            if (!buProjectIds.isEmpty()) {
                rt = countMethodForProjects(buProjectIds, "RT");
                ut = countMethodForProjects(buProjectIds, "UT");
                pt = countMethodForProjects(buProjectIds, "PT");
                mt = countMethodForProjects(buProjectIds, "MT");
                aut = countMethodForProjects(buProjectIds, "AUT");
                pa = countMethodForProjects(buProjectIds, "PA");
                tofd = countMethodForProjects(buProjectIds, "TOFD");
                dr = countMethodForProjects(buProjectIds, "DR");
            }
            wl.setRt(rt); wl.setUt(ut); wl.setPt(pt); wl.setMt(mt);
            wl.setAut(aut); wl.setPa(pa); wl.setTofd(tofd); wl.setDr(dr);
            wl.setTotal(rt + ut + pt + mt + aut + pa + tofd + dr);
            workloads.add(wl);
        }

        // Company-level projects — 与事业部同级展示
        List<SysProject> companyProjects = projectMapper.selectList(
            new LambdaQueryWrapper<SysProject>().eq(SysProject::getProjectType, "COMPANY_DIRECT"));
        for (SysProject proj : companyProjects) {
            DashboardStats.WeeklyWorkload wl = new DashboardStats.WeeklyWorkload();
            wl.setDeptName(proj.getProjectName());
            List<Long> singleProject = List.of(proj.getId());
            wl.setRt(countMethodForProjects(singleProject, "RT"));
            wl.setUt(countMethodForProjects(singleProject, "UT"));
            wl.setPt(countMethodForProjects(singleProject, "PT"));
            wl.setMt(countMethodForProjects(singleProject, "MT"));
            wl.setAut(countMethodForProjects(singleProject, "AUT"));
            wl.setPa(countMethodForProjects(singleProject, "PA"));
            wl.setTofd(countMethodForProjects(singleProject, "TOFD"));
            wl.setDr(countMethodForProjects(singleProject, "DR"));
            wl.setTotal(wl.getRt() + wl.getUt() + wl.getPt() + wl.getMt() + wl.getAut() + wl.getPa() + wl.getTofd() + wl.getDr());
            workloads.add(wl);
        }
        stats.setWeeklyWorkload(workloads);

        // Project locations for map — 有工程地址的项目
        List<SysProject> allProjects = projectMapper.selectList(
            new LambdaQueryWrapper<SysProject>().isNotNull(SysProject::getProjectLocation).ne(SysProject::getProjectLocation, ""));
        stats.setProjectLocations(allProjects.stream().map(p -> {
            DashboardStats.ProjectLocation pl = new DashboardStats.ProjectLocation();
            pl.setProjectId(p.getId());
            pl.setProjectName(p.getProjectName());
            pl.setProjectCode(p.getProjectCode());
            pl.setLocation(p.getProjectLocation());
            return pl;
        }).collect(Collectors.toList()));

        return stats;
    }

    private int countMethodForProjects(List<Long> projectIds, String method) {
        if (projectIds == null || projectIds.isEmpty()) return 0;
        Long count = inspectionMapper.selectCount(
            new LambdaQueryWrapper<InspectionRecord>()
                .in(InspectionRecord::getProjectId, projectIds)
                .eq(InspectionRecord::getInspectionMethod, method)
                .ge(InspectionRecord::getCreateTime, java.time.LocalDateTime.now().minusDays(7))
        );
        return count != null ? count.intValue() : 0;
    }

    private List<Long> getProjectIds(Long userId) {
        List<UserRoleRel> rels = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId)
        );
        if (rels.isEmpty()) return List.of();
        List<String> roles = rels.stream()
            .map(r -> roleMapper.selectById(r.getRoleId()))
            .filter(r -> r != null)
            .map(SysRole::getRoleCode)
            .collect(Collectors.toList());
        if (roles.contains("SYSTEM_ADMIN") || roles.contains("COMPANY_ADMIN")) return null;
        return userProjectRelMapper.selectList(
            new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId)
        ).stream().map(UserProjectRel::getProjectId).collect(Collectors.toList());
    }
}

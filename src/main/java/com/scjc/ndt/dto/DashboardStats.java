package com.scjc.ndt.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardStats {
    private Long totalInspections;
    private Double qualifiedRate;
    private Long pendingCount;
    private Long pendingSignCount;
    private Long pendingEntryCount;
    private Long weeklyNew;
    private Long activeProjects;
    private List<MethodDistribution> methodDistribution;
    private List<WeeklyWorkload> weeklyWorkload;

    @Data
    public static class MethodDistribution {
        private String method;
        private Long count;
        private Double percentage;
    }

    @Data
    public static class WeeklyWorkload {
        private String deptName;
        private Integer rt;
        private Integer ut;
        private Integer pt;
        private Integer mt;
        private Integer aut;
        private Integer pa;
        private Integer tofd;
        private Integer dr;
        private Integer total;
        private Double qualifiedRate;
    }
}

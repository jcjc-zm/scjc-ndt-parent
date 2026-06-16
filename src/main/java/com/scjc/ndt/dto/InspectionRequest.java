package com.scjc.ndt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InspectionRequest {
    private Long projectId;
    private String constructionUnit;
    @NotBlank(message = "焊口编号不能为空")
    private String weldNo;
    private String instructionNo;
    private LocalDate instructionDate;
    private String inspectionMethod;
    private String projectName;
    private String unitProjectName;
    private String buDept;
    private String specification;
    private String material;
    private String grooveType;
    private String position;
    private String weldingMethod;
    private String ratio;
    private String inspectionStandard;
    private String qualifiedLevel;
    private String inspectionItem;
    private String welderCode;
    private String weldingDept;
    private BigDecimal inspectionLength;
    private String processCardNo;
    private String samplingInstructionNo;
    private LocalDate inspectionDate;
    private String resultLevel;
    private String inspectionConclusion;
    private String unqualifiedHandling;
    private String reportDefectPosition;
    private String reportDefectNature;
    private BigDecimal reportDefectLength;
    private String unqualifiedDefectType;
    private String remark;
    private String inspectorName;
    private String boxNo;
    private BigDecimal filmLength;
    private Integer filmCount;
    private String imageUrl;
    private Integer levelI;
    private Integer levelIi;
    private Integer levelIii;
    private Integer levelIv;
    private List<DefectPosition> defectPositions;

    @Data
    public static class DefectPosition {
        private Integer pos;
        private String defect;
        private BigDecimal length;
        private String level;
        private String other;
    }
}

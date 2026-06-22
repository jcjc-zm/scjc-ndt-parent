package com.scjc.ndt.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.scjc.ndt.common.FlexibleLocalDateDeserializer;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InspectionRequest {
    /** 创建时的校验分组 — 更新时不需要焊口编号 */
    public interface OnCreate {}

    private Long projectId;
    private String constructionUnit;
    @NotBlank(message = "焊口编号不能为空", groups = OnCreate.class)
    private String weldNo;
    private String instructionNo;
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
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
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
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
    private String filmLength;
    private Integer filmCount;
    private String imageUrl;
    private Integer levelI;
    private Integer levelIi;
    private Integer levelIii;
    private Integer levelIv;

    // ── RT射线检测专用字段 ──
    private String filmModel;
    private String filmSpec;
    private String leadScreen;
    private String iqiModel;
    private String iqiPosition;
    private String requiredIqi;
    private String sourceType;
    private String equipmentModel;
    private String tubeVoltage;
    private String tubeCurrent;
    private String focalDistance;
    private String exposureTime;
    private String techniqueType;
    private String filmProcessing;
    private String developmentTime;
    private String developmentTemperature;
    private String filmDensityRange;
    private String inspectionTechLevel;
    private String heatTreatmentStatus;
    private String inspectionTiming;
    private String pressureEquipmentCategory;
    private String plateThickness;
    private String iqiValue;
    private String transilluminationLength;
    private String defectDetails;
    private Integer inspectionCount;
    private Integer repairCount;
    private Integer reinspectionCount;
    private Integer extendedInspectionCount;
    private String firstPassYield;
    private String finalYield;
    private String projectCode;
    private String reviewerName;
    private String technicalLeadName;

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

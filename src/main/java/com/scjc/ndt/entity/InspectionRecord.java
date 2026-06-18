package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("inspection_record")
public class InspectionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String constructionUnit;
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
    private String inspectionReportUrl;
    private String originalRecordUrl;
    private String defectPositions;
    private Integer levelI;
    private Integer levelIi;
    private Integer levelIii;
    private Integer levelIv;

    // ── RT射线检测专用字段 (胶片射线报告A) ──
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

    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

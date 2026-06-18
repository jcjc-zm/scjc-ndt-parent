package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("process_card")
public class ProcessCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String processCardNo;
    private String specification;
    private String techniqueType;
    private String iqiValue;
    private String iqiModel;
    private String equipmentModel;
    private String equipmentNo;
    private String focalSpotSize;
    private String iqiPosition;
    private String focalDistance;
    private String penetrationThickness;
    private String tubeVoltage;
    private String exposureTime;
    private String transilluminationLength;
    private Integer exposureCount;
    private String filmSpec;
    private String densityRange;
    private String tubeCurrent;
    private String filmType;
    private String workFilmDistance;
    private String techLevel;
    private String heatTreatmentStatus;
    private String leadScreen;
    private String sourceType;
    private String exposureParam;
    private String transilluminationParam;
    private String filmProcessing;
    private String developmentTime;
    private String developmentTemperature;
    private String sourceActivity;
    private Integer filmCount;
    private String remark;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

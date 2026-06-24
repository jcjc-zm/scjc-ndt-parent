package com.scjc.ndt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProjectRequest {
    @NotBlank(message = "项目编号不能为空")
    private String projectCode;
    @NotBlank(message = "项目名称不能为空")
    private String projectName;
    private String unitProjectName;
    private Long parentId;
    private String buName;
    private String projectType;
    private String constructionUnit;
    private String designUnit;
    private String supervisionUnit;
    private String contractNo;
    private BigDecimal contractAmount;
    private String projectManager;
    private String projectLocation;
    private String projectDescription;
}

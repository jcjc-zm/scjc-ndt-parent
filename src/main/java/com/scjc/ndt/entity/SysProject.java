package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_project")
public class SysProject {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String projectCode;
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
    private String status;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

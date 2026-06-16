package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report_template")
public class ReportTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String templateName;
    private String templateType;
    private String methodType;
    private String description;
    private String layoutConfig;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

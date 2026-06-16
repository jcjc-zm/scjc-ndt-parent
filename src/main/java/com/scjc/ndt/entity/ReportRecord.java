package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report_record")
public class ReportRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inspectionId;
    private Long templateId;
    private String reportNo;
    private String reportContent;
    private String imageSelections;
    private String status;
    private String pdfUrl;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

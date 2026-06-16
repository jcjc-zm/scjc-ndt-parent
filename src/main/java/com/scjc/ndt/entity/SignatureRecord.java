package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("signature_record")
public class SignatureRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private Long signatoryId;
    private String signatoryRole;
    private Integer signOrder;
    private String signStatus;
    private LocalDateTime signTime;
    private String comment;
    private String signatureImageUrl;
}

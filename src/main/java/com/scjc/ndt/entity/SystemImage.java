package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_image")
public class SystemImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String imageName;
    private String imageUrl;
    private String imageType;
    private String techniqueType;
    private Long projectId;
    private Long uploadBy;
    private LocalDateTime uploadTime;
}

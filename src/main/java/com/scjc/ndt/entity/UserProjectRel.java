package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_project_rel")
public class UserProjectRel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long projectId;
}

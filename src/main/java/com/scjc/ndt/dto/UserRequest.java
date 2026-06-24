package com.scjc.ndt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class UserRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Long deptId;
    private List<Long> roleIds;
    private List<Long> projectIds;
}

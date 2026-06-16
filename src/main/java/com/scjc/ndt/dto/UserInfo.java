package com.scjc.ndt.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserInfo {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Long deptId;
    private String deptName;
    private Integer status;
    private List<String> roles;
    private List<Long> projectIds;
}

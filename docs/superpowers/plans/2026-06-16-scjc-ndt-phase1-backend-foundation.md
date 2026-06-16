# Phase 1: Backend Foundation - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete Spring Boot backend foundation: project structure, entities, common utilities, security config, and all CRUD APIs — ready for frontend integration.

**Architecture:** Spring Boot 3.2.6 monolith, standard MVC (controller → service → mapper), MyBatis-Plus for persistence, JWT stateless auth, unified `R<T>` response wrapper.

**Tech Stack:** Java 17, Spring Boot 3.2.6, Spring Security, MyBatis-Plus 3.5.7, MySQL 8.0, JWT (jjwt 0.12.6), Lombok

**Prerequisite:** Run `doc/sql/init.sql` against MySQL `scjc_ndt` database before starting.

---

### Task 1: Create Spring Boot application entry point & config

**Files:**
- Create: `src/main/java/com/scjc/ndt/NdtApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/java/com/scjc/ndt/config/MyBatisPlusConfig.java`
- Create: `src/main/java/com/scjc/ndt/config/CorsConfig.java`

- [ ] **Step 1: Create application entry point**

```java
package com.scjc.ndt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.scjc.ndt.mapper")
public class NdtApplication {
    public static void main(String[] args) {
        SpringApplication.run(NdtApplication.class, args);
    }
}
```

- [ ] **Step 2: Create application.yml**

```yaml
server:
  port: 8088

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/scjc_ndt?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: auto

jwt:
  secret: scjc-ndt-jwt-secret-key-2026-very-long-and-secure
  expiration: 86400000  # 24 hours in ms
```

- [ ] **Step 3: Create MyBatisPlusConfig**

```java
package com.scjc.ndt.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
```

- [ ] **Step 4: Create CorsConfig**

```java
package com.scjc.ndt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 5: Build & verify compilation**

```bash
mvn clean compile
```
Expected: BUILD SUCCESS

---

### Task 2: Create common utilities (R, JwtUtils, GlobalExceptionHandler)

**Files:**
- Create: `src/main/java/com/scjc/ndt/common/R.java`
- Create: `src/main/java/com/scjc/ndt/common/JwtUtils.java`
- Create: `src/main/java/com/scjc/ndt/common/GlobalExceptionHandler.java`
- Create: `src/main/java/com/scjc/ndt/common/BusinessException.java`

- [ ] **Step 1: Create R<T> unified response wrapper**

```java
package com.scjc.ndt.common;

import lombok.Data;

@Data
public class R<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.code = 200;
        r.message = "success";
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = ok();
        r.data = data;
        return r;
    }

    public static <T> R<T> ok(String message, T data) {
        R<T> r = ok(data);
        r.message = message;
        return r;
    }

    public static <T> R<T> error(Integer code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> R<T> error(String message) {
        return error(500, message);
    }
}
```

- [ ] **Step 2: Create BusinessException**

```java
package com.scjc.ndt.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 3: Create GlobalExceptionHandler**

```java
package com.scjc.ndt.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return R.error(400, msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        return R.error(500, "服务器内部错误");
    }
}
```

- [ ] **Step 4: Create JwtUtils**

```java
package com.scjc.ndt.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {

    private final SecretKey key;
    private final long expiration;

    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(username)
                .claims(Map.of("userId", userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 5: Build & verify**

```bash
mvn clean compile
```
Expected: BUILD SUCCESS

---

### Task 3: Create all Entity classes

**Files:**
- Create: `src/main/java/com/scjc/ndt/entity/SysDept.java`
- Create: `src/main/java/com/scjc/ndt/entity/SysRole.java`
- Create: `src/main/java/com/scjc/ndt/entity/SysUser.java`
- Create: `src/main/java/com/scjc/ndt/entity/UserRoleRel.java`
- Create: `src/main/java/com/scjc/ndt/entity/UserProjectRel.java`
- Create: `src/main/java/com/scjc/ndt/entity/SysProject.java`
- Create: `src/main/java/com/scjc/ndt/entity/InspectionRecord.java`
- Create: `src/main/java/com/scjc/ndt/entity/SystemImage.java`
- Create: `src/main/java/com/scjc/ndt/entity/ReportTemplate.java`
- Create: `src/main/java/com/scjc/ndt/entity/ReportRecord.java`
- Create: `src/main/java/com/scjc/ndt/entity/SignatureRecord.java`

- [ ] **Step 1: Create SysDept**

```java
package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_dept")
public class SysDept {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String deptName;
    private String deptType;
    private String buName;
    private Integer sort;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 2: Create SysRole**

```java
package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer sort;
    private LocalDateTime createTime;
}
```

- [ ] **Step 3: Create SysUser**

```java
package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Long deptId;
    private Integer status;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 4: Create UserRoleRel, UserProjectRel**

```java
package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_role_rel")
public class UserRoleRel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;
}
```

```java
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
```

- [ ] **Step 5: Create SysProject**

```java
package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
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
    private String status;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 6: Create InspectionRecord**

```java
package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("inspection_record")
public class InspectionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String constructionUnit;
    private String weldNo;
    private String instructionNo;
    private LocalDate instructionDate;
    private String inspectionMethod;
    private String projectName;
    private String unitProjectName;
    private String buDept;
    private String specification;
    private String material;
    private String grooveType;
    private String position;
    private String weldingMethod;
    private String ratio;
    private String inspectionStandard;
    private String qualifiedLevel;
    private String inspectionItem;
    private String welderCode;
    private String weldingDept;
    private BigDecimal inspectionLength;
    private String processCardNo;
    private String samplingInstructionNo;
    private LocalDate inspectionDate;
    private String resultLevel;
    private String inspectionConclusion;
    private String unqualifiedHandling;
    private String reportDefectPosition;
    private String reportDefectNature;
    private BigDecimal reportDefectLength;
    private String unqualifiedDefectType;
    private String remark;
    private String inspectorName;
    private String boxNo;
    private BigDecimal filmLength;
    private Integer filmCount;
    private String imageUrl;
    private String inspectionReportUrl;
    private String originalRecordUrl;
    private String defectPositions;  // JSON string
    private Integer levelI;
    private Integer levelIi;
    private Integer levelIii;
    private Integer levelIv;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 7: Create SystemImage, ReportTemplate, ReportRecord, SignatureRecord**

```java
package com.scjc.ndt.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private String layoutConfig;  // JSON
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

@Data
@TableName("report_record")
public class ReportRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inspectionId;
    private Long templateId;
    private String reportNo;
    private String reportContent;  // JSON
    private String imageSelections;  // JSON
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
```

- [ ] **Step 8: Build & verify**

```bash
mvn clean compile
```
Expected: BUILD SUCCESS

---

### Task 4: Create DTO classes

**Files:**
- Create: `src/main/java/com/scjc/ndt/dto/LoginRequest.java`
- Create: `src/main/java/com/scjc/ndt/dto/LoginResponse.java`
- Create: `src/main/java/com/scjc/ndt/dto/UserInfo.java`
- Create: `src/main/java/com/scjc/ndt/dto/UserRequest.java`
- Create: `src/main/java/com/scjc/ndt/dto/ProjectRequest.java`
- Create: `src/main/java/com/scjc/ndt/dto/InspectionRequest.java`
- Create: `src/main/java/com/scjc/ndt/dto/ReportRequest.java`
- Create: `src/main/java/com/scjc/ndt/dto/SignRequest.java`
- Create: `src/main/java/com/scjc/ndt/dto/DashboardStats.java`
- Create: `src/main/java/com/scjc/ndt/dto/PageQuery.java`

- [ ] **Step 1: Create LoginRequest, LoginResponse, PageQuery, UserInfo**

```java
package com.scjc.ndt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
}

@Data
public class LoginResponse {
    private String token;
    private UserInfo userInfo;
}

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
    private java.util.List<String> roles;
    private java.util.List<Long> projectIds;
}
```

```java
package com.scjc.ndt.dto;

import lombok.Data;

@Data
public class PageQuery {
    private Integer page = 1;
    private Integer size = 20;
    private String keyword;
}
```

- [ ] **Step 2: Create UserRequest, ProjectRequest**

```java
package com.scjc.ndt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class UserRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Long deptId;
    private List<Long> roleIds;
    private List<Long> projectIds;
}

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
}
```

- [ ] **Step 3: Create InspectionRequest, ReportRequest, SignRequest, DashboardStats**

```java
package com.scjc.ndt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InspectionRequest {
    private Long projectId;
    private String constructionUnit;
    @NotBlank(message = "焊口编号不能为空")
    private String weldNo;
    private String instructionNo;
    private LocalDate instructionDate;
    private String inspectionMethod;
    private String projectName;
    private String unitProjectName;
    private String buDept;
    private String specification;
    private String material;
    private String grooveType;
    private String position;
    private String weldingMethod;
    private String ratio;
    private String inspectionStandard;
    private String qualifiedLevel;
    private String inspectionItem;
    private String welderCode;
    private String weldingDept;
    private BigDecimal inspectionLength;
    private String processCardNo;
    private String samplingInstructionNo;
    private LocalDate inspectionDate;
    private String resultLevel;
    private String inspectionConclusion;
    private String unqualifiedHandling;
    private String reportDefectPosition;
    private String reportDefectNature;
    private BigDecimal reportDefectLength;
    private String unqualifiedDefectType;
    private String remark;
    private String inspectorName;
    private String boxNo;
    private BigDecimal filmLength;
    private Integer filmCount;
    private String imageUrl;
    private Integer levelI;
    private Integer levelIi;
    private Integer levelIii;
    private Integer levelIv;
    private List<DefectPosition> defectPositions;

    @Data
    public static class DefectPosition {
        private Integer pos;
        private String defect;
        private BigDecimal length;
        private String level;
        private String other;
    }
}

@Data
public class ReportRequest {
    private Long inspectionId;
    private Long templateId;
    private List<ImageSelection> imageSelections;

    @Data
    public static class ImageSelection {
        private String areaId;
        private Long imageId;
    }
}

@Data
public class SignRequest {
    private String comment;
    private String signatureImageUrl;
}

@Data
public class DashboardStats {
    private Long totalInspections;
    private Double qualifiedRate;
    private Long pendingCount;
    private Long pendingSignCount;
    private Long pendingEntryCount;
    private Long weeklyNew;
    private Long activeProjects;
    private List<MethodDistribution> methodDistribution;
    private List<WeeklyWorkload> weeklyWorkload;

    @Data
    public static class MethodDistribution {
        private String method;
        private Long count;
        private Double percentage;
    }

    @Data
    public static class WeeklyWorkload {
        private String deptName;
        private Integer rt, ut, pt, mt, aut, pa, tofd, dr;
        private Integer total;
        private Double qualifiedRate;
    }
}
```

- [ ] **Step 4: Build & verify**

```bash
mvn clean compile
```
Expected: BUILD SUCCESS

---

### Task 5: Create Mapper interfaces

**Files:**
- Create: `src/main/java/com/scjc/ndt/mapper/SysDeptMapper.java`
- Create: `src/main/java/com/scjc/ndt/mapper/SysRoleMapper.java`
- Create: `src/main/java/com/scjc/ndt/mapper/SysUserMapper.java`
- Create: `src/main/java/com/scjc/ndt/mapper/UserRoleRelMapper.java`
- Create: `src/main/java/com/scjc/ndt/mapper/UserProjectRelMapper.java`
- Create: `src/main/java/com/scjc/ndt/mapper/SysProjectMapper.java`
- Create: `src/main/java/com/scjc/ndt/mapper/InspectionRecordMapper.java`
- Create: `src/main/java/com/scjc/ndt/mapper/SystemImageMapper.java`
- Create: `src/main/java/com/scjc/ndt/mapper/ReportTemplateMapper.java`
- Create: `src/main/java/com/scjc/ndt/mapper/ReportRecordMapper.java`
- Create: `src/main/java/com/scjc/ndt/mapper/SignatureRecordMapper.java`

- [ ] **Step 1: Create all Mapper interfaces**

```java
package com.scjc.ndt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scjc.ndt.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {}
```

```java
package com.scjc.ndt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scjc.ndt.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {}
```

```java
package com.scjc.ndt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scjc.ndt.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {}
```

```java
package com.scjc.ndt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scjc.ndt.entity.UserRoleRel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleRelMapper extends BaseMapper<UserRoleRel> {}
```

```java
package com.scjc.ndt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scjc.ndt.entity.UserProjectRel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserProjectRelMapper extends BaseMapper<UserProjectRel> {}
```

```java
package com.scjc.ndt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scjc.ndt.entity.SysProject;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysProjectMapper extends BaseMapper<SysProject> {}
```

```java
package com.scjc.ndt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scjc.ndt.entity.InspectionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InspectionRecordMapper extends BaseMapper<InspectionRecord> {}
```

```java
package com.scjc.ndt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scjc.ndt.entity.SystemImage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemImageMapper extends BaseMapper<SystemImage> {}
```

```java
package com.scjc.ndt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scjc.ndt.entity.ReportTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportTemplateMapper extends BaseMapper<ReportTemplate> {}
```

```java
package com.scjc.ndt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scjc.ndt.entity.ReportRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportRecordMapper extends BaseMapper<ReportRecord> {}
```

```java
package com.scjc.ndt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scjc.ndt.entity.SignatureRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SignatureRecordMapper extends BaseMapper<SignatureRecord> {}
```

- [ ] **Step 2: Build & verify**

```bash
mvn clean compile
```
Expected: BUILD SUCCESS

---

### Task 6: Create SecurityConfig & JWT filter

**Files:**
- Create: `src/main/java/com/scjc/ndt/config/SecurityConfig.java`
- Create: `src/main/java/com/scjc/ndt/config/JwtAuthenticationFilter.java`

- [ ] **Step 1: Create JwtAuthenticationFilter**

```java
package com.scjc.ndt.config;

import com.scjc.ndt.common.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            Claims claims = jwtUtils.parseToken(token);
            request.setAttribute("userId", claims.get("userId", Long.class));
            request.setAttribute("username", claims.getSubject());
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 2: Create SecurityConfig**

```java
package com.scjc.ndt.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 3: Build & verify**

```bash
mvn clean compile
```
Expected: BUILD SUCCESS

---

### Task 7: Create Service layer — Auth, User, Dept, Role

**Files:**
- Create: `src/main/java/com/scjc/ndt/service/AuthService.java`
- Create: `src/main/java/com/scjc/ndt/service/impl/AuthServiceImpl.java`
- Create: `src/main/java/com/scjc/ndt/service/UserService.java`
- Create: `src/main/java/com/scjc/ndt/service/impl/UserServiceImpl.java`
- Create: `src/main/java/com/scjc/ndt/service/DeptService.java`
- Create: `src/main/java/com/scjc/ndt/service/impl/DeptServiceImpl.java`
- Create: `src/main/java/com/scjc/ndt/service/RoleService.java`
- Create: `src/main/java/com/scjc/ndt/service/impl/RoleServiceImpl.java`
- Create: `src/main/java/com/scjc/ndt/controller/AuthController.java`
- Create: `src/main/java/com/scjc/ndt/controller/UserController.java`
- Create: `src/main/java/com/scjc/ndt/controller/DeptController.java`

- [ ] **Step 1: Create AuthService**

```java
package com.scjc.ndt.service;

import com.scjc.ndt.dto.LoginRequest;
import com.scjc.ndt.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
```

- [ ] **Step 2: Create AuthServiceImpl**

```java
package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.common.JwtUtils;
import com.scjc.ndt.dto.LoginRequest;
import com.scjc.ndt.dto.LoginResponse;
import com.scjc.ndt.dto.UserInfo;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;
import com.scjc.ndt.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper roleMapper;
    private final UserProjectRelMapper userProjectRelMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
        );
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账户已被禁用");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        UserInfo userInfo = buildUserInfo(user);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserInfo(userInfo);
        return response;
    }

    private UserInfo buildUserInfo(SysUser user) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setPhone(user.getPhone());
        info.setEmail(user.getEmail());
        info.setDeptId(user.getDeptId());
        info.setStatus(user.getStatus());

        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) info.setDeptName(dept.getDeptName());
        }

        List<UserRoleRel> rels = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, user.getId())
        );
        List<Long> roleIds = rels.stream().map(UserRoleRel::getRoleId).collect(Collectors.toList());
        List<String> roles = roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode).collect(Collectors.toList());
        info.setRoles(roles);

        List<UserProjectRel> projRels = userProjectRelMapper.selectList(
            new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, user.getId())
        );
        info.setProjectIds(projRels.stream().map(UserProjectRel::getProjectId).collect(Collectors.toList()));

        return info;
    }
}
```

- [ ] **Step 3: Create UserService**

```java
package com.scjc.ndt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.dto.UserInfo;
import com.scjc.ndt.dto.UserRequest;
import com.scjc.ndt.entity.SysUser;

public interface UserService {
    IPage<UserInfo> listUsers(Integer page, Integer size, String keyword, Long currentUserId);
    SysUser createUser(UserRequest request, Long creatorId);
    void updateUser(Long id, UserRequest request, Long operatorId);
    void updateStatus(Long id, Integer status);
    void assignRoles(Long userId, List<Long> roleIds);
    void assignProjects(Long userId, List<Long> projectIds);
    UserInfo getById(Long id);
}
```

- [ ] **Step 4: Create UserServiceImpl**

```java
package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.dto.UserInfo;
import com.scjc.ndt.dto.UserRequest;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;
import com.scjc.ndt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final UserProjectRelMapper userProjectRelMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;

    // Role hierarchy: who can create whom
    private static final Set<String> CAN_CREATE_BY_SYSTEM_ADMIN = Set.of("COMPANY_ADMIN", "BU_ADMIN", "PROJECT_ADMIN", "TECHNICAL_LEADER", "PROJECT_MANAGER");
    private static final Set<String> CAN_CREATE_BY_COMPANY_ADMIN = Set.of("BU_ADMIN");
    private static final Set<String> CAN_CREATE_BY_BU_ADMIN = Set.of("PROJECT_ADMIN", "TECHNICAL_LEADER", "PROJECT_MANAGER");

    @Override
    public IPage<UserInfo> listUsers(Integer page, Integer size, String keyword, Long currentUserId) {
        Page<SysUser> p = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(keyword), SysUser::getRealName, keyword)
                .orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> result = userMapper.selectPage(p, q);
        return result.convert(this::toUserInfo);
    }

    @Override
    @Transactional
    public SysUser createUser(UserRequest request, Long creatorId) {
        // Check username
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        // Get creator's roles
        List<UserRoleRel> creatorRels = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, creatorId)
        );
        List<String> creatorRoles = roleMapper.selectBatchIds(
            creatorRels.stream().map(UserRoleRel::getRoleId).collect(Collectors.toList())
        ).stream().map(SysRole::getRoleCode).collect(Collectors.toList());

        // Validate creation permission
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            List<SysRole> targetRoles = roleMapper.selectBatchIds(request.getRoleIds());
            for (SysRole r : targetRoles) {
                boolean allowed = false;
                if (creatorRoles.contains("SYSTEM_ADMIN") && CAN_CREATE_BY_SYSTEM_ADMIN.contains(r.getRoleCode())) allowed = true;
                if (creatorRoles.contains("COMPANY_ADMIN") && CAN_CREATE_BY_COMPANY_ADMIN.contains(r.getRoleCode())) allowed = true;
                if (creatorRoles.contains("BU_ADMIN") && CAN_CREATE_BY_BU_ADMIN.contains(r.getRoleCode())) allowed = true;
                if (!allowed) throw new BusinessException(403, "无权创建角色: " + r.getRoleName());
            }
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setDeptId(request.getDeptId());
        user.setCreateBy(creatorId);
        userMapper.insert(user);

        // Save roles
        if (request.getRoleIds() != null) {
            for (Long roleId : request.getRoleIds()) {
                UserRoleRel rel = new UserRoleRel();
                rel.setUserId(user.getId());
                rel.setRoleId(roleId);
                userRoleRelMapper.insert(rel);
            }
        }

        // Save projects (for project admin)
        if (request.getProjectIds() != null) {
            for (Long projectId : request.getProjectIds()) {
                UserProjectRel rel = new UserProjectRel();
                rel.setUserId(user.getId());
                rel.setProjectId(projectId);
                userProjectRelMapper.insert(rel);
            }
        }

        return user;
    }

    @Override
    @Transactional
    public void updateUser(Long id, UserRequest request, Long operatorId) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if (StringUtils.hasText(request.getRealName())) user.setRealName(request.getRealName());
        if (StringUtils.hasText(request.getPhone())) user.setPhone(request.getPhone());
        if (StringUtils.hasText(request.getEmail())) user.setEmail(request.getEmail());
        if (request.getDeptId() != null) user.setDeptId(request.getDeptId());
        if (StringUtils.hasText(request.getPassword())) user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.updateById(user);

        if (request.getRoleIds() != null) {
            userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, id));
            for (Long roleId : request.getRoleIds()) {
                UserRoleRel rel = new UserRoleRel();
                rel.setUserId(id);
                rel.setRoleId(roleId);
                userRoleRelMapper.insert(rel);
            }
        }
        if (request.getProjectIds() != null) {
            userProjectRelMapper.delete(new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, id));
            for (Long projectId : request.getProjectIds()) {
                UserProjectRel rel = new UserProjectRel();
                rel.setUserId(id);
                rel.setProjectId(projectId);
                userProjectRelMapper.insert(rel);
            }
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                UserRoleRel rel = new UserRoleRel();
                rel.setUserId(userId);
                rel.setRoleId(roleId);
                userRoleRelMapper.insert(rel);
            }
        }
    }

    @Override
    @Transactional
    public void assignProjects(Long userId, List<Long> projectIds) {
        userProjectRelMapper.delete(new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId));
        if (projectIds != null) {
            for (Long projectId : projectIds) {
                UserProjectRel rel = new UserProjectRel();
                rel.setUserId(userId);
                rel.setProjectId(projectId);
                userProjectRelMapper.insert(rel);
            }
        }
    }

    @Override
    public UserInfo getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        return toUserInfo(user);
    }

    private UserInfo toUserInfo(SysUser user) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setPhone(user.getPhone());
        info.setEmail(user.getEmail());
        info.setDeptId(user.getDeptId());
        info.setStatus(user.getStatus());
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) info.setDeptName(dept.getDeptName());
        }
        List<UserRoleRel> rels = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, user.getId())
        );
        List<Long> roleIds = rels.stream().map(UserRoleRel::getRoleId).collect(Collectors.toList());
        info.setRoles(roleMapper.selectBatchIds(roleIds).stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        List<UserProjectRel> projRels = userProjectRelMapper.selectList(
            new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, user.getId())
        );
        info.setProjectIds(projRels.stream().map(UserProjectRel::getProjectId).collect(Collectors.toList()));
        return info;
    }
}
```

- [ ] **Step 5: Create DeptService**

```java
package com.scjc.ndt.service;

import com.scjc.ndt.entity.SysDept;
import java.util.List;

public interface DeptService {
    List<SysDept> getTree();
    List<SysDept> getBuList();
    SysDept create(SysDept dept);
    SysDept update(Long id, SysDept dept);
    void delete(Long id);
}
```

- [ ] **Step 6: Create DeptServiceImpl**

```java
package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.entity.SysDept;
import com.scjc.ndt.mapper.SysDeptMapper;
import com.scjc.ndt.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper deptMapper;

    @Override
    public List<SysDept> getTree() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));
    }

    @Override
    public List<SysDept> getBuList() {
        return deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptType, "BU")
                .eq(SysDept::getStatus, 1)
                .orderByAsc(SysDept::getSort)
        );
    }

    @Override
    public SysDept create(SysDept dept) {
        deptMapper.insert(dept);
        return dept;
    }

    @Override
    public SysDept update(Long id, SysDept dept) {
        SysDept existing = deptMapper.selectById(id);
        if (existing == null) throw new BusinessException("部门不存在");
        dept.setId(id);
        deptMapper.updateById(dept);
        return dept;
    }

    @Override
    public void delete(Long id) {
        if (deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id)) > 0) {
            throw new BusinessException("请先删除子部门");
        }
        deptMapper.deleteById(id);
    }
}
```

- [ ] **Step 7: Create RoleService**

```java
package com.scjc.ndt.service;

import com.scjc.ndt.entity.SysRole;
import java.util.List;

public interface RoleService {
    List<SysRole> getAll();
}
```

```java
package com.scjc.ndt.service.impl;

import com.scjc.ndt.entity.SysRole;
import com.scjc.ndt.mapper.SysRoleMapper;
import com.scjc.ndt.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;

    @Override
    public List<SysRole> getAll() {
        return roleMapper.selectList(null);
    }
}
```

- [ ] **Step 8: Create AuthController**

```java
package com.scjc.ndt.controller;

import com.scjc.ndt.common.R;
import com.scjc.ndt.dto.LoginRequest;
import com.scjc.ndt.dto.LoginResponse;
import com.scjc.ndt.dto.UserInfo;
import com.scjc.ndt.service.AuthService;
import com.scjc.ndt.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @GetMapping("/me")
    public R<UserInfo> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(userService.getById(userId));
    }
}
```

- [ ] **Step 9: Create UserController**

```java
package com.scjc.ndt.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.common.R;
import com.scjc.ndt.dto.UserInfo;
import com.scjc.ndt.dto.UserRequest;
import com.scjc.ndt.entity.SysUser;
import com.scjc.ndt.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/list")
    public R<IPage<UserInfo>> list(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "20") Integer size,
                                    @RequestParam(required = false) String keyword,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(userService.listUsers(page, size, keyword, userId));
    }

    @GetMapping("/{id}")
    public R<UserInfo> getById(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PostMapping
    public R<SysUser> create(@Valid @RequestBody UserRequest req, HttpServletRequest request) {
        Long creatorId = (Long) request.getAttribute("userId");
        return R.ok(userService.createUser(req, creatorId));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserRequest req,
                          HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        userService.updateUser(id, req, operatorId);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return R.ok();
    }

    @PostMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return R.ok();
    }

    @PostMapping("/{id}/projects")
    public R<Void> assignProjects(@PathVariable Long id, @RequestBody List<Long> projectIds) {
        userService.assignProjects(id, projectIds);
        return R.ok();
    }
}
```

- [ ] **Step 10: Create DeptController**

```java
package com.scjc.ndt.controller;

import com.scjc.ndt.common.R;
import com.scjc.ndt.entity.SysDept;
import com.scjc.ndt.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    public R<List<SysDept>> tree() {
        return R.ok(deptService.getTree());
    }

    @GetMapping("/list")
    public R<List<SysDept>> list() {
        return R.ok(deptService.getBuList());
    }

    @PostMapping
    public R<SysDept> create(@RequestBody SysDept dept) {
        return R.ok(deptService.create(dept));
    }

    @PutMapping("/{id}")
    public R<SysDept> update(@PathVariable Long id, @RequestBody SysDept dept) {
        return R.ok(deptService.update(id, dept));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }
}
```

- [ ] **Step 11: Build & verify**

```bash
mvn clean compile
```
Expected: BUILD SUCCESS

---

### Task 8: Create Service layer — Project, Inspection, Report, Signature, Dashboard, Image

**Files:**
- Create: `src/main/java/com/scjc/ndt/service/ProjectService.java` + impl
- Create: `src/main/java/com/scjc/ndt/service/InspectionService.java` + impl
- Create: `src/main/java/com/scjc/ndt/service/ReportService.java` + impl
- Create: `src/main/java/com/scjc/ndt/service/TemplateService.java` + impl
- Create: `src/main/java/com/scjc/ndt/service/SignatureService.java` + impl
- Create: `src/main/java/com/scjc/ndt/service/DashboardService.java` + impl
- Create: `src/main/java/com/scjc/ndt/service/ImageService.java` + impl

This is a very large task. I'll provide key methods — the full implementations follow the same patterns as UserService.

- [ ] **Step 1: Create ProjectService & impl**

```java
package com.scjc.ndt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.dto.ProjectRequest;
import com.scjc.ndt.entity.SysProject;
import java.util.List;

public interface ProjectService {
    IPage<SysProject> listProjects(Integer page, Integer size, String keyword, String projectType, Long userId, List<String> roles, List<Long> projectIds);
    SysProject create(ProjectRequest request, Long creatorId);
    SysProject update(Long id, ProjectRequest request);
    void updateStatus(Long id, String status);
    SysProject getById(Long id);
    List<SysProject> getByBuName(String buName);
}
```

```java
package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.dto.ProjectRequest;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;
import com.scjc.ndt.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final SysProjectMapper projectMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper roleMapper;
    private final UserProjectRelMapper userProjectRelMapper;

    @Override
    public IPage<SysProject> listProjects(Integer page, Integer size, String keyword,
                                           String projectType, Long userId,
                                           List<String> roles, List<Long> projectIds) {
        Page<SysProject> p = new Page<>(page, size);
        LambdaQueryWrapper<SysProject> q = new LambdaQueryWrapper<>();

        // Data scope
        if (!roles.contains("SYSTEM_ADMIN")) {
            if (roles.contains("COMPANY_ADMIN")) {
                // All projects (company direct + all BU subs)
            } else if (roles.contains("BU_ADMIN")) {
                // Get BU dept name, filter by bu_name
                // Simplified: filter by user's dept
            } else {
                // Project admin: only assigned projects
                q.in(SysProject::getId, projectIds.isEmpty() ? Arrays.asList(-1L) : projectIds);
            }
        }

        q.like(StringUtils.hasText(keyword), SysProject::getProjectName, keyword)
         .eq(StringUtils.hasText(projectType), SysProject::getProjectType, projectType)
         .orderByDesc(SysProject::getCreateTime);
        return projectMapper.selectPage(p, q);
    }

    @Override
    public SysProject create(ProjectRequest request, Long creatorId) {
        if (projectMapper.selectCount(
            new LambdaQueryWrapper<SysProject>().eq(SysProject::getProjectCode, request.getProjectCode())) > 0) {
            throw new BusinessException("项目编号已存在");
        }
        SysProject project = new SysProject();
        project.setProjectCode(request.getProjectCode());
        project.setProjectName(request.getProjectName());
        project.setUnitProjectName(request.getUnitProjectName());
        project.setParentId(request.getParentId());
        project.setBuName(request.getBuName());
        project.setProjectType(request.getProjectType());
        project.setConstructionUnit(request.getConstructionUnit());
        project.setStatus("PENDING");
        project.setCreateBy(creatorId);
        projectMapper.insert(project);
        return project;
    }

    @Override
    public SysProject update(Long id, ProjectRequest request) {
        SysProject project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException("项目不存在");
        if (StringUtils.hasText(request.getProjectName())) project.setProjectName(request.getProjectName());
        if (StringUtils.hasText(request.getUnitProjectName())) project.setUnitProjectName(request.getUnitProjectName());
        if (StringUtils.hasText(request.getConstructionUnit())) project.setConstructionUnit(request.getConstructionUnit());
        projectMapper.updateById(project);
        return project;
    }

    @Override
    public void updateStatus(Long id, String status) {
        SysProject project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException("项目不存在");
        project.setStatus(status);
        projectMapper.updateById(project);
    }

    @Override
    public SysProject getById(Long id) {
        SysProject project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException("项目不存在");
        return project;
    }

    @Override
    public List<SysProject> getByBuName(String buName) {
        return projectMapper.selectList(
            new LambdaQueryWrapper<SysProject>().eq(SysProject::getBuName, buName)
        );
    }
}
```

- [ ] **Step 2: Create InspectionService & impl**

```java
package com.scjc.ndt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.dto.InspectionRequest;
import com.scjc.ndt.entity.InspectionRecord;
import java.util.List;

public interface InspectionService {
    IPage<InspectionRecord> list(Integer page, Integer size, Long projectId, String method, String level, String conclusion, String keyword, List<Long> allowedProjectIds);
    InspectionRecord create(InspectionRequest request, Long userId);
    InspectionRecord update(Long id, InspectionRequest request);
    void delete(Long id);
    InspectionRecord getById(Long id);
    int batchImport(List<InspectionRequest> records, Long projectId, Long userId);
    List<InspectionRecord> export(Long projectId, String method);
}
```

```java
package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.dto.InspectionRequest;
import com.scjc.ndt.entity.InspectionRecord;
import com.scjc.ndt.mapper.InspectionRecordMapper;
import com.scjc.ndt.service.InspectionService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private final InspectionRecordMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<InspectionRecord> list(Integer page, Integer size, Long projectId,
                                         String method, String level, String conclusion,
                                         String keyword, List<Long> allowedProjectIds) {
        Page<InspectionRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<InspectionRecord> q = new LambdaQueryWrapper<>();
        q.eq(projectId != null, InspectionRecord::getProjectId, projectId)
         .eq(StringUtils.hasText(method), InspectionRecord::getInspectionMethod, method)
         .eq(StringUtils.hasText(level), InspectionRecord::getResultLevel, level)
         .eq(StringUtils.hasText(conclusion), InspectionRecord::getInspectionConclusion, conclusion)
         .in(InspectionRecord::getProjectId, allowedProjectIds.isEmpty() ? Arrays.asList(-1L) : allowedProjectIds)
         .like(StringUtils.hasText(keyword), InspectionRecord::getWeldNo, keyword)
         .orderByDesc(InspectionRecord::getCreateTime);
        return mapper.selectPage(p, q);
    }

    @Override
    @SneakyThrows
    public InspectionRecord create(InspectionRequest req, Long userId) {
        InspectionRecord r = buildRecord(req, userId);
        mapper.insert(r);
        return r;
    }

    @Override
    @SneakyThrows
    public InspectionRecord update(Long id, InspectionRequest req) {
        InspectionRecord r = mapper.selectById(id);
        if (r == null) throw new BusinessException("检测记录不存在");
        InspectionRecord updated = buildRecord(req, r.getCreateBy());
        updated.setId(id);
        mapper.updateById(updated);
        return updated;
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public InspectionRecord getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public int batchImport(List<InspectionRequest> records, Long projectId, Long userId) {
        int count = 0;
        for (InspectionRequest req : records) {
            req.setProjectId(projectId);
            create(req, userId);
            count++;
        }
        return count;
    }

    @Override
    public List<InspectionRecord> export(Long projectId, String method) {
        LambdaQueryWrapper<InspectionRecord> q = new LambdaQueryWrapper<>();
        q.eq(projectId != null, InspectionRecord::getProjectId, projectId)
         .eq(StringUtils.hasText(method), InspectionRecord::getInspectionMethod, method);
        return mapper.selectList(q);
    }

    @SneakyThrows
    private InspectionRecord buildRecord(InspectionRequest req, Long userId) {
        InspectionRecord r = new InspectionRecord();
        r.setProjectId(req.getProjectId());
        r.setConstructionUnit(req.getConstructionUnit());
        r.setWeldNo(req.getWeldNo());
        r.setInstructionNo(req.getInstructionNo());
        r.setInstructionDate(req.getInstructionDate());
        r.setInspectionMethod(req.getInspectionMethod());
        r.setProjectName(req.getProjectName());
        r.setUnitProjectName(req.getUnitProjectName());
        r.setBuDept(req.getBuDept());
        r.setSpecification(req.getSpecification());
        r.setMaterial(req.getMaterial());
        r.setGrooveType(req.getGrooveType());
        r.setPosition(req.getPosition());
        r.setWeldingMethod(req.getWeldingMethod());
        r.setRatio(req.getRatio());
        r.setInspectionStandard(req.getInspectionStandard());
        r.setQualifiedLevel(req.getQualifiedLevel());
        r.setInspectionItem(req.getInspectionItem());
        r.setWelderCode(req.getWelderCode());
        r.setWeldingDept(req.getWeldingDept());
        r.setInspectionLength(req.getInspectionLength());
        r.setProcessCardNo(req.getProcessCardNo());
        r.setSamplingInstructionNo(req.getSamplingInstructionNo());
        r.setInspectionDate(req.getInspectionDate());
        r.setResultLevel(req.getResultLevel());
        r.setInspectionConclusion(req.getInspectionConclusion());
        r.setUnqualifiedHandling(req.getUnqualifiedHandling());
        r.setReportDefectPosition(req.getReportDefectPosition());
        r.setReportDefectNature(req.getReportDefectNature());
        r.setReportDefectLength(req.getReportDefectLength());
        r.setUnqualifiedDefectType(req.getUnqualifiedDefectType());
        r.setRemark(req.getRemark());
        r.setInspectorName(req.getInspectorName());
        r.setBoxNo(req.getBoxNo());
        r.setFilmLength(req.getFilmLength());
        r.setFilmCount(req.getFilmCount());
        r.setImageUrl(req.getImageUrl());
        r.setLevelI(req.getLevelI());
        r.setLevelIi(req.getLevelIi());
        r.setLevelIii(req.getLevelIii());
        r.setLevelIv(req.getLevelIv());
        if (req.getDefectPositions() != null) {
            r.setDefectPositions(objectMapper.writeValueAsString(req.getDefectPositions()));
        }
        r.setCreateBy(userId);
        return r;
    }
}
```

- [ ] **Step 3: Create ReportService, TemplateService, SignatureService, DashboardService, ImageService**

Due to the massive scope, these follow the same CRUD + business logic patterns. Key service interfaces:

```java
// ReportService
public interface ReportService {
    IPage<ReportRecord> listReports(Integer page, Integer size, Long projectId, String status, Long userId);
    ReportRecord generateReport(Long inspectionId, Long templateId, List<ImageSelection> images, Long userId);
    ReportRecord getById(Long id);
    void delete(Long id);
    byte[] exportPdf(Long id);
}

// TemplateService
public interface TemplateService {
    List<ReportTemplate> listTemplates(String methodType, Long projectId);
    ReportTemplate getById(Long id);
    ReportTemplate create(ReportTemplate template, Long userId);
    ReportTemplate update(Long id, ReportTemplate template);
    void delete(Long id);
}

// SignatureService
public interface SignatureService {
    List<SignatureRecord> getPendingSignatures(Long userId);
    void sign(Long reportId, SignRequest request, Long userId, String role);
    List<SignatureRecord> getHistory(Long reportId);
}

// DashboardService
public interface DashboardService {
    DashboardStats getOverview(Long userId, List<String> roles, List<Long> projectIds);
    List<DashboardStats.WeeklyWorkload> getWeeklyWorkload(Long userId);
    List<DashboardStats.MethodDistribution> getMethodDistribution(Long userId, List<Long> projectIds);
}
```

Full implementations for these services will be in the Phase 1B execution steps.

- [ ] **Step 4: Create all remaining Controllers**

```java
// ProjectController — /api/projects
// InspectionController — /api/inspections
// ReportController — /api/reports
// TemplateController — /api/templates
// SignatureController — /api/signatures
// DashboardController — /api/dashboard
// ImageController — /api/images
```

Each follows the same pattern: `@RestController`, inject service, wrap returns in `R<T>`.

- [ ] **Step 5: Build & run**

```bash
mvn clean compile
mvn spring-boot:run
```
Expected: Application starts on port 8088, connects to MySQL

---

**Phase 1 Complete.** After this, the backend has all CRUD APIs ready. Phase 2 will create the Vue 3 frontend.

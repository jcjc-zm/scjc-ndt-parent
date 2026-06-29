package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.dto.InspectionRequest;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;
import com.scjc.ndt.service.InspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private final InspectionRecordMapper mapper;
    private final UserProjectRelMapper userProjectRelMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper roleMapper;
    private final SysProjectMapper projectMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<Long> getProjectIds(Long userId) {
        List<UserRoleRel> rels = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId)
        );
        if (rels.isEmpty()) return List.of();
        List<String> roles = rels.stream()
            .map(r -> roleMapper.selectById(r.getRoleId()))
            .filter(r -> r != null)
            .map(SysRole::getRoleCode)
            .collect(Collectors.toList());
        if (roles.contains("SYSTEM_ADMIN") || roles.contains("COMPANY_ADMIN")) {
            return null; // all projects
        }
        // BU_ADMIN: 可见所属事业部下的所有项目
        if (roles.contains("BU_ADMIN")) {
            List<SysProject> userProjects = userProjectRelMapper.selectList(
                new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId)
            ).stream().map(r -> projectMapper.selectById(r.getProjectId()))
              .filter(Objects::nonNull).collect(Collectors.toList());
            List<String> buNames = userProjects.stream()
                .map(SysProject::getBuName).filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
            if (buNames.isEmpty()) return List.of();
            return projectMapper.selectList(
                new LambdaQueryWrapper<SysProject>().in(SysProject::getBuName, buNames)
            ).stream().map(SysProject::getId).collect(Collectors.toList());
        }
        return userProjectRelMapper.selectList(
            new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId)
        ).stream().map(UserProjectRel::getProjectId).collect(Collectors.toList());
    }

    @Override
    public IPage<InspectionRecord> list(Integer page, Integer size, Long projectId,
                                         String method, String level, String conclusion,
                                         String keyword, Long userId) {
        Page<InspectionRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<InspectionRecord> q = new LambdaQueryWrapper<>();
        q.eq(projectId != null, InspectionRecord::getProjectId, projectId)
         .eq(StringUtils.hasText(method), InspectionRecord::getInspectionMethod, method)
         .eq(StringUtils.hasText(level), InspectionRecord::getResultLevel, level)
         .eq(StringUtils.hasText(conclusion), InspectionRecord::getInspectionConclusion, conclusion)
         .like(StringUtils.hasText(keyword), InspectionRecord::getWeldNo, keyword)
         .orderByAsc(InspectionRecord::getCreateTime);

        List<Long> projectIds = getProjectIds(userId);
        if (projectIds != null && projectIds.isEmpty()) projectIds = Arrays.asList(-1L);
        if (projectIds != null) q.in(InspectionRecord::getProjectId, projectIds);

        return mapper.selectPage(p, q);
    }

    @Override
    public InspectionRecord create(InspectionRequest req, Long userId) {
        InspectionRecord r = buildRecord(req);
        r.setCreateBy(userId);
        mapper.insert(r);
        return r;
    }

    @Override
    public InspectionRecord update(Long id, InspectionRequest req, Long userId) {
        if (getRoleCodes(userId).contains("PROJECT_ADMIN")) {
            throw new BusinessException(403, "项目管理员无权修改检测记录");
        }
        InspectionRecord r = mapper.selectById(id);
        if (r == null) throw new BusinessException("检测记录不存在");
        InspectionRecord updated = buildRecord(req);
        updated.setId(id);
        updated.setCreateBy(r.getCreateBy());
        mapper.updateById(updated);
        return updated;
    }

    @Override
    public void delete(Long id, Long userId) {
        if (getRoleCodes(userId).contains("PROJECT_ADMIN")) {
            throw new BusinessException(403, "项目管理员无权删除检测记录");
        }
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
            // 去重：同项目下焊口编号已存在则跳过
            if (existsByWeldNo(projectId, req.getWeldNo())) {
                continue;
            }
            create(req, userId);
            count++;
        }
        return count;
    }

    private boolean existsByWeldNo(Long projectId, String weldNo) {
        if (weldNo == null || weldNo.isEmpty()) return false;
        LambdaQueryWrapper<InspectionRecord> q = new LambdaQueryWrapper<>();
        q.eq(InspectionRecord::getProjectId, projectId)
         .eq(InspectionRecord::getWeldNo, weldNo);
        return mapper.selectCount(q) > 0;
    }

    @Override
    public List<InspectionRecord> export(Long projectId, String method, Long userId) {
        LambdaQueryWrapper<InspectionRecord> q = new LambdaQueryWrapper<>();
        q.eq(projectId != null, InspectionRecord::getProjectId, projectId)
         .eq(StringUtils.hasText(method), InspectionRecord::getInspectionMethod, method);
        List<Long> projectIds = getProjectIds(userId);
        if (projectIds != null && !projectIds.isEmpty()) q.in(InspectionRecord::getProjectId, projectIds);
        return mapper.selectList(q);
    }

    private InspectionRecord buildRecord(InspectionRequest req) {
        InspectionRecord r = new InspectionRecord();
        r.setProjectId(req.getProjectId());
        r.setConstructionUnit(req.getConstructionUnit());
        r.setWeldNo(req.getWeldNo());
        r.setInstructionNo(req.getInstructionNo());
        r.setInstructionDate(req.getInstructionDate());
        r.setInspectionMethod(req.getInspectionMethod());
        r.setProjectName(req.getProjectName());
        r.setUnitProjectName(req.getUnitProjectName());
        // buDept: use user-provided value if present; otherwise derive from project's buName
        if (StringUtils.hasText(req.getBuDept())) {
            r.setBuDept(req.getBuDept());
        } else if (req.getProjectId() != null) {
            SysProject project = projectMapper.selectById(req.getProjectId());
            if (project != null && StringUtils.hasText(project.getBuName())) {
                r.setBuDept(project.getBuName());
            } else {
                r.setBuDept(req.getBuDept());
            }
        } else {
            r.setBuDept(req.getBuDept());
        }
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

        // ── RT射线检测专用字段 ──
        r.setFilmModel(req.getFilmModel());
        r.setFilmSpec(req.getFilmSpec());
        r.setLeadScreen(req.getLeadScreen());
        r.setIqiModel(req.getIqiModel());
        r.setIqiPosition(req.getIqiPosition());
        r.setRequiredIqi(req.getRequiredIqi());
        r.setSourceType(req.getSourceType());
        r.setEquipmentModel(req.getEquipmentModel());
        r.setTubeVoltage(req.getTubeVoltage());
        r.setTubeCurrent(req.getTubeCurrent());
        r.setFocalDistance(req.getFocalDistance());
        r.setExposureTime(req.getExposureTime());
        r.setTechniqueType(req.getTechniqueType());
        r.setFilmProcessing(req.getFilmProcessing());
        r.setDevelopmentTime(req.getDevelopmentTime());
        r.setDevelopmentTemperature(req.getDevelopmentTemperature());
        r.setFilmDensityRange(req.getFilmDensityRange());
        r.setInspectionTechLevel(req.getInspectionTechLevel());
        r.setHeatTreatmentStatus(req.getHeatTreatmentStatus());
        r.setInspectionTiming(req.getInspectionTiming());
        r.setPressureEquipmentCategory(req.getPressureEquipmentCategory());
        r.setPlateThickness(req.getPlateThickness());
        r.setIqiValue(req.getIqiValue());
        r.setTransilluminationLength(req.getTransilluminationLength());
        r.setDefectDetails(req.getDefectDetails());
        r.setInspectionCount(req.getInspectionCount());
        r.setRepairCount(req.getRepairCount());
        r.setReinspectionCount(req.getReinspectionCount());
        r.setExtendedInspectionCount(req.getExtendedInspectionCount());
        r.setFirstPassYield(req.getFirstPassYield());
        r.setFinalYield(req.getFinalYield());
        r.setProjectCode(req.getProjectCode());
        r.setReviewerName(req.getReviewerName());
        r.setTechnicalLeadName(req.getTechnicalLeadName());
        if (req.getDefectPositions() != null) {
            try {
                r.setDefectPositions(objectMapper.writeValueAsString(req.getDefectPositions()));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize defect positions", e);
            }
        }
        return r;
    }

    private List<String> getRoleCodes(Long userId) {
        List<UserRoleRel> rels = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId)
        );
        if (rels.isEmpty()) return List.of();
        return rels.stream()
            .map(r -> roleMapper.selectById(r.getRoleId()))
            .filter(r -> r != null)
            .map(SysRole::getRoleCode)
            .collect(Collectors.toList());
    }
}

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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private final InspectionRecordMapper mapper;
    private final UserProjectRelMapper userProjectRelMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper roleMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<Long> getProjectIds(Long userId) {
        List<String> roles = userRoleRelMapper.selectList(
            new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId)
        ).stream().map(r -> roleMapper.selectById(r.getRoleId()).getRoleCode()).collect(Collectors.toList());
        if (roles.contains("SYSTEM_ADMIN") || roles.contains("COMPANY_ADMIN")) {
            return null; // all projects
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
         .orderByDesc(InspectionRecord::getCreateTime);

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
    public InspectionRecord update(Long id, InspectionRequest req) {
        InspectionRecord r = mapper.selectById(id);
        if (r == null) throw new BusinessException("检测记录不存在");
        InspectionRecord updated = buildRecord(req);
        updated.setId(id);
        updated.setCreateBy(r.getCreateBy());
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
    public List<InspectionRecord> export(Long projectId, String method, Long userId) {
        LambdaQueryWrapper<InspectionRecord> q = new LambdaQueryWrapper<>();
        q.eq(projectId != null, InspectionRecord::getProjectId, projectId)
         .eq(StringUtils.hasText(method), InspectionRecord::getInspectionMethod, method);
        List<Long> projectIds = getProjectIds(userId);
        if (projectIds != null) q.in(InspectionRecord::getProjectId, projectIds);
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
}

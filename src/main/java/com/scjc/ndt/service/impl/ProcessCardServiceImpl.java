package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.dto.ProcessCardRequest;
import com.scjc.ndt.entity.*;
import com.scjc.ndt.mapper.*;
import com.scjc.ndt.service.ProcessCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessCardServiceImpl implements ProcessCardService {

    private final ProcessCardMapper mapper;
    private final UserProjectRelMapper userProjectRelMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper roleMapper;
    private final SysProjectMapper projectMapper;

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
            return null;
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
    public IPage<ProcessCard> list(Integer page, Integer size, Long projectId,
                                    String keyword, Long userId) {
        Page<ProcessCard> p = new Page<>(page, size);
        LambdaQueryWrapper<ProcessCard> q = new LambdaQueryWrapper<>();
        q.eq(projectId != null, ProcessCard::getProjectId, projectId)
         .like(StringUtils.hasText(keyword), ProcessCard::getProcessCardNo, keyword)
         .orderByDesc(ProcessCard::getCreateTime);

        List<Long> projectIds = getProjectIds(userId);
        if (projectIds != null && projectIds.isEmpty()) projectIds = Arrays.asList(-1L);
        if (projectIds != null) q.in(ProcessCard::getProjectId, projectIds);

        return mapper.selectPage(p, q);
    }

    @Override
    public ProcessCard create(ProcessCardRequest req, Long userId) {
        ProcessCard r = buildRecord(req);
        r.setCreateBy(userId);
        mapper.insert(r);
        return r;
    }

    @Override
    public ProcessCard update(Long id, ProcessCardRequest req, Long userId) {
        if (getRoleCodes(userId).contains("PROJECT_ADMIN")) {
            throw new BusinessException(403, "项目管理员无权修改工艺卡");
        }
        ProcessCard r = mapper.selectById(id);
        if (r == null) throw new BusinessException("工艺卡不存在");
        ProcessCard updated = buildRecord(req);
        updated.setId(id);
        updated.setCreateBy(r.getCreateBy());
        mapper.updateById(updated);
        return updated;
    }

    @Override
    public void delete(Long id, Long userId) {
        if (getRoleCodes(userId).contains("PROJECT_ADMIN")) {
            throw new BusinessException(403, "项目管理员无权删除工艺卡");
        }
        mapper.deleteById(id);
    }

    @Override
    public ProcessCard getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public int batchImport(List<ProcessCardRequest> records, Long projectId, Long userId) {
        int count = 0;
        for (ProcessCardRequest req : records) {
            req.setProjectId(projectId);
            create(req, userId);
            count++;
        }
        return count;
    }

    @Override
    public List<ProcessCard> export(Long projectId, Long userId) {
        LambdaQueryWrapper<ProcessCard> q = new LambdaQueryWrapper<>();
        q.eq(projectId != null, ProcessCard::getProjectId, projectId);
        List<Long> projectIds = getProjectIds(userId);
        if (projectIds != null && !projectIds.isEmpty()) q.in(ProcessCard::getProjectId, projectIds);
        return mapper.selectList(q);
    }

    private ProcessCard buildRecord(ProcessCardRequest req) {
        ProcessCard r = new ProcessCard();
        r.setProjectId(req.getProjectId());
        r.setProcessCardNo(req.getProcessCardNo());
        r.setSpecification(req.getSpecification());
        r.setTechniqueType(req.getTechniqueType());
        r.setIqiValue(req.getIqiValue());
        r.setIqiModel(req.getIqiModel());
        r.setEquipmentModel(req.getEquipmentModel());
        r.setEquipmentNo(req.getEquipmentNo());
        r.setFocalSpotSize(req.getFocalSpotSize());
        r.setIqiPosition(req.getIqiPosition());
        r.setFocalDistance(req.getFocalDistance());
        r.setPenetrationThickness(req.getPenetrationThickness());
        r.setTubeVoltage(req.getTubeVoltage());
        r.setExposureTime(req.getExposureTime());
        r.setTransilluminationLength(req.getTransilluminationLength());
        r.setExposureCount(req.getExposureCount());
        r.setFilmSpec(req.getFilmSpec());
        r.setDensityRange(req.getDensityRange());
        r.setTubeCurrent(req.getTubeCurrent());
        r.setFilmType(req.getFilmType());
        r.setWorkFilmDistance(req.getWorkFilmDistance());
        r.setTechLevel(req.getTechLevel());
        r.setHeatTreatmentStatus(req.getHeatTreatmentStatus());
        r.setLeadScreen(req.getLeadScreen());
        r.setSourceType(req.getSourceType());
        r.setExposureParam(req.getExposureParam());
        r.setTransilluminationParam(req.getTransilluminationParam());
        r.setFilmProcessing(req.getFilmProcessing());
        r.setDevelopmentTime(req.getDevelopmentTime());
        r.setDevelopmentTemperature(req.getDevelopmentTemperature());
        r.setSourceActivity(req.getSourceActivity());
        r.setFilmCount(req.getFilmCount());
        r.setRemark(req.getRemark());
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

package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.entity.ReportTemplate;
import com.scjc.ndt.mapper.ReportTemplateMapper;
import com.scjc.ndt.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final ReportTemplateMapper mapper;

    @Override
    public List<ReportTemplate> listTemplates(String methodType, Long projectId) {
        LambdaQueryWrapper<ReportTemplate> q = new LambdaQueryWrapper<>();
        q.eq(StringUtils.hasText(methodType), ReportTemplate::getMethodType, methodType)
         .and(w -> w.isNull(ReportTemplate::getProjectId).or().eq(ReportTemplate::getProjectId, projectId))
         .orderByAsc(ReportTemplate::getTemplateType);
        return mapper.selectList(q);
    }

    @Override
    public ReportTemplate getById(Long id) {
        ReportTemplate t = mapper.selectById(id);
        if (t == null) throw new BusinessException("模板不存在");
        return t;
    }

    @Override
    public ReportTemplate create(ReportTemplate template, Long userId) {
        template.setCreateBy(userId);
        mapper.insert(template);
        return template;
    }

    @Override
    public ReportTemplate update(Long id, ReportTemplate template) {
        if (mapper.selectById(id) == null) throw new BusinessException("模板不存在");
        template.setId(id);
        mapper.updateById(template);
        return template;
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }
}

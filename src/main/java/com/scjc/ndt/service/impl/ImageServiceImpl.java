package com.scjc.ndt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scjc.ndt.common.BusinessException;
import com.scjc.ndt.entity.SystemImage;
import com.scjc.ndt.mapper.SystemImageMapper;
import com.scjc.ndt.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final SystemImageMapper mapper;

    @Override
    public List<SystemImage> list(String techniqueType, Long projectId) {
        return mapper.selectList(
            new LambdaQueryWrapper<SystemImage>()
                .eq(StringUtils.hasText(techniqueType), SystemImage::getTechniqueType, techniqueType)
                .and(projectId != null,
                    w -> w.isNull(SystemImage::getProjectId).or().eq(SystemImage::getProjectId, projectId))
                .orderByDesc(SystemImage::getUploadTime)
        );
    }

    @Override
    public SystemImage upload(SystemImage image, Long userId) {
        image.setUploadBy(userId);
        image.setUploadTime(LocalDateTime.now());
        mapper.insert(image);
        return image;
    }

    @Override
    public void delete(Long id) {
        if (mapper.selectById(id) == null) throw new BusinessException("图片不存在");
        mapper.deleteById(id);
    }
}

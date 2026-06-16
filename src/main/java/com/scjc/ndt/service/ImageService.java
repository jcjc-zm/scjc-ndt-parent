package com.scjc.ndt.service;

import com.scjc.ndt.entity.SystemImage;
import java.util.List;

public interface ImageService {
    List<SystemImage> list(String techniqueType, Long projectId);
    SystemImage upload(SystemImage image, Long userId);
    void delete(Long id);
}

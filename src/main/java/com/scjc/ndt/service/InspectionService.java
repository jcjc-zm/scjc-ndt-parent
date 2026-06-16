package com.scjc.ndt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.dto.InspectionRequest;
import com.scjc.ndt.entity.InspectionRecord;

import java.util.List;

public interface InspectionService {
    IPage<InspectionRecord> list(Integer page, Integer size, Long projectId,
                                  String method, String level, String conclusion,
                                  String keyword, Long userId);
    InspectionRecord create(InspectionRequest request, Long userId);
    InspectionRecord update(Long id, InspectionRequest request);
    void delete(Long id);
    InspectionRecord getById(Long id);
    int batchImport(List<InspectionRequest> records, Long projectId, Long userId);
    List<InspectionRecord> export(Long projectId, String method, Long userId);
}

package com.scjc.ndt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.dto.ProcessCardRequest;
import com.scjc.ndt.entity.ProcessCard;

import java.util.List;

public interface ProcessCardService {

    IPage<ProcessCard> list(Integer page, Integer size, Long projectId, String keyword, Long userId);

    ProcessCard create(ProcessCardRequest request, Long userId);

    ProcessCard update(Long id, ProcessCardRequest request);

    void delete(Long id);

    ProcessCard getById(Long id);

    int batchImport(List<ProcessCardRequest> records, Long projectId, Long userId);

    List<ProcessCard> export(Long projectId, Long userId);
}

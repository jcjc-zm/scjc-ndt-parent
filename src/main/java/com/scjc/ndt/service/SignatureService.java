package com.scjc.ndt.service;

import com.scjc.ndt.dto.SignRequest;
import com.scjc.ndt.entity.SignatureRecord;
import java.util.List;

public interface SignatureService {
    List<SignatureRecord> getPending(Long userId);
    void sign(Long reportId, SignRequest request, Long userId);
    List<SignatureRecord> getHistory(Long reportId);
}

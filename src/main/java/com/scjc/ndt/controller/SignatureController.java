package com.scjc.ndt.controller;

import com.scjc.ndt.common.R;
import com.scjc.ndt.dto.SignRequest;
import com.scjc.ndt.entity.SignatureRecord;
import com.scjc.ndt.service.SignatureService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signatures")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;

    @GetMapping("/pending")
    public R<List<SignatureRecord>> pending(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(signatureService.getPending(userId));
    }

    @PostMapping("/{id}/sign")
    public R<Void> sign(@PathVariable Long id, @RequestBody SignRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        signatureService.sign(id, req, userId);
        return R.ok();
    }

    @GetMapping("/{id}/history")
    public R<List<SignatureRecord>> history(@PathVariable Long id) {
        return R.ok(signatureService.getHistory(id));
    }
}

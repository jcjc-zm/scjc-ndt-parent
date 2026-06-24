package com.scjc.ndt.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.common.R;
import com.scjc.ndt.dto.ProcessCardRequest;
import com.scjc.ndt.entity.ProcessCard;
import com.scjc.ndt.service.ProcessCardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/process-cards")
@RequiredArgsConstructor
public class ProcessCardController {

    private final ProcessCardService processCardService;

    @GetMapping("/list")
    public R<IPage<ProcessCard>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(processCardService.list(page, size, projectId, keyword, userId));
    }

    @GetMapping("/{id}")
    public R<ProcessCard> getById(@PathVariable Long id) {
        return R.ok(processCardService.getById(id));
    }

    @PostMapping
    public R<ProcessCard> create(@RequestBody ProcessCardRequest req,
                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(processCardService.create(req, userId));
    }

    @PutMapping("/{id}")
    public R<ProcessCard> update(@PathVariable Long id,
                                  @RequestBody ProcessCardRequest req,
                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(processCardService.update(id, req, userId));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        processCardService.delete(id, userId);
        return R.ok();
    }

    @PostMapping("/batch-import")
    public R<Integer> batchImport(@RequestParam Long projectId,
                                   @RequestBody List<ProcessCardRequest> records,
                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(processCardService.batchImport(records, projectId, userId));
    }

    @GetMapping("/export")
    public R<List<ProcessCard>> export(@RequestParam(required = false) Long projectId,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(processCardService.export(projectId, userId));
    }
}

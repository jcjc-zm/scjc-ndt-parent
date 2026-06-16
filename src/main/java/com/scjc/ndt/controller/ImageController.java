package com.scjc.ndt.controller;

import com.scjc.ndt.common.R;
import com.scjc.ndt.entity.SystemImage;
import com.scjc.ndt.service.ImageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @GetMapping("/list")
    public R<List<SystemImage>> list(@RequestParam(required = false) String techniqueType,
                                      @RequestParam(required = false) Long projectId) {
        return R.ok(imageService.list(techniqueType, projectId));
    }

    @PostMapping("/upload")
    public R<SystemImage> upload(@RequestBody SystemImage image, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(imageService.upload(image, userId));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        imageService.delete(id);
        return R.ok();
    }
}

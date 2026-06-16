package com.scjc.ndt.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReportRequest {
    private Long inspectionId;
    private Long templateId;
    private List<ImageSelection> imageSelections;

    @Data
    public static class ImageSelection {
        private String areaId;
        private Long imageId;
    }
}

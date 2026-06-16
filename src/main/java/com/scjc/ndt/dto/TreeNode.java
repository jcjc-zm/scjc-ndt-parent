package com.scjc.ndt.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class TreeNode {
    private String id;
    private String label;
    private String type;        // COMPANY / BU / PROJECT
    private Long projectId;
    private String projectCode;
    private String status;
    private List<TreeNode> children = new ArrayList<>();
}

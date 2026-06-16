package com.scjc.ndt.dto;

import lombok.Data;

@Data
public class PageQuery {
    private Integer page = 1;
    private Integer size = 20;
    private String keyword;
}

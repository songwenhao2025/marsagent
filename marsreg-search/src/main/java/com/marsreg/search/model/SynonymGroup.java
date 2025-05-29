package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class SynonymGroup {
    private String id;
    private List<String> terms;        // 同义词组中的词
    private String category;           // 分类
    private Double weight;             // 权重
    private Boolean enabled;           // 是否启用
    private String description;        // 描述
} 
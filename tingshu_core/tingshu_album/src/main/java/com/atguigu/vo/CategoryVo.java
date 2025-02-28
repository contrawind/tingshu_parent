package com.atguigu.vo;

import lombok.Data;

import java.util.List;

@Data
public class CategoryVo {
    private Long categoryId;           // 分类ID
    private String categoryName;       // 分类名称
    private List<CategoryVo> categoryChild;  // 子分类列表
}


package com.atguigu.service;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.vo.CategoryVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * VIEW 服务类
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
public interface BaseCategoryViewService extends IService<BaseCategoryView> {

    List<CategoryVo> getAllCategoryList();
}

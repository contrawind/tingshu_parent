package com.atguigu.mapper;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.vo.CategoryVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * VIEW Mapper 接口
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
@Mapper
public interface BaseCategoryViewMapper extends BaseMapper<BaseCategoryView> {

    List<CategoryVo> getAllCategoryList();
}

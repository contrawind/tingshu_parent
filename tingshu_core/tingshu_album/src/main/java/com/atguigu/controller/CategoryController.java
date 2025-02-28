package com.atguigu.controller;

import com.atguigu.entity.BaseAttribute;
import com.atguigu.login.TingShuLogin;
import com.atguigu.mapper.BaseAttributeMapper;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.CategoryVo;
import io.minio.MinioClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
@Tag(name = "分类管理")
@RestController
@RequestMapping(value = "/api/album/category")
@Slf4j
public class CategoryController {

    @Autowired
    private BaseCategoryViewService categoryViewService;

    @Autowired
    private BaseAttributeMapper propertyKeyMapper;

    @TingShuLogin(required = true)
    @Operation(summary = "获取全部分类信息")
    @GetMapping("getAllCategoryList")
    public RetVal getAllCategoryList() {
        try {
            log.info("开始获取分类列表");
            List<CategoryVo> categoryVoList = categoryViewService.getAllCategoryList();
            if (categoryVoList == null || categoryVoList.isEmpty()) {
                log.warn("未查询到分类数据");
                return RetVal.ok(new ArrayList<>());
            }
            log.info("成功获取到{}个一级分类", categoryVoList.size());
            return RetVal.ok(categoryVoList);
        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            return RetVal.fail().message("获取分类列表失败");
        }
    }

    @Operation(summary = "根据一级分类Id查询分类属性信息")
    @GetMapping("getPropertyByCategory1Id/{category1Id}")
    public RetVal getPropertyByCategory1Id(@PathVariable Long category1Id) {
        //分类属性集合列表
        List<BaseAttribute> categoryPropertyList=propertyKeyMapper.getPropertyByCategory1Id(category1Id);
        return RetVal.ok(categoryPropertyList);
    }

}

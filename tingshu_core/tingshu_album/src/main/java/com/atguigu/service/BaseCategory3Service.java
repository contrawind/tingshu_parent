package com.atguigu.service;

import com.atguigu.entity.BaseCategory3;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 三级分类表 服务类
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
public interface BaseCategory3Service extends IService<BaseCategory3> {

    List<BaseCategory3> getCategory3ListByCategory1Id(Long category1Id);


}

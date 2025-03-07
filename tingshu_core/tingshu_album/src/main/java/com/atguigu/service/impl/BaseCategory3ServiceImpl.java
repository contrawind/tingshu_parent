package com.atguigu.service.impl;

import com.atguigu.entity.BaseCategory2;
import com.atguigu.entity.BaseCategory3;
import com.atguigu.mapper.BaseCategory3Mapper;
import com.atguigu.service.BaseCategory2Service;
import com.atguigu.service.BaseCategory3Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 三级分类表 服务实现类
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
@Service
public class BaseCategory3ServiceImpl extends ServiceImpl<BaseCategory3Mapper, BaseCategory3> implements BaseCategory3Service {

    @Autowired
    private BaseCategory2Service category2Service;
    @Autowired
    private BaseCategory3Service category3Service;

    @Override
    public List<BaseCategory3> getCategory3ListByCategory1Id(Long category1Id) {
        //根据一级分类id查询二级分类信息
        LambdaQueryWrapper<BaseCategory2> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseCategory2::getCategory1Id, category1Id);
        //根据排序字段降序排序
        wrapper.select(BaseCategory2::getId);
        //根据OrderNum排序，谁的orderNum大，谁就排在前面
        wrapper.orderByDesc(BaseCategory2::getOrderNum);
        List<BaseCategory2> category2List = category2Service.list(wrapper);
        //根据二级分类id查找三级分类信息
        List<Long> category2IdList = category2List.stream().map(BaseCategory2::getId).collect(Collectors.toList());
        LambdaQueryWrapper<BaseCategory3> wrapper3 = new LambdaQueryWrapper<>();
        //所有id
        wrapper3.in(BaseCategory3::getCategory2Id, category2IdList);
        //只查询置顶的 ,并且只查询7条
        wrapper3.eq(BaseCategory3::getIsTop, 1).last("limit 7");
        return category3Service.list(wrapper3);
    }
}

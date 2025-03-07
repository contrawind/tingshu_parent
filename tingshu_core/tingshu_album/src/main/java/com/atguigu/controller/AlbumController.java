package com.atguigu.controller;


import com.atguigu.entity.AlbumAttributeValue;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.login.TingShuLogin;
import com.atguigu.mapper.AlbumInfoMapper;
import com.atguigu.query.AlbumInfoQuery;
import com.atguigu.result.RetVal;
import com.atguigu.service.AlbumAttributeValueService;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.AlbumTempVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专辑管理控制器
 * <p>
 * 实现专辑相关的增删改查功能，包含：
 * 1. 新增专辑
 * 2. 用户专辑分页查询
 * </p>
 */
@Tag(name = "专辑管理") // Swagger文档分类标签
@RestController // 声明为RESTful控制器
@RequestMapping(value = "/api/album/albumInfo") // 基础请求路径
public class AlbumController {
    @Autowired
    private AlbumInfoService albumInfoService; // 专辑服务层接口

    @Autowired
    private AlbumInfoMapper albumInfoMapper; // 专辑数据访问层接口

    /**
     * 新增专辑接口
     *
     * @param albumInfo 通过请求体接收的专辑信息JSON对象
     * @return 统一响应格式（操作成功）
     */
    @TingShuLogin // 自定义登录验证注解（需要登录才能访问）
    @Operation(summary = "新增专辑") // Swagger操作描述
    @PostMapping("saveAlbumInfo") // 处理POST请求
    public RetVal saveAlbumInfo(@RequestBody AlbumInfo albumInfo) {
        // 调用服务层保存专辑信息
        albumInfoService.saveAlbumInfo(albumInfo);
        // 返回操作成功响应
        return RetVal.ok();
    }

    /**
     * 用户专辑分页查询接口
     *
     * @param pageNum        当前页码（从路径获取）
     * @param pageSize       每页显示条数（从路径获取）
     * @param albumInfoQuery 查询条件对象（从请求体获取，可选）
     * @return 带分页数据的统一响应格式
     */
    @TingShuLogin // 需要登录验证
    @Operation(summary = "分页查询专辑信息")
    @PostMapping("getUserAlbumByPage/{pageNum}/{pageSize}") // 组合路径参数和请求体参数
    public RetVal getUserAlbumByPage(
            @Parameter(name = "pageNum", description = "当前页码", required = true) // Swagger参数说明
            @PathVariable Long pageNum,
            @Parameter(name = "pageSize", description = "每页记录数", required = true)
            @PathVariable Long pageSize,
            @Parameter(name = "albumInfoQuery", description = "查询对象", required = false)
            @RequestBody AlbumInfoQuery albumInfoQuery) {

        // 从安全上下文获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        albumInfoQuery.setUserId(userId); // 设置查询条件中的用户ID

        // 创建分页参数对象
        IPage<AlbumTempVo> pageParam = new Page<>(pageNum, pageSize);
        // 执行分页查询
        pageParam = albumInfoMapper.getUserAlbumByPage(pageParam, albumInfoQuery);

        // 返回分页结果
        return RetVal.ok(pageParam);
    }

    @Operation(summary = "根据id查询专辑信息")
    @GetMapping("getAlbumInfoById/{albumId}")
    public RetVal<AlbumInfo> getAlbumInfoById(@PathVariable Long albumId) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfoById(albumId);
        return RetVal.ok(albumInfo);
    }

    @Operation(summary = "修改专辑")
    @PutMapping("updateAlbumInfo")
    public RetVal updateAlbumInfo(@RequestBody AlbumInfo albumInfo) {
        albumInfoService.updateAlbumInfo(albumInfo);
        //修改成功之后应该返回true或false
        return RetVal.ok();
    }

    @Operation(summary = "删除专辑")
    @DeleteMapping("deleteAlbumInfo/{albumId}")
    public RetVal deleteAlbumInfo(@PathVariable Long albumId) {
        albumInfoService.deleteAlbumInfo(albumId);
        return RetVal.ok();
    }

    /**
     * 搜索模块
     *
     * @param albumId
     * @return
     */
    @Autowired
    private AlbumAttributeValueService albumPropertyValueService;

    @Operation(summary = "根据albumId查询专辑属性值")
    @GetMapping("getAlbumPropertyValue/{albumId}")
    public List<AlbumAttributeValue> getAlbumPropertyValue(@PathVariable Long albumId) {
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        List<AlbumAttributeValue> attributeValueList = albumPropertyValueService.list(wrapper);
        return attributeValueList;
    }
}
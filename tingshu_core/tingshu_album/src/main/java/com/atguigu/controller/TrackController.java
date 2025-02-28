package com.atguigu.controller;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.BaseAttribute;
import com.atguigu.entity.TrackInfo;
import com.atguigu.login.TingShuLogin;
import com.atguigu.mapper.BaseAttributeMapper;
import com.atguigu.mapper.TrackInfoMapper;
import com.atguigu.query.AlbumInfoQuery;
import com.atguigu.query.TrackInfoQuery;
import com.atguigu.result.RetVal;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.service.TrackInfoService;
import com.atguigu.service.VodService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.AlbumTempVo;
import com.atguigu.vo.CategoryVo;
import com.atguigu.vo.TrackTempVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
@Tag(name = "声音管理")
@RestController
@RequestMapping(value = "/api/album/trackInfo")
public class TrackController {
    @Autowired
    private AlbumInfoService albumInfoService;
    @Autowired
    private VodService vodService;
    @TingShuLogin
    @Operation(summary = "根据用户ID查询用户的专辑信息")
    @GetMapping("findAlbumByUserId")
    public RetVal findAlbumByUserId() {
        Long userId = AuthContextHolder.getUserId();
        //根据用户ID查询专辑信息
        LambdaQueryWrapper<AlbumInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumInfo::getUserId,userId);
        //查询专辑ID和专辑名称
        wrapper.select(AlbumInfo::getId,AlbumInfo::getAlbumTitle);
        List<AlbumInfo> albumInfoList = albumInfoService.list(wrapper);
        return RetVal.ok(albumInfoList);
    }

    @Operation(summary = "上传声音")
    @PostMapping("uploadTrack")
    public RetVal uploadTrack(MultipartFile file) {
        Map<String, Object> retMap = vodService.uploadTrack(file);
        return RetVal.ok(retMap);
    }

    @Autowired
    private TrackInfoService trackInfoService;
    @Operation(summary = "新增声音")
    @TingShuLogin
    @PostMapping("saveTrackInfo")
    public RetVal saveTrackInfo(@RequestBody TrackInfo trackInfo) {//传的是一个声音的对象
        trackInfoService.saveTrackInfo(trackInfo);
        //返回true或false
        return RetVal.ok();
    }

    @Autowired
    private TrackInfoMapper trackInfoMapper;
    @TingShuLogin
    @Operation(summary = "分页查询声音")
    @PostMapping("findUserTrackPage/{pageNum}/{pageSize}")
    public RetVal findUserTrackPage(
            @PathVariable Long pageNum,
            @PathVariable Long pageSize,
            @RequestBody TrackInfoQuery trackInfoQuery) {
        trackInfoQuery.setUserId( AuthContextHolder.getUserId());
        //分页
        IPage<TrackTempVo> pageParam = new Page<>(pageNum, pageSize);
        pageParam = trackInfoMapper.findUserTrackPage(pageParam, trackInfoQuery);
        return RetVal.ok(pageParam);
    }
    @Operation(summary = "根据id获取声音信息")
    @GetMapping("getTrackInfoById/{trackId}")
    public RetVal getTrackInfoById(@PathVariable Long trackId) {
        TrackInfo trackInfo = trackInfoService.getById(trackId);
        return RetVal.ok(trackInfo);
    }

    @Operation(summary = "修改声音")
    @PutMapping("updateTrackInfoById")
    public RetVal updateTrackInfoById(@RequestBody TrackInfo trackInfo) {
        trackInfoService.updateTrackInfoById(trackInfo);
        return RetVal.ok();
    }
    @Operation(summary = "删除声音")
    @DeleteMapping("deleteTrackInfo/{trackId}")
    public RetVal deleteTrackInfo(@PathVariable Long trackId) {
        trackInfoService.deleteTrackInfo(trackId);
        return RetVal.ok();
    }

}

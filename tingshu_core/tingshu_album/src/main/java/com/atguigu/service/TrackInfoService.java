package com.atguigu.service;

import com.atguigu.entity.TrackInfo;
import com.atguigu.vo.AlbumTrackListVo;
import com.atguigu.vo.TrackTempVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 声音信息 服务类
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
public interface TrackInfoService extends IService<TrackInfo> {

    void saveTrackInfo(TrackInfo trackInfo);

    void updateTrackInfoById(TrackInfo trackInfo);

    void deleteTrackInfo(Long trackId);

    IPage<AlbumTrackListVo> getAlbumDetailTrackByPage(@Param("pageParam") IPage<AlbumTrackListVo> pageParam, @Param("albumId") Long albumId);

    List<TrackTempVo> getTrackVoList(List<Long> trackIdList);

    List<Map<String, Object>> getTrackListToChoose(Long trackId);

    List<TrackInfo> getTrackListPrepareToBuy(Long trackId, Integer buyNum);
}

package com.atguigu.service;

import com.atguigu.entity.TrackInfo;
import com.baomidou.mybatisplus.extension.service.IService;

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
}

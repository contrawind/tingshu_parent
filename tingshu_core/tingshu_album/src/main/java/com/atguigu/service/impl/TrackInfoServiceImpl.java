package com.atguigu.service.impl;

import com.atguigu.constant.SystemConstant;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.TrackInfo;
import com.atguigu.entity.TrackStat;
import com.atguigu.mapper.TrackInfoMapper;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.TrackInfoService;
import com.atguigu.service.TrackStatService;
import com.atguigu.service.VodService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.AlbumTrackListVo;
import com.atguigu.vo.TrackTempVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 声音信息 服务实现类
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
@Service
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {
    @Autowired
    private VodService vodService;
    @Autowired
    private AlbumInfoService albumInfoService;

    @Autowired
    private TrackStatService trackStatService;

    @Transactional
    @Override
    public void saveTrackInfo(TrackInfo trackInfo) {
        trackInfo.setUserId(AuthContextHolder.getUserId());
        trackInfo.setStatus(SystemConstant.TRACK_APPROVED);
        vodService.getTrackMediaInfo(trackInfo);
        //查询专辑中声音编号最大的值
        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId());
        wrapper.orderByAsc(TrackInfo::getOrderNum);
        wrapper.select(TrackInfo::getOrderNum);
        wrapper.last("limit 1");
        TrackInfo maxOrderNumTrackInfo = getOne(wrapper);
        int orderNum = 1;
        if (maxOrderNumTrackInfo != null) {
            orderNum = maxOrderNumTrackInfo.getOrderNum() + 1;
        }
        trackInfo.setOrderNum(orderNum);
        //保存声音
        save(trackInfo);
        //更新专辑中的声音数量
        //查到专辑信息
        AlbumInfo albumInfo = albumInfoService.getById(trackInfo.getAlbumId());
        //专辑中的声音数量+1
        int includeTrackCount = albumInfo.getIncludeTrackCount() + 1;
        albumInfo.setIncludeTrackCount(includeTrackCount);
        albumInfoService.updateAlbumInfo(albumInfo);
        //声音的初始化数据
        List<TrackStat> trackStatList = buildTrackStatData(trackInfo.getId());
        //保存声音统计信息
        trackStatService.saveBatch(trackStatList);

    }

    @Override
    public void updateTrackInfoById(TrackInfo trackInfo) {
        vodService.getTrackMediaInfo(trackInfo);
        updateById(trackInfo);
    }

    @Transactional
    @Override
    public void deleteTrackInfo(Long trackId) {
        //更新专辑声音个数
        //查到声音信息
        TrackInfo trackInfo = getById(trackId);
        AlbumInfo albumInfo = albumInfoService.getById(trackInfo.getAlbumId());
        int includeTrackCount = albumInfo.getIncludeTrackCount() - 1;
        albumInfo.setIncludeTrackCount(includeTrackCount);
        albumInfoService.updateById(albumInfo);
        removeById(trackId);
        //删除统计信息
        trackStatService.remove(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, trackId));
        //删除声音
        vodService.removeTrack(trackInfo.getMediaFileId());
    }

    @Override
    public IPage<AlbumTrackListVo> getAlbumDetailTrackByPage(IPage<AlbumTrackListVo> pageParam, Long albumId) {
        pageParam = baseMapper.getAlbumTrackAndStatInfo(pageParam, albumId);
        List<AlbumTrackListVo> albumTrackVoList = pageParam.getRecords();
        AlbumInfo albumInfo = albumInfoService.getById(albumId);
        Long userId = AuthContextHolder.getUserId();
        //如果用户没有登录
        if (userId == null) {
            //不是免费的专辑
            if (!SystemConstant.FREE_ALBUM.equals(albumInfo.getPayType())) {
                //获取付费的声音列表
                List<AlbumTrackListVo> albumTrackNeedPayList = albumTrackVoList.stream().filter(f -> f.getOrderNum().intValue() > albumInfo.getTracksForFree().intValue())
                        .collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(albumTrackNeedPayList)) {
                    albumTrackNeedPayList.forEach(f -> f.setIsShowPaidMark(true));
                }
            }
        }
        return pageParam;
    }

    private List<TrackStat> buildTrackStatData(Long trackId) {
        List<TrackStat> trackStatList = new ArrayList<>();
        initTrackStat(trackId, trackStatList, SystemConstant.PLAY_NUM_TRACK);
        initTrackStat(trackId, trackStatList, SystemConstant.COLLECT_NUM_TRACK);
        initTrackStat(trackId, trackStatList, SystemConstant.PRAISE_NUM_TRACK);
        initTrackStat(trackId, trackStatList, SystemConstant.COMMENT_NUM_TRACK);
        return trackStatList;
    }

    private static void initTrackStat(Long trackId, List<TrackStat> trackStatList, String statType) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statType);
        trackStat.setStatNum(0);
        trackStatList.add(trackStat);
    }
}

package com.atguigu.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.constant.RedisConstant;
import com.atguigu.constant.SystemConstant;
import com.atguigu.entity.AlbumAttributeValue;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.AlbumStat;
import com.atguigu.mapper.AlbumInfoMapper;
import com.atguigu.service.AlbumAttributeValueService;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.AlbumStatService;
import com.atguigu.util.AuthContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 专辑信息 服务实现类
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
@Service
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {
    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;
    @Autowired
    private AlbumStatService albumStatService;

    @Override
    public AlbumInfo getAlbumInfoById(Long albumId) {
        //根据主键id查询id信息
        // AlbumInfo albumInfo = getAlbumInfoFromDB(albumId);
        //融入redis
        AlbumInfo albumInfo = getAlbumInfoFromRedis(albumId);
        return albumInfo;
    }

    @Autowired
    private RedisTemplate redisTemplate;

    private AlbumInfo getAlbumInfoFromRedis(Long albumId) {
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        //定义key
        String cacheKey = RedisConstant.ALBUM_INFO_PREFIX + albumId;
        AlbumInfo albumInfoRedis = (AlbumInfo) redisTemplate.opsForValue().get(cacheKey);
        if (albumInfoRedis == null) {
            AlbumInfo albumInfoDB = getAlbumInfoFromDB(albumId);
            redisTemplate.opsForValue().set(cacheKey, albumInfoDB);
            return albumInfoDB;
        }
        return albumInfoRedis;
    }

    private @NotNull AlbumInfo getAlbumInfoFromDB(Long albumId) {
        AlbumInfo albumInfo = getById(albumId);
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueService.list(wrapper);
        albumInfo.setAlbumPropertyValueList(albumAttributeValueList);
        return albumInfo;
    }

    @Override
    public void updateAlbumInfo(AlbumInfo albumInfo) {
        //修改专辑基本信息
        updateById(albumInfo);
        //删除原有专辑标签属性信息
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumInfo.getId());
        albumAttributeValueService.remove(wrapper);
        //保存专辑标签属性
        List<AlbumAttributeValue> albumPropertyValueList = albumInfo.getAlbumPropertyValueList();
        if (!CollectionUtils.isEmpty(albumPropertyValueList)) {
            for (AlbumAttributeValue albumAttributeValue : albumPropertyValueList) {
                //设置专辑id
                albumAttributeValue.setAlbumId(albumInfo.getId());
            }
            albumAttributeValueService.saveBatch(albumPropertyValueList);
        }
        //TODO
    }


    @Override
    public void deleteAlbumInfo(Long albumId) {
        //删除专辑基本信息
        removeById(albumId);
        //删除专辑标签属性信息
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        albumAttributeValueService.remove(wrapper);
        //删除专辑统计信息
        albumStatService.remove(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, albumId));
        //TODO
    }

    @Transactional//写入要么全部成功，要么全部回滚，避免因部分失败导致脏数据或逻辑错误
    @Override
    public void saveAlbumInfo(AlbumInfo albumInfo) {
        Long userId = AuthContextHolder.getUserId();
        albumInfo.setUserId(userId);
        //默认审核通过
        albumInfo.setStatus(SystemConstant.ALBUM_APPROVED);
        //付费专辑前5集免费
        //判断专辑是否付费
        if (!SystemConstant.FREE_ALBUM.equals(albumInfo.getPayType())) {//收费

            albumInfo.setTracksForFree(5);
        }
        //保存专辑的基本信息
        save(albumInfo);
        //保存专辑标签属性
        List<AlbumAttributeValue> albumPropertyValueList = albumInfo.getAlbumPropertyValueList();
        if (!CollectionUtils.isEmpty(albumPropertyValueList)) {
            for (AlbumAttributeValue albumAttributeValue : albumPropertyValueList) {
                //设置专辑id
                albumAttributeValue.setAlbumId(albumInfo.getId());
                //albumAttributeValueService.save(albumAttributeValue);
            }
            albumAttributeValueService.saveBatch(albumPropertyValueList);
        }
        //保存专辑的统计信息
        List<AlbumStat> albumStatList = buildAlbumStatData(albumInfo.getId());
        albumStatService.saveBatch(albumStatList);
        //todo
    }

    //初始化专辑统计信息
    private List<AlbumStat> buildAlbumStatData(Long albumId) {
        ArrayList<AlbumStat> albumStatList = new ArrayList<>();
        //初始化专辑统计信息
        initAlbumStat(albumId, albumStatList, SystemConstant.PLAY_NUM_ALBUM);
        initAlbumStat(albumId, albumStatList, SystemConstant.SUBSCRIBE_NUM_ALBUM);
        initAlbumStat(albumId, albumStatList, SystemConstant.BUY_NUM_ALBUM);
        initAlbumStat(albumId, albumStatList, SystemConstant.COMMENT_NUM_ALBUM);
        return albumStatList;
    }

    private static void initAlbumStat(Long albumId, ArrayList<AlbumStat> albumStatList, String statType) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(0);
        albumStatList.add(albumStat);
    }
}

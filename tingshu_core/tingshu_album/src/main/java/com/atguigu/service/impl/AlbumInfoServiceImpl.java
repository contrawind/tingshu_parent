package com.atguigu.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.cache.TingShuCache;
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
import com.atguigu.util.SleepUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    @TingShuCache("albumInfo")
    @Override
    public AlbumInfo getAlbumInfoById(Long albumId) {
        AlbumInfo albumInfo = getAlbumInfoFromDB(albumId);
        //AlbumInfo albumInfo = getAlbumInfoFromRedis(albumId);
        //AlbumInfo albumInfo = getAlbumInfoFromRedisWitThreadLocal(albumId);
        //AlbumInfo albumInfo = getAlbumInfoFromRedisson(albumId);
        return albumInfo;
    }

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter bloomFilter;

    private AlbumInfo getAlbumInfoFromRedisson(Long albumId) {


        String cacheKey = RedisConstant.ALBUM_INFO_PREFIX + albumId;
        AlbumInfo albumInfoRedis = (AlbumInfo) redisTemplate.opsForValue().get(cacheKey);
        String lockKey = "lock" + albumId;
        RLock lock = redissonClient.getLock(lockKey);
        redissonClient.getLock(lockKey);
        if (albumInfoRedis == null) {
            lock.lock();//加锁

            try {
                boolean flag = bloomFilter.contains(albumId);
                if (flag) {
                    AlbumInfo albumInfoDB = getAlbumInfoFromDB(albumId);
                    redisTemplate.opsForValue().set(cacheKey, albumInfoDB);
                    return albumInfoDB;
                }
            } finally {
                lock.unlock();//解锁
            }
        }
        return albumInfoRedis;
    }


    ThreadLocal<String> threadLocal = new ThreadLocal<>();

//    private AlbumInfo getAlbumInfoFromRedisWitThreadLocal(Long albumId) {
//        String cacheKey = RedisConstant.ALBUM_INFO_PREFIX + albumId;
//        AlbumInfo albumInfoRedis = (AlbumInfo) redisTemplate.opsForValue().get(cacheKey);
//        //锁的粒度太大
//        String lockKey = "lock-" + albumId;
//        if (albumInfoRedis == null) {
//            String token = threadLocal.get();
//            boolean accquireLock = false;
//            if (!StringUtils.isEmpty(token)) {
//                accquireLock = true;
//            } else {
//                token = UUID.randomUUID().toString();
//                accquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 3, TimeUnit.SECONDS);
//            }
//            if (accquireLock) {
//                AlbumInfo albumInfoDB = getAlbumInfoFromDB(albumId);
//                redisTemplate.opsForValue().set(cacheKey, albumInfoDB);
//                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
//                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
//                redisScript.setScriptText(luaScript);
//                redisScript.setResultType(Long.class);
//                redisTemplate.execute(redisScript, Arrays.asList(lockKey), token);
//                //擦屁股
//                threadLocal.remove();
//                return albumInfoDB;
//            } else {
//                while (true) {
//                    SleepUtils.millis(50);
//                    boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 3, TimeUnit.SECONDS);
//                    if (retryAccquireLock) {
//                        threadLocal.set(token);
//                        break;
//                    }
//                }
//                return getAlbumInfoFromRedisWitThreadLocal(albumId);
//            }
//        }
//        return albumInfoRedis;
//
//
//    }

    @Autowired
    private RedisTemplate redisTemplate;

    private AlbumInfo getAlbumInfoFromRedis(Long albumId) {
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
//        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        String cacheKey = RedisConstant.ALBUM_INFO_PREFIX + albumId;
        AlbumInfo albumInfoRedis = (AlbumInfo) redisTemplate.opsForValue().get(cacheKey);
        if (albumInfoRedis == null) {
            AlbumInfo albumInfoDB = getAlbumInfoFromDB(albumId);
            redisTemplate.opsForValue().set(cacheKey, albumInfoDB);
            return albumInfoDB;
        }
        return albumInfoRedis;
    }

    @NotNull
    private AlbumInfo getAlbumInfoFromDB(Long albumId) {
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

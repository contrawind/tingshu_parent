package com.atguigu.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.constant.KafkaConstant;
import com.atguigu.constant.SystemConstant;
import com.atguigu.entity.UserCollect;
import com.atguigu.entity.UserListenProcess;
import com.atguigu.service.KafkaService;
import com.atguigu.service.ListenService;
import com.atguigu.service.TrackInfoService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.MongoUtil;
import com.atguigu.vo.TrackStatMqVo;
import com.atguigu.vo.TrackTempVo;
import com.atguigu.vo.UserCollectVo;
import com.atguigu.vo.UserListenProcessVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ListenServiceImpl implements ListenService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Map<String, Object> getRecentlyPlay() {
        //获取用户id
        Long userId = AuthContextHolder.getUserId();
        Query query = Query.query(Criteria.where("userId").is(userId));
        Sort sort = Sort.by(Sort.Direction.DESC, "updateTime");
        query.with(sort);
        //查询mongodb中是否有用户播放记录
        UserListenProcess listenProcess = mongoTemplate.findOne(query, UserListenProcess.class,
                MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        if (listenProcess == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("albumId", listenProcess.getAlbumId());
        map.put("trackId", listenProcess.getTrackId());
        return map;

    }

    @Override
    public void updatePlaySecond(UserListenProcessVo userListenProcessVo) {
        //获取用户id
        Long userId = AuthContextHolder.getUserId();
        //查询mongodb中是否有用户播放记录
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId()));
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class,
                MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        if (userListenProcess != null) {
            //把播放进度信息存入起来  存储在mongodb中
            userListenProcess = new UserListenProcess();
            BeanUtils.copyProperties(userListenProcessVo, userListenProcess);
            userListenProcess.setId(ObjectId.get().toString());//可以生成一个id的对象
            userListenProcess.setUserId(userId);
            userListenProcess.setIsShow(1);
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setUpdateTime(new Date());
            mongoTemplate.save(userListenProcess, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        } else {
            //更新mongodb中的播放记录
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            userListenProcess.setUpdateTime(new Date());
            mongoTemplate.save(userListenProcess, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        }
        //更新播放次数  同一个专辑同一个用户同一个声音 每天只记录一次播放量
        String key = "user:track" + new DateTime().toString("yyyyMMdd") + ":" + userListenProcessVo.getTrackId();
        Boolean isExist = redisTemplate.opsForValue().getBit(key, userId);
        if (!isExist) {
            redisTemplate.opsForValue().setBit(key, userId, true);
            //设置一天后过期
            //LocalDateTime localDateTime = LocalDateTime.now().plusDays(1);

            redisTemplate.expire(key, 1, TimeUnit.DAYS);
            //发送消息增加播放量
            TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
            trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-", ""));
            trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
            trackStatMqVo.setTarckId(userListenProcessVo.getTrackId());
            trackStatMqVo.setStatType(SystemConstant.PLAY_NUM_TRACK);
            trackStatMqVo.setCount(1);
            kafkaService.sendMessage(KafkaConstant.UPDATE_TRACK_STAT_QUEUE, JSON.toJSONString(trackStatMqVo));
        }
    }

    @Override
    public BigDecimal getLastPlaySecond(Long trackId) {
        //获取用户id
        Long userId = AuthContextHolder.getUserId();
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        //查询mongodb中是否有用户播放记录
        UserListenProcess listenProcess = mongoTemplate.findOne(query, UserListenProcess.class,
                MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        if (listenProcess != null) {
            return listenProcess.getBreakSecond();
        }
        return new BigDecimal(0);
    }

    @Override
    public boolean collectTrack(Long trackId) {
        //获取用户id
        Long userId = AuthContextHolder.getUserId();
        //查询mongodb中是否有用户收藏记录
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        long count = mongoTemplate.count(query, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
        //判断是否已经收藏
        if (count == 0) {
            UserCollect userCollect = new UserCollect();
            userCollect.setId(ObjectId.get().toString());
            userCollect.setUserId(userId);
            userCollect.setTrackId(trackId);
            userCollect.setCreateTime(new Date());
            mongoTemplate.save(userCollect, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
            //更新声音点赞量
            TrackStatMqVo trackStatVo = new TrackStatMqVo();
            trackStatVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-", ""));
            trackStatVo.setTarckId(trackId);
            trackStatVo.setStatType(SystemConstant.COLLECT_NUM_TRACK);
            trackStatVo.setCount(1);
            kafkaService.sendMessage(KafkaConstant.UPDATE_TRACK_STAT_QUEUE, JSON.toJSONString(trackStatVo));
            return true;
        } else {
            mongoTemplate.remove(query, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
            TrackStatMqVo trackStatVo = new TrackStatMqVo();
            trackStatVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-", ""));
            trackStatVo.setTarckId(trackId);
            trackStatVo.setStatType(SystemConstant.COLLECT_NUM_TRACK);
            trackStatVo.setCount(-1);
            kafkaService.sendMessage(KafkaConstant.UPDATE_TRACK_STAT_QUEUE, JSON.toJSONString(trackStatVo));
            return false;
        }
    }

    @Override
    public boolean isCollect(Long trackId) {
        //获取用户id
        Long userId = AuthContextHolder.getUserId();
        //查询mongodb中是否有用户收藏记录
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        long count = mongoTemplate.count(query, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
        if (count > 0) {
            return true;
        }
        return false;
    }

    @Autowired
    private TrackInfoService trackInfoService;

    @Override
    public IPage<UserCollectVo> getUserCollectByPage(Integer pageNum, Integer pageSize) {
        Long userId = AuthContextHolder.getUserId();
        Query query = Query.query(Criteria.where("userId").is(userId));
        //这里pageNum要减1
        PageRequest pageable = PageRequest.of(pageNum - 1, pageSize);
        query.with(pageable);
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        query.with(sort);
        List<UserCollect> userCollectList = mongoTemplate.find(query, UserCollect.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
        long total = mongoTemplate.count(query, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
        List<Long> trackIdList = userCollectList.stream().map(UserCollect::getTrackId).collect(Collectors.toList());
        List<UserCollectVo> userCollectVoList = null;
        if (!CollectionUtils.isEmpty(trackIdList)) {
            List<TrackTempVo> trackVoList = trackInfoService.getTrackVoList(trackIdList);
            Map<Long, TrackTempVo> trackTempVoMap = trackVoList.stream().collect(Collectors.toMap(TrackTempVo::getTrackId, trackVo -> trackVo));
            userCollectVoList = userCollectList.stream().map(userCollect -> {
                UserCollectVo userCollectVo = new UserCollectVo();
                Long trackId = userCollect.getTrackId();
                TrackTempVo trackTempVo = trackTempVoMap.get(trackId);
                BeanUtils.copyProperties(trackTempVo, userCollectVo);
                userCollectVo.setTrackId(trackId);
                userCollectVo.setCreateTime(userCollect.getCreateTime());
                return userCollectVo;
            }).collect(Collectors.toList());
        }
        return new Page(pageNum, pageSize, total).setRecords(userCollectVoList);
    }
}


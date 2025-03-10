package com.atguigu.service.impl;

import com.atguigu.entity.UserInfo;
import com.atguigu.entity.UserPaidAlbum;
import com.atguigu.entity.UserPaidTrack;
import com.atguigu.mapper.UserInfoMapper;
import com.atguigu.service.UserInfoService;
import com.atguigu.service.UserPaidAlbumService;
import com.atguigu.service.UserPaidTrackService;
import com.atguigu.util.AuthContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {
    @Autowired
    private UserPaidAlbumService userPaidAlbumService;
    @Autowired
    private UserPaidTrackService userPaidTrackService;

    @Override
    public HashMap<Long, Boolean> getUserShowPaidMarkOrNot(Long albumId, List<Long> needPayTrackIdList) {
        HashMap<Long, Boolean> showPaidMarkMap = new HashMap<>();
        Long userId = AuthContextHolder.getUserId();
        //查询用户购买过的专辑
        LambdaQueryWrapper<UserPaidAlbum> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidAlbum::getUserId, userId);
        wrapper.eq(UserPaidAlbum::getAlbumId, albumId);
        UserPaidAlbum userPaidAlbum = userPaidAlbumService.getOne(wrapper);
        if (userPaidAlbum != null) {
            //这个专辑里面的声音用户都不需要付费
            needPayTrackIdList.forEach(trackId -> {
                showPaidMarkMap.put(trackId, false);
            });
            return showPaidMarkMap;
        } else {
            //用户购买过的声音
            LambdaQueryWrapper<UserPaidTrack> paidTrackWrapper = new LambdaQueryWrapper<>();
            paidTrackWrapper.eq(UserPaidTrack::getUserId, userId);
            paidTrackWrapper.in(UserPaidTrack::getTrackId, needPayTrackIdList);
            //查询出已经支付的声音列表
            List<UserPaidTrack> userPaidTrackList = userPaidTrackService.list(paidTrackWrapper);
            List<Long> paidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
            needPayTrackIdList.forEach(trackId -> {
                if (paidTrackIdList.contains(trackId)) {
                    //已购买声音  不需要显示购买图标
                    showPaidMarkMap.put(trackId, false);
                } else {
                    //未购买声音  需要显示购买图标
                    showPaidMarkMap.put(trackId, true);
                }
            });
        }
        return showPaidMarkMap;
    }
}

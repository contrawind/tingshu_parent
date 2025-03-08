package com.atguigu.service;

import com.atguigu.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * 用户 服务类
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
public interface UserInfoService extends IService<UserInfo> {

    HashMap<Long, Boolean> getUserShowPaidMarkOrNot(Long albumId, List<Long> needPayTrackIdList);
}

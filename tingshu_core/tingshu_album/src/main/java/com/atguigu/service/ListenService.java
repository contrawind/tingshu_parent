package com.atguigu.service;

import com.atguigu.vo.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface ListenService {

    Map<String, Object> getRecentlyPlay();

    void updatePlaySecond(UserListenProcessVo userListenProcessVo);

    BigDecimal getLastPlaySecond(Long trackId);
}

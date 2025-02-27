package com.atguigu.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.constant.RedisConstant;
import com.atguigu.entity.UserInfo;
import com.atguigu.login.TingShuLogin;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserInfoService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.atguigu.constant.RedisConstant.USER_LOGIN_KEY_TIMEOUT;

/**
 * <p>
 * vip服务配置表 前端控制器
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
@RestController
@RequestMapping("/api/user/wxLogin")
public class WxLoginController {

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Operation(summary = "小程序授权登录接口")
    @GetMapping("wxLogin/{code}")
    public RetVal wxLogin(@PathVariable String code) throws Exception {
        //调用微信登录接口 获取用户的openid
        WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
        String openid = sessionInfo.getOpenid();
        //从数据库中查询用户信息是否存在SELECT * FROM `user_info` WHERE wx_open_id='odo3j4qjcVhjHxPX4A4bmmyVJ4O0'
        //每个人的openid都是唯一的  只能查出一个
        //QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getWxOpenId, openid);
        UserInfo userInfo = userInfoService.getOne(wrapper);
        //如果认证失败， 不存在 往数据库中添加
        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setNickname("听友" + System.currentTimeMillis());
            userInfo.setAvatarUrl("https:baidu.com");
            userInfo.setWxOpenId(openid);
            //是否为会员
            userInfo.setIsVip(0);
            userInfoService.save(userInfo);
        }

        //如果认证成功 存在  往redis中添加用户信息
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String userKey = RedisConstant.USER_LOGIN_KEY_PREFIX + uuid;
        redisTemplate.opsForValue().set(userKey, userInfo, USER_LOGIN_KEY_TIMEOUT, TimeUnit.MINUTES);
        //返回一个token给前端
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("token", uuid);
        return RetVal.ok(retMap);
    }
    @TingShuLogin   //要获取个人信息要登录
    @Operation(summary = "获取用户个人信息")
    @GetMapping("/getUserInfo")
    public RetVal getUserInfo() {
        Long userId = AuthContextHolder.getUserId();
        UserInfo userInfo = userInfoService.getById(userId);
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo, userInfoVo);
        return RetVal.ok(userInfoVo);
    }

}

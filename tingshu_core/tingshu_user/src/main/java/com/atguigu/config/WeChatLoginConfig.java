package com.atguigu.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeChatLoginConfig {
    @Autowired
    private WeChatProperties wxChatProperties;

    //交给了spring管理
    @Bean
    public WxMaService wxMaService() {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        //设置微信小程序的appid和appsecret
        config.setAppid(wxChatProperties.getAppId());
        config.setSecret(wxChatProperties.getAppSecret());
        config.setMsgDataFormat("JSON");
        WxMaServiceImpl service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
    //配置微信登录的appid和appsecret


}


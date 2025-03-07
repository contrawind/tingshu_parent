package com.atguigu.controller;

import com.atguigu.entity.UserInfo;
import com.atguigu.entity.VipServiceConfig;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserInfoService;
import com.atguigu.service.VipServiceConfigService;
import com.atguigu.vo.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "vip服务管理接口")
@RestController
@RequestMapping("api/user/vipConfig")
public class VipConfigController {
    @Autowired
    private VipServiceConfigService vipServiceConfigService;
    @Operation(summary = "获取所有的VIP配置")
    @GetMapping("findAllVipConfig")
    public RetVal findAllVipConfig()  {
        List<VipServiceConfig> list = vipServiceConfigService.list();
        return RetVal.ok(list);
    }

}

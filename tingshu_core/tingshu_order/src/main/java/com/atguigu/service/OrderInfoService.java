package com.atguigu.service;

import com.atguigu.vo.OrderInfoVo;
import com.atguigu.vo.TradeVo;

public interface OrderInfoService {
    OrderInfoVo confirmOrder(TradeVo tradeVo);
}

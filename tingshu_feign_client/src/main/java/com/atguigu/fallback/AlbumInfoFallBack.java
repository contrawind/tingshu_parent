//package com.atguigu.fallback;
//
//import com.atguigu.AlbumFeignClient;
//import com.atguigu.entity.AlbumAttributeValue;
//import com.atguigu.entity.AlbumInfo;
//import com.atguigu.result.RetVal;
//
//import java.util.List;
//
//public class AlbumInfoFallBack implements AlbumFeignClient {
//    @Override
//    public RetVal<AlbumInfo> getAlbumInfoById(Long albumId) {
//        //编写一写逻辑
//        return RetVal.fail().message("对不起，系统正忙");
//    }
//
//    @Override
//    public List<AlbumAttributeValue> getAlbumPropertyValue(Long albumId) {
//        return List.of();
//    }
//}
//

package com.atguigu.consumer;

import com.atguigu.constant.KafkaConstant;
import com.atguigu.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SearchConsumer {
    @Autowired
    private SearchService searchService;

    @KafkaListener(topics = KafkaConstant.ONSALE_ALBUM_QUEUE)
    //专辑上架
    public void OnSaleAlbum(Long albumId) {
        if (albumId != null) {
            searchService.onSaleAlbum(albumId);
        }
    }

    @KafkaListener(topics = KafkaConstant.OFFSALE_ALBUM_QUEUE)
    //专辑下架
    public void OffSaleAlbum(Long albumId) {
        if (albumId != null) {
            searchService.offSaleAlbum(albumId);
        }
    }
}


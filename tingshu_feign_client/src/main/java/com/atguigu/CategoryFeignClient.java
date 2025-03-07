package com.atguigu;

import com.atguigu.entity.AlbumAttributeValue;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.BaseCategory3;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(value = "tingshu-album")//,fallback = AlbumInfoFallBack.class)
public interface CategoryFeignClient {
    @GetMapping("/api/album/category/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id);
    @GetMapping("/api/album/category/getCategory3ListByCategory1Id/{category1Id}")
    public RetVal<List<BaseCategory3>> getCategory3ListByCategory1Id(@PathVariable Long category1Id);
}

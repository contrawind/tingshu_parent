package com.atguigu.mapper;

import com.atguigu.entity.AlbumStat;
import com.atguigu.vo.AlbumStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 专辑统计 Mapper 接口
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
public interface AlbumStatMapper extends BaseMapper<AlbumStat> {

    AlbumStatVo getAlbumStatInfo(@Param("albumId") Long albumId);
}

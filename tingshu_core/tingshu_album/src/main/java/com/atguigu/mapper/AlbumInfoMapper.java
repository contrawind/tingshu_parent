package com.atguigu.mapper;

import com.atguigu.entity.AlbumInfo;
import com.atguigu.query.AlbumInfoQuery;
import com.atguigu.vo.AlbumTempVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 专辑信息 Mapper 接口
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    IPage<AlbumTempVo> getUserAlbumByPage(@Param("pageParam") IPage<AlbumTempVo> pageParam, @Param("albumInfoQuery") AlbumInfoQuery albumInfoQuery);
}

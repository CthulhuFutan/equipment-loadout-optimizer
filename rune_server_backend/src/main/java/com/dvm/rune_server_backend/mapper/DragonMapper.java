package com.dvm.rune_server_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dvm.rune_server_backend.model.entity.Dragons;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author 25826
 */
@Mapper
public interface DragonMapper extends BaseMapper<Dragons> {
    // 1.检索一个人所拥有的所有龙：根据用户id检索龙 + 根据龙的名称/类别/属性检索龙

    // 2.查看一条龙的详细信息：在 1 的条件下，检索某个龙的编号的信息

    // 3.添加一条龙

    // 4.删除一条龙

    // 5.修改一条龙的信息
}

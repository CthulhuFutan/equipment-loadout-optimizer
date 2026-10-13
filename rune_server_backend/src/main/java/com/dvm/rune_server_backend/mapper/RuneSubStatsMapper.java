package com.dvm.rune_server_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dvm.rune_server_backend.model.entity.RuneSubStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author 25826
 * 与 RuneMapper 一起进行符文的数据信息处理
 */
@Mapper
public interface RuneSubStatsMapper extends BaseMapper<RuneSubStats> {

}

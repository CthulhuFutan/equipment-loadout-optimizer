package com.dvm.rune_server_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dvm.rune_server_backend.model.entity.DragonEquipment;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author 25826
 */
@Mapper
public interface DragonEquipmentMapper extends BaseMapper<DragonEquipment> {
    // 1.查看当前龙装备的符文

    // 2.对当前龙的符文进行修改

    // 3.删除当前龙的搭配
}

package com.dvm.rune_server_backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dvm.rune_server_backend.model.dto.DragonRequest;
import com.dvm.rune_server_backend.model.entity.Dragons;

/**
 * @author 25826
 */
public interface DragonService {

    /**
     * 根据用户id检索龙 + 根据龙的名称/类别/属性检索龙
     * List<Dragons> selectDragonByUserIdAndCondition(Long userId, String dragonName, String element, String category);
     * 新增分页效果
     */
    IPage<Dragons> selectDragonByUserIdAndCondition(Long userId, String dragonName, String element, String category, int pageNum, int pageSize);

    // 查看一条龙的详细信息
    Dragons selectDragonByDragonId(Long userId, Long dragonId);

    // CRUD
    int insertDragon(Long userId, DragonRequest dto);
    int updateDragon(Long userId, Long dragonId, DragonRequest dto);
    int deleteDragon(Long userId, Long dragonId);

    // 考虑到部分龙可能没有AI分析结果，因此在这里需要考虑空值填补
    void refreshBaseAnalysis(Long userId, Long dragonId);
}

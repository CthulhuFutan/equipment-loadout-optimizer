package com.dvm.rune_server_backend.service;

import com.dvm.rune_server_backend.common.Code;
import com.dvm.rune_server_backend.model.vo.RuneVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author 25826
 *
 */
public interface DragonEquipmentService {

    // 查看某条龙当前装备的符文
    List<RuneVO> runeListByDragon(Long userId, Long dragonId);

    /**
     * 对龙的符文进行保存
     * 调整：拆卸符文后，count == 0，会出现更新失败错误信息
     * 将 0 设置为正常存储内容，对于找不到的角色，设置返回值为 -1
     */
    int saveEquipment(Long userId, Long dragonId, List<Long> runeIds);

    /**
     * 针对符文不可装备的两种规则进行处理：414/415
     * 1.检查符文是不是该角色正在装备的
     * 2.检查符文是否已经被 lock=1：上锁无法访问
      */
    Code checkEquipment(Long userId, Long dragonId, List<Long> runeIds);
    // 删除龙之前，将该龙装备的全部符文解锁
    int releaseLockedRunes(Long userId, Long dragonId);
}
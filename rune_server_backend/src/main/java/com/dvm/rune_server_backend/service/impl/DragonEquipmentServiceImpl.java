package com.dvm.rune_server_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dvm.rune_server_backend.common.Code;
import com.dvm.rune_server_backend.mapper.DragonEquipmentMapper;
import com.dvm.rune_server_backend.mapper.DragonMapper;
import com.dvm.rune_server_backend.model.entity.DragonEquipment;
import com.dvm.rune_server_backend.model.entity.Dragons;
import com.dvm.rune_server_backend.model.vo.RuneVO;
import com.dvm.rune_server_backend.service.DragonEquipmentService;
import com.dvm.rune_server_backend.service.RuneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author 25826
 */
@Service
@RequiredArgsConstructor
public class DragonEquipmentServiceImpl implements DragonEquipmentService {

    private final DragonEquipmentMapper equipmentMapper;
    private final DragonMapper dragonMapper;
    private final RuneService runeService;

    @Override
    public List<RuneVO> runeListByDragon(Long userId, Long dragonId) {
        QueryWrapper<DragonEquipment> wrapper = new QueryWrapper<DragonEquipment>().eq("dragon_id", dragonId);
        List<DragonEquipment> rows = equipmentMapper.selectList(wrapper);
        List<RuneVO> result = new ArrayList<>();
        for (DragonEquipment row : rows) {
            RuneVO vo = runeService.selectRuneByRuneId(userId, row.getRuneId());
            if (vo != null) {
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public int saveEquipment(Long userId, Long dragonId, List<Long> runeIds) {
        QueryWrapper<Dragons> wrapper = new QueryWrapper<Dragons>().eq("user_id", userId).eq("dragon_id", dragonId);
        Dragons dragon = dragonMapper.selectOne(wrapper);
        if (dragon == null) {
            return -1;
        }

        // 确定符文属于某个用户，防止跨用户误删符文
        List<RuneVO> owned = new ArrayList<>();
        Set<Long> set = new HashSet<>();
        if(runeIds != null){
            for(Long runeId: runeIds){
                if(!set.add(runeId)) { continue; }
                RuneVO vo = runeService.selectRuneByRuneId(userId, runeId);
                if (vo != null) {
                    owned.add(vo);
                }
            }
        }

        // 现装备符文拆卸
        QueryWrapper<DragonEquipment> curEquip = new QueryWrapper<DragonEquipment>().eq("dragon_id", dragonId);
        equipmentMapper.delete(curEquip);

        // 目标符文可能在别的龙上，针对这些符文进行拆卸
        if(!owned.isEmpty()){
            List<Long> ownedIds = owned.stream().map(RuneVO::getRuneId).toList();
            QueryWrapper<DragonEquipment> otherEquip = new QueryWrapper<DragonEquipment>().in("rune_id", ownedIds);
            equipmentMapper.delete(otherEquip);
        }

        int count = 0;
        for (RuneVO vo: owned) {
            if (vo == null || vo.getSlotNumber() == null) {
                continue;
            }
            DragonEquipment equipment = new DragonEquipment();
            equipment.setDragonId(dragonId);
            equipment.setRuneId(vo.getRuneId());
            equipment.setSlotNumber(vo.getSlotNumber());
            equipmentMapper.insert(equipment);
            count++;
        }

        return count;
    }

    @Override
    public Code checkEquipment(Long userId, Long dragonId, List<Long> runeIds) {
        // 获取当前装备的符文 id 列表
        List<Long> equipRunes = equipmentMapper.selectList(
                        new QueryWrapper<DragonEquipment>().select("rune_id").eq("dragon_id", dragonId))
                .stream().map(DragonEquipment::getRuneId).toList();
        // 获取预备装备的符文 id 列表 → 可以拆卸为空数组
        List<Long> submitted = (runeIds == null) ? List.of() : runeIds;

        for (Long runeId : equipRunes) {
            if (submitted.contains(runeId)) {
                continue;
            }
            // 415：锁定符文不可卸下
            RuneVO vo = runeService.selectRuneByRuneId(userId, runeId);
            if (vo != null && Boolean.TRUE.equals(vo.getLocked())) {
                return Code.RUNE_LOCKED_REMOVE;
            }
        }
        for (Long runeId : submitted) {
            // 自身当前装备的符文，锁定也可以正常执行
            if (equipRunes.contains(runeId)) {
                continue;
            }
            // 414：锁定的、未被该龙装备的符文无法装备
            RuneVO vo = runeService.selectRuneByRuneId(userId, runeId);
            if (vo != null && Boolean.TRUE.equals(vo.getLocked())) {
                return Code.RUNE_LOCKED_EQUIP;
            }
        }

        return null;
    }

    @Override
    @Transactional
    public int releaseLockedRunes(Long userId, Long dragonId) {
        List<Long> runeIds = equipmentMapper.selectList(
                        new QueryWrapper<DragonEquipment>().select("rune_id").eq("dragon_id", dragonId))
                .stream().map(DragonEquipment::getRuneId).toList();
        return runeIds.isEmpty() ? 0 : runeService.setLock(userId, runeIds, false);
    }
}

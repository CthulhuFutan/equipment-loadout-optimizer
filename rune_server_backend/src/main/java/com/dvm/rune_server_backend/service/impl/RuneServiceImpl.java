package com.dvm.rune_server_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.dvm.rune_server_backend.common.RuneConstants;
import com.dvm.rune_server_backend.mapper.DragonEquipmentMapper;
import com.dvm.rune_server_backend.mapper.RuneMapper;
import com.dvm.rune_server_backend.mapper.RuneSubStatsMapper;
import com.dvm.rune_server_backend.model.dto.RuneRequest;
import com.dvm.rune_server_backend.model.entity.DragonEquipment;
import com.dvm.rune_server_backend.model.entity.RuneSubStats;
import com.dvm.rune_server_backend.model.entity.Runes;
import com.dvm.rune_server_backend.model.vo.RuneVO;
import com.dvm.rune_server_backend.model.vo.RuneVO.SubStatsVO;
import com.dvm.rune_server_backend.service.RuneService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 25826
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RuneServiceImpl implements RuneService {

    private final RuneMapper runeMapper;
    private final RuneSubStatsMapper subStatsMapper;
    private final DragonEquipmentMapper equipmentMapper;

    @Override
    public List<RuneVO> selectRuneByUserIdAndCondition(Long userId, Integer slotNumber, String setType, String mainStatType) {
        // 1. 主表
        QueryWrapper<Runes> wrapper = new QueryWrapper<Runes>().eq("user_id", userId)
                .eq(slotNumber != null,"slot_number", slotNumber)
                .eq(setType != null,"set_type", setType)
                .eq(mainStatType != null,"main_stat_type", mainStatType)
                .orderByAsc("set_type")
                .orderByDesc("star_rating")
                .orderByAsc("quality")
                .orderByDesc("main_stat_type")
                .orderByAsc("rune_id");
        List<Runes> runeList = runeMapper.selectList(wrapper);
        // 2. 子表
        List<Long> runeIds = runeList.stream().map(Runes::getRuneId).toList();
        Map<Long, List<RuneSubStats>> subMap;
        if (!runeIds.isEmpty()) {
            QueryWrapper<RuneSubStats> subWrapper = new QueryWrapper<RuneSubStats>().in("rune_id", runeIds).orderByAsc("sort_order");
            List<RuneSubStats> allSubs = subStatsMapper.selectList(subWrapper);
            subMap = allSubs.stream().collect(Collectors.groupingBy(RuneSubStats::getRuneId));
        } else {
            subMap = Map.of();
        }
        // 3. 组装 VO
        List<RuneVO> voList = runeList.stream().map(rune -> {
            RuneVO vo = new RuneVO();
            BeanUtils.copyProperties(rune, vo);
            List<RuneSubStats> subs = subMap.getOrDefault(rune.getRuneId(), List.of());
            List<SubStatsVO> subVOs = subs.stream().map(sub -> {
                SubStatsVO sv = new SubStatsVO();
                BeanUtils.copyProperties(sub, sv);
                return sv;
            }).toList();
            vo.setSubStats(subVOs);
            return vo;
        }).toList();

        Map<Long, DragonEquipment> equipMap = new HashMap<>();
        if (!runeIds.isEmpty()) {
            QueryWrapper<DragonEquipment> equipWrapper = new QueryWrapper<DragonEquipment>().in("rune_id", runeIds);
            List<DragonEquipment> equipList = equipmentMapper.selectList(equipWrapper);
            equipMap = equipList.stream().collect(Collectors.toMap(
                    DragonEquipment::getRuneId, e -> e, (a, b) -> a));
        }
        for (RuneVO vo : voList) {
            DragonEquipment eq = equipMap.get(vo.getRuneId());
            vo.setEquipped(eq != null);
            vo.setEquippedDragonId(eq != null ? eq.getDragonId() : null);
        }

        return voList;
    }

    @Override
    public RuneVO selectRuneByRuneId(Long userId, Long runeId) {
        QueryWrapper<Runes> wrapper = new QueryWrapper<Runes>().eq("user_id", userId).eq("rune_id", runeId);
        Runes rune = runeMapper.selectOne(wrapper);
        if (rune == null) {
            return null;
        }

        QueryWrapper<RuneSubStats> subWrapper = new QueryWrapper<RuneSubStats>().eq("rune_id", runeId).orderByAsc("sort_order");
        List<RuneSubStats> subs = subStatsMapper.selectList(subWrapper);

        RuneVO vo = new RuneVO();
        BeanUtils.copyProperties(rune, vo);
        vo.setSubStats(subs.stream().map(sub -> {
            RuneVO.SubStatsVO sv = new RuneVO.SubStatsVO();
            BeanUtils.copyProperties(sub, sv);
            return sv;
        }).toList());

        QueryWrapper<DragonEquipment> equipWrapper = new QueryWrapper<DragonEquipment>().eq("rune_id", runeId);
        DragonEquipment eq = equipmentMapper.selectOne(equipWrapper);
        vo.setEquipped(eq != null);
        vo.setEquippedDragonId(eq != null ? eq.getDragonId() : null);
        return vo;
    }

    @Override
    public int insertRune(Long userId, RuneRequest dto) {
        if (dto.getMainStatType() != null && !RuneConstants.isMainStatAllowed(dto.getSlotNumber(), dto.getMainStatType())) {
            throw new IllegalArgumentException("槽位 " + dto.getSlotNumber() + " 不允许主属性: " + dto.getMainStatType());
        }
        Runes rune = new Runes();
        BeanUtils.copyProperties(dto, rune);
        rune.setUserId(userId);
        runeMapper.insert(rune);

        subInsert(rune, dto);
        return 1;
    }

    @Override
    @Transactional
    public int updateRune(Long userId, Long runeId, RuneRequest dto) {
        if (dto.getMainStatType() != null && !RuneConstants.isMainStatAllowed(dto.getSlotNumber(), dto.getMainStatType())) {
            throw new IllegalArgumentException(dto.getSlotNumber() + "号槽 " + " 不允许主属性: " + dto.getMainStatType());
        }
        QueryWrapper<Runes> wrapper = new QueryWrapper<Runes>().eq("user_id", userId).eq("rune_id", runeId);
        if(runeMapper.selectOne(wrapper) == null){
            return 0;
        }
        Runes rune = new Runes();
        BeanUtils.copyProperties(dto, rune);
        rune.setRuneId(runeId);
        rune.setUserId(userId);
        runeMapper.updateById(rune);

        // 无子词条id号：先删后插
        if (dto.getSubStats() != null) {
            QueryWrapper<RuneSubStats> subWrapper = new QueryWrapper<RuneSubStats>().eq("rune_id", runeId);
            subStatsMapper.delete(subWrapper);
            subInsert(rune, dto);
        }
        return 1;
    }

    @Override
    @Transactional
    public int deleteRune(Long userId, Long runeId) {
        QueryWrapper<Runes> wrapper = new QueryWrapper<Runes>().eq("user_id", userId).eq("rune_id", runeId);
        if(runeMapper.selectOne(wrapper) == null){
            return 0;
        }
        QueryWrapper<RuneSubStats> subWrapper = new QueryWrapper<RuneSubStats>().eq("rune_id", runeId);
        subStatsMapper.delete(subWrapper);
        runeMapper.delete(wrapper);
        return 1;
    }

    @Override
    @Transactional
    public int setLock(Long userId, List<Long> runeIds, boolean locked){
        if (runeIds == null || runeIds.isEmpty()) {
            return 0;
        }
        UpdateWrapper<Runes> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId)
               .in("rune_id", runeIds)
               .set("locked", locked);
        return runeMapper.update(null, wrapper);
    }

    public void subInsert(Runes rune, RuneRequest dto){
        if(dto.getSubStats() != null){
            for(RuneRequest.SubStatItem item : dto.getSubStats()){
                RuneSubStats sub = new RuneSubStats();
                BeanUtils.copyProperties(item, sub);
                sub.setRuneId(rune.getRuneId());
                subStatsMapper.insert(sub);
            }
        }
    }
}

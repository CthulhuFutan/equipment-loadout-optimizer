package com.dvm.rune_server_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dvm.rune_server_backend.common.RuneConstants;
import com.dvm.rune_server_backend.mapper.DragonMapper;
import com.dvm.rune_server_backend.model.dto.DragonRequest;
import com.dvm.rune_server_backend.model.entity.Dragons;
import com.dvm.rune_server_backend.model.vo.RuneOptimizeInitVO;
import com.dvm.rune_server_backend.service.AiService;
import com.dvm.rune_server_backend.service.DragonEquipmentService;
import com.dvm.rune_server_backend.service.DragonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 25826
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DragonServiceImpl implements DragonService {

    private final DragonMapper dragonMapper;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final DragonEquipmentService equipmentService;
    // 线程池
    @Qualifier("applicationTaskExecutor")
    private final TaskExecutor taskExecutor;


    @Override
    public IPage<Dragons> selectDragonByUserIdAndCondition(Long userId, String dragonName, String element, String category, int pageNum, int pageSize) {
        // 数据安全性考虑
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = Math.min(Math.max(1, pageSize), 100);
        Page<Dragons> page = new Page<>(safePageNum, safePageSize);

        LambdaQueryWrapper<Dragons> wrapper = new LambdaQueryWrapper<Dragons>().eq(Dragons::getUserId, userId)
                .like(dragonName != null, Dragons::getDragonName, dragonName)
                .eq(category != null, Dragons::getCategory, category)
                .eq(element != null, Dragons::getElement, element)
                .orderByAsc(Dragons::getElement)
                .orderByAsc(Dragons::getCategory)
                .orderByAsc(Dragons::getDragonId);

        return dragonMapper.selectPage(page, wrapper);
    }

    @Override
    public Dragons selectDragonByDragonId(Long userId, Long dragonId) {
        QueryWrapper<Dragons> wrapper = new QueryWrapper<Dragons>().eq("user_id", userId).eq("dragon_id", dragonId);
        return dragonMapper.selectOne(wrapper);
    }

    @Override
    public int insertDragon(Long userId, DragonRequest dto) {
        Dragons dragon = new Dragons();
        dragon.setUserId(userId);
        // 同名属性复制
        BeanUtils.copyProperties(dto, dragon);
        int rows = dragonMapper.insert(dragon);
        if(rows > 0){
            // 异步添加AI分析
            refreshBaseAnalysisAsync(dragon);
        }
        return rows;
    }

    @Override
    public int updateDragon(Long userId, Long dragonId, DragonRequest dto) {
        QueryWrapper<Dragons> wrapper = new QueryWrapper<Dragons>().eq("user_id", userId).eq("dragon_id", dragonId);
        if(dragonMapper.selectOne(wrapper) == null){
            return 0;
        }
        Dragons dragon = new Dragons();
        BeanUtils.copyProperties(dto, dragon);
        dragon.setDragonId(dragonId);
        dragon.setUserId(userId);
        int rows = dragonMapper.updateById(dragon);
        if (rows > 0) {
            refreshBaseAnalysisAsync(dragon);
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteDragon(Long userId, Long dragonId) {
        QueryWrapper<Dragons> wrapper = new QueryWrapper<Dragons>().eq("user_id", userId).eq("dragon_id", dragonId);
        if(dragonMapper.selectOne(wrapper) == null){
            return 0;
        }
        // 删除前需要对其装备的锁定符文进行解锁
        equipmentService.releaseLockedRunes(userId, dragonId);

        return dragonMapper.delete(wrapper);
    }

    /**
     * 后台异步AI分析
     * 需处理一条龙的结果被多次处理的AI分析并发问题
     */
    private final Set<Long> analyzing = ConcurrentHashMap.newKeySet();
    private void refreshBaseAnalysisAsync(Dragons dragon) {
        Long dragonId = dragon.getDragonId();
        if (dragonId == null || !analyzing.add(dragonId)) {
            return;
        }
        taskExecutor.execute(() -> {
            try {
                // 如果已有分析则跳过，首先要获取到有效的 aiAnalysis信息
                Dragons hasAi = dragonMapper.selectById(dragonId);
//                if(hasAi != null && hasAi.getAiAnalysis() != null && !hasAi.getAiAnalysis().isBlank()){
//                    return;
//                }
                RuneOptimizeInitVO vo = aiService.analyzeScene(dragon, null, null);
                if (vo != null) {
                    dragon.setAiAnalysis(objectMapper.writeValueAsString(vo));
                    dragonMapper.updateById(dragon);
                }
            } catch (Exception e) {
                log.warn("基础AI分析失败, {}: {}", dragon.getDragonName(), e.getMessage());
            } finally {
                analyzing.remove(dragonId);
            }
        });
    }

    @Override
    public void refreshBaseAnalysis(Long userId, Long dragonId) {
        Dragons dragon = selectDragonByDragonId(userId, dragonId);
        if (dragon == null) {
            return;
        }
        refreshBaseAnalysisAsync(dragon);
    }
}

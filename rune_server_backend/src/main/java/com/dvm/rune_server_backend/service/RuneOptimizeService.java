package com.dvm.rune_server_backend.service;

import com.dvm.rune_server_backend.model.dto.RuneOptimizeRequest;
import com.dvm.rune_server_backend.model.dto.RuneSceneRequest;
import com.dvm.rune_server_backend.model.vo.RuneOptimizeInitVO;
import com.dvm.rune_server_backend.model.vo.RuneOptimizeVO;

/**
 * @author 25826
 */
public interface RuneOptimizeService {
    /**
     * 获取最初的AI分析结果，从JSON字段进行拆分
     * 如果没有，则采用通用模板权重
     * 如果存在，则以 weights 作为基础权重参考内容，rationale返回空字符串/给予用户查看的兜底说明
     */
    RuneOptimizeInitVO getInit(Long userId, Long dragonId);

    /**
     * 取全部符文→根据是否其他龙装备进行过滤→单个符文算分→6个槽位各自挑选最高分符文→汇总返回
     */
    RuneOptimizeVO optimize(Long userId, RuneOptimizeRequest dto);

    /**
     * 基于当前场景需求当场计算权重
     * 场景优先级最高
     * 场景分析结果不持久化到 aiAnalysis
     */
    RuneOptimizeInitVO analyzeScene(Long userId, RuneSceneRequest dto);
}

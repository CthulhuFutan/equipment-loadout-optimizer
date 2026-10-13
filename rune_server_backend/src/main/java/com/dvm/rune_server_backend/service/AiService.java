package com.dvm.rune_server_backend.service;

import com.dvm.rune_server_backend.model.entity.Dragons;
import com.dvm.rune_server_backend.model.vo.RuneOptimizeInitVO;

import java.util.Map;

/**
 * @author 25826
 * AI 分析统一入口
 */
public interface AiService {

    /**
     * 检查当前AI是否可用
     */
    boolean isAvailable();

    /**
     * 发起一次对话，返回模型产出的原始文本
     * 若AI不可用 / 请求失败，返回 null
     * AI调用的参数：
     * （1）model：指定模型
     * （2）message = role（system/user/assistant/……） + content
     * （3）temperature：值越低输出越保守
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 基于SKILL.md对龙进行词条权重分析
     * 返回：清洗后的分析结果；AI 失败 / 解析失败返回 null
     */
    RuneOptimizeInitVO analyzeScene(Dragons dragon, Map<String,Integer> currentWeights, String sceneText);

}

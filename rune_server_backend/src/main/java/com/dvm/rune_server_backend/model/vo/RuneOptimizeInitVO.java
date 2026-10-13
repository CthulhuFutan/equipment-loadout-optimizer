package com.dvm.rune_server_backend.model.vo;

import lombok.Data;

import java.util.Map;

/**
 * @author 25826
 * 获取权重信息给出的权重倾向
 * 符文的选择需要基于 weight 来进行
 * rationale用于进行解释说明
 * 结构说明：
 * {
 *   "weights": {"攻击力%": 8, "致命率%": 8, "致命伤害%": 8, "命中%": 8},
 *   "rationale": "攻击龙保证角色攻击相关词条具有较高权重"
 * }
 */
@Data
public class RuneOptimizeInitVO {
    private Map<String, Integer> weights;
    private String rationale;
}

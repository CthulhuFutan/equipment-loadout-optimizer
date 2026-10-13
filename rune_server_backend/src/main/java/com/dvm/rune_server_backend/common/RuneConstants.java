package com.dvm.rune_server_backend.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 25826
 * 符文搭配打分所用的常量
 */
public final class RuneConstants {

    // 禁用实例化
    private RuneConstants() {}

    // 套装类型 / 品质白名单
    public static final String SET_TYPE_REGEX = "^(活动|体力|机会|坚固|强烈|伤害|快速|吸血|冥想|反击|生存|决心|仇恨)$";
    public static final String QUALITY_REGEX = "^(橙色|紫色|蓝色|灰色)$";

    // 参与打分的 10 个词条
    // 攻击力+/防御力+/生命力+ 权重恒为 0
    public static final List<String> PERCENT_STATS = List.of(
            "攻击力%", "防御力%", "生命力%", "致命率%", "致命伤害%",
            "攻击速度%", "命中%", "回避%", "效果命中%", "效果抵抗%"
    );
    // 各槽位可选的主属性词条
    public static final Map<Integer, List<String>> SLOT_MAIN_STATS = Map.of(
            1, List.of("攻击力+"),
            2, List.of("攻击力%", "防御力%", "生命力%", "攻击速度%"),
            3, List.of("防御力+"),
            4, List.of("攻击力%", "防御力%", "生命力%", "致命率%", "致命伤害%"),
            5, List.of("生命力+"),
            6, List.of("攻击力%", "防御力%", "生命力%", "效果命中%", "效果抵抗%")
    );
    // 基础词条权重，范围 0-10
    public static final Map<String, Map<String, Integer>> CATEGORY_BASE_WEIGHTS = Map.of(
            "攻击", Map.of("攻击力%", 8, "致命率%", 8, "致命伤害%", 8, "命中%", 10),
            "防御", Map.of("生命力%", 9, "防御力%", 9),
            "辅助", Map.of("效果命中%", 10, "生命力%", 8, "防御力%", 8),
            "治疗", Map.of("生命力%", 9, "防御力%", 9)
    );

    // 套组激活信息：激活一次所需件数 + 是否可叠加生效
    // 等效于构建了一个字段为 final 的类
    public record SetStackInfo(int threshold, boolean stackable) {}

    // 各套组的激活阈值与可叠性
    public static final Map<String, SetStackInfo> SET_STACK = Map.ofEntries(
            Map.entry("体力", new SetStackInfo(2, true)),
            Map.entry("机会", new SetStackInfo(2, true)),
            Map.entry("坚固", new SetStackInfo(2, true)),
            Map.entry("强烈", new SetStackInfo(4, true)),
            Map.entry("伤害", new SetStackInfo(2, true)),
            Map.entry("快速", new SetStackInfo(4, true)),
            Map.entry("吸血", new SetStackInfo(2, false)),
            Map.entry("冥想", new SetStackInfo(4, false)),
            Map.entry("反击", new SetStackInfo(2, false)),
            Map.entry("生存", new SetStackInfo(2, true)),
            Map.entry("决心", new SetStackInfo(4, false)),
            Map.entry("仇恨", new SetStackInfo(2, true)),
            Map.entry("活动", new SetStackInfo(1, false))
    );

    // 可计算套装与套装数值
    public record SetBonusInfo(String stat, int value) {}
    public static final List<String> NUMERICAL_SETS = List.of("体力", "机会", "坚固", "强烈", "伤害", "快速");
    public static final Map<String, SetBonusInfo> SET_BONUS = Map.of(
            "体力", new SetBonusInfo("生命力%", 20),
            "机会", new SetBonusInfo("致命率%", 20),
            "坚固", new SetBonusInfo("防御力%", 20),
            "强烈", new SetBonusInfo("攻击力%", 40),
            "伤害", new SetBonusInfo("致命伤害%", 20),
            "快速", new SetBonusInfo("攻击速度%", 40));

    // 套组加成得分:给定各数值套组的件数向量,按 Σ 权重[加成词条] × 加成值 × ⌊件数/件数阈值⌋ 计算可叠加的套组总加成
    public static double setBonusScore(Map<String, Integer> weights, int[] counts) {
        double bonus = 0;
        for (int i = 0; i < NUMERICAL_SETS.size(); i++) {
            int count = counts[i];
            // 没有该套组 → 跳过
            if (count <= 0) { continue; }
            String set = NUMERICAL_SETS.get(i);
            SetBonusInfo info = SET_BONUS.get(set);
            int stacks = count / setThreshold(set);
            // 没达到激活阈值 → 跳过
            if (stacks <= 0) { continue; }
            Integer w = weights.get(info.stat());
            // 没设权重 → 跳过
            if (w == null || w <= 0) { continue; }
            bonus += (double) w * info.value() * stacks;
        }
        return bonus;
    }


    // 意外传入未知套组时的兜底信息
    private static final SetStackInfo DEFAULT_SET_INFO = new SetStackInfo(7, false);

    // 全 0 权重（10 个 % 词条，LinkedHashMap 保证顺序稳定）
    public static Map<String, Integer> emptyWeights() {
        Map<String, Integer> w = new LinkedHashMap<>();
        for (String s : PERCENT_STATS) {
            w.put(s, 0);
        }
        return w;
    }

    // 根据龙的类别返回默认权重，可被其他描述覆盖
    public static Map<String, Integer> baseWeights(String category) {
        Map<String, Integer> w = emptyWeights();
        Map<String, Integer> base = CATEGORY_BASE_WEIGHTS.getOrDefault(category, Map.of());
        for (String s : PERCENT_STATS) {
            if (base.containsKey(s)) {
                w.put(s, base.get(s));
            }
        }
        return w;
    }

    // 权重清洗：仅考虑 10 个 % 词条，并设置阈值
    public static Map<String, Integer> sanitizeWeights(Map<String, Integer> raw) {
        Map<String, Integer> w = emptyWeights();
        if (raw == null || raw.isEmpty()) {
            return w;
        }
        for (String stat : PERCENT_STATS) {
            Integer v = raw.get(stat);
            if (v != null) {
                w.put(stat, Math.max(0, Math.min(10, v)));
            }
        }
        return w;
    }
    
    // 校验某槽位的主属性是否合法
    public static boolean isMainStatAllowed(int slot, String mainStatType) {
        List<String> allowed = SLOT_MAIN_STATS.getOrDefault(slot, List.of());
        return allowed.contains(mainStatType);
    }

    // 取出某套组的激活阈值；未知套组默认 7
    public static int setThreshold(String setType) {
        return SET_STACK.getOrDefault(setType, DEFAULT_SET_INFO).threshold();
    }

    // 取出某套组是否可叠加；未知套组默认 false
    public static boolean isSetStackable(String setType) {
        return SET_STACK.getOrDefault(setType, DEFAULT_SET_INFO).stackable();
    }

    // 前端搭配选择栏位
    public static final Map<String, List<Integer>> SET_REQ_TIERS = Map.ofEntries(
            Map.entry("体力", List.of(2,4,6)), Map.entry("机会", List.of(2,4,6)),
            Map.entry("坚固", List.of(2,4,6)), Map.entry("伤害", List.of(2,4,6)),
            Map.entry("生存", List.of(2,4,6)),
            Map.entry("强烈", List.of(4)),      Map.entry("快速", List.of(4)),
            Map.entry("冥想", List.of(4)),      Map.entry("决心", List.of(4)),
            Map.entry("吸血", List.of(2)),      Map.entry("反击", List.of(2)),
            Map.entry("仇恨", List.of(2)));
}

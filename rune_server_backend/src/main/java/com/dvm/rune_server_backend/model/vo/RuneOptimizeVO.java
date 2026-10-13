package com.dvm.rune_server_backend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * @author 25826
 * 由于套装效果的存在，还需要考虑总体搭配的分数结果
 */
@Data
public class RuneOptimizeVO {
    // 现行推荐行：按当前分 + 套组加成的最优 6 槽
    private List<RuneOptimizeSlotVO> slotResults;
    // 推荐行总分
    private Double totalScore;

    // 研磨后推荐行：按研磨后潜在分 + 套组加成的最优 6 槽
    private List<GrindSlotVO> grindSlots;
    // 研磨行总分
    private Double grindTotalScore;

    // 用户选择套组符文数量不足的警告信息
    private String warning;
}

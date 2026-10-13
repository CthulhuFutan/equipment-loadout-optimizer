package com.dvm.rune_server_backend.model.vo;

import lombok.Data;

/**
 * @author 25826
 * 研磨搭配行的单个槽位：符文 + 当前分 + 潜在分 + 研磨建议
 */
@Data
public class GrindSlotVO {
    private Integer slotNumber;
    private RuneVO rune;
    // 当前分
    private Double score;
    // 最优研磨后的潜在分
    private Double potentialScore;
    // 研磨建议
    private GrindAdviceVO grind;
}

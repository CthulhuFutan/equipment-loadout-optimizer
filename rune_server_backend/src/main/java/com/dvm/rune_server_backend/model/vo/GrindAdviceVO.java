package com.dvm.rune_server_backend.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author 25826
 * 研磨搭配行中，单个槽位的研磨建议
 */
@Data
public class GrindAdviceVO {

    private Boolean grindable;
    // 是否已研磨过
    private Boolean alreadyGround;
    // 已研磨的词条类型
    private String groundStatType;
    // 已研磨词条的当前数值
    private BigDecimal groundStatValue;
    // 建议目标词条
    private String targetStat;
    // 下限值：研磨数值需大于此值
    private BigDecimal minValue;
    private String message;
}

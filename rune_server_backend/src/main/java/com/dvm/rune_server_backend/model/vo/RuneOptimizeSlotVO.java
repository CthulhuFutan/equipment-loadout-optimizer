package com.dvm.rune_server_backend.model.vo;

import lombok.Data;

/**
 * @author 25826
 * 对单个符文槽的符文选择和分数统计
 */
@Data
public class RuneOptimizeSlotVO {
    private Integer slotNumber;
    private RuneVO rune;
    private Double score;
}

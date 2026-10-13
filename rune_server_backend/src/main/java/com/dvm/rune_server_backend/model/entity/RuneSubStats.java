package com.dvm.rune_server_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author 25826
 * RuneSubStats表，符文副属性表
 */
@Data
public class RuneSubStats {
    @TableId(value = "rune_sub_id")
    private Long runeSubId;
    private Long runeId;
    private String statType;
    private BigDecimal statValue;
    // 0-4，用于表示这个是第几条副属性，0表示额外属性，1-4表示子属性
    private Integer sortOrder;
}

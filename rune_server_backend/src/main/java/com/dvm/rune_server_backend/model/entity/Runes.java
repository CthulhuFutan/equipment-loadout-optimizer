package com.dvm.rune_server_backend.model.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author 25826
 * Runes表，符文表
 */
@Data
@TableName("runes")
public class Runes {
    @TableId(value = "rune_id")
    private Long runeId;
    private Long userId;
    private String imageUrl;
    private String runeName;
    // 符文槽编号1-6
    private Integer slotNumber;
    // 符文类型：体力/机会/坚固/强烈/伤害/快速/吸血/冥想/反击/生存/决心/仇恨/活动
    private String setType;
    // 符文品质：灰色/蓝色/紫色/橙色
    private String quality;
    private Integer starRating;
    private Integer levels;
    // 主属性词条及属性值
    private String mainStatType;
    private BigDecimal mainStatValue;
    // 是否可研磨（≥12级 且 子词条全解锁）。由数据库自行判断生成
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Boolean isGrindable;
    // 被研磨词条的 sortOrder（对应 rune_sub_stats.sort_order），null=未研磨
    private Long grindSortOrder;
    // 符文锁定，不可被其他角色装备
    private Boolean locked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

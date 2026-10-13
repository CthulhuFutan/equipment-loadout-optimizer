package com.dvm.rune_server_backend.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author 25826
 * 将 Runes 与 runeSubStats类的数据合并，便于一并传输和显示
 */
@Data
public class RuneVO {
    private Long runeId;
    private String imageUrl;
    private String runeName;
    private Integer slotNumber;
    private String setType;
    private String quality;
    private Integer starRating;
    private Integer levels;
    private String mainStatType;
    private BigDecimal mainStatValue;
    private Boolean isGrindable;
    private Long grindSortOrder;
    private Boolean locked;
    private Boolean equipped;

    // 子属性列表
    private List<SubStatsVO> subStats;

    @Data
    public static class SubStatsVO {
        private Long runeSubId;
        private String statType;
        private BigDecimal statValue;
        // 0：额外词条；1-4：子词条
        private Integer sortOrder;
    }

    // 哪条龙装备了该符文
    private Long equippedDragonId;
}

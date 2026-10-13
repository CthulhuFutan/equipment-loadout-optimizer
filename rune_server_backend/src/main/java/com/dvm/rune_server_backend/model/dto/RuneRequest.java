package com.dvm.rune_server_backend.model.dto;

import com.dvm.rune_server_backend.common.RuneConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author 25826
 */
@Data
@Order
public class RuneRequest {
    private String imageUrl;
    private String runeName;
    @NotNull(message = "槽位不能为空")
    @Min(value = 1, message = "槽位范围 1-6")
    @Max(value = 6, message = "槽位范围 1-6")
    private Integer slotNumber;
    @NotBlank(message = "符文类型不能为空")
    @Pattern(regexp = RuneConstants.SET_TYPE_REGEX, message = "符文类型不在允许范围内")
    private String setType;
    @NotBlank(message = "品质不能为空")
    @Pattern(regexp = RuneConstants.QUALITY_REGEX, message = "品质不在允许范围内")
    private String quality;
    private Integer starRating;
    @Min(value = 0, message = "强化等级范围 0-15")
    @Max(value = 15, message = "强化等级范围 0-15")
    private Integer levels;
    @NotBlank(message = "主属性类型不能为空")
    private String mainStatType;
    @NotNull(message = "主属性数值不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "主属性数值必须大于 0")
    private BigDecimal mainStatValue;

    // 被研磨的子词条，null表示未研磨
    private Long grindSortOrder;
    private Boolean locked;

    // 子属性列表
    @Valid
    private List<SubStatItem> subStats;

    @Data
    public static class SubStatItem {
        @NotBlank(message = "词条类型不能为空")
        private String statType;
        @NotNull(message = "词条数值不能为空")
        @DecimalMin(value = "0.0", inclusive = false, message = "词条数值必须大于 0")
        private BigDecimal statValue;
        // 0：额外词条；1-4：子词条
        @Min(value = 0, message = "sortOrder 范围 0-4")
        @Max(value = 4, message = "sortOrder 范围 0-4")
        private Integer sortOrder;
    }
}

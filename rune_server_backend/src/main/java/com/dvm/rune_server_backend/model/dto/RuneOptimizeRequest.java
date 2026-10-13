package com.dvm.rune_server_backend.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * @author 25826
 */
@Data
public class RuneOptimizeRequest {
    @NotNull(message = "请选中一条龙")
    private Long dragonId;
    private Map<String, Integer> weights;
    // 开关仅用于考虑是否将其他龙装备的符文也考虑在内，自己装备的符文默认考虑在内
    private Boolean includeOthersEquipped;
}

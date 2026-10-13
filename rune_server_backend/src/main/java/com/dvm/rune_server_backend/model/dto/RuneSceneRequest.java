package com.dvm.rune_server_backend.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * @author 25826
 */
@Data
public class RuneSceneRequest {
    @NotNull(message = "请选中一条龙")
    private Long dragonId;
    private Map<String, Integer> weights;
    private String scene;
}

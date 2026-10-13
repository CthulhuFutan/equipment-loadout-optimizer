package com.dvm.rune_server_backend.model.dto;

import lombok.Data;

import java.util.List;

/**
 * @author 25826
 * 符文锁定/解锁
 */
@Data
public class RuneLockRequest {
    private List<Long> runeIds;
    private Boolean locked;
}
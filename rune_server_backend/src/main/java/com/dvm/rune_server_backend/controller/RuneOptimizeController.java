package com.dvm.rune_server_backend.controller;

import com.dvm.rune_server_backend.common.Result;
import com.dvm.rune_server_backend.common.SecurityUtils;
import com.dvm.rune_server_backend.model.dto.RuneOptimizeRequest;
import com.dvm.rune_server_backend.model.dto.RuneSceneRequest;
import com.dvm.rune_server_backend.service.RuneOptimizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author 25826
 */
@RestController
@RequestMapping("/api/optimize")
@RequiredArgsConstructor
public class RuneOptimizeController {

    private final RuneOptimizeService runeOptimizeService;

    @GetMapping("/init")
    public Result<?> getInit(@RequestParam Long dragonId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(runeOptimizeService.getInit(userId, dragonId));
    }

    @PostMapping
    public Result<?> optimize(@Valid @RequestBody RuneOptimizeRequest dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(runeOptimizeService.optimize(userId, dto));
    }

    @PostMapping("/scene")
    public Result<?> analyzeScene(@Valid @RequestBody RuneSceneRequest dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(runeOptimizeService.analyzeScene(userId, dto));
    }
}

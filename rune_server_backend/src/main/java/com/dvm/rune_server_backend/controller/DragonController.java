package com.dvm.rune_server_backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dvm.rune_server_backend.common.Code;
import com.dvm.rune_server_backend.common.Result;
import com.dvm.rune_server_backend.common.SecurityUtils;
import com.dvm.rune_server_backend.model.dto.DragonRequest;
import com.dvm.rune_server_backend.model.entity.Dragons;
import com.dvm.rune_server_backend.service.DragonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
/**
 * @author 25826
 */
@RestController
@RequestMapping("/api/dragons")
@RequiredArgsConstructor
public class DragonController {

    private final DragonService dragonService;

    @GetMapping
    public Result<?> getDragonList(@RequestParam(required = false) String dragonName,
                                   @RequestParam(required = false) String element,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(defaultValue = "1") int pageNum,
                                   @RequestParam(defaultValue = "20") int pageSize){
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<Dragons> dragonList = dragonService.selectDragonByUserIdAndCondition(userId, dragonName, element, category, pageNum, pageSize);
        return Result.success(dragonList);
    }

    @GetMapping("/{dragonId}")
    public Result<?> getDragon(@PathVariable Long dragonId){
        Long userId = SecurityUtils.getCurrentUserId();
        Dragons dragon = dragonService.selectDragonByDragonId(userId, dragonId);
        return Result.success(dragon);
    }

    @PostMapping
    public Result<?> insertDragon(@Valid @RequestBody DragonRequest dto){
        Long userId = SecurityUtils.getCurrentUserId();
        int i = dragonService.insertDragon(userId, dto);
        return i > 0 ? Result.success(i) : Result.error(Code.INSERT_ERROR);
    }

    @PutMapping("/{dragonId}")
    public Result<?> updateDragon(@PathVariable Long dragonId, @Valid @RequestBody DragonRequest dto){
        Long userId = SecurityUtils.getCurrentUserId();
        int i = dragonService.updateDragon(userId, dragonId, dto);
        return i > 0 ? Result.success(i) : Result.error(Code.UPDATE_ERROR);
    }

    @DeleteMapping("/{dragonId}")
    public Result<?> deleteDragon(@PathVariable Long dragonId) {
        Long userId = SecurityUtils.getCurrentUserId();
        int i = dragonService.deleteDragon(userId, dragonId);
        return i > 0 ? Result.success(i) : Result.error(Code.DELETE_ERROR);
    }
}

package com.dvm.rune_server_backend.controller;

import com.dvm.rune_server_backend.common.Code;
import com.dvm.rune_server_backend.common.Result;
import com.dvm.rune_server_backend.common.SecurityUtils;
import com.dvm.rune_server_backend.model.vo.RuneVO;
import com.dvm.rune_server_backend.service.DragonEquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 25826
 * 符文装备提取统一处理
 */
@RestController
@RequestMapping("/api/dragons")
@RequiredArgsConstructor
public class DragonEquipmentController {

    private final DragonEquipmentService equipmentService;

    // 读取当前符文搭配
    @GetMapping("/{dragonId}/equipment")
    public Result<?> getDragonEquipment(@PathVariable Long dragonId){
        Long userId = SecurityUtils.getCurrentUserId();
        List<RuneVO> runeList = equipmentService.runeListByDragon(userId, dragonId);
        return Result.success(runeList);
    }

    // 更新当前符文搭配
    @PostMapping("/{dragonId}/equipment")
    public Result<?> saveDragonEquipment(@PathVariable Long dragonId, @RequestBody List<Long> runeIds) {
        Long userId = SecurityUtils.getCurrentUserId();
        // 更新前校验符文是否被锁定
        Code lockError = equipmentService.checkEquipment(userId, dragonId, runeIds);
        if (lockError != null){
            return Result.error(lockError);
        }
        int i = equipmentService.saveEquipment(userId, dragonId, runeIds);
        return i >= 0 ? Result.success(i) : Result.error(Code.UPDATE_ERROR);
    }
}
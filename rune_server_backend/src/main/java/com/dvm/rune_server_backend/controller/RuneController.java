package com.dvm.rune_server_backend.controller;

import com.dvm.rune_server_backend.common.Code;
import com.dvm.rune_server_backend.common.Result;
import com.dvm.rune_server_backend.common.SecurityUtils;
import com.dvm.rune_server_backend.model.dto.RuneLockRequest;
import com.dvm.rune_server_backend.model.dto.RuneRequest;
import com.dvm.rune_server_backend.model.vo.RuneVO;
import com.dvm.rune_server_backend.service.RuneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 25826
 */
@RestController
@RequestMapping("/api/runes")
@RequiredArgsConstructor
public class RuneController {

    private final RuneService runeService;

    @GetMapping
    public Result<?> getRuneList(@RequestParam(required = false) Integer slotNumber,
                                 @RequestParam(required = false) String setType,
                                 @RequestParam(required = false) String mainStatType){
        Long userId = SecurityUtils.getCurrentUserId();
        List<RuneVO> runeList = runeService.selectRuneByUserIdAndCondition(userId, slotNumber, setType, mainStatType);
        return Result.success(runeList);
    }

    @GetMapping("/{runeId}")
    public Result<?> getRune(@PathVariable Long runeId){
        Long userId = SecurityUtils.getCurrentUserId();
        RuneVO rune = runeService.selectRuneByRuneId(userId, runeId);
        return Result.success(rune);
    }

    @PostMapping
    public Result<?> insertRune(@Valid @RequestBody RuneRequest dto){
        Long userId = SecurityUtils.getCurrentUserId();
        int i = runeService.insertRune(userId, dto);
        return i > 0 ? Result.success() : Result.error(Code.INSERT_ERROR);
    }

    @PutMapping("/{runeId}")
    public Result<?> updateRune(@PathVariable Long runeId, @Valid @RequestBody RuneRequest dto){
        Long userId = SecurityUtils.getCurrentUserId();
        int i = runeService.updateRune(userId, runeId, dto);
        return i > 0 ? Result.success() : Result.error(Code.UPDATE_ERROR);
    }

    @DeleteMapping("/{runeId}")
    public Result<?> deleteRune(@PathVariable Long runeId){
        Long userId = SecurityUtils.getCurrentUserId();
        // 符文锁定判断
        RuneVO vo = runeService.selectRuneByRuneId(userId, runeId);
        if (vo == null) {
            return Result.error(Code.DELETE_ERROR);
        }
        if (Boolean.TRUE.equals(vo.getLocked())) {
            return Result.error(Code.RUNE_LOCKED_DELETE);
        }
        int i = runeService.deleteRune(userId, runeId);
        return i > 0 ? Result.success() : Result.error(Code.DELETE_ERROR);
    }

    // 符文上锁和解锁
    @PutMapping("/lock")
    public Result<?> setRuneLock(@RequestBody RuneLockRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (req.getRuneIds() == null || req.getRuneIds().isEmpty()) {
            return Result.error(Code.UPDATE_ERROR);
        }
        int n = runeService.setLock(userId, req.getRuneIds(), Boolean.TRUE.equals(req.getLocked()));
        return n > 0 ? Result.success(n) : Result.error(Code.UPDATE_ERROR);
    }
}

package com.dvm.rune_server_backend.service;

import com.dvm.rune_server_backend.model.dto.RuneRequest;
import com.dvm.rune_server_backend.model.vo.RuneVO;

import java.util.List;

/**
 * @author 25826
 */
public interface RuneService {

    /**
     * 根据用户id检索符文
     * 也可以通过符文槽编号、符文类型、主属性词条进行筛选
     */
    List<RuneVO> selectRuneByUserIdAndCondition(Long userId, Integer slotNumber, String setType, String mainStatType);

    // 符文详细信息
    RuneVO selectRuneByRuneId(Long userId, Long runeId);

    // CRUD
    int insertRune(Long userId, RuneRequest dto);
    int updateRune(Long userId, Long runeId, RuneRequest dto);
    int deleteRune(Long userId, Long runeId);

    // 符文锁定状态设置
    int setLock(Long userId, List<Long> runeIds, boolean locked);
}

package com.dvm.rune_server_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author 25826
 * DragonEquipment表，龙装备符文的结果显示表
 */
@Data
@TableName("dragon_equipment")
public class DragonEquipment {
    @TableId(value = "equip_id")
    private Long equipId;
    private Long dragonId;
    private Long runeId;
    // 符文装备槽编号，范围1-6
    private Integer slotNumber;
}

package com.dvm.rune_server_backend.model.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 25826
 * Dragons表
 */
@Data
@TableName("dragons")
public class Dragons {
    @TableId(value = "dragon_id")
    private Long dragonId;
    private Long userId;
    private String dragonName;
    private String dragonImage;
    // 属性，包含水/火/地/光/暗
    private String element;
    // 类别：包含攻击/防御/辅助/治疗
    private String category;

    private String leaderSkillName;
    private String leaderSkillIcon;
    private String leaderSkillDesc;

    private String activeSkillName;
    private String activeSkillIcon;
    private String activeSkillDesc;
    private Integer manaCost;

    @TableField("passive_skill_1_name")
    private String passiveSkill1Name;
    @TableField("passive_skill_1_icon")
    private String passiveSkill1Icon;
    @TableField("passive_skill_1_desc")
    private String passiveSkill1Desc;

    @TableField("passive_skill_2_name")
    private String passiveSkill2Name;
    @TableField("passive_skill_2_icon")
    private String passiveSkill2Icon;
    @TableField("passive_skill_2_desc")
    private String passiveSkill2Desc;

    @TableField("passive_skill_3_name")
    private String passiveSkill3Name;
    @TableField("passive_skill_3_icon")
    private String passiveSkill3Icon;
    @TableField("passive_skill_3_desc")
    private String passiveSkill3Desc;

    private String treasureName;
    private String treasureIcon;
    private String treasureDesc;
    // JSON格式的AI分析结果
    private String aiAnalysis;
    // 避免NULL被跳过
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String runeSetReqs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

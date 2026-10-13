package com.dvm.rune_server_backend.model.dto;

import com.dvm.rune_server_backend.common.DragonConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author 25826
 */
@Data
public class DragonRequest {
    @NotBlank(message = "龙的名称不能为空")
    private String dragonName;
    private String dragonImage;
    @NotBlank(message = "属性不能为空")
    @Pattern(regexp = DragonConstants.ELEMENT_REGEX, message = "属性不在允许范围内")
    private String element;
    @NotBlank(message = "类别不能为空")
    @Pattern(regexp = DragonConstants.CATEGORY_REGEX, message = "类别不在允许范围内")
    private String category;

    private String leaderSkillName;
    private String leaderSkillIcon;
    private String leaderSkillDesc;

    private String activeSkillName;
    private String activeSkillIcon;
    private String activeSkillDesc;
    private Integer manaCost;

    private String passiveSkill1Name;
    private String passiveSkill1Icon;
    private String passiveSkill1Desc;
    private String passiveSkill2Name;
    private String passiveSkill2Icon;
    private String passiveSkill2Desc;
    private String passiveSkill3Name;
    private String passiveSkill3Icon;
    private String passiveSkill3Desc;

    private String treasureName;
    private String treasureIcon;
    private String treasureDesc;

    private String runeSetReqs;
}

package com.dvm.rune_server_backend.common;

/**
 * @author 25826
 * 龙的属性 / 类别白名单，用于防止内容超出枚举范围
 * 与数据库的 ENUM 声明顺序保持一致
 */
public class DragonConstants {
    private DragonConstants() {}

    // 正则表达式约束
    public static final String ELEMENT_REGEX  = "^(火|水|地|光|暗)$";
    public static final String CATEGORY_REGEX = "^(攻击|防御|辅助|治疗)$";
}

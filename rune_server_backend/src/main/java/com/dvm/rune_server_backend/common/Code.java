package com.dvm.rune_server_backend.common;

/**
 * @author 25826
 * 状态码及对应描述信息
 */
public enum Code {

    LOGIN_ERROR(401, "用户名或密码错误"),
    USER_EXISTS(402, "用户已存在"),
    LOGIN_TIME_OUT(403, "登录失效"),

    INSERT_ERROR(411, "添加失败"),
    UPDATE_ERROR(412, "更新失败"),
    DELETE_ERROR(413, "删除失败"),

    FILE_EMPTY(421, "文件为空"),
    FILE_SAVE_ERROR(422, "文件保存失败"),
    FILE_TYPE_ERROR(423, "文件类型错误"),
    FILE_SIZE_EXCEED(424, "文件大小超过限制"),

    RUNE_LOCKED_EQUIP(414, "该符文已锁定，不可装备"),
    RUNE_LOCKED_REMOVE(415, "该符文已锁定，不可从当前龙卸下"),
    RUNE_LOCKED_DELETE(416, "该符文已锁定，不可删除"),
    ;

    private final int code;
    private final String message;

    Code(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}

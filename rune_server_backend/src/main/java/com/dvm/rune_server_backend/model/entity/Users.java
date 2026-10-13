package com.dvm.rune_server_backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 25826
 * users表
 * 主键字段不为 Id，需要使用TableId注解说明
 */
@Data
@TableName("users")
public class Users {
    @TableId(value = "user_id")
    private Long userId;
    private String username;
    private String passwordHash;
    private LocalDateTime createdAt;
}

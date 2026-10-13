package com.dvm.rune_server_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.core.annotation.Order;

/**
 * @author 25826
 */
@Data
@Order
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}

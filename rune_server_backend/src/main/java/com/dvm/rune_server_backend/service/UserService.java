package com.dvm.rune_server_backend.service;

import com.dvm.rune_server_backend.common.Result;

/**
 * @author 25826
 */
public interface UserService {

    // 注册
    Result<?> register(String username, String password);

    // 登录
    Result<?> login(String username, String password);
}

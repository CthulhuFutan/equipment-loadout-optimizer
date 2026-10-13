package com.dvm.rune_server_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dvm.rune_server_backend.common.Code;
import com.dvm.rune_server_backend.common.Result;
import com.dvm.rune_server_backend.config.JwtUtil;
import com.dvm.rune_server_backend.mapper.UserMapper;
import com.dvm.rune_server_backend.model.entity.Users;
import com.dvm.rune_server_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 25826
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 注册
    @Override
    public Result<?> register(String username, String password) {
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<Users>().eq(Users::getUsername, username);
        if (!userMapper.selectList(wrapper).isEmpty()) {
            return Result.error(Code.USER_EXISTS);
        }
        Users user = new Users();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        userMapper.insert(user);
        return Result.success();
    }

    // 登录
    @Override
    public Result<?> login(String username, String password) {
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<Users>().eq(Users::getUsername, username);
        Users user = userMapper.selectOne(wrapper);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return Result.error(Code.LOGIN_ERROR);
        }
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername());
        // 前端传输 token、username、userId
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getUsername());
        data.put("userId", user.getUserId());
        return Result.success(data);
    }
}

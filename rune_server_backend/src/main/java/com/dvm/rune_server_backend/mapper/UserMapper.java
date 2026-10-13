package com.dvm.rune_server_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dvm.rune_server_backend.model.entity.Users;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author 25826
 * BaseMapper 已经提供了所有基础 CRUD，不需要手写 CRUD 方法
 */
@Mapper
public interface UserMapper extends BaseMapper<Users> {
    // 1. 根据id进行索引

    // 2. 根据用户名进行索引

    // 3. 新增一个用户（注册）

    // 4. 检查该用户名是否存在（username >= 1）
}

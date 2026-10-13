package com.dvm.rune_server_backend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 25826
 * 数据返回对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public static<T> Result<T> success(){
        return new Result<>(200, "success", null);
    }
    public static<T> Result<T> success(T data){
        return new Result<>(200, "success", data);
    }
    public static<T> Result<T> error(Integer code, String message){
        return new Result<>(code, message, null);
    }
    public static<T> Result<T> error(Code status){
        return new Result<>(status.getCode(), status.getMessage(), null);
    }

}

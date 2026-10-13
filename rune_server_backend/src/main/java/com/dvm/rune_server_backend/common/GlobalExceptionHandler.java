package com.dvm.rune_server_backend.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
/**
 * @author 25826
 * 报错统一处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // @Valid校验失败时触发
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e){
        /*
         步骤：取出容器 → 从容器中取出所有错误信息并返回List<FieldError> → 把List转成一个个流，便于链式处理
                       → 对每个 FieldError 取出它的 defaultMessage，即DTO中的message → 将流里面的字符串使用 "," 拼接起来
         String msg = e.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(","));
         */
        // 更改为按顺序仅输出第一条报错信息
        Field[] fields = e.getBindingResult().getTarget().getClass().getDeclaredFields();
        Map<String, Integer> fieldOrder = new HashMap<>();
        for (int i = 0; i < fields.length; i++) {
            fieldOrder.put(fields[i].getName(), i);
        }

        String msg = e.getBindingResult()
                      .getFieldErrors()
                      .stream()
                      .sorted(Comparator.comparingInt(fe -> fieldOrder.getOrDefault(fe.getField(), Integer.MAX_VALUE)))
                      .findFirst()
                      .map(DefaultMessageSourceResolvable::getDefaultMessage)
                      .orElse("参数校验失败");
        return Result.error(400, msg);
    }

    // 上传文件超过 spring.servlet.multipart.max-file-size / max-request-size
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e){
        log.warn("上传文件超出大小限制: {}", e.getMessage());
        return Result.error(Code.FILE_SIZE_EXCEED);
    }

    // 全局兜底
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e){
        log.error("系统异常", e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }
}

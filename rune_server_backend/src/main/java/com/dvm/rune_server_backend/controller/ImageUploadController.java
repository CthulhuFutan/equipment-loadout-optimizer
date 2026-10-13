package com.dvm.rune_server_backend.controller;

import com.dvm.rune_server_backend.common.Code;
import com.dvm.rune_server_backend.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @author 25826
 * 进行图片上传
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class ImageUploadController {

    // 文件类型白名单
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    // 传输文件夹路径白名单
    private static final Set<String> UPLOAD_TYPES = Set.of("dragon", "skill", "treasure");
    // 单文件大小上限设置为 3MB
    private static final long MAX_FILE_SIZE = 3L * 1024 * 1024;

    @PostMapping
    public Result<?> upload(@RequestParam("file") MultipartFile file, @RequestParam String type){
        // 防止 type=../../ 拼进路径造成目录穿越
        if(!UPLOAD_TYPES.contains(type)){
            return Result.error(Code.FILE_TYPE_ERROR);
        }
        if(file.isEmpty()){
            return Result.error(Code.FILE_EMPTY);
        }
        if(file.getSize() > MAX_FILE_SIZE){
            return Result.error(Code.FILE_SIZE_EXCEED);
        }
        // 拦截非图片文件
        String contentType = file.getContentType();
        if(contentType == null || !contentType.startsWith("image/")){
            return Result.error(Code.FILE_TYPE_ERROR);
        }
        // 文件名净化：只取 basename，去掉多余路径成分（../、C:\ 等），根据需求保留原名
        String rawName = file.getOriginalFilename();
        String fileName = rawName == null ? "" : rawName.replace('\\', '/');
        int slash = fileName.lastIndexOf('/');
        if(slash >= 0){
            fileName = fileName.substring(slash + 1);
        }
        if(fileName.isEmpty() || fileName.equals(".") || fileName.equals("..")
                || fileName.startsWith("..") || fileName.length() > 200
                || !ALLOWED_IMAGE_TYPES.contains(getExtension(fileName))){
            return Result.error(Code.FILE_TYPE_ERROR);
        }
        String root = System.getProperty("user.dir");
        File dir = new File(root, "uploads/" + type + "/");
        if(!dir.exists()){
            dir.mkdirs();
        }
        try {
            file.transferTo(new File(dir, fileName));
        } catch (IOException e) {
            return Result.error(Code.FILE_SAVE_ERROR);
        }
        String url = "/uploads/" + type + "/" + fileName;
        return Result.success(url);
    }

    // 获取文件扩展名
    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot == -1 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}

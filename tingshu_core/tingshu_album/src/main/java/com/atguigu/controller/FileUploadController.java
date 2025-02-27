package com.atguigu.controller;

import com.atguigu.result.RetVal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.atguigu.minio.MinioUploader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author 林长启
 * @since 2025-02-19
 */
@Tag(name = "上传管理接口")
@RestController
@RequestMapping(value = "/api/album")
public class FileUploadController {
    @Autowired
    private MinioUploader minioUploader;

    @Operation(summary = "文件上传")
    @PostMapping("fileUpload")
    public RetVal fileUpload(MultipartFile file) throws Exception {
        String retUrl = minioUploader.uploadFile(file);
        return RetVal.ok(retUrl);
    }

}
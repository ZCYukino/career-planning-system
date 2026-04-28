package com.career.planning.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {

    String uploadFile(MultipartFile file, String directory);

    String uploadFile(byte[] data, String fileName, String directory);

    void deleteFile(String fileUrl);

    String getFileUrl(String objectKey);

    /**
     * 若为本账号 OSS 的公网 URL，则使用 SDK 带鉴权下载（适用于私有 Bucket；公开桶也可走此路径）。
     * 无法识别为当前 OSS 时返回 null。
     */
    byte[] downloadByPublicUrl(String fileUrl);
}

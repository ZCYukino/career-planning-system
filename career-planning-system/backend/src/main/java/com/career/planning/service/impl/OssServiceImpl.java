package com.career.planning.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.career.planning.common.BusinessException;
import com.career.planning.config.OssConfig;
import com.career.planning.service.OssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@ConditionalOnBean(OSS.class)
public class OssServiceImpl implements OssService {

    private static final Logger log = LoggerFactory.getLogger(OssServiceImpl.class);

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssConfig ossConfig;

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID().toString() + extension;
        String objectKey = directory + "/" + getDatePath() + "/" + fileName;

        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossConfig.getBucketName(),
                    objectKey,
                    file.getInputStream()
            );
            ossClient.putObject(putObjectRequest);
            log.info("文件上传成功: {}", objectKey);
            return getFileUrl(objectKey);
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadFile(byte[] data, String fileName, String directory) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("文件数据不能为空");
        }

        String objectKey = directory + "/" + getDatePath() + "/" + fileName;

        PutObjectRequest putObjectRequest = new PutObjectRequest(
                ossConfig.getBucketName(),
                objectKey,
                new ByteArrayInputStream(data)
        );
        ossClient.putObject(putObjectRequest);
        log.info("文件上传成功: {}", objectKey);
        return getFileUrl(objectKey);
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }

        String objectKey = extractObjectKey(fileUrl);
        if (StringUtils.hasText(objectKey)) {
            ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
            log.info("文件删除成功: {}", objectKey);
        }
    }

    @Override
    public byte[] downloadByPublicUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)
                || (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"))) {
            return null;
        }
        String key = extractObjectKey(fileUrl);
        if (!StringUtils.hasText(key) || key.startsWith("http://") || key.startsWith("https://")) {
            log.debug("无法从 URL 解析为本 Bucket 的 objectKey，跳过 OSS SDK 下载: {}", fileUrl);
            return null;
        }
        try (OSSObject object = ossClient.getObject(ossConfig.getBucketName(), key)) {
            byte[] bytes = object.getObjectContent().readAllBytes();
            log.info("OSS SDK 下载简历成功 key={}, bytes={}", key, bytes.length);
            return bytes;
        } catch (Exception e) {
            log.warn("OSS SDK 下载简历失败 url={}: {}", fileUrl, e.getMessage());
            return null;
        }
    }

    @Override
    public String getFileUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }

        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }

        String endpoint = ossConfig.getEndpoint();
        if (endpoint.startsWith("https://")) {
            endpoint = endpoint.substring(8);
        } else if (endpoint.startsWith("http://")) {
            endpoint = endpoint.substring(7);
        }

        return "https://" + ossConfig.getBucketName() + "." + endpoint + "/" + objectKey;
    }

    private String getDatePath() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    private String extractObjectKey(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }

        String url = fileUrl.trim();
        // 去掉 query string 和 fragment
        int q = url.indexOf('?');
        if (q >= 0) {
            url = url.substring(0, q);
        }
        int h = url.indexOf('#');
        if (h >= 0) {
            url = url.substring(0, h);
        }

        String endpoint = ossConfig.getEndpoint();
        if (endpoint.startsWith("https://")) {
            endpoint = endpoint.substring(8);
        } else if (endpoint.startsWith("http://")) {
            endpoint = endpoint.substring(7);
        }

        String bucketName = ossConfig.getBucketName();
        String prefix = bucketName + "." + endpoint + "/";

        // 优先匹配 "bucket.endpoint/" 格式（标准 OSS URL）
        if (url.contains(prefix)) {
            int index = url.indexOf(prefix);
            String key = url.substring(index + prefix.length());
            log.debug("从URL提取objectKey (前缀匹配): {} -> {}", fileUrl, key);
            return key;
        }

        // 兜底：如果bucketName直接出现在路径中（简化格式）
        // 例如: https://oss-cn-hangzhou.aliyuncs.com/bucketName/path 或 file:///path
        if (url.contains(bucketName + "/")) {
            int index = url.indexOf(bucketName + "/");
            String key = url.substring(index + bucketName.length() + 1);
            log.debug("从URL提取objectKey (bucket路径): {} -> {}", fileUrl, key);
            return key;
        }

        // 无法解析，返回原URL作为key
        log.warn("无法从URL提取objectKey，返回原值: {}", fileUrl);
        return fileUrl;
    }
}

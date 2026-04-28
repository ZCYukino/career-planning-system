package com.career.planning.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Bean
    @ConditionalOnExpression("'${aliyun.oss.access-key-id:}' != '' && '${aliyun.oss.access-key-secret:}' != ''")
    public OSS ossClient() {
        System.out.println("========================================");
        System.out.println("阿里云 OSS 配置:");
        System.out.println("  Endpoint: " + endpoint);
        System.out.println("  Bucket: " + bucketName);
        System.out.println("  AccessKeyId: " + (accessKeyId != null && !accessKeyId.isEmpty() ? accessKeyId.substring(0, 8) + "***" : "未配置"));
        System.out.println("  AccessKeySecret: " + (accessKeySecret != null && !accessKeySecret.isEmpty() ? "已配置" : "未配置"));
        System.out.println("OSS Client 初始化成功!");
        System.out.println("========================================");
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}

package com.career.planning.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.career.planning.service.EmbeddingService;
import com.career.planning.util.NullSafeUtil;
import com.career.planning.util.StringUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 阿里云百炼文本嵌入服务
 * 使用 text-embedding-v4 模型（2048维度）
 * 仅在 ai.embedding-provider=dashscope 时加载
 */
@Service
@ConditionalOnProperty(name = "ai.embedding-provider", havingValue = "dashscope", matchIfMissing = true)
public class DashScopeEmbeddingServiceImpl implements EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(DashScopeEmbeddingServiceImpl.class);

    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
    private static final String MODEL = "text-embedding-v4";
    private static final int DIMENSIONS = 2048;

    @Value("${dashscope.api-key}")
    private String apiKey;

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public float[] embed(String text) {
        if (StringUtil.isBlank(text)) {
            logger.warn("输入文本为空，返回零向量");
            return new float[DIMENSIONS];
        }
        try {
            return generateEmbedding(text);
        } catch (Exception e) {
            logger.error("生成文本向量失败: {}", e.getMessage());
            return new float[DIMENSIONS];
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        if (NullSafeUtil.isEmpty(texts)) {
            return results;
        }
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public int getDimension() {
        return DIMENSIONS;
    }

    private float[] generateEmbedding(String text) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("input", text);
        requestBody.put("dimensions", DIMENSIONS);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toJSONString(), MEDIA_TYPE_JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "无响应体";
                logger.error("百炼 Embedding API 调用失败: HTTP {} - {}", response.code(), errorBody);
                return new float[DIMENSIONS];
            }

            String responseBody = response.body().string();
            return parseEmbeddingFromResponse(responseBody);
        }
    }

    private float[] parseEmbeddingFromResponse(String responseBody) {
        try {
            JSONObject responseJson = JSON.parseObject(responseBody);

            if (responseJson.containsKey("error")) {
                JSONObject error = responseJson.getJSONObject("error");
                String errorMsg = error != null ? error.getString("message") : "未知错误";
                logger.error("百炼 Embedding API 错误: {}", errorMsg);
                return new float[DIMENSIONS];
            }

            JSONObject data = responseJson.getJSONArray("data").getJSONObject(0);
            JSONArray embeddingArray = data.getJSONArray("embedding");

            float[] result = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                result[i] = embeddingArray.getFloatValue(i);
            }

            logger.debug("成功解析 {} 维向量", result.length);
            return result;

        } catch (Exception e) {
            logger.error("解析 embedding 响应失败: {}", e.getMessage());
            return new float[DIMENSIONS];
        }
    }
}

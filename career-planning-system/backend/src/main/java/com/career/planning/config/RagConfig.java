package com.career.planning.config;

import com.career.planning.service.QdrantRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG服务配置
 * 负责在应用启动时初始化RAG服务
 */
@Configuration
public class RagConfig {

    private static final Logger logger = LoggerFactory.getLogger(RagConfig.class);

    @Autowired(required = false)
    private QdrantRagService qdrantRagService;

    /**
     * 应用启动后初始化RAG服务
     * 如果Qdrant不可用，会自动跳过初始化
     */
    @Bean
    public ApplicationRunner ragInitializer() {
        return args -> {
            logger.info("========== RAG服务初始化检查 ==========");

            if (qdrantRagService != null) {
                if (qdrantRagService.isServiceAvailable()) {
                    logger.info("Qdrant服务可用，正在初始化向量数据库...");
                    try {
                        qdrantRagService.initializeCollection();
                        logger.info("RAG服务初始化完成！");
                    } catch (Exception e) {
                        logger.warn("RAG服务初始化失败，将使用MySQL降级方案: {}", e.getMessage());
                    }
                } else {
                    logger.warn("Qdrant服务不可用，将使用MySQL数据库作为备选方案");
                    logger.warn("如需启用RAG功能，请启动Qdrant: scripts\\start-qdrant.bat");
                }
            } else {
                logger.info("Qdrant服务未配置，使用MySQL数据库作为知识库");
            }

            logger.info("==========================================");
        };
    }
}

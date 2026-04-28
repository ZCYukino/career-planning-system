-- ============================================================
-- 职业规划系统数据库初始化脚本（评委一键版）
-- 数据库: career_planning_system
-- 字符集: utf8mb4
-- 说明: 基于 MyBatis-Plus 实体类自动生成，与代码完全一致
--       包含 15 个核心岗位的完整种子数据
--       popularity_score / job_description / industry / work_location / company_name
--       等字段已同步 job_profiles_15.json 的真实数据
-- ============================================================

CREATE DATABASE IF NOT EXISTS `career_planning_system`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `career_planning_system`;

-- 禁用外键检查（兼容旧数据残留和 DROP TABLE 执行顺序）
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 第一步：删除旧表（按依赖顺序，从外到内）
-- ============================================================
DROP TABLE IF EXISTS `job_match_record`;
DROP TABLE IF EXISTS `career_plan_report`;
DROP TABLE IF EXISTS `student_ability_profile`;
DROP TABLE IF EXISTS `job_skill_tag`;
DROP TABLE IF EXISTS `job_skill_dimension`;
DROP TABLE IF EXISTS `job_dimension_match`;
DROP TABLE IF EXISTS `job_transfer_path`;
DROP TABLE IF EXISTS `job_promotion_path`;
DROP TABLE IF EXISTS `job_promotion_relation`;
DROP TABLE IF EXISTS `job_basic_info`;
DROP TABLE IF EXISTS `student_info`;
DROP TABLE IF EXISTS `local_knowledge_base`;
DROP TABLE IF EXISTS `knowledge_vector`;

-- ============================================================
-- 第二步：创建表结构（与 Java 实体类 @TableName 完全一致）
-- ============================================================

-- 2.1 学生信息表
CREATE TABLE `student_info` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`             VARCHAR(100) NOT NULL COMMENT '用户名',
    `password`             VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `name`                 VARCHAR(100)          DEFAULT NULL COMMENT '姓名',
    `gender`               VARCHAR(10)            DEFAULT NULL COMMENT '性别',
    `email`                VARCHAR(255)           DEFAULT NULL COMMENT '邮箱',
    `age`                  INT                    DEFAULT NULL COMMENT '年龄',
    `major`                VARCHAR(200)           DEFAULT NULL COMMENT '专业',
    `graduation_school`    VARCHAR(200)          DEFAULT NULL COMMENT '毕业院校',
    `education`            VARCHAR(50)            DEFAULT NULL COMMENT '学历',
    `graduation_year`      INT                    DEFAULT NULL COMMENT '毕业年份',
    `career_intention`     VARCHAR(500)          DEFAULT NULL COMMENT '职业意向',
    `personality_traits`  VARCHAR(500)          DEFAULT NULL COMMENT '性格特质',
    `work_experience_years` VARCHAR(100)         DEFAULT NULL COMMENT '工作年限',
    `resume_file_path`     VARCHAR(500)          DEFAULT NULL COMMENT '简历文件路径',
    `create_time`          DATETIME               DEFAULT NULL COMMENT '创建时间',
    `update_time`          DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生信息表';

-- 2.2 岗位基本信息表
CREATE TABLE `job_basic_info` (
    `id`                BIGINT           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_code`          VARCHAR(100)              DEFAULT NULL COMMENT '岗位编码',
    `job_name`          VARCHAR(200)             DEFAULT NULL COMMENT '岗位名称',
    `job_rank`          INT                      DEFAULT NULL COMMENT '岗位等级',
    `job_count`         INT                      DEFAULT NULL COMMENT '岗位数量',
    `popularity_score`  DECIMAL(10,6)           DEFAULT NULL COMMENT '热门程度评分',
    `industry`          VARCHAR(200)              DEFAULT NULL COMMENT '行业',
    `salary_range`      VARCHAR(100)              DEFAULT NULL COMMENT '薪资范围',
    `work_location`     VARCHAR(200)              DEFAULT NULL COMMENT '工作地点',
    `company_name`      VARCHAR(200)              DEFAULT NULL COMMENT '公司名称',
    `company_size`      VARCHAR(100)              DEFAULT NULL COMMENT '公司规模',
    `company_nature`    VARCHAR(100)              DEFAULT NULL COMMENT '公司性质',
    `job_description`   TEXT                     DEFAULT NULL COMMENT '岗位描述',
    `company_intro`     TEXT                     DEFAULT NULL COMMENT '公司介绍',
    `is_core_job`       TINYINT(1)               DEFAULT NULL COMMENT '是否核心岗位',
    `created_at`        DATETIME                  DEFAULT NULL COMMENT '创建时间',
    `updated_at`        DATETIME                  DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_name` (`job_name`),
    KEY `idx_industry` (`industry`),
    KEY `idx_is_core_job` (`is_core_job`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位基本信息表';

-- 2.3 岗位晋升关系表（此前完全缺失）
CREATE TABLE `job_promotion_relation` (
    `id`                    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `current_job_id`        BIGINT              DEFAULT NULL COMMENT '当前岗位ID',
    `current_job_name`      VARCHAR(200)         DEFAULT NULL COMMENT '当前岗位名称',
    `next_job_id`           BIGINT              DEFAULT NULL COMMENT '晋升目标岗位ID',
    `next_job_name`         VARCHAR(200)         DEFAULT NULL COMMENT '晋升目标岗位名称',
    `promotion_path_desc`   TEXT                DEFAULT NULL COMMENT '晋升路径描述',
    `required_skills`        TEXT                DEFAULT NULL COMMENT '所需技能补充',
    `experience_years`      VARCHAR(100)         DEFAULT NULL COMMENT '经验年限要求',
    `salary_increase_range`  VARCHAR(100)         DEFAULT NULL COMMENT '薪资涨幅范围',
    `created_at`            DATETIME             DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_current_job_name` (`current_job_name`),
    KEY `idx_next_job_name` (`next_job_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位晋升关系表';

-- 2.4 岗位技能标签表（此前完全缺失）
CREATE TABLE `job_skill_tag` (
    `id`                BIGINT            NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id`            BIGINT                    DEFAULT NULL COMMENT '岗位ID',
    `tag_name`          VARCHAR(200)              DEFAULT NULL COMMENT '标签名称',
    `dimension`         VARCHAR(100)              DEFAULT NULL COMMENT '维度',
    `weight`            DECIMAL(10,6)             DEFAULT NULL COMMENT '权重',
    `requirement_level` VARCHAR(100)              DEFAULT NULL COMMENT '要求等级',
    `created_at`        DATETIME                  DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_dimension` (`dimension`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位技能标签表';

-- 2.5 岗位换岗路径表
CREATE TABLE `job_transfer_path` (
    `id`                 BIGINT           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_job_id`      BIGINT                   DEFAULT NULL COMMENT '源岗位ID',
    `source_job_name`    VARCHAR(200)              DEFAULT NULL COMMENT '源岗位名称',
    `target_job_name`    VARCHAR(200)              DEFAULT NULL COMMENT '目标岗位名称',
    `difficulty_level`   INT                      DEFAULT NULL COMMENT '难度等级',
    `required_skills`     TEXT                     DEFAULT NULL COMMENT '所需技能',
    `transfer_duration`  VARCHAR(100)              DEFAULT NULL COMMENT '转换时长',
    `advantage_analysis` TEXT                     DEFAULT NULL COMMENT '优势分析',
    `path_index`         INT                      DEFAULT NULL COMMENT '路径索引',
    `created_at`         DATETIME                  DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_source_job_name` (`source_job_name`),
    KEY `idx_target_job_name` (`target_job_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位换岗路径表';

-- 2.6 学生能力画像表
CREATE TABLE `student_ability_profile` (
    `id`                    BIGINT            NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id`            BIGINT                    DEFAULT NULL COMMENT '学生ID',
    `dimension`             VARCHAR(100)              DEFAULT NULL COMMENT '能力维度',
    `score`                 DECIMAL(10,6)             DEFAULT NULL COMMENT '评分',
    `completeness_score`    DECIMAL(10,6)             DEFAULT NULL COMMENT '完整度评分',
    `competitiveness_score` DECIMAL(10,6)             DEFAULT NULL COMMENT '竞争力评分',
    `details`               TEXT                     DEFAULT NULL COMMENT '详细说明',
    `shortcomings`          TEXT                     DEFAULT NULL COMMENT '不足之处',
    `strengths`             TEXT                     DEFAULT NULL COMMENT '优势之处',
    `created_at`            DATETIME                  DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_dimension` (`dimension`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生能力画像表';

-- 2.7 职业规划报告表
CREATE TABLE `career_plan_report` (
    `id`              BIGINT           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id`      BIGINT                   DEFAULT NULL COMMENT '学生ID',
    `target_job_id`   BIGINT                   DEFAULT NULL COMMENT '目标岗位ID',
    `target_job_name` VARCHAR(255)              DEFAULT NULL COMMENT '目标岗位名称',
    `match_score`     DECIMAL(10,6)             DEFAULT NULL COMMENT '匹配度评分',
    `report_content`  TEXT                     DEFAULT NULL COMMENT '报告内容',
    `action_plan`     TEXT                     DEFAULT NULL COMMENT '行动计划',
    `status`          VARCHAR(50)               DEFAULT NULL COMMENT '状态',
    `progress`        INT                      DEFAULT 0 COMMENT '生成进度 0-100',
    `current_step`    INT                      DEFAULT 1 COMMENT '当前阶段 1-4',
    `step_name`       VARCHAR(255)              DEFAULT NULL COMMENT '当前阶段名称',
    `student_profile_json` TEXT                 DEFAULT NULL COMMENT '学生画像JSON',
    `job_profile_json` TEXT                    DEFAULT NULL COMMENT '岗位画像JSON',
    `match_analysis_json` TEXT                  DEFAULT NULL COMMENT '人岗匹配分析JSON',
    `error_message`   TEXT                     DEFAULT NULL COMMENT '错误信息',
    `created_at`      DATETIME                  DEFAULT NULL COMMENT '创建时间',
    `updated_at`      DATETIME                  DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_target_job_id` (`target_job_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='职业规划报告表';

-- 为已有数据库添加 target_job_name 字段（如果表已存在）
-- ALTER TABLE `career_plan_report` ADD COLUMN `target_job_name` VARCHAR(255) DEFAULT NULL COMMENT '目标岗位名称' AFTER `target_job_id`;

-- 2.8 岗位维度匹配表
CREATE TABLE `job_dimension_match` (
    `id`                 BIGINT            NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id`             BIGINT                    DEFAULT NULL COMMENT '岗位ID',
    `dimension`          VARCHAR(100)              DEFAULT NULL COMMENT '维度',
    `weight`             DECIMAL(10,6)             DEFAULT NULL COMMENT '权重',
    `score`              DECIMAL(10,6)             DEFAULT NULL COMMENT '评分',
    `description`         TEXT                     DEFAULT NULL COMMENT '描述',
    `evaluation_criteria` TEXT                     DEFAULT NULL COMMENT '评价标准',
    `created_at`         DATETIME                  DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_dimension` (`dimension`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位维度匹配表';

-- 2.9 岗位晋升路径表
CREATE TABLE `job_promotion_path` (
    `id`              BIGINT           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id`          BIGINT                   DEFAULT NULL COMMENT '岗位ID',
    `stage`           VARCHAR(100)              DEFAULT NULL COMMENT '阶段',
    `stage_name`      VARCHAR(200)              DEFAULT NULL COMMENT '阶段名称',
    `experience_years` VARCHAR(100)              DEFAULT NULL COMMENT '经验年限',
    `description`      TEXT                     DEFAULT NULL COMMENT '阶段描述',
    `salary_range`    VARCHAR(100)              DEFAULT NULL COMMENT '薪资范围',
    `created_at`      DATETIME                  DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位晋升路径表';

-- 2.10 岗位技能维度表
CREATE TABLE `job_skill_dimension` (
    `id`               BIGINT            NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id`           BIGINT                    DEFAULT NULL COMMENT '岗位ID',
    `dimension`        VARCHAR(100)              DEFAULT NULL COMMENT '维度',
    `skill_content`    TEXT                     DEFAULT NULL COMMENT '技能内容',
    `weight`           DECIMAL(10,6)             DEFAULT NULL COMMENT '权重',
    `requirement_level` VARCHAR(100)            DEFAULT NULL COMMENT '要求等级',
    `created_at`       DATETIME                  DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_dimension` (`dimension`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位技能维度表';

-- 2.11 本地知识库表
CREATE TABLE `local_knowledge_base` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`       VARCHAR(500)         DEFAULT NULL COMMENT '标题',
    `content`     TEXT                 DEFAULT NULL COMMENT '内容',
    `category`    VARCHAR(100)          DEFAULT NULL COMMENT '分类',
    `file_path`   VARCHAR(500)          DEFAULT NULL COMMENT '文件路径',
    `tags`        VARCHAR(500)          DEFAULT NULL COMMENT '标签',
    `keywords`    VARCHAR(500)          DEFAULT NULL COMMENT '关键词',
    `source`      VARCHAR(200)          DEFAULT NULL COMMENT '来源',
    `created_at`  DATETIME             DEFAULT NULL COMMENT '创建时间',
    `updated_at`  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_keywords` (`keywords`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='本地知识库表';

-- 2.12 知识库向量表（rank 为 MySQL 保留字，使用反引号转义）
CREATE TABLE `knowledge_vector` (
    `id`               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `vector_type`       VARCHAR(100)          DEFAULT NULL COMMENT '向量类型',
    `job_name`         VARCHAR(200)          DEFAULT NULL COMMENT '关联岗位名称',
    `content`           TEXT                  DEFAULT NULL COMMENT '向量内容',
    `vector_embedding`  TEXT                  DEFAULT NULL COMMENT '向量嵌入（JSON）',
    `metadata`          TEXT                  DEFAULT NULL COMMENT '元数据JSON',
    `rank`             INT                   DEFAULT NULL COMMENT '关联岗位排名（MySQL保留字）',
    `chunk_id`         VARCHAR(100)          DEFAULT NULL COMMENT '片段来源标识',
    `created_at`        DATETIME              DEFAULT NULL COMMENT '创建时间',
    `updated_at`        DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_vector_type` (`vector_type`),
    KEY `idx_job_name` (`job_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库向量表';

-- ============================================================
-- 第三步：插入岗位基础数据（15个核心岗位）
-- 数据来源：data/job_profiles_15.json，已同步真实招聘数据
-- ============================================================

INSERT INTO `job_basic_info` (`job_code`, `job_name`, `job_rank`, `job_count`, `popularity_score`, `industry`, `salary_range`, `work_location`, `company_name`, `company_size`, `company_nature`, `job_description`, `is_core_job`) VALUES
(1, 'Java开发工程师', 1, 190, 150.0,
 '计算机软件;IT服务;互联网;网络/信息安全;电子商务',
 '1.2-2万/4000-7000元/6000-8000元/2000-3000元/2-3万',
 '济南-历下区;大连-甘井子区;武汉-武昌区;石家庄-桥西区;西安-新城区',
 '济南北海软件工程有限公司;辽宁省大连市世骥计算机软件开发有限公司;南京海泰医疗信息系统有限公司;河北晨光网络科技有限公司;大连高新园区仁源计算机软件开发有限公司',
 '100-499人;50-99人;100-499人;50-99人;100-499人',
 '民营企业;民营企业;民营企业;民营企业;民营企业',
 '【岗位职责】此岗位可接收2026届应届毕业生\n1、按产品需求规格说明书，进行软件功能模块的开发，包括前端页面、后端接口功能开发；\n2、根据项目任务计划按时完成软件编码和单元测试工作；\n3、按照开发流程编写相应模块的设计文档，与产品经理、测试工程师、其他团队沟通合作，保证产品研发工作的正常开展；\n4、负责配合测试人员进行软件平台BUG的修复；\n5、深度参与信息化系统开发，推动企业研发、营销、财务、智能物流及数字化工厂等业务场景的全面数字化转型；\n6、熟练运用 COZE、DIFY 等平台，独立完成 AI 流程编排应用的开发工作，并持续进行优化迭代；\n7、积极探索大模型技术在实际业务场景的应用路径，将 AI 应用无缝集成至数字化平台，有效提升信息化智能水平。\n\n【基本要求】\n1、计算机类相关专业，本科及以上学历，具备扎实的专业理论基础；\n2、Java 开发能力：熟练掌握 Java 语言，熟悉Spring Cloud、Spring、SpringBoot、MyBatis、MySQL、Redis、Zookeeper等框架及应用；深入理解微服务架构，熟练运用 Dubbo 框架进行服务开发与管理；\n3、大模型部署经验：拥有大模型部署实践经验，熟悉 Ollama 或 xinference 等平台；\n4、AI 流程开发：至少熟练掌握 COZE、DIFY 其中一种平台，可独立完成流程编排应用的开发任务；\n5、技术储备：熟悉 Linux 系统操作及 Shell 脚本编写，掌握 Docker 容器化技术。\n\n【Java资深工程师/架构师】\n1、计算机相关专业，本科及以上学历，8年以上工作经验，985、211优先；\n2、8年及以上JAVA开发经验，精通JVM原理；5年及以上架构设计经验，3年及以上微服务研发经验；\n3、精通Spring-Boot、Spring-Cloud、MyBatis、MySQL，精通Redis、MongoDB、HBase，精通消息中间件RocketMQ、Kafka等；\n4、熟练掌握Docker、Kubernetes，熟悉DevOps相关工具（Git、Maven、Jenkins、Ansible等）；\n5、熟练掌握主流Cesiumjs三维GIS引擎，具有三维GIS平台应用开发经验。',
 1),

(2, '测试工程师', 2, 523, 311.5,
 '计算机软件;IT服务;互联网;电子/半导体/集成电路;电子商务',
 '7000-13000元/5000-7000元/5000-8000元/8000-11000元/5000-9000元',
 '成都-锦江区;上海-黄浦区;天津-西青区;昆明-呈贡区;承德-丰宁满族自治县',
 '源创客成都科技有限公司;天阳科技;华清瑞达;软通动力信息技术(集团)股份有限公司;深圳市易玛特科技有限公司',
 '100-499人;1000-9999人;100-499人;10000人以上;50-99人',
 '民营企业;民营企业;民营企业;民营企业;民营企业',
 '【主要职责】\n·参与需求评审，理解产品功能和业务逻辑\n·根据需求文档编写测试用例，执行功能测试、回归测试\n·使用缺陷跟踪工具（如禅道、JIRA）记录、跟踪和验证缺陷\n·执行API测试和基本的数据库验证\n·参与测试计划的制定和测试报告的编写\n·在指导下完成性能测试的基本执行工作\n·遵守团队的质量标准和测试流程\n\n【基本要求】\n·计算机相关专业本科及以上学历\n·了解软件测试基本理论和测试流程\n·熟悉常见的测试方法（黑盒测试、边界值分析、等价类划分等）\n·了解至少一种缺陷管理工具（JIRA、禅道等）\n·具备基础的SQL知识，能编写简单查询语句\n·良好的逻辑思维能力和问题分析能力\n·细心、耐心，具有良好的文档编写能力\n·良好的团队协作精神和沟通能力\n\n【加分项】\n·了解基本的自动化测试工具（如Selenium、Postman）\n·了解基本的Linux命令\n·有实习或项目测试经验',
 1),

(3, '技术支持工程师', 3, 256, 163.0,
 '计算机软件;IT服务;互联网;网络/信息安全',
 '4000-6000元/3000-4000元/4000-7000元/5000-8000元/6000-8000元',
 '重庆-江北区;贵阳-云岩区;营口-鲅鱼圈区;广州-南沙区;上海-松江区',
 '广州红海云计算股份有限公司;安宁盈科;武汉浩谱海洋探测系统有限公司;广东化一环境科技有限公司;超年实业(上海)有限公司',
 '1000-9999人;50-99人;50-99人;50-99人;100-499人',
 '民营企业;民营企业;民营企业;民营企业;民营企业',
 '【岗位职责】\n1、负责易控系列软件产品、软件方案的售前售后技术支持；\n2、参与软件产品的需求沟通、方案制做、产品功能演示和方案展示；\n3、负责软件产品平台方案在相关各行业的深入推广，落实实施过程中的相应技术支持；\n4、客户需求获取、沟通、确认，协调解决客户技术问题和产品问题；\n5、产品常见问题的整理归档，搜集并整理用户反馈信息，提出改进建议和方案。\n\n【技能要求】\n1、统招自动化、计算机或相关专业本科以上学历，985/211院校优先；\n2、熟悉各类自动化软件、组态软件、人机界面、工控产品，有PLC编程经验和项目调试工作经验优先；\n3、掌握工业自动化监控和信息化管理产品的数据采集、展示，信息管理、网络化和信息化的专业知识；\n4、理解工业自动化领域细分行业（如医药/半导体/新能源/市政环保等）的工控软件应用场景；\n5、熟悉C#、C++等编程语言，熟悉数据库优先。\n\n【综合素质】\n1、具有较强的逻辑思维能力和良好的学习能力；\n2、具有良好的团队协作精神和沟通表达能力；\n3、勤奋上进，具有强大的自我驱动力及目标结果导向，享受挑战自我。',
 1),

(4, '实施工程师', 4, 324, 202.0,
 '计算机软件;电气机械/电力设备;仪器仪表制造;IT服务;科学技术推广',
 '120-150元/天/7000-10000元/5000-10000元/3000-4000元/5000-8000元',
 '广州-天河区;上海-嘉定区;石家庄-桥西区;重庆-渝中区;合肥-蜀山区',
 '广东南方数码科技股份有限公司;安科瑞;数码大方;重庆佳格电子工程有限公司;安徽中安创智信息技术有限公司',
 '100-499人;1000-9999人;100-499人;50-99人;100-499人',
 '民营企业;民营企业;民营企业;民营企业;民营企业',
 '【职位职责】\n1、负责部门内的日常管理，参与分解部门的工作计划，负责部门内部人员的工作安排，保证工作流程顺畅；\n2、协助处理部门内部人员的绩效考核、技能培训等工作；\n3、编制技术支撑相关制度及管理办法；\n4、负责项目的售前技术支持，完成售前咨询、方案设计、投标书制作、投标及应答；\n5、配合客户经理编写技术方案，负责项目策划及组织实施工作；\n6、响应及解决客户的技术要求及疑问，通过平台数据对项目进行专业分析，并反馈客户；\n7、及时响应并处理项目运营过程中遇到的技术类问题及故障，确保项目正常进行；\n8、组织项目预算管理，主持对项目组的考核和评价。\n\n【任职要求】\n1、本科及以上学历，计算机、软件、电子等相关专业，2年以上实施经验；\n2、熟悉各类自动化软件、组态软件、人机界面、工控产品；\n3、掌握工业自动化监控和信息化管理产品的数据采集、展示，信息管理专业知识；\n4、熟悉C#、C++等编程语言，熟悉数据库优先；\n5、具有较强的逻辑思维能力、良好的学习能力和团队协作精神。',
 1),

(5, '前端开发工程师', 5, 154, 127.0,
 '计算机软件;互联网;IT服务;人力资源服务;咨询服务',
 '3000-4000元/1.1-1.8万/5000-7000元·13薪/4000-8000元/9000-12000元',
 '东莞-虎门镇;广州-越秀区;深圳-龙岗区;南京-浦口区;成都-郫都区',
 '东莞市恒亚罗斯计算机科技有限公司;北京易速聘人力资源有限公司;深圳市易玛特科技有限公司;南京捷帝科技有限公司;北京易才博普奥管理顾问有限公司',
 '50-99人;100-499人;50-99人;50-99人;100-499人',
 '民营企业;民营企业;民营企业;民营企业;民营企业',
 '【岗位职责】\n1、依据设计稿完成页面制作，实现项目所需动态效果，对网络平台产品做出优化建议和意见；\n2、负责物联网平台部Web大屏数据可视化（ECharts/D3.js）、运营和运维管理系统Web端（Vue3.0）及移动端APP（Uniapp/Flutter）的功能开发与优化；\n3、参与能源管理系统、运维平台等项目的交互设计与性能调优；\n4、对接数据中台API，实现复杂业务数据的动态渲染与交互逻辑，开发通用组件库；\n5、解决多端兼容性问题，与产品经理、后端工程师紧密协作，确保需求高效落地。\n\n【任职要求】\n1、熟练掌握HTML、CSS、ECharts、JavaScript，熟悉ES6+语法；\n2、熟练使用Vue.js或React.js进行Web端开发，有实际项目经验；\n3、熟悉移动端开发技术，包括H5、小程序、React Native/Flutter/Uniapp等；\n4、具备良好的编码习惯，熟悉前端模块化、组件化开发模式；\n5、了解Web性能优化、浏览器兼容性、前端安全等相关知识；\n6、有物联网、能源管理、数据可视化等相关经验者优先；\n7、熟悉Webpack/Vite等构建工具，具备性能优化经验。',
 1),

(6, 'C/C++开发工程师', 6, 155, 127.5,
 '汽车研发/制造;网络/信息安全;电子设备制造;电子/半导体/集成电路;船舶/航空/航天/火车制造',
 '1.2-1.5万·17薪/5000-8000元/9000-15000元·14薪/1.2-2.4万/1-2万',
 '重庆-渝北区;长春-绿园区;济南-历下区;北京-海淀区;马鞍山-含山县',
 '联合汽车电子有限公司（UAES）;吉林省荣汇鑫网络技术有限公司;山东华云三维科技有限公司;北京润科通用技术有限公司;南京智鸥航空科技有限公司',
 '1000-9999人;50-99人;100-499人;1000-9999人;50-99人',
 '民营企业;民营企业;民营企业;民营企业;民营企业',
 '【岗位要求（手术机器人方向）】\n1、负责手术机器人软件的开发与维护，使用Qt框架进行图形界面开发；\n2、根据产品需求，设计并实现高效、稳定的用户交互界面；\n3、参与软件架构的设计，优化现有代码，提高系统性能；\n4、与硬件、算法等团队协作，集成多种设备和功能模块；\n5、对项目中的技术问题进行分析和解决，确保系统的可靠性与安全性；\n6、参与系统需求分析、设计文档撰写与评审工作。\n\n【任职要求】\n1、计算机、软件工程等相关专业本科及以上学历；\n2、熟练掌握C++语言，具有Qt框架下的实际开发经验；\n3、了解跨平台开发，熟悉Linux/Windows开发环境；\n4、有医疗设备、机器人等行业软件开发经验者优先；\n5、具备良好的问题分析与解决能力，具有团队协作精神；\n6、熟悉常见的版本控制工具（如Git），有敏捷开发经验者优先。\n\n【加分项】\n·熟悉三维GIS引擎（Cesiumjs）优先\n·有大型网络管理系统（NMS）开发经验优先\n·熟悉容器化技术（Docker、Kubernetes）者优先',
 1),

(7, '运维工程师', 7, 100, 100.0,
 '计算机软件;人工智能;IT服务;电子设备制造;通信/网络设备',
 '10000-20000元/8000-15000元/12000-25000元',
 '杭州-西湖区;成都-高新区;西安-高新区;广州-天河区;苏州-工业园区',
 '深圳腾讯云计算有限公司;上海网宿科技股份有限公司;北京云智天下科技有限公司',
 '10000人以上;1000-9999人;100-499人',
 '民营企业;民营企业;民营企业',
 '【岗位职责】\n1、负责公司线上生产环境的运维保障，包括服务器、网络、应用等系统的监控、告警与故障处理；\n2、设计并实现自动化运维工具和脚本（Shell、Python），提升运维效率和可靠性；\n3、负责Docker容器化部署与Kubernetes集群管理，编写Helm Chart，实现服务的快速发布与扩缩容；\n4、设计并维护CI/CD流水线（GitLab CI/Jenkins），保障代码高质量、安全地交付到生产环境；\n5、负责日志收集与分析平台（ELK/Loki）的建设，为业务问题排查和性能分析提供支持；\n6、建立并维护运维文档和知识库，制定运维规范和应急预案；\n7、参与技术选型评审，从运维视角提出架构优化建议。\n\n【任职要求】\n1、本科及以上学历，计算机相关专业，3年以上运维工作经验；\n2、精通Linux系统管理，熟悉Shell脚本编程，能独立编写自动化脚本；\n3、熟练掌握Docker和Kubernetes（K8S），有生产环境集群管理经验优先；\n4、熟悉Nginx、Tomcat、MySQL、Redis、RabbitMQ等常用中间件的部署与调优；\n5、熟悉Ansible、SaltStack等配置管理工具，具备自动化运维实践经验；\n6、了解Prometheus+Grafana监控系统搭建与告警配置；\n7、具备良好的问题排查能力和应急响应经验。',
 1),

(8, 'Python开发工程师', 8, 100, 100.0,
 '计算机软件;人工智能;互联网',
 '12000-25000元/18000-35000元/8000-16000元',
 '西安-高新区;杭州-西湖区;广州-天河区;北京-海淀区;成都-高新区',
 '上海寻梦科技有限公司;北京字节跳动科技有限公司;深圳腾讯科技有限公司',
 '10000人以上;10000人以上;10000人以上',
 '民营企业;民营企业;民营企业',
 '【岗位职责】\n1、负责基于Python的后端服务开发，设计并实现高并发、高可用的RESTful API；\n2、使用Django/Flask/FastAPI等框架构建业务系统，完成模块设计、编码和单元测试；\n3、负责数据库设计与优化（MySQL/PostgreSQL/Redis），编写高效SQL，处理数据迁移；\n4、实现异步任务队列（Celery+RabbitMQ/Redis），处理定时任务和后台异步逻辑；\n5、参与系统架构设计，进行技术方案评审，编写技术设计和接口文档；\n6、负责服务的容器化部署（Docker），编写Dockerfile和docker-compose，完成灰度发布与回滚方案；\n7、对代码进行持续优化，提升系统性能和可维护性。\n\n【任职要求】\n1、本科及以上学历，计算机相关专业，2年以上Python后端开发经验；\n2、熟练掌握Django/Flask/FastAPI至少一种主流Web框架，有独立完成项目经验；\n3、熟悉MySQL/PostgreSQL数据库，了解索引原理、事务机制，能进行SQL调优；\n4、熟悉Redis/Memcached等缓存技术，了解其使用场景和注意事项；\n5、熟悉Git版本控制，了解RESTful API设计与规范；\n6、了解Linux服务器环境，能够进行基本的服务器部署与运维操作。',
 1),

(9, '算法工程师', 9, 100, 100.0,
 '人工智能;计算机软件;互联网',
 '20000-40000元/15000-30000元/30000-60000元',
 '武汉-洪山区;深圳-南山区;广州-天河区;成都-高新区;北京-海淀区',
 '深圳商汤科技有限公司;上海依图网络科技有限公司;北京旷视科技有限公司',
 '1000-9999人;1000-9999人;1000-9999人',
 '民营企业;民营企业;民营企业',
 '【岗位职责】\n1、负责机器学习/深度学习算法的研究与落地，将算法模型部署到生产环境，服务千万级用户；\n2、基于业务需求进行数据分析、特征工程、模型设计与训练，持续优化模型性能指标；\n3、针对推荐系统、NLP、CV等业务场景，研发并迭代核心算法，推动业务指标提升；\n4、跟踪学术界最新研究成果，将前沿算法应用于实际业务，形成技术壁垒；\n5、编写算法文档、模型说明和复盘报告，参与算法技术评审与分享；\n6、与产品和工程团队紧密协作，理解业务需求，推动算法端到端落地。\n\n【任职要求】\n1、硕士及以上学历，计算机科学、人工智能、数学、统计学等相关专业；\n2、扎实的机器学习和深度学习基础，熟悉常见算法（LR、SVM、GBDT、CNN、RNN、Transformer等）原理；\n3、精通Python，熟悉TensorFlow/PyTorch至少一种深度学习框架，能独立完成模型训练与调优；\n4、熟悉NLP/推荐/搜索/图像等领域之一，有实际项目经验优先；\n5、良好的数学基础（高数、线代、概率论），能够复现论文算法；\n6、了解大数据处理（Hive/Spark）和模型工程化部署（TensorFlow Serving/Triton）优先；\n7、具有良好的沟通能力和团队协作精神，有顶会论文或Kaggle竞赛经验优先。',
 1),

(10, '数据分析师', 10, 100, 100.0,
 '电子商务;互联网;计算机软件',
 '15000-30000元/10000-20000元/8000-15000元',
 '广州-天河区;苏州-工业园区;北京-海淀区;西安-高新区;南京-雨花台区',
 '上海拼多多科技有限公司;北京京东世纪贸易有限公司;广州唯品会股份有限公司',
 '10000人以上;10000人以上;10000人以上',
 '民营企业;民营企业;民营企业',
 '【岗位职责】\n1、深入理解业务需求，通过SQL/Python对业务数据进行提取、清洗和分析，输出有价值的业务洞察报告；\n2、建立并维护业务数据指标体系，搭建可视化看板（Tableau/PowerBI），定期追踪核心业务指标变化；\n3、基于数据分析发现业务问题和机会，提出可落地的优化建议，推动业务策略迭代；\n4、参与AB实验设计和效果评估，建立因果推断方法，量化策略带来的业务增量；\n5、与产品、运营、研发团队协作，通过数据驱动方式支持产品优化和用户增长；\n6、编写数据字典和维护数据文档，推动数据资产标准化和复用。\n\n【任职要求】\n1、本科及以上学历，统计学、数学、计算机等相关专业，2年以上数据分析经验；\n2、精通SQL，熟悉MySQL/PostgreSQL/Hive，能够独立完成复杂查询和ETL任务；\n3、熟练使用Python（pandas、numpy、scipy）进行数据分析，熟悉常用统计方法；\n4、熟悉Tableau/PowerBI/Superset等可视化工具，能够独立搭建数据看板；\n5、了解数据仓库和维度建模理论，有大数据处理经验（Hive/Spark）优先；\n6、具备良好的业务敏感度和逻辑思维，能从数据中发现业务规律和异常；\n7、良好的沟通表达能力和跨团队协作能力。',
 1),

(11, 'UI设计师', 11, 100, 100.0,
 '互联网;游戏;计算机软件',
 '8000-15000元/18000-35000元/12000-25000元',
 '苏州-工业园区;南京-雨花台区;杭州-西湖区;武汉-洪山区;成都-高新区',
 '上海莉莉丝科技股份有限公司;北京快手科技有限公司;深圳腾讯游戏事业部',
 '1000-9999人;10000人以上;10000人以上',
 '民营企业;民营企业;民营企业',
 '【岗位职责】\n1、负责产品UI设计与视觉设计，独立完成高保真设计稿，维护并迭代设计规范与组件库；\n2、参与用户研究和可用性测试，收集用户反馈，理解目标用户心理和操作习惯；\n3、负责产品界面图标、插画等素材设计，配合前端工程师实现UI还原；\n4、与产品经理和交互设计师紧密协作，参与产品需求评审，从设计视角提出专业意见；\n5、关注业界设计趋势，持续优化产品视觉体验，提升品牌识别度和用户满意度；\n6、跟踪设计效果数据，分析用户行为，迭代优化设计方案。\n\n【任职要求】\n1、本科及以上学历，视觉传达、艺术设计、数字媒体等相关专业，2年以上UI设计经验；\n2、精通Figma/Sketch/Ai/Ps等设计工具，能独立完成从概念到高保真原型的设计；\n3、具备良好的审美和创意能力，对色彩、排版、字体有敏锐的感知；\n4、熟悉iOS/Android/Web设计规范，了解响应式设计和适配原则；\n5、有动效设计能力（Principle/After Effects）和产品思维优先；\n6、具良好沟通能力，能清晰表达设计思路，提供作品集。',
 1),

(12, '嵌入式开发工程师', 12, 67, 93.5,
 '物联网;电子半导体',
 '15000-30000元/20000-40000元/10000-20000元',
 '西安-高新区;上海-浦东新区;北京-海淀区;武汉-洪山区;广州-天河区',
 '北京小米科技有限责任公司;广州小鹏汽车科技有限公司;深圳大疆创新科技有限公司',
 '10000人以上;10000人以上;10000人以上',
 '民营企业;民营企业;民营企业',
 '【岗位职责】\n1、负责嵌入式系统软件设计、开发与调试，基于ARMCortex-M/R系列芯片完成嵌入式应用开发；\n2、参与硬件原理图评审和PCB设计评审，从软件视角提出可制造性设计（DFM）建议；\n3、编写嵌入式驱动代码（GPIO、UART、I2C、SPI、PWM、DMA等），完成外设驱动开发和HAL层封装；\n4、基于FreeRTOS/uCOS/RT-Thread等实时操作系统进行多任务程序设计，完成任务划分、优先级配置和任务间通信；\n5、负责嵌入式产品固件开发、调试和测试，编写底层软件设计文档和接口说明；\n6、参与产品联调联测，解决硬件、软件和系统层面的技术问题；\n7、持续优化代码体积、运行效率和功耗，提升嵌入式产品竞争力。\n\n【任职要求】\n1、本科及以上学历，电子信息工程、自动化、通信工程、计算机等相关专业，2年以上嵌入式开发经验；\n2、精通C语言编程，熟悉嵌入式C常用数据结构和算法，了解C++优先；\n3、熟悉ARMCortex-M/R架构，有STM32/NXP/TI等MCU开发经验优先；\n4、熟悉嵌入式实时操作系统（FreeRTOS/uCOS/RT-Thread），能够进行多任务编程和驱动开发；\n5、熟练使用示波器、逻辑分析仪等调试工具，具备硬件问题定位能力；\n6、熟悉CAN、UART、I2C、SPI等通信协议，有蓝牙/ZigBee/WiFi无线开发经验优先。',
 1),

(13, 'AI工程师', 13, 70, 85.0,
 '互联网;人工智能',
 '15000-30000元/30000-60000元/20000-40000元',
 '武汉-洪山区;北京-海淀区;杭州-西湖区;苏州-工业园区;成都-高新区',
 '北京百度时代网络技术有限公司;深圳华为技术有限公司;杭州海康威视数字技术股份有限公司',
 '10000人以上;10000人以上;10000人以上',
 '民营企业;民营企业;民营企业',
 '【岗位职责】\n1、负责AI应用的产品化落地，将机器学习/深度学习模型集成到实际业务系统中，服务千万级用户；\n2、基于业务场景选择合适的AI方案（NLP/CV/语音/多模态），完成模型选型、训练、部署和迭代优化；\n3、使用Docker/Kubernetes实现AI模型的容器化部署和弹性扩缩容，保障服务的高可用性；\n4、负责模型性能监控与优化（推理延迟、吞吐量、显存占用），设计AB测试方案评估模型效果；\n5、与产品经理和算法工程师紧密协作，理解业务需求，将AI能力转化为用户价值；\n6、跟进AI前沿技术动态，探索新技术的落地可能性，推动团队AI能力持续提升。\n\n【任职要求】\n1、本科及以上学历，计算机科学、人工智能等相关专业，2年以上AI应用开发经验；\n2、熟悉Python主流AI框架（TensorFlow/PyTorch），能够进行模型训练、调试和优化；\n3、具备模型部署经验，熟悉TensorFlow Serving/Triton/ONNXRuntime/Flashlight等推理框架；\n4、熟悉Docker和Kubernetes，了解MLOps流程（模型注册、版本管理、A/B测试、灰度发布）；\n5、了解机器学习/深度学习基础（CNN/RNN/Transformer/Attention机制等），有NLP或CV项目经验优先；\n6、良好的工程能力（代码规范、单元测试、CI/CD），有AI产品落地经验优先。',
 1),

(14, '网络安全工程师', 14, 100, 100.0,
 '信息安全;计算机软件;互联网',
 '15000-30000元/10000-20000元/20000-40000元',
 '深圳-南山区;广州-天河区;苏州-工业园区;西安-高新区;上海-浦东新区',
 '深圳深信服科技股份有限公司;北京奇安信科技集团股份有限公司;杭州安恒信息技术股份有限公司',
 '1000-9999人;10000人以上;1000-9999人',
 '民营企业;民营企业;民营企业',
 '【岗位职责】\n1、负责公司网络安全防护体系建设，设计并实施安全策略，保障业务系统安全稳定运行；\n2、部署和维护防火墙、WAF、IPS/IDS、堡垒机等安全设备，监控安全日志，分析安全告警；\n3、负责渗透测试和漏洞挖掘，定期对Web应用、APP、服务器进行安全评估，输出漏洞报告并推动修复；\n4、建设和维护安全运营中心（SOC），制定安全事件响应流程，快速处置安全事件和应急响应；\n5、参与公司等级保护建设和合规审计，编写安全管理规范和安全培训材料；\n6、跟踪最新安全动态和威胁情报，评估新型攻击手法，更新防护策略。\n\n【任职要求】\n1、本科及以上学历，网络安全、信息安全、计算机科学等相关专业，2年以上安全工作经验；\n2、熟悉主流安全攻击手法（SQL注入、XSS、CSRF、越权、文件上传等），有渗透测试实战经验；\n3、精通KaliLinux、BurpSuite、Nmap、Metasploit等渗透测试工具；\n4、熟悉网络安全设备（防火墙、WAF、IDS/IPS）原理和配置，有运维经验优先；\n5、了解Linux系统安全加固、MySQL/Nginx等应用安全配置；\n6、具备较强逻辑分析能力和文档撰写能力，有CISP/CISSP/OSCP等证书优先。',
 1),

(15, '硬件测试工程师', 15, 128, 109.0,
 '计算机软件;人工智能;IT服务;电子设备制造;通信/网络设备',
 '4000-6000元/7000-8000元/1000-2000元/7000-9000元/1-2万',
 '威海-环翠区;西安-未央区;遵义-红花岗区;赣州-章贡区;广州-海珠区',
 '山东中天宇信信息技术有限公司;陕西跃北机电设备有限公司;贵州兴黔信息科技有限公司;福建八盛汽车科技有限公司;拓保软件',
 '50-99人;50-99人;50-99人;50-99人;1000-9999人',
 '民营企业;民营企业;民营企业;民营企业;民营企业',
 '【岗位职责】\n1、负责硬件产品（通信设备、电子模块、智能终端等）的功能测试、性能测试和可靠性测试，制定测试方案和用例；\n2、使用万用表、示波器、信号源、频谱仪等仪器进行硬件板卡和整机的测试与调试，记录测试数据并编写测试报告；\n3、执行高低温、振动、EMC等环境可靠性试验，分析失效模式，提出改进建议；\n4、参与硬件设计评审（原理图/PCB），从测试角度提出可测试性设计（DFT）建议；\n5、协助研发工程师进行硬件故障定位与分析，配合完成问题复现和解决方案验证；\n6、维护测试设备和工装夹具，建立测试规范和操作指导书（SOP）。\n\n【任职要求】\n1、本科及以上学历，电子信息工程、自动化、测控技术与仪器等相关专业，2年以上硬件测试经验；\n2、熟练使用万用表、示波器、信号源、频谱仪等通用电子测量仪器；\n3、具备硬件基础知识，理解电路原理图，能独立完成电路板的调试和简单故障排查；\n4、熟悉硬件测试流程和方法，了解环境可靠性试验标准（GB/T2423/IEC60068）；\n5、具有一定文档撰写能力，能编写规范的测试用例、测试报告和调试记录；\n6、细心严谨，有较强的问题分析能力和质量意识；\n7、有通信、电子或汽车电子行业硬件测试经验优先。',
 1);

-- ============================================================
-- 第四步：插入岗位七维度技能数据
-- ============================================================

-- Java开发工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(1,'专业技能','Java、Spring、SpringBoot、MySQL、Redis、微服务、MyBatis、Maven、Git、Linux、Docker',2.0,'精通'),
(1,'证书','OCP、Spring认证、PMP、信息系统项目管理师',0.8,'基础'),
(1,'创新能力','框架设计创新、代码优化、性能调优、技术方案创新',1.2,'熟练'),
(1,'学习能力','新技术跟进、源码学习、技术分享、社区参与',1.0,'熟练'),
(1,'抗压能力','项目deadline、线上问题处理、高并发压力',1.0,'基础'),
(1,'沟通能力','需求沟通、技术方案评审、团队协作、代码评审',0.8,'熟练'),
(1,'实习能力','企业级项目开发、团队协作、版本管理经验',0.6,'基础');

-- 测试工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(2,'专业技能','功能测试、自动化测试、Selenium、Postman、JMeter、SQL、Linux、Python、接口测试、性能测试',2.0,'精通'),
(2,'证书','ISTQB、软件评测师、Jmeter认证、Loadrunner认证',0.8,'基础'),
(2,'创新能力','测试方法创新、自动化框架设计、测试工具开发',1.2,'熟练'),
(2,'学习能力','测试技术跟进、行业测试标准、测试工具学习',1.0,'熟练'),
(2,'抗压能力','项目上线压力、回归测试时间紧、多项目并行',1.0,'基础'),
(2,'沟通能力','缺陷沟通、测试报告汇报、需求确认、跨部门协作',0.8,'熟练'),
(2,'实习能力','测试项目实践、缺陷跟踪经验、测试文档编写',0.6,'基础');

-- 技术支持工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(3,'专业技能','故障诊断、远程支持、客户服务、Windows、Linux、网络基础、技术文档',2.0,'精通'),
(3,'证书','HCIA、HCIP、ITIL认证、CCNA',0.8,'基础'),
(3,'创新能力','问题解决方案优化、服务流程改进、知识库建设',1.2,'熟练'),
(3,'学习能力','产品知识学习、新技术学习、行业解决方案学习',1.0,'熟练'),
(3,'抗压能力','客户紧急问题、多客户并发、7x24待命',1.0,'基础'),
(3,'沟通能力','客户沟通、远程协助、需求收集、服务意识',0.8,'精通'),
(3,'实习能力','技术支持实习、客户服务经验、现场问题处理',0.6,'基础');

-- 实施工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(4,'专业技能','项目实施、系统部署、SQL、需求分析、项目管理、ERP、CRM、文档编写',2.0,'精通'),
(4,'证书','PMP、信息系统项目管理师、ERP认证、ACP',0.8,'基础'),
(4,'创新能力','实施方案优化、实施流程改进、客户需求引导',1.2,'熟练'),
(4,'学习能力','行业解决方案学习、产品培训、客户业务学习',1.0,'熟练'),
(4,'抗压能力','项目交付压力、客户现场问题、多项目并行',1.0,'基础'),
(4,'沟通能力','客户需求沟通、跨部门协调、项目汇报、干系人管理',0.8,'精通'),
(4,'实习能力','项目实施实习、客户现场经验、需求调研实践',0.6,'基础');

-- 前端开发工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(5,'专业技能','HTML、CSS、JavaScript、Vue、React、Node.js、Webpack、TypeScript、Ajax、Bootstrap',2.0,'精通'),
(5,'证书','Vue认证、React认证、前端工程师认证、W3C认证',0.8,'基础'),
(5,'创新能力','组件封装、交互体验优化、性能优化方案、设计创新',1.2,'熟练'),
(5,'学习能力','前端新技术、框架源码学习、设计趋势跟踪',1.0,'熟练'),
(5,'抗压能力','页面兼容性、需求变更频繁、上线时间压力',1.0,'基础'),
(5,'沟通能力','UI设计沟通、后端接口对接、产品需求确认',0.8,'熟练'),
(5,'实习能力','前端项目开发、全栈项目经验、组件开发实践',0.6,'基础');

-- C/C++开发工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(6,'专业技能','C++、C语言、STL、Linux、多线程、Socket、数据结构、算法、GDB、Makefile',2.0,'精通'),
(6,'证书','C++认证、嵌入式工程师认证、ACM竞赛证书',0.8,'基础'),
(6,'创新能力','架构设计创新、性能优化创新、底层技术创新',1.2,'熟练'),
(6,'学习能力','C++新标准、系统底层学习、硬件知识学习',1.0,'熟练'),
(6,'抗压能力','内存管理压力、性能要求高、兼容性挑战',1.0,'基础'),
(6,'沟通能力','技术方案讨论、代码评审、跨平台协作',0.8,'熟练'),
(6,'实习能力','底层开发项目、系统级开发经验、嵌入式项目',0.6,'基础');

-- 运维工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(7,'专业技能','Linux、Shell、Docker、Kubernetes、Ansible、Nginx、Tomcat、MySQL、Redis、监控',2.0,'精通'),
(7,'证书','RHCE、RHCA、K8S认证、阿里云ACE、腾讯云TCA',0.8,'基础'),
(7,'创新能力','自动化运维创新、监控告警优化、成本优化方案',1.2,'熟练'),
(7,'学习能力','DevOps学习、云原生技术、自动化工具学习',1.0,'熟练'),
(7,'抗压能力','7x24服务保障、故障应急处理、变更风险',1.0,'精通'),
(7,'沟通能力','运维汇报、跨团队协作、SRE沟通、值班交接',0.8,'熟练'),
(7,'实习能力','运维实践、自动化脚本开发、监控系统配置',0.6,'基础');

-- Python开发工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(8,'专业技能','Python、Django、Flask、FastAPI、MySQL、PostgreSQL、Docker、Linux、Redis、Celery',2.0,'精通'),
(8,'证书','Python认证、Django认证、PMP、阿里云认证',0.8,'基础'),
(8,'创新能力','框架设计创新、自动化工具开发、脚本效率优化',1.2,'熟练'),
(8,'学习能力','Python新特性、生态工具学习、快速学习能力',1.0,'熟练'),
(8,'抗压能力','快速开发压力、代码质量要求、版本迭代压力',1.0,'基础'),
(8,'沟通能力','API设计讨论、团队协作、产品需求沟通',0.8,'熟练'),
(8,'实习能力','Web开发项目、API开发经验、脚本开发实践',0.6,'基础');

-- 算法工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(9,'专业技能','Python、TensorFlow、PyTorch、机器学习、深度学习、NLP、CV、推荐算法、数据分析、Linux',2.0,'精通'),
(9,'证书','机器学习认证、深度学习认证、Kaggle认证、ACM竞赛证书',0.8,'基础'),
(9,'创新能力','算法创新、模型优化创新、特征工程创新、论文发表',1.2,'精通'),
(9,'学习能力','论文复现、前沿算法跟踪、数学基础学习',1.0,'精通'),
(9,'抗压能力','算法效果压力、算力限制、论文发表压力',1.0,'基础'),
(9,'沟通能力','算法方案评审、团队技术讨论、跨部门协作',0.8,'熟练'),
(9,'实习能力','AI项目实践、数据科学项目、竞赛参赛经验',0.6,'基础');

-- 数据分析师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(10,'专业技能','Python、SQL、Pandas、NumPy、Excel、Tableau、PowerBI、统计学、数据可视化、Spark',2.0,'精通'),
(10,'证书','CDA数据分析师、Tableau认证、PMP、阿里云数据分析师',0.8,'基础'),
(10,'创新能力','可视化创新、分析维度拓展、自动化报表创新',1.2,'熟练'),
(10,'学习能力','数据分析方法、BI工具学习、统计进阶学习',1.0,'熟练'),
(10,'抗压能力','报告截止、数据质量挑战、多方需求协调',1.0,'基础'),
(10,'沟通能力','数据分析汇报、业务沟通、跨部门协作、可视化展示',0.8,'精通'),
(10,'实习能力','数据分析项目、BI报表开发、数据清洗实践',0.6,'基础');

-- UI设计师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(11,'专业技能','Figma、Sketch、Photoshop、Illustrator、AI、Axure、Principle、动效设计、UI设计、UX设计',2.0,'精通'),
(11,'证书','Adobe认证、UX认证、UI设计师认证、Google UX证书',0.8,'基础'),
(11,'创新能力','设计创新、交互体验优化、设计系统建立、设计趋势',1.2,'熟练'),
(11,'学习能力','设计趋势、新工具学习、设计规范更新',1.0,'熟练'),
(11,'抗压能力','设计稿修改、多方意见协调、上线时间压力',1.0,'基础'),
(11,'沟通能力','设计评审、产品沟通、开发协作、用户调研',0.8,'精通'),
(11,'实习能力','设计项目实践、设计规范制定、用户研究实践',0.6,'基础');

-- 嵌入式开发工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(12,'专业技能','C、嵌入式C、ARM、STM32、RTOS、Linux、ucos、GPIO、UART、I2C、SPI、PCB基础',2.0,'精通'),
(12,'证书','嵌入式工程师认证、ARM认证、RTOS认证、电子设计竞赛证书',0.8,'基础'),
(12,'创新能力','驱动开发创新、功耗优化创新、实时性改进创新',1.2,'熟练'),
(12,'学习能力','芯片手册学习、嵌入式系统学习、硬件知识学习',1.0,'熟练'),
(12,'抗压能力','硬件调试压力、实时性要求、资源限制挑战',1.0,'基础'),
(12,'沟通能力','硬件团队协作、固件配合、测试验证沟通',0.8,'熟练'),
(12,'实习能力','嵌入式项目开发、硬件调试经验、驱动开发实践',0.6,'基础');

-- AI工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(13,'专业技能','Python、TensorFlow、PyTorch、机器学习、深度学习、模型部署、Docker、K8S、特征工程、模型优化',2.0,'精通'),
(13,'证书','机器学习认证、深度学习认证、AWS AI认证、阿里云AI认证',0.8,'基础'),
(13,'创新能力','模型创新、AI应用创新、工程化创新、产品创新',1.2,'精通'),
(13,'学习能力','AI前沿论文、新框架学习、行业应用学习',1.0,'精通'),
(13,'抗压能力','模型效果压力、上线时效、算力成本压力',1.0,'基础'),
(13,'沟通能力','算法评审、产品协作、技术汇报、跨团队协作',0.8,'熟练'),
(13,'实习能力','AI项目实践、模型部署经验、AI产品开发',0.6,'基础');

-- 网络安全工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(14,'专业技能','网络安全、渗透测试、Kali Linux、Web安全、漏洞挖掘、防火墙、WAF、等保、应急响应、日志分析',2.0,'精通'),
(14,'证书','CISP、CISSP、OSCP、等级保护测评师、CompTIA Security+',0.8,'基础'),
(14,'创新能力','安全架构创新、防护方案优化、应急响应创新',1.2,'熟练'),
(14,'学习能力','漏洞技术跟踪、攻击手法学习、合规要求学习',1.0,'熟练'),
(14,'抗压能力','安全事件压力、合规审计压力、漏洞修复时限',1.0,'精通'),
(14,'沟通能力','安全汇报、技术培训、跨部门协作、合规沟通',0.8,'熟练'),
(14,'实习能力','安全测试项目、安全运维经验、CTF竞赛经验',0.6,'基础');

-- 硬件测试工程师
INSERT INTO `job_skill_dimension` (`job_id`, `dimension`, `skill_content`, `weight`, `requirement_level`) VALUES
(15,'专业技能','硬件测试、万用表、示波器、频谱仪、可靠性测试、环境测试、失效分析、测试报告、HIL、EMI',2.0,'精通'),
(15,'证书','硬件测试工程师认证、可靠性工程师认证、ISO认证内审员',0.8,'基础'),
(15,'创新能力','测试方法改进、测试效率提升、测试设备优化',1.2,'熟练'),
(15,'学习能力','测试标准学习、仪器使用学习、行业标准学习',1.0,'熟练'),
(15,'抗压能力','测试周期压力、质量判断压力、多任务并行',1.0,'基础'),
(15,'沟通能力','测试报告汇报、研发沟通、客户对接、供应商协调',0.8,'熟练'),
(15,'实习能力','硬件测试项目、可靠性测试经验、测试设备使用',0.6,'基础');

-- ============================================================
-- 第五步：插入岗位晋升路径数据
-- ============================================================

-- Java开发工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(1,'entry','Java初级开发工程师','1-3年','完成基础开发任务，学习企业级开发规范','8K-15K'),
(1,'mid','Java中级开发工程师','3-5年','独立负责模块开发，参与技术方案设计','15K-25K'),
(1,'senior','Java高级开发工程师/技术专家','5-8年','主导核心技术研发，解决复杂技术问题','25K-40K'),
(1,'expert','Java架构师','8年以上','系统架构设计，技术选型，团队技术指导','40K-60K'),
(1,'management','技术经理/研发总监','8年以上','团队管理，项目统筹，技术战略规划','50K-80K');

-- 测试工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(2,'entry','初级测试工程师','1-2年','执行测试用例，提交缺陷报告','6K-12K'),
(2,'mid','中级测试工程师','2-3年','设计测试用例，开展自动化测试','12K-20K'),
(2,'senior','高级测试工程师/测试开发工程师','3-5年','测试框架开发，性能测试，安全测试','20K-35K'),
(2,'expert','测试架构师/测试专家','5年以上','测试体系建立，质量流程优化','35K-50K'),
(2,'management','测试经理/质量总监','5年以上','测试团队管理，质量体系建立','40K-60K');

-- 技术支持工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(3,'entry','技术支持工程师','1-2年','客户问题解答，远程支持','6K-12K'),
(3,'mid','高级技术支持工程师','2-3年','复杂问题处理，技术培训','12K-20K'),
(3,'senior','技术支持专家/售前工程师','3-5年','技术方案编写，客户培训','20K-30K'),
(3,'expert','解决方案架构师','5年以上','解决方案设计，技术咨询','30K-45K'),
(3,'management','技术支持经理/服务总监','5年以上','服务团队管理，服务质量把控','35K-55K');

-- 实施工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(4,'entry','实施工程师','1-2年','系统部署，客户培训','7K-13K'),
(4,'mid','高级实施工程师','2-3年','项目管理实施交付','13K-22K'),
(4,'senior','实施专家/项目经理','3-5年','大型项目实施管理','22K-35K'),
(4,'expert','解决方案专家','5年以上','行业解决方案设计','35K-50K'),
(4,'management','实施总监/交付总监','5年以上','实施团队管理，交付战略','45K-70K');

-- 前端开发工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(5,'entry','前端初级开发工程师','1-2年','页面开发，组件实现','7K-14K'),
(5,'mid','前端中级开发工程师','2-3年','模块开发，技术方案实现','14K-22K'),
(5,'senior','前端高级开发工程师','3-5年','前端架构，性能优化','22K-35K'),
(5,'expert','前端架构师/前端专家','5年以上','前端技术规划，团队技术指导','35K-50K'),
(5,'management','前端负责人/技术经理','5年以上','前端团队管理，技术决策','40K-60K');

-- C/C++开发工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(6,'entry','C/C++初级开发工程师','1-3年','模块开发，代码编写','8K-16K'),
(6,'mid','C/C++中级开发工程师','3-5年','核心模块开发，系统设计','16K-28K'),
(6,'senior','C/C++高级开发工程师','5-8年','底层开发，性能优化','28K-45K'),
(6,'expert','C/C++系统架构师','8年以上','系统架构设计，技术攻关','45K-65K'),
(6,'management','技术经理/研发总监','8年以上','研发团队管理，技术战略','50K-80K');

-- 运维工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(7,'entry','初级运维工程师','1-2年','日常巡检，故障处理','6K-12K'),
(7,'mid','中级运维工程师/DevOps','2-3年','自动化运维，监控系统','12K-20K'),
(7,'senior','高级运维工程师/SRE','3-5年','稳定性保障，容量规划','20K-35K'),
(7,'expert','SRE专家/平台架构师','5年以上','平台架构设计，成本优化','35K-50K'),
(7,'management','运维经理/运维总监','5年以上','运维团队管理，运维战略','40K-60K');

-- Python开发工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(8,'entry','Python初级开发工程师','1-2年','功能开发，脚本编写','8K-15K'),
(8,'mid','Python中级开发工程师','2-3年','模块设计，API开发','15K-25K'),
(8,'senior','Python高级开发工程师','3-5年','架构设计，性能优化','25K-40K'),
(8,'expert','Python技术专家/架构师','5年以上','技术规划，团队指导','40K-55K'),
(8,'management','技术经理','5年以上','团队管理，技术决策','45K-65K');

-- 算法工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(9,'entry','初级算法工程师','1-2年','算法实现，数据处理','12K-20K'),
(9,'mid','中级算法工程师','2-3年','模型研发，效果优化','20K-35K'),
(9,'senior','高级算法工程师','3-5年','算法创新，落地应用','35K-50K'),
(9,'expert','算法专家/研究员','5年以上','前沿研究，技术突破','50K-70K'),
(9,'management','算法团队负责人/技术总监','5年以上','团队管理，算法战略','55K-80K');

-- 数据分析师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(10,'entry','初级数据分析师','1-2年','数据处理，报表开发','7K-14K'),
(10,'mid','中级数据分析师/BI工程师','2-3年','数据分析，专题分析','14K-25K'),
(10,'senior','高级数据分析师/数据产品经理','3-5年','数据产品，数据驱动决策','25K-40K'),
(10,'expert','数据挖掘专家/数据科学家','5年以上','数据创新，业务价值挖掘','40K-55K'),
(10,'management','数据分析负责人','5年以上','分析团队，数据战略','45K-65K');

-- UI设计师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(11,'entry','UI设计助理/初级UI设计师','1-2年','设计执行，切图标注','6K-12K'),
(11,'mid','UI设计师/视觉设计师','2-3年','独立设计，设计规范','12K-22K'),
(11,'senior','高级UI设计师/资深设计师','3-5年','设计体系，创新设计','22K-35K'),
(11,'expert','UI专家/视觉设计专家','5年以上','设计战略，品牌设计','35K-50K'),
(11,'management','设计主管/设计经理','5年以上','设计团队，设计管理','40K-60K');

-- 嵌入式开发工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(12,'entry','初级嵌入式开发工程师','1-2年','驱动开发，模块实现','8K-16K'),
(12,'mid','中级嵌入式开发工程师','2-3年','系统开发，硬件调试','16K-28K'),
(12,'senior','高级嵌入式开发工程师','3-5年','系统架构，性能优化','28K-45K'),
(12,'expert','嵌入式系统专家','5年以上','技术专家，系统架构','45K-60K'),
(12,'management','嵌入式团队负责人','5年以上','团队管理，技术规划','50K-70K');

-- AI工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(13,'entry','AI工程师','0-1年','AI应用开发，模型调用','12K-22K'),
(13,'mid','中级AI工程师','1-3年','模型部署，效果优化','22K-38K'),
(13,'senior','高级AI工程师/机器学习工程师','3-5年','AI产品研发，创新应用','38K-55K'),
(13,'expert','AI架构师/首席AI工程师','5年以上','AI架构，技术突破','55K-75K'),
(13,'management','AI研发总监','5年以上','研发管理，AI战略','60K-85K');

-- 网络安全工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(14,'entry','网络安全工程师','0-1年','安全运维，安全监控','8K-16K'),
(14,'mid','中级网络安全工程师','1-3年','渗透测试，安全评估','16K-28K'),
(14,'senior','高级网络安全工程师/渗透测试工程师','3-5年','安全架构，应急响应','28K-45K'),
(14,'expert','安全架构师/安全专家','5年以上','安全战略，合规建设','45K-65K'),
(14,'management','安全总监/CISO','5年以上','安全管理，安全战略','50K-75K');

-- 硬件测试工程师
INSERT INTO `job_promotion_path` (`job_id`, `stage`, `stage_name`, `experience_years`, `description`, `salary_range`) VALUES
(15,'entry','初级硬件测试工程师','0-1年','功能测试，环境测试','5K-10K'),
(15,'mid','硬件测试工程师','1-3年','可靠性测试，失效分析','10K-18K'),
(15,'senior','高级硬件测试工程师/可靠性工程师','3-5年','测试标准，测试体系','18K-30K'),
(15,'expert','可靠性专家/硬件测试专家','5年以上','测试战略，质量体系','30K-45K'),
(15,'management','测试经理/质量经理','5年以上','测试团队，质量管理','35K-55K');

-- ============================================================
-- 第六步：插入岗位四维度匹配评分
-- ============================================================

INSERT INTO `job_dimension_match` (`job_id`, `dimension`, `weight`, `score`, `description`, `evaluation_criteria`) VALUES
(1,'基础要求',0.20,85.0,'学历、专业、基本技能','计算机相关专业本科及以上，掌握Java基础'),
(1,'职业技能',0.40,88.0,'专业技能掌握程度','Spring/MySQL/Redis/微服务等技能'),
(1,'职业素养',0.25,82.0,'沟通、团队、抗压能力','团队协作、沟通表达、抗压能力'),
(1,'发展潜力',0.15,80.0,'学习、创新潜力','技术学习能力、创新意识'),
(2,'基础要求',0.20,82.0,'学历、专业、基本技能','计算机相关专业本科及以上，了解测试理论'),
(2,'职业技能',0.40,85.0,'测试技能掌握程度','功能测试/自动化测试/性能测试等'),
(2,'职业素养',0.25,84.0,'细心、沟通、质量意识','细心程度、沟通能力、质量意识'),
(2,'发展潜力',0.15,78.0,'测试技术学习潜力','测试方法学习、自动化能力提升'),
(3,'基础要求',0.20,78.0,'学历、专业、基本技能','大专及以上，技术基础'),
(3,'职业技能',0.40,82.0,'技术支持技能','故障诊断、远程支持、技术文档'),
(3,'职业素养',0.25,88.0,'服务意识、沟通能力','客户服务意识、沟通表达'),
(3,'发展潜力',0.15,75.0,'技术深度发展潜力','技术深度学习、售前能力'),
(4,'基础要求',0.20,80.0,'学历、专业、基本技能','本科及以上，了解业务流程'),
(4,'职业技能',0.40,83.0,'实施交付技能','项目管理、系统部署、需求分析'),
(4,'职业素养',0.25,86.0,'沟通协调、项目管理','沟通协调、项目把控'),
(4,'发展潜力',0.15,78.0,'项目管理发展潜力','PMP、解决方案能力'),
(5,'基础要求',0.20,82.0,'学历、专业、基本技能','计算机/设计相关专业本科'),
(5,'职业技能',0.40,85.0,'前端技能','Vue/React/HTML/CSS/JS'),
(5,'职业素养',0.25,80.0,'审美、沟通、设计理解','审美能力、UI沟通协作'),
(5,'发展潜力',0.15,82.0,'全栈发展潜力','全栈学习、设计能力'),
(6,'基础要求',0.20,85.0,'学历、专业、基本技能','计算机相关专业本科，C语言基础'),
(6,'职业技能',0.40,88.0,'C/C++专业技能','C++/STL/数据结构/算法'),
(6,'职业素养',0.25,78.0,'逻辑思维、细心','逻辑思维、内存管理细心'),
(6,'发展潜力',0.15,82.0,'系统级发展潜力','底层技术、系统架构'),
(7,'基础要求',0.20,80.0,'学历、专业、基本技能','本科及以上，Linux基础'),
(7,'职业技能',0.40,84.0,'运维技能','Linux/Docker/K8S/监控'),
(7,'职业素养',0.25,85.0,'责任心、应急能力','7x24责任意识、故障应急'),
(7,'发展潜力',0.15,80.0,'DevOps发展潜力','自动化、平台化能力'),
(8,'基础要求',0.20,82.0,'学历、专业、基本技能','本科及以上，编程基础'),
(8,'职业技能',0.40,85.0,'Python技能','Django/Flask/数据库/网络编程'),
(8,'职业素养',0.25,80.0,'学习能力、代码质量','快速学习、代码规范'),
(8,'发展潜力',0.15,84.0,'全栈/AI发展潜力','全栈能力、数据能力'),
(9,'基础要求',0.20,88.0,'学历、专业、数学基础','硕士优先，数学基础扎实'),
(9,'职业技能',0.40,90.0,'算法技能','ML/DL/NLP/CV/推荐算法'),
(9,'职业素养',0.25,82.0,'研究能力、创新','论文研究、创新能力'),
(9,'发展潜力',0.15,88.0,'AI领域发展潜力','前沿技术、算法创新'),
(10,'基础要求',0.20,80.0,'学历、专业、统计基础','本科及以上，统计学基础'),
(10,'职业技能',0.40,84.0,'分析技能','SQL/Python/Tableau/Excel'),
(10,'职业素养',0.25,85.0,'业务理解、沟通表达','业务洞察、数据讲故事'),
(10,'发展潜力',0.15,82.0,'数据科学潜力','机器学习、数据产品'),
(11,'基础要求',0.20,75.0,'学历、设计基础','设计相关专业本科，有作品集'),
(11,'职业技能',0.40,86.0,'设计技能','Figma/PS/Axure/设计规范'),
(11,'职业素养',0.25,85.0,'审美、创意、沟通','审美能力、创新设计、沟通协作'),
(11,'发展潜力',0.15,80.0,'UX发展潜力','用户体验、研究能力'),
(12,'基础要求',0.20,85.0,'学历、专业、硬件基础','电子/计算机本科，硬件基础'),
(12,'职业技能',0.40,87.0,'嵌入式技能','C/ARM/RTOS/驱动开发'),
(12,'职业素养',0.25,78.0,'细心、耐心','硬件调试耐心、细心'),
(12,'发展潜力',0.15,80.0,'系统架构潜力','软硬件结合、系统思维'),
(13,'基础要求',0.20,88.0,'学历、专业、AI基础','硕士优先，AI基础'),
(13,'职业技能',0.40,90.0,'AI工程技能','模型部署/MLOps/特征工程'),
(13,'职业素养',0.25,82.0,'工程能力、协作','工程化能力、产品协作'),
(13,'发展潜力',0.15,88.0,'AI架构发展潜力','AI系统架构、技术领导'),
(14,'基础要求',0.20,83.0,'学历、专业、安全基础','本科及以上，网络安全基础'),
(14,'职业技能',0.40,86.0,'安全技能','渗透测试/安全运维/应急响应'),
(14,'职业素养',0.25,85.0,'责任感、保密意识','安全责任、保密意识'),
(14,'发展潜力',0.15,82.0,'安全架构潜力','安全架构、合规建设'),
(15,'基础要求',0.20,80.0,'学历、专业、电子基础','本科及以上，电子基础'),
(15,'职业技能',0.40,84.0,'测试技能','仪器使用/可靠性测试/失效分析'),
(15,'职业素养',0.25,82.0,'细心、质量意识','测试细心、质量把关'),
(15,'发展潜力',0.15,78.0,'质量管理潜力','质量体系、测试标准');

-- ============================================================
-- 第七步：插入岗位换岗路径数据
-- ============================================================

INSERT INTO `job_transfer_path` (`source_job_id`, `source_job_name`, `target_job_name`, `difficulty_level`, `required_skills`, `transfer_duration`, `advantage_analysis`, `path_index`) VALUES
(1,'Java开发工程师','前端开发工程师',3,'HTML、CSS、JavaScript、Vue','6-12月','理解接口设计，全栈转型快',1),
(1,'Java开发工程师','大数据工程师',3,'Hadoop、Spark、Flink','1-2年','Java技术栈直接复用',2),
(1,'Java开发工程师','技术经理',4,'项目管理、团队管理','2-3年','技术深度积累深厚',3),
(1,'Java开发工程师','DevOps工程师',3,'Docker、K8S、CI/CD','6-12月','熟悉Linux环境',4),
(2,'测试工程师','Java开发工程师',3,'Java、自动化测试框架','1-2年','测试经验让你更懂代码质量',1),
(2,'测试工程师','前端开发工程师',3,'JavaScript、Vue','6-12月','熟悉Web技术',2),
(2,'测试工程师','产品经理',3,'产品设计、需求分析','1-2年','质量意识强，需求理解深入',3),
(2,'测试工程师','项目经理',3,'PMP、项目管理','1-2年','测试项目管理优势大',4),
(3,'技术支持工程师','实施工程师',2,'项目实施、交付管理','3-6月','技术支持到实施自然过渡',1),
(3,'技术支持工程师','Java开发工程师',3,'Java、技术支持开发','1-2年','技术问题解决能力强',2),
(3,'技术支持工程师','项目经理',3,'PMP、项目管理','1-2年','技术支持项目管理优势大',3),
(3,'技术支持工程师','产品经理',4,'产品设计、售前产品','1-2年','技术支持到售前/产品转型',4),
(4,'实施工程师','项目经理',2,'PMP、项目管理','1-2年','实施经验直接用于项目管理',1),
(4,'实施工程师','技术支持工程师',2,'故障诊断、技术文档','3-6月','实施到技术支持自然过渡',2),
(4,'实施工程师','产品经理',3,'产品设计、解决方案','1-2年','实施到解决方案产品转型',3),
(4,'实施工程师','Java开发工程师',4,'Java、ERP开发','1-2年','ERP实施到开发是常见路径',4),
(5,'前端开发工程师','Java开发工程师',3,'Java、Spring Boot','1-2年','前端已理解全栈概念',1),
(5,'前端开发工程师','UI设计师',3,'Figma、Sketch、设计规范','1-2年','界面开发经验，设计思维有优势',2),
(5,'前端开发工程师','全栈工程师',2,'Node.js、后端框架','6-12月','前端技能已是全栈基础',3),
(5,'前端开发工程师','产品经理',3,'产品设计、需求分析','1-2年','前端用户思维，产品转型快',4),
(6,'C/C++开发工程师','嵌入式开发工程师',2,'ARM、RTOS、嵌入式C','6-12月','C++是嵌入式主流语言',1),
(6,'C/C++开发工程师','Java开发工程师',3,'Java、后端开发','1-2年','编程基础扎实，转后端快',2),
(6,'C/C++开发工程师','游戏开发工程师',2,'Unity3D、Unreal、图形学','6-12月','C++是游戏引擎核心语言',3),
(6,'C/C++开发工程师','音视频开发工程师',3,'FFmpeg、WebRTC、编解码','1-2年','底层开发经验直接复用',4),
(7,'运维工程师','DevOps工程师',2,'K8S、Docker、CI/CD','6-12月','运维基础直接复用',1),
(7,'运维工程师','Java开发工程师',4,'Java、自动化运维','1-2年','运维开发(SRE)方向',2),
(7,'运维工程师','DBA工程师',3,'MySQL、PG、Oracle','6-12月','数据库运维经验直接复用',3),
(7,'运维工程师','项目经理',4,'PMP、运维管理','2-3年','运维团队管理发展方向',4),
(8,'Python开发工程师','数据分析师',2,'SQL、Python数据分析','3-6月','Python是数据分析主流语言',1),
(8,'Python开发工程师','算法工程师',3,'机器学习、深度学习','1-2年','Python是算法主流语言',2),
(8,'Python开发工程师','Java开发工程师',3,'Java、后端开发','1-2年','编程基础扎实',3),
(8,'Python开发工程师','前端开发工程师',3,'JavaScript、Vue、React','6-12月','Python+前端=全栈',4),
(9,'算法工程师','数据分析师',2,'SQL、Python数据分析','3-6月','算法和数据天然结合',1),
(9,'算法工程师','产品经理',3,'产品设计、AI产品','1-2年','AI技术背景，产品优势大',2),
(9,'算法工程师','Java开发工程师',4,'Java、工程化','1-2年','算法落地需要工程化能力',3),
(9,'算法工程师','项目经理',4,'PMP、团队管理','2-3年','算法团队管理发展方向',4),
(10,'数据分析师','算法工程师',3,'机器学习、深度学习','1-2年','数据分析是算法基础',1),
(10,'数据分析师','Java开发工程师',4,'Java、数据平台','1-2年','数据平台开发需求大',2),
(10,'数据分析师','产品经理',3,'产品设计、数据产品','1-2年','数据驱动产品决策',3),
(10,'数据分析师','项目经理',4,'PMP、BI项目管理','1-2年','数据分析项目管理专长',4),
(11,'UI设计师','前端开发工程师',3,'HTML、CSS、JavaScript','6-12月','设计转前端有天然优势',1),
(11,'UI设计师','产品经理',3,'产品设计、UX研究','1-2年','设计背景产品经理优势大',2),
(11,'UI设计师','项目经理',4,'PMP、设计团队管理','2-3年','设计团队管理发展方向',3),
(11,'UI设计师','UX研究员',3,'用户研究、可用性测试','6-12月','设计到用户研究自然过渡',4),
(12,'嵌入式开发工程师','硬件测试工程师',2,'硬件测试方法、仪器使用','3-6月','嵌入式测试是专业方向',1),
(12,'嵌入式开发工程师','Java开发工程师',4,'Java、IoT平台','1-2年','物联网平台开发需求大',2),
(12,'嵌入式开发工程师','测试工程师',3,'嵌入式测试','6-12月','嵌入式测试是专业方向',3),
(12,'嵌入式开发工程师','项目经理',4,'PMP、嵌入式项目管理','2-3年','嵌入式项目管理专长',4),
(13,'AI工程师','算法工程师',2,'算法研究、前沿技术','6-12月','AI工程师本身具备算法能力',1),
(13,'AI工程师','大数据工程师',3,'数据工程、特征工程','1-2年','AI落地依赖数据工程',2),
(13,'AI工程师','产品经理',3,'AI产品设计、需求分析','1-2年','AI背景，产品经理转型有优势',3),
(13,'AI工程师','运维工程师',3,'MLOps、模型部署','6-12月','AI运维是新兴方向',4),
(14,'网络安全工程师','运维工程师',2,'Linux、Shell、监控','3-6月','安全运维本就是安全工程师工作',1),
(14,'网络安全工程师','Java开发工程师',3,'Java安全编程、安全框架','1-2年','安全编程，企业安全开发需求大',2),
(14,'网络安全工程师','测试工程师',2,'安全测试、渗透测试方法','6-12月','安全测试是测试的特殊领域',3),
(14,'网络安全工程师','项目经理',4,'PMP、安全项目管理','2-3年','安全项目管理发展方向',4),
(15,'硬件测试工程师','嵌入式开发工程师',3,'C、ARM、RTOS','1-2年','硬件测试经验，嵌入式入门快',1),
(15,'硬件测试工程师','质量工程师',2,'质量管理、ISO标准','6-12月','测试即质量工作，自然转型',2),
(15,'硬件测试工程师','测试工程师',2,'软件测试方法','3-6月','测试基础相通',3),
(15,'硬件测试工程师','项目经理',4,'PMP、测试项目管理','2-3年','测试团队管理发展方向',4);

-- ============================================================
-- 第八步：启用外键检查
-- ============================================================
SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Database initialization completed successfully.' AS status;

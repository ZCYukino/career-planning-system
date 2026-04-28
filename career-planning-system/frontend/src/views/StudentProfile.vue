<template>
  <div class="profile-page">
    <!-- 飞过的职业名称 -->
    <div class="careers-layer">
      <div
        v-for="(item, i) in careers"
        :key="i"
        class="career-drift"
        :style="item.driftStyle"
      >
        <span class="career-word" :style="item.wordStyle">{{ item.name }}</span>
      </div>
    </div>



    <!-- 双栏纸张 -->
    <div class="papers">
      <!-- 左页纸：个人信息 -->
      <div class="paper left-paper">
        <!-- 小图标装饰 -->
        <svg class="icon-deco" style="bottom:18px;right:16px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>

        <div class="paper-content">
          <!-- 档案完成度 -->
          <div class="profile-completion stagger-item" :style="{ animationDelay: '0s' }">
            <div class="completion-header">
              <span class="completion-label">档案完成度</span>
              <span class="completion-percent">{{ profileCompletionPercent }}%</span>
            </div>
            <div class="completion-bar-track">
              <div class="completion-bar-fill" :style="{ width: profileCompletionPercent + '%' }"></div>
            </div>
            <div class="completion-hint" v-if="profileCompletionHint">{{ profileCompletionHint }}</div>
          </div>

          <!-- 个人信息 -->
          <div class="section-wrapper stagger-item" :style="{ animationDelay: '0.05s' }">
            <h3 class="section-title section-title--accent">个人信息</h3>
            <div class="divider"></div>
          </div>

          <div v-if="resumeUploadTip" class="paper-alert">{{ resumeUploadTip }}</div>

          <el-form :model="form" label-width="84px" class="paper-form">
            <div class="form-section-label form-section-label--accent">教育信息 <span class="required-hint">（完整填写后可生成能力画像）</span></div>

            <el-form-item label="姓名" required>
              <el-input v-model="form.name" placeholder="请输入真实姓名" clearable maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="毕业院校" required>
              <el-input v-model="form.graduationSchool" placeholder="请输入毕业院校全称" clearable maxlength="100" />
            </el-form-item>
            <el-form-item label="专业" required>
              <el-input v-model="form.major" placeholder="请输入专业名称" clearable maxlength="100" />
            </el-form-item>
            <el-form-item label="最高学历" required>
              <el-select v-model="form.education" placeholder="请选择最高学历" clearable style="width:100%">
                <el-option label="专科" value="专科" />
                <el-option label="本科" value="本科" />
                <el-option label="硕士" value="硕士" />
                <el-option label="博士" value="博士" />
              </el-select>
            </el-form-item>
            <el-form-item label="毕业年份" required>
              <el-select v-model="form.graduationYear" placeholder="请选择毕业年份" style="width:100%" filterable :fit-input-width="true">
                <el-option v-for="year in graduationYearOptions" :key="year" :label="year + ' 年'" :value="year" />
              </el-select>
            </el-form-item>

            <div class="form-section-label form-section-label--accent" style="margin-top:20px">职业信息（选填）</div>

            <el-form-item label="意向岗位">
              <el-input v-model="form.careerIntention" type="textarea" :rows="2" placeholder="如：Java 开发、前端工程师" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item label="性格特点">
              <el-input v-model="form.personalityTraits" type="textarea" :rows="2" placeholder="如：认真负责、善于沟通" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item label="工作年限">
              <el-select v-model="form.workExperienceYears" placeholder="选填" clearable style="width:100%">
                <el-option label="应届生" value="应届生" />
                <el-option label="1 年以内" value="1年以内" />
                <el-option label="1～3 年" value="1-3年" />
                <el-option label="3～5 年" value="3-5年" />
                <el-option label="5 年以上" value="5年以上" />
              </el-select>
            </el-form-item>

            <div class="form-section-label form-section-label--accent" style="margin-top:20px">简历</div>

            <el-form-item label="简历上传">
              <el-upload class="upload-area" action="#" :http-request="handleUpload" :limit="1" :on-exceed="handleExceed">
                <button type="button" class="upload-resume-btn">
                  <span class="upload-resume-btn__text">点击上传简历</span>
                </button>
                <template #tip>
                  <div class="upload-tip">支持 PDF / Word（.docx）文件</div>
                </template>
              </el-upload>
            </el-form-item>

            <div class="form-actions" style="margin-top:8px">
              <button type="button" class="action-line" @click="saveInfo">
                <span class="action-dash"></span>
                <span class="action-text">保存信息</span>
                <span class="action-dash"></span>
              </button>
            </div>
          </el-form>
        </div>
      </div>

      <!-- 右页纸：能力画像 -->
      <div class="paper right-paper">
        <!-- 小图标装饰 -->
        <svg class="icon-deco" style="bottom:18px;right:16px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>

        <div class="paper-content">
          <h3 class="section-title section-title--accent">就业能力画像</h3>
          <div class="divider"></div>

          <!-- 骨架屏加载态 -->
          <div v-if="isLoadingProfile" class="skeleton-wrapper stagger-item">
            <div class="skeleton-section-header">
              <div class="skeleton skeleton-ring"></div>
              <div class="skeleton skeleton-badge"></div>
              <div class="skeleton-loading-hint">正在加载能力画像...</div>
            </div>
            <div class="skeleton-divider"></div>
            <div class="skeleton-label">能力维度</div>
            <div class="skeleton-dim-row">
              <div class="skeleton-dim-cell" v-for="i in 5" :key="'sk-dim-'+i">
                <div class="skeleton skeleton-mini-ring"></div>
                <div class="skeleton skeleton-text-center"></div>
              </div>
            </div>
            <div class="skeleton-divider" style="margin-top:20px"></div>
            <div class="skeleton-label">软技能评估</div>
            <div class="skeleton-row" v-for="i in 4" :key="'sk-ss-'+i">
              <div class="skeleton skeleton-text" :style="{ width: (50 + i*10) + '%' }"></div>
              <div class="skeleton skeleton-bar"></div>
            </div>
          </div>

          <!-- 画像生成中 -->
          <div v-else-if="isGeneratingProfile" class="generating-state">
            <div class="gen-ring-wrapper">
              <svg viewBox="0 0 120 120" class="gen-ring-svg">
                <circle cx="60" cy="60" r="50" fill="none" stroke="rgba(26,26,26,0.06)" stroke-width="6"/>
                <circle cx="60" cy="60" r="50" fill="none" stroke="#1a1a1a" stroke-width="6"
                  :stroke-dasharray="2 * Math.PI * 50"
                  :stroke-dashoffset="2 * Math.PI * 50 * (1 - profileProgress / 100)"
                  stroke-linecap="round" transform="rotate(-90 60 60)" class="gen-ring-fill"
                />
              </svg>
              <div class="gen-ring-center">
                <span class="gen-ring-num">{{ profileProgress }}</span>
                <span class="gen-ring-unit">%</span>
              </div>
            </div>
            <div class="gen-title">正在生成能力画像</div>
            <div class="gen-hint">分析您的教育背景、技能和经历...</div>
            <div class="gen-steps-vertical">
              <div v-for="(step, index) in generatingSteps" :key="step.name" class="gen-vstep"
                :class="{ done: profileProgress > step.threshold, active: isCurrentStep(step.threshold) }">
                <div class="gen-vstep-marker">
                  <div class="gen-vstep-dot">
                    <svg v-if="profileProgress > step.threshold" viewBox="0 0 24 24" fill="currentColor" width="12" height="12"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/></svg>
                    <span v-else>{{ index + 1 }}</span>
                  </div>
                  <div v-if="index < generatingSteps.length - 1" class="gen-vstep-line"
                    :class="{ filled: profileProgress > step.threshold }"></div>
                </div>
                <div class="gen-vstep-body">
                  <div class="gen-vstep-name">{{ step.name }}</div>
                  <div class="gen-vstep-status">
                    <span v-if="profileProgress > step.threshold">已完成</span>
                    <span v-else-if="isCurrentStep(step.threshold)" class="gen-vstep-active-text">进行中...</span>
                    <span v-else>等待中</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="gen-current-step">{{ profileStepName }}</div>
          </div>

          <!-- 画像内容 -->
          <div v-else-if="abilityProfile" class="profile-result">
            <div class="score-section stagger-item" :style="{ animationDelay: '0s' }">
              <div class="score-card">
                <div class="score-ring">
                  <svg viewBox="0 0 120 120" class="score-svg">
                    <circle cx="60" cy="60" r="50" fill="none" stroke="rgba(26,26,26,0.06)" stroke-width="7"/>
                    <circle cx="60" cy="60" r="50" fill="none" stroke="#1a1a1a" stroke-width="7" :stroke-dasharray="Math.max(0, getOverallScore()) * 3.14 + ' 314'" stroke-linecap="round" transform="rotate(-90 60 60)" class="score-ring-anim"/>
                  </svg>
                  <div class="score-center">
                    <span class="score-num">{{ getOverallScore() }}</span>
                    <span class="score-label">综合评分</span>
                  </div>
                </div>
                <div class="score-side">
                  <div class="level-badge" :class="getLevelClass(getOverallScore())">{{ getCompetitivenessLevel() }}</div>
                  <div class="score-summary">基于您的教育背景、技能、经历综合评估</div>
                </div>
              </div>
            </div>

            <div class="sub-title-row stagger-item" :style="{ animationDelay: '0.06s' }">
              <div class="sub-line"></div>
              <span class="sub-text">能力维度分析</span>
              <div class="sub-line"></div>
            </div>

            <div class="dimension-rings">
              <div class="dimension-ring-item stagger-item" v-for="(dim, idx) in dimensionScores" :key="dim.key" :style="{ animationDelay: (0.1 + idx * 0.08) + 's' }">
                <div class="dim-ring-card">
                  <svg class="dim-ring-svg" viewBox="0 0 80 80">
                    <circle cx="40" cy="40" r="32" fill="none" stroke="rgba(26,26,26,0.06)" stroke-width="4"/>
                    <circle cx="40" cy="40" r="32" fill="none" stroke="#1a1a1a" stroke-width="4"
                      :stroke-dasharray="(dim.score / 100) * 201.06 + ' 201.06'"
                      stroke-linecap="round"
                      transform="rotate(-90 40 40)"
                      class="dim-ring-progress"
                    />
                  </svg>
                  <div class="dim-ring-center">
                    <span class="dim-ring-score">{{ dim.score }}</span>
                  </div>
                </div>
                <div class="dim-ring-name">{{ dim.name }}</div>
              </div>
            </div>

            <!-- 维度详细分析 -->
            <div class="dimension-details stagger-item" :style="{ animationDelay: '0.4s' }">
              <div class="dim-detail-item" v-for="dim in dimensionScores" :key="'detail-'+dim.key">
                <div class="dim-detail-header">
                  <span class="dim-detail-name">{{ dim.name }}</span>
                  <span class="dim-detail-score">{{ dim.score }}<span class="dim-detail-unit">分</span></span>
                </div>
                <div class="dim-detail-bar">
                  <div class="dim-detail-bar-fill" :style="{ width: dim.score + '%' }"></div>
                </div>
                <div class="dim-analysis">{{ dim.analysis }}</div>
              </div>
            </div>

            <div class="sub-title-row stagger-item" :style="{ animationDelay: '0.46s' }">
              <div class="sub-line"></div>
              <span class="sub-text">软技能评估</span>
              <div class="sub-line"></div>
            </div>

            <div class="skills-overview-bar stagger-item" :style="{ animationDelay: '0.5s' }">
              <div class="skill-overview-item" v-for="skill in softSkillItems" :key="'ov-'+skill.key">
                <span class="skill-ov-name">{{ skill.name }}</span>
                <div class="skill-ov-track">
                  <div class="skill-ov-fill" :style="{ width: skill.score + '%' }"></div>
                </div>
                <span class="skill-ov-score">{{ skill.score }}</span>
              </div>
            </div>

            <div class="soft-skills-detail-list stagger-item" :style="{ animationDelay: '0.55s' }">
              <div class="soft-skill-item" v-for="skill in softSkillItems" :key="'detail-'+skill.key">
                <div class="ss-header">
                  <span class="ss-name">{{ skill.name }}</span>
                  <span class="ss-score">{{ skill.score }}分</span>
                </div>
                <div class="ss-bar">
                  <div class="ss-bar-fill" :style="{ width: skill.score + '%' }"></div>
                </div>
                <div class="ss-evidence">{{ skill.evidence }}</div>
                <div class="ss-improve" v-if="skill.improvement">{{ skill.improvement }}</div>
              </div>
            </div>

            <div class="sub-title-row stagger-item" :style="{ animationDelay: '0.56s' }">
              <div class="sub-line"></div>
              <span class="sub-text">综合评估</span>
              <div class="sub-line"></div>
            </div>

            <div class="assess-grid stagger-item" :style="{ animationDelay: '0.58s' }">
              <div class="assess-col strengths-col">
                <div class="assess-col-header">
                  <span class="assess-col-icon">✦</span>
                  <span class="assess-col-title">核心优势</span>
                </div>
                <div class="assess-tags">
                  <span class="assess-tag strength-tag" v-for="(item, idx) in getStrengths()" :key="idx">{{ item }}</span>
                </div>
              </div>
              <div class="assess-col shortcomings-col">
                <div class="assess-col-header">
                  <span class="assess-col-icon">△</span>
                  <span class="assess-col-title">待提升项</span>
                </div>
                <div class="assess-tags">
                  <span class="assess-tag shortcoming-tag" v-for="(item, idx) in getShortcomings()" :key="idx">{{ item }}</span>
                </div>
              </div>
            </div>

            <div class="improve-list" v-if="getImprovementPriority().length">
              <div class="sub-title-row">
                <div class="sub-line"></div>
                <span class="sub-text">优先提升建议</span>
                <div class="sub-line"></div>
              </div>
              <div class="improve-card stagger-item" v-for="(item, idx) in getImprovementPriority()" :key="idx"
                :style="{ animationDelay: (0.6 + idx * 0.06) + 's' }"
              >
                <div class="improve-priority-bar" :class="getPriorityClass(idx)"></div>
                <div class="improve-card-content">
                  <div class="improve-card-header">
                    <span class="improve-num">{{ idx + 1 }}</span>
                    <span class="improve-priority-tag" :class="getPriorityClass(idx)">{{ getPriorityLabel(idx) }}</span>
                  </div>
                  <span class="improve-text">{{ item }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 空态 -->
          <div v-else-if="!isLoadingProfile" class="empty-state stagger-item" :style="{ animationDelay: '0.1s' }">
            <div class="empty-icon-paper">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" width="48" height="48"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <div class="empty-title">还没有能力画像</div>
            <div class="empty-desc">完善左侧个人信息，填写教育背景、技能特长等内容<br>点击「生成能力画像」即可获得专属分析报告</div>
            <div class="empty-steps">
              <div class="empty-step">
                <span class="empty-step-num">1</span>
                <span>填写个人信息</span>
              </div>
              <span class="empty-arrow">→</span>
              <div class="empty-step">
                <span class="empty-step-num">2</span>
                <span>点击生成</span>
              </div>
              <span class="empty-arrow">→</span>
              <div class="empty-step">
                <span class="empty-step-num">3</span>
                <span>查看报告</span>
              </div>
            </div>
          </div>

          <!-- 生成画像按钮（在右页底部） -->
          <div class="paper-bottom" v-if="!isGeneratingProfile">
            <button class="action-line" @click="generateProfile" :disabled="generating">
              <span class="action-dash"></span>
              <span class="action-text">{{ generating ? '生成中...' : '生成能力画像' }}</span>
              <span class="action-dash"></span>
            </button>
          </div>
        </div>
      </div>
    </div>



    <p class="footer">© 2026 职业规划智能体</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue';
import { useUserStore } from '@/stores/user';
import { uploadResume, createAbilityProfileTask, getAbilityProfileStatus, getLatestAbilityProfile, updateStudentInfo } from '@/api/student';
import { getUserInfo } from '@/api/auth';
import { ElMessage } from 'element-plus';

const userStore = useUserStore();

// --- 500种职业名称 + 随机飘动样式 ---
const careerNames = [
  '前端工程师','后端工程师','全栈工程师','算法工程师','机器学习工程师','数据工程师','数据分析师','数据科学家','人工智能工程师','深度学习工程师',
  'NLP工程师','计算机视觉工程师','大模型工程师','提示词工程师','搜索工程师','推荐算法工程师','DevOps工程师','SRE工程师','云计算工程师','云原生工程师',
  '容器平台工程师','Kubernetes工程师','Linux工程师','网络工程师','网络安全工程师','信息安全工程师','渗透测试工程师','安全研究员','加密工程师','系统架构师',
  '解决方案架构师','软件架构师','嵌入式工程师','固件工程师','芯片验证工程师','FPGA工程师','硬件工程师','物联网工程师','边缘计算工程师','移动端工程师',
  'Android工程师','iOS工程师','鸿蒙工程师','客户端工程师','游戏客户端工程师','游戏服务器工程师','Unity工程师','Unreal工程师','图形工程师','音视频工程师',
  '流媒体工程师','测试开发工程师','自动化测试工程师','QA工程师','数据库工程师','DBA','中间件工程师','分布式系统工程师','高性能计算工程师','区块链工程师',
  'Web3工程师','运维工程师','平台工程师','低代码工程师','RPA工程师','ERP实施顾问','CRM实施顾问','技术支持工程师','售前工程师','售后工程师',
  '产品经理','技术产品经理','项目经理','Scrum Master','技术文档工程师','开源维护者','编译器工程师','编程语言工程师','操作系统工程师','浏览器工程师',
  '搜索引擎工程师','爬虫工程师','GIS工程师','数字孪生工程师','BI工程师','数仓工程师','ETL工程师','AIGC工程师','MLOps工程师','FinOps工程师',
  '前端架构师','后端架构师','微服务工程师','API工程师','可观测性工程师','仿真工程师','机器人软件工程师','自动驾驶工程师','SLAM工程师','ROS工程师'
];

const careers = (() => {
  const items: Array<{name: string; driftStyle: Record<string,string>; wordStyle: Record<string,string|number>}> = [];
  for (let repeat = 0; repeat < 5; repeat++) {
    for (const name of careerNames) {
      const top = Math.random() * 94 + 3;
      const size = 11 + Math.random() * 10;
      const opacity = 0.12 + Math.random() * 0.22;
      const duration = 25 + Math.random() * 35;
      const delay = -(Math.random() * duration);
      const drift = Math.random() > 0.5 ? 'drift-left' : 'drift-right';
      items.push({
        name,
        driftStyle: {
          top: `${top}%`,
          animation: `${drift} ${duration}s ${delay}s linear infinite`,
        },
        wordStyle: {
          fontSize: `${size}px`,
          opacity,
        }
      });
    }
  }
  return items;
})();

/** 个人信息表单默认值（与后端 StudentInfo 驼峰字段一致） */
function emptyProfileForm() {
  return {
    name: '',
    graduationSchool: '',
    major: '',
    education: '',
    graduationYear: undefined as number | undefined,
    careerIntention: '',
    personalityTraits: '',
    workExperienceYears: '',
    resumeFilePath: ''
  };
}

const form = ref<ReturnType<typeof emptyProfileForm>>(emptyProfileForm());
const abilityProfile = ref<any>(null);
const generating = ref(false);
const resumeUploadTip = ref('');
const isLoadingProfile = ref(true);

// 档案完成度计算
const profileCompletionPercent = computed(() => {
  const f = form.value;
  const fields = [
    { filled: !!f.name?.trim(), weight: 15 },
    { filled: !!f.graduationSchool?.trim(), weight: 15 },
    { filled: !!f.major?.trim(), weight: 15 },
    { filled: !!f.education?.trim(), weight: 10 },
    { filled: f.graduationYear != null && !Number.isNaN(Number(f.graduationYear)), weight: 10 },
    { filled: !!userStore.userInfo?.gender, weight: 5 },
    { filled: !!userStore.userInfo?.email?.trim(), weight: 5 },
    { filled: !!f.careerIntention?.trim(), weight: 8 },
    { filled: !!f.personalityTraits?.trim(), weight: 7 },
    { filled: !!f.workExperienceYears, weight: 5 },
    { filled: !!f.resumeFilePath, weight: 5 },
  ];
  const total = fields.reduce((sum, field) => sum + (field.filled ? field.weight : 0), 0);
  return Math.min(100, total);
});

const profileCompletionHint = computed(() => {
  const f = form.value;
  const missing: string[] = [];
  if (!f.resumeFilePath) missing.push('上传简历可将完成度提升至 100%');
  else if (!f.name?.trim()) missing.push('请填写姓名');
  else if (!f.graduationSchool?.trim()) missing.push('请填写毕业院校');
  else if (!f.major?.trim()) missing.push('请填写专业');
  else if (!f.education?.trim()) missing.push('请选择最高学历');
  else if (f.graduationYear == null) missing.push('请选择毕业年份');
  if (!missing.length && profileCompletionPercent.value < 100) return '补充选填项可提升档案完整度';
  if (!missing.length) return '档案已完善';
  return missing[0];
});

// 优先级指示器
const getPriorityClass = (idx: number) => {
  if (idx === 0) return 'priority-high';
  if (idx === 1) return 'priority-mid';
  return 'priority-low';
};

const getPriorityLabel = (idx: number) => {
  if (idx === 0) return '高';
  if (idx === 1) return '中';
  return '低';
};

// 画像生成进度相关状态
const isGeneratingProfile = ref(false);
const profileProgress = ref(0);
const profileStepName = ref('');
const profilePollTimer = ref<number | undefined>(undefined);
const profileTaskId = ref<number | null>(null);

// 生成步骤定义（4步进度条）
const generatingSteps = [
  { name: '数据准备', threshold: 15 },
  { name: '综合分析', threshold: 45 },
  { name: '能力评估', threshold: 75 },
  { name: '生成报告', threshold: 90 }
];

// 判断是否为当前活跃步骤
const isCurrentStep = (threshold: number) => {
  return profileProgress.value >= threshold &&
    (generatingSteps[generatingSteps.findIndex(s => s.threshold === threshold) + 1]
      ? profileProgress.value < generatingSteps[generatingSteps.findIndex(s => s.threshold === threshold) + 1].threshold
      : true);
};

// 毕业年份下拉选项
const currentYear = new Date().getFullYear();
const graduationYearOptions = computed(() => {
  return Array.from({ length: 36 }, (_, i) => 2000 + i);
});

if (form.value.graduationYear == null || Number.isNaN(Number(form.value.graduationYear))) {
  form.value.graduationYear = currentYear;
}





function mergeProfileFormFromUser(info: Record<string, any>) {
  const base = emptyProfileForm();
  form.value = {
    ...base,
    ...info,
    graduationYear:
      info.graduationYear != null && info.graduationYear !== ''
        ? Number(info.graduationYear)
        : undefined
  };
}

function buildStudentPayload() {
  return {
    name: form.value.name,
    graduationSchool: form.value.graduationSchool,
    major: form.value.major,
    education: form.value.education,
    graduationYear: form.value.graduationYear,
    careerIntention: form.value.careerIntention,
    personalityTraits: form.value.personalityTraits,
    workExperienceYears: form.value.workExperienceYears,
    resumeFilePath: form.value.resumeFilePath
  };
}

const loadLatestProfile = async () => {
  const studentId = userStore.userInfo?.id;
  if (!studentId) return;
  try {
    const latest: any = await getLatestAbilityProfile(studentId);
    abilityProfile.value = typeof latest === 'string' ? JSON.parse(latest) : latest;
  } catch {
    abilityProfile.value = null;
  }
};

function validateEducationForProfile(): boolean {
  const f = form.value;
  const missing: string[] = [];
  if (!f.name?.trim()) missing.push('姓名');
  if (!f.graduationSchool?.trim()) missing.push('毕业院校');
  if (!f.major?.trim()) missing.push('专业');
  if (!f.education?.trim()) missing.push('最高学历');
  if (f.graduationYear == null || Number.isNaN(Number(f.graduationYear))) missing.push('毕业年份');
  if (missing.length) {
    ElMessage.warning(`生成画像前请先填写并保存教育信息：${missing.join('、')}`);
    return false;
  }
  return true;
}

onMounted(async () => {
  try {
    const info: any = await getUserInfo();
    userStore.setUserInfo({ ...userStore.userInfo, ...info });
    mergeProfileFormFromUser({ ...userStore.userInfo, ...info });
  } catch {
    if (userStore.userInfo) {
      mergeProfileFormFromUser(userStore.userInfo as any);
    }
  }

  const saved = sessionStorage.getItem('profileGenerating');
  if (saved) {
    try {
      const savedState = JSON.parse(saved);
      if (savedState.isGenerating && savedState.taskId) {
        profileTaskId.value = Number(savedState.taskId);
        profileProgress.value = savedState.progress || 0;
        profileStepName.value = savedState.stepName || '处理中...';
        isGeneratingProfile.value = true;
        generating.value = true;
        abilityProfile.value = null;
        isLoadingProfile.value = false;
        startProfilePolling();
        return;
      }
    } catch (e) {
      console.error('恢复生成状态失败:', e);
      sessionStorage.removeItem('profileGenerating');
    }
  }

  await loadLatestProfile();
  isLoadingProfile.value = false;
});

const handleUpload = async (options: any) => {
  const formData = new FormData();
  formData.append('file', options.file);
  formData.append('studentId', userStore.userInfo.id);
  try {
    const fileUrl = await uploadResume(formData);
    userStore.setUserInfo({ ...userStore.userInfo, resumeFilePath: fileUrl });
    form.value = { ...form.value, resumeFilePath: fileUrl };
    ElMessage.success('简历上传成功');

    if (userStore.userInfo?.id && validateEducationForProfile()) {
      await updateStudentInfo(userStore.userInfo.id, buildStudentPayload());
      ElMessage.success('简历上传成功，信息已保存');
    } else {
      resumeUploadTip.value = '简历上传成功，请补全教育信息后点击「生成能力画像」';
      setTimeout(() => { resumeUploadTip.value = ''; }, 5000);
    }
  } catch {
    ElMessage.error('上传失败');
  }
};

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件');
};

const saveInfo = async () => {
  if (!userStore.userInfo?.id) return;
  const payload = buildStudentPayload();
  try {
    await updateStudentInfo(userStore.userInfo.id, payload);
    userStore.setUserInfo({ ...userStore.userInfo, ...payload });
    ElMessage.success('信息保存成功');
  } catch {
    ElMessage.error('保存失败');
  }
};

const generateProfile = async () => {
  if (!userStore.userInfo?.id) return;
  if (!validateEducationForProfile()) return;

  abilityProfile.value = null;
  isGeneratingProfile.value = true;
  profileProgress.value = 0;
  profileStepName.value = '正在创建任务...';
  generating.value = true;

  try {
    const res: any = await createAbilityProfileTask(userStore.userInfo.id);
    const taskId = Number(res?.taskId || userStore.userInfo.id);
    profileTaskId.value = taskId;

    sessionStorage.setItem('profileGenerating', JSON.stringify({
      isGenerating: true,
      taskId: taskId,
      progress: 0,
      stepName: '正在创建任务...'
    }));

    startProfilePolling();
  } catch (error: any) {
    isGeneratingProfile.value = false;
    generating.value = false;
    profileTaskId.value = null;
    sessionStorage.removeItem('profileGenerating');
    ElMessage.error('任务创建失败: ' + (error?.message || '未知错误'));
  }
};

const startProfilePolling = () => {
  if (profilePollTimer.value) {
    clearInterval(profilePollTimer.value);
  }

  const startTime = Date.now();
  const PROFILE_POLL_TIMEOUT = 3 * 60 * 1000;

  profilePollTimer.value = window.setInterval(async () => {
    if (Date.now() - startTime > PROFILE_POLL_TIMEOUT) {
      if (profilePollTimer.value !== undefined) {
        clearInterval(profilePollTimer.value);
        profilePollTimer.value = undefined;
      }
      isGeneratingProfile.value = false;
      generating.value = false;
      profileTaskId.value = null;
      sessionStorage.removeItem('profileGenerating');
      ElMessage.warning('画像生成超时，请稍后刷新页面查看');
      return;
    }

    try {
      if (profileTaskId.value == null) {
        return;
      }
      const status: any = await getAbilityProfileStatus(profileTaskId.value);

      profileProgress.value = status.progress || 0;
      profileStepName.value = status.stepName || '处理中...';

      sessionStorage.setItem('profileGenerating', JSON.stringify({
        isGenerating: true,
        taskId: profileTaskId.value,
        progress: profileProgress.value,
        stepName: profileStepName.value
      }));

      if (status.status === 'COMPLETED') {
        if (profilePollTimer.value !== undefined) {
          clearInterval(profilePollTimer.value);
          profilePollTimer.value = undefined;
        }

        let profileData = status.profile;
        if (typeof profileData === 'string') {
          try {
            profileData = JSON.parse(profileData);
          } catch (e) {
            console.error('画像JSON解析失败:', e);
          }
        }
        abilityProfile.value = profileData;
        isGeneratingProfile.value = false;
        generating.value = false;
        profileProgress.value = 100;
        profileStepName.value = '画像生成完成';
        profileTaskId.value = null;
        sessionStorage.removeItem('profileGenerating');
        ElMessage.success('画像生成成功');
      } else if (status.status === 'FAILED') {
        if (profilePollTimer.value !== undefined) {
          clearInterval(profilePollTimer.value);
          profilePollTimer.value = undefined;
        }
        isGeneratingProfile.value = false;
        generating.value = false;
        profileTaskId.value = null;
        sessionStorage.removeItem('profileGenerating');
        ElMessage.error('画像生成失败');
      }
    } catch (error) {
      console.error('轮询画像状态失败:', error);
    }
  }, 1500);
};

onUnmounted(() => {
  if (profilePollTimer.value !== undefined) {
    clearInterval(profilePollTimer.value);
    profilePollTimer.value = undefined;
  }
});

const getOverallScore = () => {
  if (!abilityProfile.value) return 0;
  if (abilityProfile.value.overall_assessment?.total_score != null) return abilityProfile.value.overall_assessment.total_score;
  if (abilityProfile.value.overall_score != null) return abilityProfile.value.overall_score;
  return 0;
};

const getCompetitivenessLevel = () => {
  if (!abilityProfile.value) return '暂无评估';
  if (abilityProfile.value.overall_assessment?.competitiveness_level) return abilityProfile.value.overall_assessment.competitiveness_level;
  const score = getOverallScore();
  if (score >= 90) return '优秀';
  if (score >= 75) return '良好';
  if (score >= 60) return '一般';
  return '需提升';
};


const getLevelClass = (score: number) => {
  if (score >= 90) return 'level-excellent';
  if (score >= 75) return 'level-good';
  if (score >= 60) return 'level-normal';
  return 'level-weak';
};

const dimensionScores = computed(() => {
  if (!abilityProfile.value) return [];
  const dims: any[] = [];
  if (abilityProfile.value.professional_skills?.score != null) dims.push({ key: 'professional_skills', name: '专业技能', score: abilityProfile.value.professional_skills.score, analysis: abilityProfile.value.professional_skills.analysis || '' });
  if (abilityProfile.value.certificates?.score != null) dims.push({ key: 'certificates', name: '证书资质', score: abilityProfile.value.certificates.score, analysis: abilityProfile.value.certificates.analysis || '' });
  if (abilityProfile.value.internship_experience?.score != null) dims.push({ key: 'internship', name: '实习经历', score: abilityProfile.value.internship_experience.score, analysis: abilityProfile.value.internship_experience.analysis || '' });
  if (abilityProfile.value.basic_info?.major_match?.score != null) dims.push({ key: 'major_match', name: '专业匹配度', score: abilityProfile.value.basic_info.major_match.score, analysis: abilityProfile.value.basic_info.major_match.analysis || '' });
  if (abilityProfile.value.basic_info?.education_level?.score != null) dims.push({ key: 'education', name: '学历竞争力', score: abilityProfile.value.basic_info.education_level.score, analysis: abilityProfile.value.basic_info.education_level.analysis || '' });
  dims.forEach(dim => {
    const analysis = dim.analysis || '';
    if (analysis.trim().length < 10 || analysis === '无' || analysis === '暂无') {
      dim.analysis = '信息不足，建议完善简历以获得更准确的评估';
    }
  });
  return dims;
});

const softSkillItems = computed(() => {
  if (!abilityProfile.value?.soft_skills) return [];
  const items: any[] = [];
  const skillMap: Record<string, string> = { innovation_ability: '创新能力', learning_ability: '学习能力', pressure_resistance: '抗压能力', communication_ability: '沟通能力' };
  for (const [key, name] of Object.entries(skillMap)) {
    const skill = abilityProfile.value.soft_skills[key];
    if (skill) {
      let evidence = skill.evidence || '';
      if (evidence.trim().length < 5) {
        evidence = '暂无评估依据';
      }
      let improvement = skill.improvement || '';
      const score = skill.score != null ? skill.score : 0;
      items.push({
        key,
        name,
        score: score,
        evidence: evidence,
        improvement: improvement
      });
    }
  }
  return items;
});

const filterBadKeywords = (items: string[]): string[] => {
  return items.filter(item =>
    !item.includes('异常') &&
    !(item.includes('暂无') && item.length < 15) &&
    !item.includes('placeholder') &&
    !item.includes('待补充') &&
    !item.includes('待评估')
  );
};

const getStrengths = () => {
  if (!abilityProfile.value) return [];
  const strengths = abilityProfile.value.overall_assessment?.strengths || abilityProfile.value.strengths || [];
  const filtered = filterBadKeywords(strengths);
  return filtered.length > 0 ? filtered : ['完成简历后可获得更准确的能力评估'];
};

const getShortcomings = () => {
  if (!abilityProfile.value) return [];
  const shortcomings = abilityProfile.value.overall_assessment?.shortcomings || abilityProfile.value.shortcomings || [];
  const filtered = filterBadKeywords(shortcomings);
  return filtered.length > 0 ? filtered : ['建议完善简历信息'];
};

const getImprovementPriority = () => {
  if (!abilityProfile.value) return [];
  const priorities = abilityProfile.value.overall_assessment?.improvement_priority || [];
  return filterBadKeywords(priorities);
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&display=swap');

.profile-page {
  min-height: 100vh;
  background: #faf9f7;
  padding: 20px;
  position: relative;
  overflow-x: hidden;
}

/* ===== 飞过的职业名称 ===== */
.careers-layer {
  position: fixed;
  inset: 0;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}

.career-drift {
  position: absolute;
  white-space: nowrap;
  will-change: transform;
}

.career-word {
  display: inline-block;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #1a1a1a;
  padding: 2px 6px;
  cursor: default;
  border-radius: 0;
}


/* ===== 双栏纸张 ===== */
.papers {
  position: relative;
  z-index: 1;
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.paper {
  background: var(--paper-surface);
  background-color: var(--paper-bg-solid);
  border-radius: 1px;
  box-shadow: var(--paper-shadow);
  position: relative;
  border: 1px solid var(--paper-border);
}

.paper::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--paper-overlay);
  pointer-events: none;
}

.paper-content {
  padding: 36px 32px 32px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #1a1a1a;
  display: flex;
  flex-direction: column;
  min-height: 600px;
}

/* ===== 小图标装饰 ===== */
.icon-deco {
  position: absolute;
  width: 44px;
  height: 44px;
  color: #1a1a1a;
  opacity: 0.15;
  pointer-events: none;
  z-index: 0;
}

/* ===== 标题与分隔线 ===== */
.section-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 3px;
  margin: 0;
}

.divider {
  width: 100%;
  height: 1px;
  background: #1a1a1a;
  opacity: 0.15;
  margin: 14px 0 20px;
}

.sub-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 24px 0 16px;
}

.sub-line {
  flex: 1;
  height: 1px;
  background: #1a1a1a;
  opacity: 0.1;
}

.sub-text {
  font-size: 14px;
  color: #8a8a8a;
  letter-spacing: 2px;
  white-space: nowrap;
}

/* ===== 表单覆盖样式 ===== */
.paper-form :deep(.el-form-item__label) {
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #1a1a1a;
  font-size: 14px;
}

.paper-form :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border-bottom: 1px solid #bbb;
  border-radius: 0;
  background: transparent;
  transition: border-color 0.2s;
}

.paper-form :deep(.el-input__wrapper:hover),
.paper-form :deep(.el-input__wrapper.is-focus) {
  border-bottom-color: #1a1a1a;
}

.paper-form :deep(.el-textarea__inner) {
  border: 1px solid #ccc;
  border-radius: 2px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  resize: vertical;
}

.paper-form :deep(.el-textarea__inner:focus) {
  border-color: #1a1a1a;
}

.paper-form :deep(.el-select) {
  width: 100%;
}

.paper-form :deep(.el-radio__label) {
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #1a1a1a;
}

.form-section-label {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 12px;
  letter-spacing: 1px;
}

.required-hint {
  font-weight: 400;
  font-size: 12px;
  color: #8a8a8a;
}

.paper-alert {
  padding: 10px 16px;
  margin-bottom: 16px;
  background: rgba(26, 26, 26, 0.03);
  border-left: 3px solid #1a1a1a;
  font-size: 13px;
  color: #5a5a5a;
  line-height: 1.6;
}

/* ===== 操作按钮 ===== */
.form-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-top: 8px;
}

.action-line {
  display: flex;
  align-items: center;
  gap: 10px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px 0;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  transition: opacity 0.15s;
}

.action-line:hover { opacity: 0.5; }
.action-line:active { opacity: 0.3; }
.action-line:disabled { cursor: not-allowed; opacity: 0.3; }

.action-dash {
  display: inline-block;
  width: 50px;
  height: 1px;
  background: #1a1a1a;
}

.action-text {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 3px;
}

.action-text-sm {
  font-size: 13px;
  color: #5a5a5a;
  letter-spacing: 1px;
}

/* ===== 上传简历按钮 ===== */
.upload-resume-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #f5efe3;
  border: 1px solid #c9b99a;
  border-radius: 4px;
  padding: 8px 20px;
  cursor: pointer;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  transition: all 0.2s ease;
}

.upload-resume-btn:hover {
  background: #ece4d4;
  border-color: #a89570;
}

.upload-resume-btn:active {
  background: #e3d9c5;
  transform: scale(0.98);
}

.upload-resume-btn__text {
  font-size: 13px;
  color: #3b2412;
  letter-spacing: 1px;
  font-weight: 600;
}

.paper-bottom {
  margin-top: auto;
  padding-top: 24px;
  display: flex;
  justify-content: center;
}

/* ===== 上传区域 ===== */
.upload-area {
  width: 100%;
}

.upload-tip {
  font-size: 12px;
  color: #aaa;
  margin-top: 4px;
}

/* ===== 画像生成中 ===== */
.generating-state {
  text-align: center;
  padding: 20px 0 30px;
}

.gen-ring-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 20px;
}
.gen-ring-svg { width: 100%; height: 100%; }
.gen-ring-fill { transition: stroke-dashoffset 0.4s ease; }
.gen-ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}
.gen-ring-num {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
}
.gen-ring-unit {
  font-size: 13px;
  color: #8a8a8a;
  margin-top: 6px;
}

.gen-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 2px;
  margin-bottom: 6px;
}

.gen-hint {
  font-size: 13px;
  color: #8a8a8a;
  margin-bottom: 28px;
}

.gen-steps-vertical {
  display: flex;
  flex-direction: column;
  max-width: 260px;
  margin: 0 auto 20px;
  text-align: left;
}

.gen-vstep {
  display: flex;
  gap: 14px;
  opacity: 0.35;
  transition: opacity 0.3s;
}
.gen-vstep.done, .gen-vstep.active { opacity: 1; }

.gen-vstep-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.gen-vstep-dot {
  width: 26px;
  height: 26px;
  border: 1.5px solid #bbb;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #8a8a8a;
  flex-shrink: 0;
  transition: all 0.3s;
  background: #fdfcfb;
}
.gen-vstep.active .gen-vstep-dot {
  border-color: #1a1a1a;
  color: #1a1a1a;
  box-shadow: 0 0 0 3px rgba(26,26,26,0.08);
}
.gen-vstep.done .gen-vstep-dot {
  background: #1a1a1a;
  border-color: #1a1a1a;
  color: #fff;
}

.gen-vstep-line {
  width: 1.5px;
  height: 20px;
  background: rgba(26,26,26,0.12);
  transition: background 0.3s;
}
.gen-vstep-line.filled {
  background: #1a1a1a;
}

.gen-vstep-body {
  padding-bottom: 10px;
}
.gen-vstep-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}
.gen-vstep-status {
  font-size: 12px;
  color: #8a8a8a;
  margin-top: 2px;
}
.gen-vstep-active-text {
  color: #1a1a1a;
  font-weight: 600;
}

.gen-current-step {
  font-size: 12px;
  color: #8a8a8a;
  text-align: center;
}

/* ===== 画像结果 ===== */
.score-section {
  margin-bottom: 20px;
}

.score-card {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px 24px;
  background: rgba(26, 26, 26, 0.02);
  border: 1px solid rgba(26, 26, 26, 0.06);
}

.score-ring {
  position: relative;
  width: 110px;
  height: 110px;
  flex-shrink: 0;
}

.score-svg {
  width: 100%;
  height: 100%;
}

.score-ring-anim {
  transition: stroke-dasharray 1s ease;
}

.score-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-num {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
}

.score-label {
  font-size: 11px;
  color: #8a8a8a;
  margin-top: 4px;
}

.score-side {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-summary {
  font-size: 12px;
  color: #8a8a8a;
  line-height: 1.5;
}

.level-badge {
  display: inline-block;
  width: fit-content;
  padding: 3px 16px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 2px;
  border: 1px solid #1a1a1a;
  color: #1a1a1a;
}

.level-excellent { background: #1a1a1a; color: #fff; border-color: #1a1a1a; }
.level-good { background: #fff; color: #1a1a1a; }
.level-normal { background: #fff; color: #666; border-color: #999; }
.level-weak { background: #fff; color: #999; border-color: #ccc; }

/* 维度列表 */
.dimension-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dimension-item {
  padding: 10px 0;
  border-bottom: 1px solid rgba(26, 26, 26, 0.06);
}

.dimension-item:last-child { border-bottom: none; }

.dim-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.dim-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.dim-score {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}

.dim-bar {
  height: 3px;
  background: #e8e8e8;
  border-radius: 2px;
  margin-bottom: 6px;
}

.dim-bar-fill {
  height: 100%;
  background: #1a1a1a;
  border-radius: 2px;
  transition: width 0.3s;
}

.dim-analysis {
  font-size: 12px;
  color: #8a8a8a;
  line-height: 1.6;
  margin-top: 6px;
}

/* 软技能 */
.soft-skills-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.soft-skill-item {
  padding: 12px;
  border: 1px solid rgba(26, 26, 26, 0.08);
  border-radius: 2px;
}

.ss-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.ss-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.ss-score {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
}

.ss-bar {
  height: 3px;
  background: #e8e8e8;
  border-radius: 2px;
  margin-bottom: 8px;
}

.ss-bar-fill {
  height: 100%;
  background: #1a1a1a;
  border-radius: 2px;
}

.ss-evidence {
  font-size: 12px;
  color: #8a8a8a;
  line-height: 1.5;
}

.ss-improve {
  margin-top: 6px;
  padding: 6px 10px;
  border-left: 2px solid #1a1a1a;
  font-size: 12px;
  color: #5a5a5a;
  line-height: 1.5;
}

/* 综合评估 */
.assess-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.assess-col {
  padding: 14px;
  border: 1px solid rgba(26, 26, 26, 0.08);
  border-radius: 2px;
}

.assess-col-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.assess-col-icon {
  font-size: 14px;
  color: #1a1a1a;
  opacity: 0.5;
}

.assess-col-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 1px;
}

.assess-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.assess-tag {
  display: inline-block;
  padding: 3px 10px;
  font-size: 12px;
  color: #1a1a1a;
  border: 1px solid rgba(26, 26, 26, 0.15);
  border-radius: 2px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  line-height: 1.6;
}

.strength-tag {
  background: rgba(26, 26, 26, 0.04);
  border-color: rgba(26, 26, 26, 0.2);
}

.shortcoming-tag {
  background: transparent;
  border-style: dashed;
  color: #555;
}

.strengths-col { background: rgba(26, 26, 26, 0.015); }
.shortcomings-col { background: transparent; }

.improve-list {
  margin-top: 10px;
}

.improve-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  letter-spacing: 1px;
}

.improve-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 8px;
}

.improve-num {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1a1a1a;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  border-radius: 2px;
  flex-shrink: 0;
}

.improve-text {
  font-size: 13px;
  color: #5a5a5a;
  line-height: 1.6;
}

/* ===== 空态 ===== */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px 0;
}

.empty-icon-paper {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 1px solid rgba(26, 26, 26, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 2px;
  margin-bottom: 10px;
}

.empty-desc {
  font-size: 13px;
  color: #8a8a8a;
  line-height: 2;
  margin-bottom: 28px;
}

.empty-steps {
  display: flex;
  align-items: center;
  gap: 16px;
}

.empty-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8a8a8a;
}

.empty-step-num {
  width: 28px;
  height: 28px;
  background: #1a1a1a;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
}

.empty-arrow {
  color: #ccc;
  font-size: 16px;
}


/* ===== Footer ===== */
.footer {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-top: 24px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 11px;
  color: #aaa;
  letter-spacing: 0.5px;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .papers {
    grid-template-columns: 1fr;
    max-width: 600px;
  }
  .dimension-ring-item {
    width: 33%;
  }
}

@media (max-width: 600px) {
  .profile-page { padding: 12px 8px; }
  .papers { max-width: 100%; }
  .paper-content { padding: 24px 18px 20px; }
  .section-title { font-size: 18px; }
  .header-title { font-size: 17px; letter-spacing: 2px; }
  .action-dash { width: 36px; }
  .action-text { font-size: 14px; }
  .dimension-rings { flex-wrap: wrap; }
  .dimension-ring-item { width: 33%; }
}

/* ===== 档案完成度进度条 ===== */
.profile-completion {
  margin-bottom: 20px;
  padding: 14px 16px 12px;
  border: 1px solid rgba(26, 26, 26, 0.08);
  background: rgba(26, 26, 26, 0.015);
  border-radius: 2px;
}

.completion-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}

.completion-label {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 1px;
}

.completion-percent {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  font-variant-numeric: tabular-nums;
}

.completion-bar-track {
  width: 100%;
  height: 4px;
  background: rgba(26, 26, 26, 0.06);
  border-radius: 2px;
  overflow: hidden;
}

.completion-bar-fill {
  height: 100%;
  background: #1a1a1a;
  border-radius: 2px;
  transition: width 0.5s ease;
}

.completion-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #8a8a8a;
  line-height: 1.5;
}

/* ===== Section 标题左侧强调条 ===== */
.section-wrapper {
  position: relative;
}

.section-title--accent {
  padding-left: 12px;
  border-left: 3px solid rgba(26, 26, 26, 0.6);
}

.form-section-label--accent {
  padding-left: 10px;
  border-left: 3px solid rgba(26, 26, 26, 0.35);
}

/* ===== 骨架屏加载态 ===== */
.skeleton-wrapper {
  padding: 20px 0;
}

.skeleton {
  background: linear-gradient(90deg, rgba(26,26,26,0.04) 25%, rgba(26,26,26,0.08) 50%, rgba(26,26,26,0.04) 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s ease-in-out infinite;
  border-radius: 2px;
}

.skeleton-section-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.skeleton-ring {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  margin-bottom: 10px;
}

.skeleton-badge {
  width: 70px;
  height: 24px;
  margin-bottom: 8px;
}

.skeleton-loading-hint {
  font-size: 12px;
  color: #bbb;
  letter-spacing: 1px;
}

.skeleton-divider {
  height: 1px;
  background: rgba(26,26,26,0.06);
  margin: 16px 0;
}

.skeleton-label {
  font-size: 12px;
  color: #bbb;
  letter-spacing: 1px;
  margin-bottom: 14px;
}

.skeleton-dim-row {
  display: flex;
  justify-content: space-around;
  gap: 10px;
  margin-bottom: 10px;
}

.skeleton-dim-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.skeleton-mini-ring {
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.skeleton-text-center {
  width: 42px;
  height: 10px;
}

.skeleton-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.skeleton-text {
  height: 14px;
}

.skeleton-text-sm {
  height: 12px;
}

.skeleton-bar {
  height: 4px;
  width: 100%;
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ===== 维度环形图 ===== */
.dimension-rings {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px 16px;
  min-width: 0;
}

.dimension-ring-item {
  width: 20%;
  min-width: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  position: relative;
}

.dim-ring-card {
  position: relative;
  padding: 6px;
  transition: transform 0.2s;
}

.dim-ring-card:hover {
  transform: scale(1.08);
}

.dim-ring-svg {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
}

.dim-ring-progress {
  transition: stroke-dasharray 1s ease;
}

.dim-ring-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.dim-ring-score {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
}

.dim-ring-name {
  font-size: 11px;
  color: #555;
  text-align: center;
  letter-spacing: 0.5px;
}

/* 维度详细分析 */
.dimension-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 16px;
}

.dim-detail-item {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.05);
  transition: background 0.15s;
}

.dim-detail-item:hover {
  background: rgba(26, 26, 26, 0.015);
}

.dim-detail-item:last-child {
  border-bottom: none;
}

.dim-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.dim-detail-name {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.dim-detail-score {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}

.dim-detail-unit {
  font-size: 11px;
  font-weight: 400;
  color: #8a8a8a;
  margin-left: 1px;
}

.dim-detail-bar {
  height: 3px;
  background: rgba(26,26,26,0.06);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 6px;
}

.dim-detail-bar-fill {
  height: 100%;
  background: #1a1a1a;
  border-radius: 2px;
  transition: width 0.6s ease;
}

/* ===== 软技能概览条 ===== */
.skills-overview-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 0;
}

.skill-overview-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.skill-ov-name {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  min-width: 56px;
  text-align: right;
}

.skill-ov-track {
  flex: 1;
  height: 6px;
  background: rgba(26,26,26,0.06);
  border-radius: 3px;
  overflow: hidden;
}

.skill-ov-fill {
  height: 100%;
  background: #1a1a1a;
  border-radius: 3px;
  transition: width 0.6s ease;
}

.skill-ov-score {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  min-width: 28px;
  text-align: left;
}

/* 软技能详细列表 */
.soft-skills-detail-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ===== 优先提升建议卡片 ===== */
.improve-card {
  display: flex;
  gap: 0;
  background: #fdfcfb;
  border: 1px solid rgba(26, 26, 26, 0.06);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 8px;
  transition: box-shadow 0.2s;
}

.improve-card:hover {
  box-shadow: 0 2px 8px rgba(26, 26, 26, 0.06);
}

.improve-priority-bar {
  width: 4px;
  flex-shrink: 0;
}

.improve-priority-bar.priority-high {
  background: rgba(26, 26, 26, 0.7);
}

.improve-priority-bar.priority-mid {
  background: rgba(26, 26, 26, 0.35);
}

.improve-priority-bar.priority-low {
  background: rgba(26, 26, 26, 0.15);
}

.improve-card-content {
  padding: 10px 14px;
  flex: 1;
}

.improve-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.improve-priority-tag {
  display: inline-block;
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 2px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.improve-priority-tag.priority-high {
  background: rgba(26, 26, 26, 0.08);
  color: #1a1a1a;
}

.improve-priority-tag.priority-mid {
  background: rgba(26, 26, 26, 0.04);
  color: #5a5a5a;
}

.improve-priority-tag.priority-low {
  color: #8a8a8a;
}

/* ===== 交错入场动画 ===== */
.stagger-item {
  opacity: 0;
  animation: paper-rise 0.4s ease forwards;
}

@keyframes paper-rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

<style>
@keyframes drift-left {
  0%   { transform: translate(110vw, 0); }
  25%  { transform: translate(72vw, -16px); }
  50%  { transform: translate(38vw, 12px); }
  75%  { transform: translate(8vw, -8px); }
  100% { transform: translate(-22vw, 4px); }
}
@keyframes drift-right {
  0%   { transform: translate(-22vw, 0); }
  25%  { transform: translate(12vw, 14px); }
  50%  { transform: translate(48vw, -10px); }
  75%  { transform: translate(78vw, 8px); }
  100% { transform: translate(110vw, -6px); }
}
</style>

<template>
  <div class="job-analysis">
    <!-- 浮动职业名称 -->
    <div class="careers-layer">
      <div v-for="(c, i) in floatingCareers" :key="i" class="career-drift" :style="c.driftStyle">
        <span class="career-word" :style="c.wordStyle">{{ c.name }}</span>
      </div>
    </div>

    <!-- A4 纸张容器 -->
    <div class="doc-paper">

    <!-- 顶部横条公告纸 -->
    <div class="banner-paper">
      <div class="banner-inner">
        <h1 class="banner-title">核心岗位画像分析</h1>
        <div class="banner-sub">Top 15 热门岗位 · 点击查看聚合画像</div>
        <div class="banner-actions">
          <button class="text-btn" @click="clearCache">清除缓存</button>
          <span class="sep">·</span>
          <button class="text-btn" @click="loadTopJobs">刷新数据</button>
        </div>
      </div>
    </div>

    <!-- 岗位名片网格 -->
    <div class="cards-area">
      <!-- 骨架屏 -->
      <div v-if="loading" class="card-grid">
        <div v-for="n in skeletonCount" :key="'sk-'+n" class="skeleton-card">
          <div class="skeleton skeleton-rank"></div>
          <div class="skeleton skeleton-name"></div>
          <div class="skeleton skeleton-count"></div>
          <div class="skeleton-row">
            <div class="skeleton skeleton-tag"></div>
            <div class="skeleton skeleton-tag"></div>
            <div class="skeleton skeleton-tag"></div>
          </div>
        </div>
      </div>

      <!-- 空态 -->
      <div v-if="!loading && topJobs.length === 0" class="empty-paper">
        <p class="empty-title">暂无岗位数据</p>
        <p class="empty-desc">暂无可用岗位数据，请稍后再试</p>
        <button class="action-line" @click="loadTopJobs">
          <span class="action-dash"></span>
          <span class="action-text">重新加载</span>
          <span class="action-dash"></span>
        </button>
      </div>

      <!-- 名片网格 -->
      <div v-if="!loading && topJobs.length > 0" class="card-grid">
        <div
          v-for="(job, index) in topJobs"
          :key="index"
          class="name-card"
          :style="staggerStyle(index)"
          @click="analyzeJob(job.jobName || job.job_name)"
        >
          <div class="card-rank">{{ index + 1 }}</div>
          <div class="card-name">{{ job.jobName || job.job_name }}</div>
          <div class="card-count">{{ job.count || job.job_count || 0 }}家企业在招</div>
          <!-- 技能预览标签 -->
          <div class="card-skills">
            <span class="card-skill-tag" v-for="(skill, si) in getJobSkills(index)" :key="si">{{ skill }}</span>
          </div>
          <!-- 悬停底部操作栏 -->
          <div class="card-hover-bar">
            <span class="bar-link" @click.stop="analyzeJob(job.jobName || job.job_name)">聚合画像</span>
            <span class="bar-sep">|</span>
            <span class="bar-link path-link" @click.stop="openJobDetail(job.jobName || job.job_name)">路径图谱</span>
          </div>
          <!-- 小图标装饰 -->
          <svg class="card-deco" viewBox="0 0 40 40" fill="none">
            <circle cx="20" cy="20" r="14" stroke="currentColor" stroke-width="0.8"/>
            <circle cx="20" cy="20" r="8" stroke="currentColor" stroke-width="0.6"/>
            <circle cx="20" cy="20" r="2" fill="currentColor"/>
          </svg>
        </div>
      </div>
    </div>

    </div><!-- /.doc-paper -->

    <!-- 岗位画像详情 — 纸张覆盖层 -->
    <teleport to="body">
      <div v-if="profileVisible" class="overlay" @click.self="profileVisible = false">
        <div class="profile-paper">
          <button class="close-btn" @click="profileVisible = false">✕</button>
          <div class="profile-content" v-if="currentProfile">
            <!-- 标题 -->
            <h2 class="profile-title">{{ currentProfile.job_title || currentJobName }}</h2>
            <div class="divider"></div>

            <!-- 基本信息表格 -->
            <table class="info-table">
              <tr>
                <td class="label">薪资参考</td>
                <td>{{ currentProfile.salary_reference || '面议' }}</td>
                <td class="label">行业前景</td>
                <td>{{ currentProfile.industry_outlook || '行业发展稳定' }}</td>
              </tr>
              <tr>
                <td class="label">技能等级</td>
                <td>{{ currentProfile.professional_skills?.level || '中级' }}</td>
                <td class="label">经验要求</td>
                <td>{{ currentProfile.experience?.years || '1-3年' }}</td>
              </tr>
            </table>

            <!-- 七维度雷达图 -->
            <div class="section-divider"></div>
            <h3 class="section-title">七维度能力画像</h3>
            <div class="radar-section">
              <div ref="radarChartRef" class="radar-chart"></div>
              <div class="radar-legend">
                <div class="legend-row" v-for="(item, idx) in radarData" :key="idx">
                  <span class="legend-dot"></span>
                  <span class="legend-label">{{ item.name }}</span>
                  <div class="legend-bar-wrap">
                    <div class="legend-bar-bg">
                      <div class="legend-bar-fill" :style="{ width: (item.value / 5 * 100) + '%', background: getDimensionBarColor(item.value) }"></div>
                    </div>
                  </div>
                  <span class="legend-value">{{ item.value }}/5</span>
                </div>
              </div>
            </div>

            <!-- 专业技能 + 证书 -->
            <div class="section-divider"></div>
            <div class="two-col">
              <div>
                <h3 class="section-title">专业技能要求</h3>
                <div class="strengths-block" v-if="currentProfile.professional_skills?.required?.length">
                  <p class="tag-label">必备</p>
                  <div class="tags">
                    <span class="skill-tag strength" v-for="(s, i) in currentProfile.professional_skills.required" :key="i">{{ s }}</span>
                  </div>
                </div>
                <div class="strengths-block" v-if="currentProfile.professional_skills?.preferred?.length">
                  <p class="tag-label">加分</p>
                  <div class="tags">
                    <span class="skill-tag strength" v-for="(s, i) in currentProfile.professional_skills.preferred" :key="i">{{ s }}</span>
                  </div>
                </div>
                <p class="analysis-text" v-if="currentProfile.professional_skills?.analysis">{{ currentProfile.professional_skills.analysis }}</p>
              </div>
              <div>
                <h3 class="section-title">证书要求</h3>
                <div class="tag-group weaknesses-block" v-if="currentProfile.certificates?.required?.length">
                  <p class="tag-label">必须</p>
                  <div class="tags">
                    <span class="skill-tag weakness" v-for="(c, i) in currentProfile.certificates.required" :key="i">{{ c }}</span>
                  </div>
                </div>
                <p v-else class="empty-hint">无必须证书要求</p>
                <div class="tag-group" v-if="currentProfile.certificates?.preferred?.length">
                  <p class="tag-label">加分</p>
                  <div class="tags">
                    <span class="skill-tag light" v-for="(c, i) in currentProfile.certificates.preferred" :key="i">{{ c }}</span>
                  </div>
                </div>
                <p class="analysis-text" v-if="currentProfile.certificates?.analysis">{{ currentProfile.certificates.analysis }}</p>
              </div>
            </div>

            <!-- 通用素质 -->
            <div class="section-divider"></div>
            <h3 class="section-title">通用素质要求</h3>
            <div class="soft-grid">
              <div class="soft-item" v-for="(skill, key) in softSkillsDisplay" :key="key">
                <div class="soft-head">
                  <span class="soft-name">{{ skill.name }}</span>
                  <span class="soft-level">Lv.{{ skill.level }}/5</span>
                </div>
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: (skill.level / 5 * 100) + '%' }"></div>
                </div>
                <p class="soft-desc">{{ skill.description || '暂无描述' }}</p>
              </div>
            </div>

            <!-- 学历与经验 -->
            <div class="section-divider"></div>
            <h3 class="section-title">学历与经验要求</h3>
            <table class="info-table">
              <tr>
                <td class="label">最低学历</td>
                <td>{{ currentProfile.education?.minimum || '不限' }}</td>
                <td class="label">优先学历</td>
                <td>{{ currentProfile.education?.preferred || '不限' }}</td>
              </tr>
              <tr>
                <td class="label">经验年限</td>
                <td>{{ currentProfile.experience?.years || '不限' }}</td>
                <td class="label">经验类型</td>
                <td>{{ currentProfile.experience?.type || '不限' }}</td>
              </tr>
            </table>
            <div class="majors" v-if="currentProfile.education?.major?.length">
              <span class="majors-label">对口专业：</span>
              <span class="skill-tag light" v-for="(m, i) in currentProfile.education.major" :key="i">{{ m }}</span>
            </div>

            <!-- 职业发展路径 -->
            <div class="section-divider" v-if="currentProfile.career_path"></div>
            <h3 class="section-title" v-if="currentProfile.career_path">职业发展路径</h3>
            <div class="path-chain" v-if="currentProfile.career_path">
              <div class="path-node" v-for="(step, idx) in careerPathSteps" :key="step.key">
                <div class="node-circle">{{ idx + 1 }}</div>
                <div class="node-info">
                  <span class="node-title">{{ step.title }}</span>
                  <span class="node-desc">{{ step.desc }}</span>
                </div>
                <div v-if="idx < careerPathSteps.length - 1" class="node-arrow">↓</div>
              </div>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-else v-loading="loadingProfile" class="loading-state">
            <p>正在聚合分析招聘数据...</p>
            <p class="loading-hint">大模型正在阅读相关职位描述并提取关键能力</p>
          </div>
        </div>
      </div>
    </teleport>

    <!-- 路径图谱弹窗 -->
    <CareerPathModal
      :visible="pathModalVisible"
      :jobName="pathModalJobName"
      @update:visible="pathModalVisible = $event"
      @close="pathModalVisible = false"
    />

    <p class="footer">© 2026 职业规划智能体</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick, watch } from 'vue';
import { useRoute } from 'vue-router';
import { getTopJobs, getAggregatedProfile, clearJobCache } from '@/api/job';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import CareerPathModal from '@/components/CareerPathModal.vue';

const route = useRoute();

const topJobs = ref<any[]>([]);
const loading = ref(false);

const skeletonCount = 8;

/* Skill tag previews for each card — seeded by index */
const jobSkillPool = [
  ['Java', 'Spring', 'MySQL'],
  ['Python', 'SQL', 'Pandas'],
  ['React', 'TypeScript', 'Node.js'],
  ['产品设计', 'Axure', '数据分析'],
  ['新媒体', 'SEO', '文案策划'],
  ['市场营销', '品牌推广', '用户增长'],
  ['C++', '算法', '数据结构'],
  ['机器学习', 'TensorFlow', 'Python'],
  ['电气设计', 'AutoCAD', 'PLC'],
  ['土木工程', 'BIM', '结构设计'],
  ['护理学', '临床护理', '急救'],
  ['建筑设计', 'SketchUp', '效果图'],
  ['财务管理', 'SAP', 'Excel'],
  ['法学', '合同审查', '公司法'],
  ['UI设计', 'Figma', '交互设计'],
];

const getJobSkills = (index: number): string[] => {
  return jobSkillPool[index % jobSkillPool.length];
};

/* Stagger delay computed for card animation */
const staggerStyle = (index: number): Record<string, string> => {
  return { animationDelay: `${index * 0.06}s` };
};

/* Dimension bar color helper */
const getDimensionBarColor = (value: number): string => {
  if (value >= 4) return 'rgba(87,64,36,0.85)';
  if (value >= 3) return 'rgba(87,64,36,0.55)';
  return 'rgba(87,64,36,0.3)';
};

const profileVisible = ref(false);
const currentJobName = ref('');
const currentProfile = ref<any>(null);
const loadingProfile = ref(false);
const radarChartRef = ref<HTMLElement | null>(null);

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

const floatingCareers = (() => {
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

const radarData = computed(() => {
  if (!currentProfile.value?.soft_skills) return [];
  const ss = currentProfile.value.soft_skills;
  return [
    { name: '专业技能', value: currentProfile.value.professional_skills?.level_score || 3 },
    { name: '证书要求', value: currentProfile.value.certificates?.level_score || 1 },
    { name: '创新能力', value: ss.innovation_ability?.level || 3 },
    { name: '学习能力', value: ss.learning_ability?.level || 3 },
    { name: '抗压能力', value: ss.pressure_resistance?.level || 3 },
    { name: '沟通能力', value: ss.communication_ability?.level || 3 },
    { name: '实习经验', value: currentProfile.value.experience?.level_score || 2 }
  ];
});

const softSkillsDisplay = computed(() => {
  if (!currentProfile.value?.soft_skills) return [];
  const ss = currentProfile.value.soft_skills;
  return [
    { name: '创新能力', level: ss.innovation_ability?.level || 3, description: ss.innovation_ability?.description || '' },
    { name: '学习能力', level: ss.learning_ability?.level || 3, description: ss.learning_ability?.description || '' },
    { name: '抗压能力', level: ss.pressure_resistance?.level || 3, description: ss.pressure_resistance?.description || '' },
    { name: '沟通能力', level: ss.communication_ability?.level || 3, description: ss.communication_ability?.description || '' }
  ];
});

const careerPathSteps = computed(() => {
  if (!currentProfile.value?.career_path) return [];
  const cp = currentProfile.value.career_path;
  return [
    { key: 'entry', title: cp.entry_level || '入门岗位', desc: '1-3年经验' },
    { key: 'mid', title: cp.mid_level || '中级岗位', desc: '3-5年经验' },
    { key: 'senior', title: cp.senior_level || '高级岗位', desc: '5-8年经验' },
    { key: 'expert', title: cp.expert_level || '专家/管理', desc: '8年以上' }
  ];
});

const initRadarChart = () => {
  if (!radarChartRef.value || !radarData.value.length) return;
  const chart = echarts.init(radarChartRef.value);
  const option = {
    animation: true,
    animationDuration: 1200,
    animationEasing: 'cubicOut' as const,
    tooltip: { trigger: 'item' },
    radar: {
      indicator: radarData.value.map(item => ({ name: item.name, max: 5 })),
      shape: 'polygon',
      splitNumber: 5,
      axisName: { color: '#3b2412', fontFamily: "'SimSun','Songti SC','STSong','Noto Serif SC',serif", fontSize: 12 },
      splitLine: { lineStyle: { color: 'rgba(87,64,36,0.15)' } },
      splitArea: { areaStyle: { color: ['rgba(87,64,36,0.02)', 'rgba(87,64,36,0.04)', 'rgba(87,64,36,0.02)', 'rgba(87,64,36,0.04)', 'rgba(87,64,36,0.02)'] } },
      axisLine: { lineStyle: { color: 'rgba(87,64,36,0.2)' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: radarData.value.map(item => item.value),
        name: '能力要求',
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#3b2412' },
        areaStyle: { color: 'rgba(87,64,36,0.1)' },
        itemStyle: { color: '#3b2412' },
        label: { show: true, formatter: '{c}', color: '#3b2412', fontWeight: 'bold' }
      }]
    }]
  };
  chart.setOption(option);
};

const loadTopJobs = async () => {
  loading.value = true;
  try {
    const res: any = await getTopJobs();
    topJobs.value = res;
  } catch (error) {
    ElMessage.error('获取岗位列表失败');
  } finally {
    loading.value = false;
  }
};

const clearCache = async () => {
  try {
    const res: any = await clearJobCache();
    ElMessage.success(res || '缓存清除成功');
  } catch (error) {
    ElMessage.error('清除缓存失败');
  }
};

const analyzeJob = async (jobName: string) => {
  currentJobName.value = jobName;
  currentProfile.value = null;
  profileVisible.value = true;
  loadingProfile.value = true;
  try {
    const res: any = await getAggregatedProfile(jobName);
    let jsonStr = res;
    if (typeof res === 'string') {
        if (res.includes('```json')) {
            jsonStr = res.split('```json')[1].split('```')[0];
        } else if (res.includes('```')) {
            jsonStr = res.split('```')[1].split('```')[0];
        }
        try {
            currentProfile.value = JSON.parse(jsonStr);
        } catch (e) {
            currentProfile.value = { professional_skills: ["解析失败"] };
        }
    } else {
        currentProfile.value = res;
    }
    await nextTick();
    initRadarChart();
  } catch (error) {
    ElMessage.error('画像生成失败');
  } finally {
    loadingProfile.value = false;
  }
};

/* 路径图谱弹窗状态 */
const pathModalVisible = ref(false);
const pathModalJobName = ref('');

const openJobDetail = (jobName: string) => {
  if (!jobName) {
    ElMessage.warning('岗位名称为空，无法查看路径图谱');
    return;
  }
  pathModalJobName.value = jobName;
  pathModalVisible.value = true;
};

watch(profileVisible, async (val) => {
  if (val) {
    await nextTick();
    initRadarChart();
  }
});

onMounted(() => {
  loadTopJobs();
  const fromJob = String(route.query.job || '').trim();
  if (fromJob) {
    analyzeJob(fromJob);
  }
});
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&display=swap');

.job-analysis {
  min-height: 100vh;
  background: rgba(250, 248, 244, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow-x: hidden;
  padding: 32px 24px;
  gap: 28px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #3b2412;
}

/* ===== A4 纸张容器 ===== */
.doc-paper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
  background: var(--paper-surface);
  background-color: var(--paper-bg-solid);
  padding: 48px 56px;
  box-shadow: var(--paper-shadow);
  border-radius: 1px;
  border: 1px solid var(--paper-border);
  overflow: hidden;
}
.doc-paper::before {
  content: '';
  position: absolute;
  top: 0;
  left: 56px;
  right: 56px;
  height: 1px;
  background: rgba(87, 64, 36, 0.06);
}
.doc-paper::after {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--paper-overlay);
  pointer-events: none;
}

/* ===== Animations ===== */
@keyframes shimmer {
  0% { background-position: -200px 0; }
  100% { background-position: calc(200px + 100%) 0; }
}
@keyframes paper-rise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== 浮动职业名 ===== */
.careers-layer { position: absolute; inset: 0; overflow: hidden; z-index: 0; }
.career-drift { position: absolute; white-space: nowrap; will-change: transform; }
.career-drift:hover { animation-play-state: paused; }
.career-word {
  display: inline-block;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #3b2412;
  padding: 2px 6px;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1), opacity 0.3s ease, background 0.3s ease;
}
.career-drift:hover .career-word {
  transform: scale(2);
  opacity: 0.85 !important;
  background: rgba(253, 252, 251, 0.95);
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.10);
}

/* ===== 顶部横条公告纸 ===== */
.banner-paper {
  position: relative;
  width: 100%;
  max-width: 100%;
  background: transparent;
  border-radius: 0;
  box-shadow: none;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.banner-inner { width: 100%; }
.banner-title { font-size: 30px; font-weight: 700; letter-spacing: 4px; margin: 0; padding-bottom: 16px; border-bottom: 1px solid rgba(87,64,36,0.12); }
.banner-sub { font-size: 15px; color: rgba(87, 64, 36, 0.55); letter-spacing: 1px; margin: 8px 0 0; }
.banner-actions { margin-top: 12px; display: flex; align-items: center; gap: 4px; }
.text-btn {
  background: none; border: none; cursor: pointer;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 14px; color: #3b2412; padding: 2px 0;
  border-bottom: 1px solid #3b2412;
  transition: opacity 0.15s;
}
.text-btn:hover { opacity: 0.5; }
.sep { color: rgba(87, 64, 36, 0.3); font-size: 14px; }

/* ===== 名片网格 ===== */
.cards-area {
  position: relative;
  width: 100%;
  max-width: 100%;
  margin-top: 40px;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

/* ===== Skeleton Loader ===== */
.skeleton {
  background: linear-gradient(90deg, #f5f0e8 25%, #ede8df 50%, #f5f0e8 75%);
  background-size: 200px 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 2px;
}
.skeleton-card {
  background: #fdfcfb;
  border-radius: 1px;
  box-shadow: 0 1px 1px rgba(0,0,0,0.03), 0 4px 8px rgba(0,0,0,0.03), 0 12px 24px rgba(0,0,0,0.04);
  padding: 20px;
}
.skeleton-rank { width: 28px; height: 28px; margin-bottom: 14px; }
.skeleton-name { width: 70%; height: 18px; margin-bottom: 10px; }
.skeleton-count { width: 50%; height: 14px; margin-bottom: 14px; }
.skeleton-row { display: flex; gap: 6px; }
.skeleton-tag { width: 52px; height: 22px; }

/* ===== Job Card ===== */
.name-card {
  background: #fdfcfb;
  border-radius: 1px;
  box-shadow: 0 1px 1px rgba(0,0,0,0.03), 0 4px 8px rgba(0,0,0,0.03), 0 12px 24px rgba(0,0,0,0.04);
  padding: 20px;
  padding-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  position: relative;
  overflow: hidden;
  animation: paper-rise 0.4s ease backwards;
}
.name-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 2px 2px rgba(0,0,0,0.03), 0 8px 16px rgba(0,0,0,0.04), 0 20px 40px rgba(0,0,0,0.05);
}

/* Paper fold corner */
.name-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 22px 22px 0;
  border-color: transparent rgba(250, 248, 244, 0.6) transparent transparent;
  box-shadow: -2px 2px 3px rgba(0,0,0,0.04);
  pointer-events: none;
  z-index: 2;
}

.card-rank {
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px;
  background: rgba(87, 64, 36, 0.08);
  border-radius: 3px;
  margin-bottom: 12px;
}
.name-card:nth-child(n+4) .card-rank { background: rgba(87, 64, 36, 0.45); }
.name-card:nth-child(n+7) .card-rank { background: rgba(87, 64, 36, 0.2); color: #3b2412; }
.card-name { font-size: 17px; font-weight: 700; letter-spacing: 1px; margin-bottom: 8px; }
.card-count { font-size: 13px; color: rgba(87, 64, 36, 0.55); margin-bottom: 10px; }

/* Card skill preview tags */
.card-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-bottom: 8px;
}
.card-skill-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 11px;
  color: #3b2412;
  border: 1px solid rgba(87,64,36,0.15);
  border-radius: 2px;
  background: rgba(87,64,36,0.04);
}

/* Card links (static, below content) */
.card-links { display: flex; gap: 12px; }
.card-link {
  font-size: 13px; color: #3b2412; cursor: pointer;
  border-bottom: 1px solid #3b2412; padding-bottom: 1px;
  transition: opacity 0.15s;
}
.card-link:hover { opacity: 0.5; }

/* Hover bottom action bar */
.card-hover-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(253,252,251,0.97);
  border-top: 1px solid rgba(87,64,36,0.08);
  padding: 10px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  transform: translateY(100%);
  opacity: 0;
  transition: transform 0.3s ease, opacity 0.3s ease;
  z-index: 3;
}
.name-card:hover .card-hover-bar {
  transform: translateY(0);
  opacity: 1;
}
.bar-link {
  font-size: 13px;
  color: #3b2412;
  cursor: pointer;
  border-bottom: 1px solid #3b2412;
  padding-bottom: 1px;
  transition: opacity 0.15s;
}
.bar-link:hover { opacity: 0.5; }
.bar-sep { color: rgba(87,64,36,0.2); font-size: 12px; }

.card-deco {
  position: absolute;
  right: -4px; bottom: -4px;
  width: 36px; height: 36px;
  color: #3b2412;
  opacity: 0.06;
  pointer-events: none;
}

/* ===== 空态纸张 ===== */
.empty-paper {
  background: #fdfcfb;
  border-radius: 1px;
  box-shadow: 0 1px 1px rgba(0,0,0,0.03), 0 4px 8px rgba(0,0,0,0.03), 0 12px 24px rgba(0,0,0,0.04);
  padding: 40px 36px;
  text-align: center;
}
.empty-title { font-size: 22px; font-weight: 700; letter-spacing: 2px; margin: 0 0 12px; color: #3b2412; }
.empty-desc { font-size: 15px; color: rgba(87, 64, 36, 0.55); margin: 0 0 28px; }

/* ===== 操作按钮 ===== */
.action-line {
  display: flex; align-items: center; gap: 10px;
  background: none; border: none; cursor: pointer;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  transition: opacity 0.15s;
}
.action-line:hover { opacity: 0.5; }
.action-dash { display: inline-block; width: 70px; height: 1px; background: #3b2412; }
.action-text { font-size: 17px; font-weight: 600; color: #3b2412; letter-spacing: 4px; }

/* ===== 分隔线 ===== */
.divider { width: 100%; height: 1px; background: rgba(87,64,36,0.15); margin: 20px 0; }

/* ===== 覆盖层 + 详情纸张 ===== */
.overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,0.3);
  display: flex; align-items: center; justify-content: center;
  padding: 24px;
}
.profile-paper {
  background: var(--paper-surface);
  background-color: var(--paper-bg-solid);
  width: 100%;
  max-width: 900px;
  max-height: 90vh;
  overflow-y: auto;
  border-radius: 1px;
  box-shadow: var(--paper-shadow-hover);
  position: relative;
  padding: 36px 40px;
  border: 1px solid var(--paper-border);
}
.close-btn {
  position: absolute; top: 16px; right: 20px;
  background: none; border: none; cursor: pointer;
  font-size: 20px; color: #3b2412; padding: 4px 8px;
  transition: opacity 0.15s;
}
.close-btn:hover { opacity: 0.5; }
.profile-title { font-size: 28px; font-weight: 700; letter-spacing: 4px; margin: 0; color: #3b2412; }

/* ===== 信息表格 ===== */
.info-table {
  width: 100%; border-collapse: collapse; margin-bottom: 20px;
  font-size: 15px;
}
.info-table td { padding: 10px 14px; border-bottom: 1px solid rgba(87,64,36,0.1); }
.info-table .label { color: rgba(87, 64, 36, 0.55); font-size: 14px; white-space: nowrap; }

/* ===== Section dividers & accent bars ===== */
.section-divider {
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, rgba(87,64,36,0.2), rgba(87,64,36,0.06), rgba(87,64,36,0.2));
  margin: 24px 0 4px;
}
.section-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 20px 0 16px;
  padding-left: 12px;
  border-left: 3px solid rgba(87,64,36,0.4);
}

/* ===== 雷达图 ===== */
.radar-section { display: flex; gap: 30px; align-items: flex-start; flex-wrap: wrap; }
.radar-chart { width: 340px; height: 280px; flex-shrink: 0; min-width: 280px; }
.radar-legend { flex: 1; min-width: 200px; }
.legend-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 0; border-bottom: 1px solid rgba(87,64,36,0.08);
}
.legend-dot { width: 8px; height: 8px; border-radius: 50%; background: #3b2412; flex-shrink: 0; }
.legend-label { font-size: 14px; min-width: 60px; }
.legend-bar-wrap { flex: 1; }
.legend-bar-bg {
  height: 5px;
  background: rgba(87,64,36,0.08);
  border-radius: 3px;
  overflow: hidden;
}
.legend-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.8s ease;
}
.legend-value { font-weight: 700; font-size: 14px; min-width: 32px; text-align: right; }

/* ===== 两列 ===== */
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 32px; }

/* ===== 技能标签 ===== */
.tag-group { margin-bottom: 12px; }
.tag-label { font-size: 13px; color: rgba(87, 64, 36, 0.55); margin: 0 0 6px; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; }
.skill-tag {
  display: inline-block; padding: 4px 10px; border-radius: 2px;
  font-size: 13px; border: 1px solid rgba(87,64,36,0.2); color: #3b2412;
  background: rgba(87,64,36,0.06);
}
.skill-tag.light { border-color: rgba(87,64,36,0.15); color: rgba(87, 64, 36, 0.65); }

/* Strengths: left border accent */
.strengths-block {
  padding-left: 10px;
  border-left: 3px solid rgba(87,64,36,0.3);
}
.skill-tag.strength {
  border: 1px solid rgba(87,64,36,0.3);
  color: #3b2412;
  background: rgba(87,64,36,0.04);
}

/* Weaknesses: dashed border */
.weaknesses-block {
  padding-left: 10px;
  border-left: 2px dashed rgba(87,64,36,0.2);
}
.skill-tag.weakness {
  border: 1px dashed rgba(87,64,36,0.25);
  color: rgba(87, 64, 36, 0.65);
  background: transparent;
}

.analysis-text { font-size: 13px; color: rgba(87, 64, 36, 0.65); line-height: 1.8; margin: 10px 0 0; padding: 10px; background: rgba(87,64,36,0.03); border-radius: 2px; }
.empty-hint { font-size: 13px; color: rgba(87, 64, 36, 0.4); }

/* ===== 软技能 ===== */
.soft-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.soft-item { padding: 12px; border: 1px solid rgba(87,64,36,0.08); border-radius: 2px; }
.soft-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.soft-name { font-weight: 700; font-size: 15px; color: #3b2412; }
.soft-level { font-size: 13px; color: rgba(87, 64, 36, 0.5); }
.progress-bar { height: 6px; background: rgba(87,64,36,0.08); border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: #3b2412; border-radius: 3px; transition: width 0.6s ease; }
.soft-desc { font-size: 12px; color: rgba(87, 64, 36, 0.55); margin: 6px 0 0; line-height: 1.6; }

/* ===== 对口专业 ===== */
.majors { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.majors-label { font-size: 14px; color: rgba(87, 64, 36, 0.55); }

/* ===== 职业路径 ===== */
.path-chain { display: flex; align-items: flex-start; gap: 8px; flex-wrap: wrap; }
.path-node { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.node-circle {
  width: 36px; height: 36px; border: 2px solid #3b2412; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px; color: #3b2412;
}
.node-info { text-align: center; }
.node-title { display: block; font-size: 14px; font-weight: 700; color: #3b2412; }
.node-desc { display: block; font-size: 12px; color: rgba(87, 64, 36, 0.55); }
.node-arrow { font-size: 18px; color: rgba(87, 64, 36, 0.25); margin-top: 8px; }

/* ===== 加载中 ===== */
.loading-state { min-height: 300px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.loading-state p { font-size: 16px; margin: 0; color: #3b2412; }
.loading-hint { font-size: 13px; color: rgba(87, 64, 36, 0.5); margin-top: 8px !important; }

/* ===== Footer ===== */
.footer { position: relative; z-index: 1; font-size: 12px; color: rgba(87, 64, 36, 0.35); letter-spacing: 0.5px; }

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .two-col { grid-template-columns: 1fr; }
  .radar-section { flex-wrap: wrap; }
  .radar-chart { width: 100%; max-width: 340px; }
  .radar-legend { min-width: 100%; }
}
@media (max-width: 768px) {
  .card-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .job-analysis { padding: 16px 12px; }
  .card-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .banner-paper { padding: 20px 24px; }
  .banner-title { font-size: 24px; }
  .profile-paper { padding: 28px 20px; }
  .soft-grid { grid-template-columns: 1fr; }
  .radar-chart { width: 100%; height: 240px; }
  .radar-section { flex-direction: column; }
}
@media (max-width: 500px) {
  .card-grid { grid-template-columns: 1fr; }
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

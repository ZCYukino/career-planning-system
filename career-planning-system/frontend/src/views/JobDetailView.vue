<template>
  <div class="detail-page">
    <!-- 飞过的职业名称 -->
    <div class="careers-layer">
      <div v-for="(item, i) in careers" :key="i" class="career-drift" :style="item.driftStyle">
        <span class="career-word" :style="item.wordStyle">{{ item.name }}</span>
      </div>
    </div>

    <!-- 返回 -->
    <div class="page-nav">
      <button class="nav-back nav-back-tab" @click="goBack">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
        返回
      </button>
      <span class="nav-title">岗位详情与职业路径图谱</span>
    </div>

    <!-- 错误提示 -->
    <div class="paper-alert" v-if="errorMsg">{{ errorMsg }}</div>

    <!-- 加载中 — 纸张容器包裹，与聚合画像加载风格一致 -->
    <div class="loading-paper" v-if="loading">
      <div v-loading="true" element-loading-text="" class="loading-inner">
        <p class="loading-main-text">正在加载路径图谱...</p>
        <p class="loading-hint">大模型正在聚合岗位画像与职业路径数据</p>
      </div>
    </div>

    <template v-else-if="profile">
      <!-- 横卡纸：岗位基本信息 -->
      <div class="top-card animated-section" style="--stagger:0">
        <svg class="icon-deco" style="bottom:18px;right:16px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>
        <div class="top-card-content">
          <div class="top-header">
            <h2 class="top-title">{{ jobName || '未选择岗位' }}</h2>
            <span class="top-level">岗位画像</span>
          </div>

          <!-- 岗位信息标签 -->
          <div class="job-info-tags">
            <div class="job-info-tag">
              <span class="job-info-tag-label">薪资范围</span>
              <span class="job-info-tag-value">{{ profile.salary_reference || '8K-15K' }}</span>
            </div>
            <div class="job-info-tag">
              <span class="job-info-tag-label">学历要求</span>
              <span class="job-info-tag-value">{{ profile.education?.minimum || '本科及以上' }}</span>
            </div>
            <div class="job-info-tag">
              <span class="job-info-tag-label">行业领域</span>
              <span class="job-info-tag-value">互联网/IT</span>
            </div>
            <div class="job-info-tag">
              <span class="job-info-tag-label">经验要求</span>
              <span class="job-info-tag-value">{{ profile.experience?.years || '1-3年' }}</span>
            </div>
          </div>

          <!-- 基本信息 -->
          <div class="info-row">
            <div class="info-cell">
              <span class="info-label">薪资参考</span>
              <span class="info-value salary">{{ profile.salary_reference || '面议' }}</span>
            </div>
            <div class="info-cell">
              <span class="info-label">行业前景</span>
              <span class="info-value">{{ profile.industry_outlook || '暂无' }}</span>
            </div>
            <div class="info-cell">
              <span class="info-label">技能等级</span>
              <span class="info-value">{{ profile.professional_skills?.level || '中级' }}</span>
            </div>
          </div>

          <div class="divider"></div>

          <!-- 雷达图 -->
          <div class="radar-section">
            <div class="sub-title-row">
              <div class="sub-line"></div>
              <span class="sub-text">七维度能力画像</span>
              <div class="sub-line"></div>
            </div>
            <div class="radar-wrap">
              <div ref="radarChartRef" class="radar-chart"></div>
              <div class="radar-legend">
                <div class="legend-item" v-for="(item, idx) in radarData" :key="idx">
                  <span class="legend-dot"></span>
                  <span class="legend-label">{{ item.name }}</span>
                  <span class="legend-value">{{ item.value }}/5</span>
                </div>
              </div>
            </div>
          </div>

          <div class="divider"></div>

          <!-- 专业技能 -->
          <div class="sub-title-row">
            <div class="sub-line"></div>
            <span class="sub-text">专业技能要求</span>
            <div class="sub-line"></div>
          </div>
          <div class="skills-section">
            <div class="skill-group">
              <div class="skill-group-title">必备技能</div>
              <div class="skill-tags">
                <span class="skill-tag" v-for="(skill, idx) in (profile.professional_skills?.required || [])" :key="idx">{{ skill }}</span>
                <span v-if="!profile.professional_skills?.required?.length" class="empty-text">暂无必备技能要求</span>
              </div>
            </div>
            <div class="skill-group" v-if="profile.professional_skills?.preferred?.length">
              <div class="skill-group-title">加分技能</div>
              <div class="skill-tags">
                <span class="skill-tag secondary" v-for="(skill, idx) in profile.professional_skills.preferred" :key="idx">{{ skill }}</span>
              </div>
            </div>
            <div class="skill-analysis" v-if="profile.professional_skills?.analysis">{{ profile.professional_skills.analysis }}</div>
          </div>

          <div class="divider"></div>

          <!-- 证书要求 -->
          <div class="sub-title-row">
            <div class="sub-line"></div>
            <span class="sub-text">证书要求</span>
            <div class="sub-line"></div>
          </div>
          <div class="cert-section">
            <div class="cert-row" v-if="profile.certificates?.required?.length">
              <span class="cert-label">必须：</span>
              <span class="skill-tag" v-for="(cert, idx) in profile.certificates.required" :key="idx">{{ cert }}</span>
            </div>
            <div class="cert-row" v-if="profile.certificates?.preferred?.length">
              <span class="cert-label">加分：</span>
              <span class="skill-tag secondary" v-for="(cert, idx) in profile.certificates.preferred" :key="idx">{{ cert }}</span>
            </div>
            <div class="cert-row" v-if="!profile.certificates?.required?.length && !profile.certificates?.preferred?.length">
              <span class="empty-text">无证书要求</span>
            </div>
          </div>

          <div class="divider"></div>

          <!-- 软技能 -->
          <div class="sub-title-row">
            <div class="sub-line"></div>
            <span class="sub-text">通用素质要求</span>
            <div class="sub-line"></div>
          </div>
          <div class="soft-grid">
            <div class="soft-item" v-for="(skill, key) in softSkillsDisplay" :key="key">
              <div class="soft-header">
                <span class="soft-name">{{ skill.name }}</span>
                <span class="soft-level">Lv.{{ skill.level }}</span>
              </div>
              <div class="soft-desc">{{ skill.description || '暂无描述' }}</div>
              <div class="soft-bar">
                <div class="soft-bar-fill" :style="{ width: (skill.level / 5) * 100 + '%' }"></div>
              </div>
            </div>
          </div>

          <div class="divider"></div>

          <!-- 学历经验 -->
          <div class="sub-title-row">
            <div class="sub-line"></div>
            <span class="sub-text">学历与经验要求</span>
            <div class="sub-line"></div>
          </div>
          <div class="edu-section">
            <div class="edu-row">
              <div class="edu-cell"><span class="edu-label">最低学历</span><span class="edu-val">{{ profile.education?.minimum || '不限' }}</span></div>
              <div class="edu-cell"><span class="edu-label">优先学历</span><span class="edu-val">{{ profile.education?.preferred || '不限' }}</span></div>
              <div class="edu-cell"><span class="edu-label">经验要求</span><span class="edu-val">{{ profile.experience?.years || '不限' }}</span></div>
            </div>
            <div class="majors-row" v-if="profile.education?.major?.length">
              <span class="cert-label">对口专业：</span>
              <span class="skill-tag secondary" v-for="(major, idx) in profile.education.major" :key="idx">{{ major }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 双栏：晋升 + 换岗 -->
      <div class="papers">
        <!-- 左页：晋升路径 -->
        <div class="paper path-paper animated-section" style="--stagger:0">
          <svg class="icon-deco" style="bottom:18px;right:16px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 22V8"/><path d="M5 12H2a10 10 0 0 0 20 0h-3"/><path d="M8 5l4-3 4 3"/></svg>
          <div class="paper-content">
            <h3 class="section-title section-title-accent">晋升路径</h3>
            <div class="section-gradient-divider"></div>

            <div v-if="promotionNodes.length" class="promo-timeline">
              <div v-for="(node, idx) in promotionNodes" :key="idx" class="timeline-node" :class="{ 'timeline-current': idx === 0 }">
                <div class="timeline-connector" v-if="idx > 0"></div>
                <div class="timeline-circle">
                  <span class="timeline-number">{{ idx + 1 }}</span>
                </div>
                <div class="timeline-label">
                  <div class="timeline-name">{{ node }}</div>
                  <div class="timeline-stage">{{ getStageLabel(idx) }}</div>
                </div>
              </div>
            </div>

            <div v-if="promotionPaths.length" class="promo-details">
              <div class="sub-title-row">
                <div class="sub-line"></div>
                <span class="sub-text">晋升路径详解</span>
                <div class="sub-line"></div>
              </div>
              <div class="promo-detail-item" v-for="(item, idx) in promotionPaths" :key="idx">
                <div class="pdi-header">
                  <span class="pdi-exp">{{ item.experienceYears || item.experience_years || '1-3年' }}</span>
                  <span class="pdi-arrow">{{ item.current_job_name || item.currentJobName || jobName }} → {{ item.next_job_name || item.nextJobName }}</span>
                </div>
                <div class="pdi-desc">{{ item.promotion_path_desc || item.promotionPathDesc || '晋升说明待补充' }}</div>
                <div class="pdi-skills" v-if="item.required_skills || item.requiredSkills">
                  <span class="pdi-skills-label">核心技能：</span>
                  <span class="skill-tag small" v-for="(skill, sIdx) in splitSkills(item.required_skills || item.requiredSkills)" :key="sIdx">{{ skill }}</span>
                </div>
              </div>
            </div>

            <div v-else-if="profile?.career_path" class="promo-details">
              <div class="sub-title-row">
                <div class="sub-line"></div>
                <span class="sub-text">职业发展路径</span>
                <div class="sub-line"></div>
              </div>
              <div class="promo-timeline">
                <div v-for="(level, idx) in careerPathSteps" :key="level.key" class="timeline-node" :class="{ 'timeline-current': idx === 0 }">
                  <div class="timeline-connector" v-if="idx > 0"></div>
                  <div class="timeline-circle">
                    <span class="timeline-number">{{ idx + 1 }}</span>
                  </div>
                  <div class="timeline-label">
                    <div class="timeline-name">{{ level.title }}</div>
                    <div class="timeline-stage">{{ level.desc }}</div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="!promotionNodes.length && !profile?.career_path" class="empty-paper">
              <span>暂无晋升路径数据</span>
            </div>
          </div>
        </div>

        <!-- 右页：换岗路径 -->
        <div class="paper path-paper animated-section" style="--stagger:1">
          <svg class="icon-deco" style="bottom:18px;right:16px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 3h18v18H3z"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>
          <div class="paper-content">
            <h3 class="section-title section-title-accent">转岗路径</h3>
            <div class="section-gradient-divider"></div>

            <div v-if="transferPaths.length" class="transfer-section">
              <!-- 中心节点 -->
              <div class="transfer-center">
                <div class="center-name">{{ jobName }}</div>
                <div class="center-label">当前岗位</div>
              </div>

              <!-- 纸片卡片网格 -->
              <div class="transfer-card-grid">
                <div class="transfer-paper-card" v-for="(path, idx) in transferPaths.slice(0, 6)" :key="idx">
                  <div class="fold-corner"></div>
                  <div class="transfer-paper-arrow">↗</div>
                  <div class="transfer-paper-target">{{ path.targetJobName || path.target_job_name }}</div>
                  <div class="transfer-paper-diff">
                    难度
                    <span class="diff-dots-inline">
                      <span v-for="d in 5" :key="d" class="diff-dot-sm" :class="{ filled: d <= (path.difficulty || path.difficulty_score || 3) }"></span>
                    </span>
                  </div>
                </div>
              </div>

              <div class="divider" style="margin:20px 0"></div>

              <!-- 详细列表 -->
              <div class="sub-title-row">
                <div class="sub-line"></div>
                <span class="sub-text">横向发展详情</span>
                <div class="sub-line"></div>
              </div>
              <el-collapse>
                <el-collapse-item
                  v-for="(path, idx) in transferPaths"
                  :key="idx"
                  :title="(path.targetJobName || path.target_job_name) + ' - ' + (path.difficulty || path.difficulty_score || 3) + '/5难度'"
                  :name="idx"
                >
                  <div class="td-row"><span class="td-label">目标岗位</span><span>{{ path.targetJobName || path.target_job_name }}</span></div>
                  <div class="td-row"><span class="td-label">转型周期</span><span>{{ path.transferDuration || path.transfer_duration || path.duration || '待确定' }}</span></div>
                  <div class="td-row"><span class="td-label">转型难度</span>
                    <span class="diff-dots">
                      <span v-for="d in 5" :key="d" class="diff-dot" :class="{ filled: d <= (path.difficulty || path.difficulty_score || 3) }"></span>
                    </span>
                  </div>
                  <div class="td-row"><span class="td-label">所需技能</span>
                    <div class="td-skills">
                      <span class="skill-tag small" v-for="(skill, sIdx) in splitSkills(path.requiredSkills || path.required_skills || '')" :key="sIdx">{{ skill }}</span>
                      <span v-if="!splitSkills(path.requiredSkills || path.required_skills || '').length" class="empty-text">待确定</span>
                    </div>
                  </div>
                  <div class="td-row"><span class="td-label">转型优势</span><span>{{ path.advantage || path.transfer_advantage || '暂无说明' }}</span></div>
                </el-collapse-item>
              </el-collapse>
            </div>

            <div v-else class="empty-paper">
              <span>暂无转岗路径数据</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 空态 -->
    <div v-if="!loading && !profile && !errorMsg" class="empty-state">
      <div class="empty-icon-paper">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" width="48" height="48"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>
      </div>
      <div class="empty-title">暂无岗位画像数据</div>
      <div class="empty-desc">请从岗位列表进入查看详情</div>
      <button class="action-line" @click="goBack">
        <span class="action-dash"></span>
        <span class="action-text">返回岗位列表</span>
        <span class="action-dash"></span>
      </button>
    </div>

    <p class="footer">© 2026 职业规划智能体</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getAggregatedProfile, getCareerPaths } from '@/api/job';
import * as echarts from 'echarts';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const errorMsg = ref('');
const profile = ref<any>(null);
const promotionPaths = ref<any[]>([]);
const transferPaths = ref<any[]>([]);
const radarChartRef = ref<HTMLElement | null>(null);

// --- 浮动职业名称 ---
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
        driftStyle: { top: `${top}%`, animation: `${drift} ${duration}s ${delay}s linear infinite` },
        wordStyle: { fontSize: `${size}px`, opacity }
      });
    }
  }
  return items;
})();

const radarData = computed(() => {
  if (!profile.value?.soft_skills) return [];
  const ss = profile.value.soft_skills;
  return [
    { name: '专业技能', value: profile.value.professional_skills?.level_score || 3 },
    { name: '证书要求', value: profile.value.certificates?.level_score || 1 },
    { name: '创新能力', value: ss.innovation_ability?.level || 3 },
    { name: '学习能力', value: ss.learning_ability?.level || 3 },
    { name: '抗压能力', value: ss.pressure_resistance?.level || 3 },
    { name: '沟通能力', value: ss.communication_ability?.level || 3 },
    { name: '实习经验', value: profile.value.experience?.level_score || 2 }
  ];
});

const softSkillsDisplay = computed(() => {
  if (!profile.value?.soft_skills) return [];
  const ss = profile.value.soft_skills;
  return [
    { name: '创新能力', level: ss.innovation_ability?.level || 3, description: ss.innovation_ability?.description || '' },
    { name: '学习能力', level: ss.learning_ability?.level || 3, description: ss.learning_ability?.description || '' },
    { name: '抗压能力', level: ss.pressure_resistance?.level || 3, description: ss.pressure_resistance?.description || '' },
    { name: '沟通能力', level: ss.communication_ability?.level || 3, description: ss.communication_ability?.description || '' }
  ];
});

const jobName = computed(() => String(route.query.job || '').trim());

const promotionNodes = computed(() => {
  const nodes: string[] = [];
  for (const item of promotionPaths.value) {
    const current = item.current_job_name || item.currentJobName;
    const next = item.next_job_name || item.nextJobName;
    if (current && !nodes.includes(current)) nodes.push(current);
    if (next && !nodes.includes(next)) nodes.push(next);
  }
  if (!nodes.length && profile.value?.career_path) {
    const cp = profile.value.career_path;
    [cp.entry_level, cp.mid_level, cp.senior_level, cp.expert_level].forEach((v: string) => {
      if (v && !nodes.includes(v)) nodes.push(v);
    });
  }
  return nodes;
});

const careerPathSteps = computed(() => {
  if (!profile.value?.career_path) return [];
  const cp = profile.value.career_path;
  return [
    { key: 'entry', title: cp.entry_level || '入门岗位', desc: '1-3年经验' },
    { key: 'mid', title: cp.mid_level || '中级岗位', desc: '3-5年经验' },
    { key: 'senior', title: cp.senior_level || '高级岗位', desc: '5-8年经验' },
    { key: 'expert', title: cp.expert_level || '专家/管理', desc: '8年以上' }
  ];
});

const getStageLabel = (idx: number) => {
  const labels = ['入门阶段', '成长阶段', '成熟阶段', '专家阶段', '管理阶段'];
  return labels[idx] || `阶段${idx + 1}`;
};

const splitSkills = (skills: string) => {
  if (!skills) return [];
  return skills.split(/[,，、]/).map(s => s.trim()).filter(s => s);
};

const initRadarChart = () => {
  if (!radarChartRef.value || !radarData.value.length) return;
  const chart = echarts.init(radarChartRef.value);
  const option = {
    tooltip: { trigger: 'item' },
    radar: {
      indicator: radarData.value.map(item => ({ name: item.name, max: 5 })),
      shape: 'polygon',
      splitNumber: 5,
      axisName: { color: '#1a1a1a', fontFamily: "'SimSun','Songti SC','STSong','Noto Serif SC',serif" },
      splitLine: { lineStyle: { color: '#ddd' } },
      splitArea: { areaStyle: { color: ['rgba(0,0,0,0.02)', 'rgba(0,0,0,0.04)', 'rgba(0,0,0,0.06)', 'rgba(0,0,0,0.08)', 'rgba(0,0,0,0.10)'] } },
      axisLine: { lineStyle: { color: '#ccc' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: radarData.value.map(item => item.value),
        name: '能力要求',
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#1a1a1a' },
        areaStyle: { color: 'rgba(26,26,26,0.15)' },
        itemStyle: { color: '#1a1a1a' },
        label: { show: true, formatter: '{c}', color: '#1a1a1a', fontWeight: 'bold' }
      }]
    }]
  };
  chart.setOption(option);
};

const goBack = () => { router.push('/jobs'); };

const loadData = async () => {
  if (!jobName.value) {
    errorMsg.value = '缺少岗位名称，请从岗位列表重新进入';
    return;
  }
  loading.value = true;
  errorMsg.value = '';
  try {
    // 先加载画像数据，确保页面有基本内容
    try {
      const profileRes: any = await getAggregatedProfile(jobName.value);
      if (typeof profileRes === 'string') {
        try { profile.value = JSON.parse(profileRes); } catch { profile.value = null; }
      } else {
        profile.value = profileRes;
      }
    } catch (profileErr: any) {
      console.warn('[JobDetailView] 加载画像数据失败:', profileErr?.message);
    }

    // 加载职业路径数据（晋升 + 转岗）
    try {
      const pathRes: any = await getCareerPaths(jobName.value);
      promotionPaths.value = pathRes?.promotionPaths || [];
      transferPaths.value = pathRes?.transferPaths || [];
    } catch (pathErr: any) {
      console.warn('[JobDetailView] 加载路径数据失败:', pathErr?.message);
      // 路径数据加载失败不阻塞页面渲染
      promotionPaths.value = [];
      transferPaths.value = [];
    }

    // 如果画像和路径都没有数据，显示错误
    if (!profile.value && promotionPaths.value.length === 0 && transferPaths.value.length === 0) {
      errorMsg.value = '暂无该岗位的详细数据，请稍后再试或返回岗位列表重新选择';
    }
  } catch (e: any) {
    errorMsg.value = '岗位详情加载失败，请稍后重试';
    ElMessage.error(errorMsg.value);
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await loadData();
  await nextTick();
  initRadarChart();
});

watch(() => profile.value, async () => {
  await nextTick();
  initRadarChart();
}, { deep: true });
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&display=swap');

/* ===== Paper-rise stagger animation ===== */
@keyframes paper-rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.animated-section {
  animation: paper-rise 0.6s ease-out both;
  animation-delay: calc(var(--stagger, 0) * 0.12s);
}

.detail-page {
  min-height: 100vh;
  background: #faf9f7;
  padding: 20px;
  position: relative;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

/* ===== 浮动职业名称 ===== */
.careers-layer {
  position: fixed;
  inset: 0;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}

.career-drift { position: absolute; white-space: nowrap; will-change: transform; }
.career-word {
  display: inline-block;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #1a1a1a;
  padding: 2px 6px;
}

/* ===== 导航 ===== */
.page-nav {
  position: relative;
  z-index: 1;
  max-width: 1400px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-back {
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
  font-size: 14px;
  color: #8a8a8a;
  transition: color 0.15s;
}

.nav-back:hover { color: #1a1a1a; }

/* Paper tab style for back button */
.nav-back-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  background: #fdfcfb;
  border: 1px solid rgba(26,26,26,0.15);
  border-bottom: none;
  padding: 6px 18px 8px;
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 1px;
  box-shadow: 0 -1px 3px rgba(0,0,0,0.03), 2px 0 6px rgba(0,0,0,0.02);
  border-radius: 1px 1px 0 0;
  transition: background 0.2s, box-shadow 0.2s;
}

.nav-back-tab:hover {
  background: #f5f4f2;
  box-shadow: 0 -2px 6px rgba(0,0,0,0.05), 3px 0 10px rgba(0,0,0,0.03);
  color: #1a1a1a;
}

.nav-back-tab svg {
  transition: transform 0.2s;
}

.nav-back-tab:hover svg {
  transform: translateX(-2px);
}

.nav-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 2px;
}

.paper-alert {
  position: relative;
  z-index: 1;
  max-width: 1400px;
  margin: 0 auto 20px;
  padding: 12px 20px;
  border-left: 3px solid #1a1a1a;
  background: #fdfcfb;
  font-size: 14px;
  color: #5a5a5a;
}

/* ===== 加载纸张容器 — 与聚合画像加载风格一致 ===== */
.loading-paper {
  position: relative;
  z-index: 1;
  max-width: 900px;
  margin: 40px auto 0;
  background: var(--paper-surface);
  background-color: var(--paper-bg-solid);
  border-radius: 1px;
  box-shadow: var(--paper-shadow);
  border: 1px solid var(--paper-border);
  padding: 48px 40px;
}

.loading-inner {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.loading-inner p {
  margin: 0;
}

.loading-main-text {
  font-size: 16px;
  color: #1a1a1a;
  letter-spacing: 2px;
}

.loading-hint {
  font-size: 13px;
  color: #8a8a8a;
  margin-top: 8px;
  letter-spacing: 1px;
}

/* ===== 横卡纸 ===== */
.top-card {
  position: relative;
  z-index: 1;
  max-width: 1400px;
  margin: 0 auto 24px;
  background: #fdfcfb;
  border-radius: 1px;
  box-shadow: 0 1px 1px rgba(0,0,0,0.03), 0 4px 8px rgba(0,0,0,0.03), 0 12px 24px rgba(0,0,0,0.04), 0 28px 52px rgba(0,0,0,0.05);
}

.icon-deco {
  position: absolute;
  width: 44px;
  height: 44px;
  color: #1a1a1a;
  opacity: 0.15;
  pointer-events: none;
  z-index: 0;
}

.top-card-content {
  padding: 32px 40px;
  color: #1a1a1a;
}

.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.top-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 4px;
  margin: 0;
}

.top-level {
  font-size: 13px;
  color: #8a8a8a;
  letter-spacing: 1px;
  border: 1px solid #ccc;
  padding: 2px 12px;
}

/* ===== Job Info Tags ===== */
.job-info-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.job-info-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 14px;
  border: 1px solid rgba(26,26,26,0.15);
  background: rgba(26,26,26,0.02);
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.job-info-tag-label {
  font-size: 11px;
  color: #8a8a8a;
  letter-spacing: 0.5px;
}

.job-info-tag-value {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 1px;
}

/* ===== 信息行 ===== */
.info-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 16px;
}

.info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label { font-size: 12px; color: #8a8a8a; }
.info-value { font-size: 15px; font-weight: 600; color: #1a1a1a; }
.info-value.salary { font-size: 17px; font-weight: 700; }

/* ===== 分隔线 ===== */
.divider {
  width: 100%;
  height: 1px;
  background: #1a1a1a;
  opacity: 0.1;
  margin: 20px 0;
}

/* ===== Section gradient divider ===== */
.section-gradient-divider {
  width: 100%;
  height: 1px;
  background: linear-gradient(to right, rgba(26,26,26,0.15), transparent);
  margin: 16px 0 20px;
}

/* ===== Section title with accent bar ===== */
.section-title-accent {
  border-left: 3px solid rgba(26,26,26,0.5);
  padding-left: 12px;
}

.sub-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.sub-line { flex: 1; height: 1px; background: #1a1a1a; opacity: 0.1; }
.sub-text { font-size: 13px; color: #8a8a8a; letter-spacing: 2px; white-space: nowrap; }

/* ===== 雷达图 ===== */
.radar-section { margin: 12px 0; }
.radar-wrap { display: flex; gap: 30px; flex-wrap: wrap; align-items: flex-start; }
.radar-chart { width: 380px; height: 300px; flex-shrink: 0; min-width: 280px; }
.radar-legend { flex: 1; min-width: 160px; }
.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  margin-bottom: 6px;
  border-bottom: 1px solid rgba(26,26,26,0.06);
}
.legend-dot { width: 8px; height: 8px; border-radius: 50%; background: #1a1a1a; flex-shrink: 0; }
.legend-label { flex: 1; font-size: 13px; color: #5a5a5a; }
.legend-value { font-size: 13px; font-weight: 700; color: #1a1a1a; }

/* ===== 技能标签 ===== */
.skill-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.skill-tag {
  display: inline-block;
  padding: 2px 10px;
  font-size: 12px;
  color: #1a1a1a;
  border: 1px solid rgba(26,26,26,0.2);
  border-radius: 2px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}
.skill-tag.secondary { border-color: rgba(26,26,26,0.1); color: #666; }
.skill-tag.small { padding: 1px 7px; font-size: 11px; }
.skill-group { margin-bottom: 12px; }
.skill-group-title { font-size: 13px; font-weight: 600; color: #1a1a1a; margin-bottom: 6px; }
.skill-analysis {
  margin-top: 10px;
  padding: 10px 14px;
  border-left: 2px solid #1a1a1a;
  font-size: 13px;
  color: #5a5a5a;
  line-height: 1.6;
}
.empty-text { font-size: 12px; color: #aaa; }

/* ===== 证书 ===== */
.cert-section { margin: 12px 0; }
.cert-row { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.cert-label { font-size: 13px; color: #5a5a5a; }

/* ===== 软技能 ===== */
.soft-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.soft-item { padding: 12px; border: 1px solid rgba(26,26,26,0.08); }
.soft-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
.soft-name { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.soft-level { font-size: 12px; color: #8a8a8a; }
.soft-desc { font-size: 12px; color: #8a8a8a; margin-bottom: 8px; line-height: 1.5; }
.soft-bar { height: 3px; background: #e8e8e8; border-radius: 2px; }
.soft-bar-fill { height: 100%; background: #1a1a1a; border-radius: 2px; }

/* ===== 学历经验 ===== */
.edu-section { margin: 12px 0; }
.edu-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 12px; }
.edu-cell { display: flex; flex-direction: column; gap: 4px; }
.edu-label { font-size: 12px; color: #8a8a8a; }
.edu-val { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.majors-row { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }

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
  padding: 32px 28px;
  color: #1a1a1a;
  min-height: 400px;
}

.section-title { font-size: 22px; font-weight: 700; color: #1a1a1a; letter-spacing: 3px; margin: 0; }

/* ===== Horizontal Timeline - Promotion Path ===== */
.promo-timeline {
  display: flex;
  align-items: flex-start;
  gap: 0;
  padding: 8px 0 4px;
  overflow-x: auto;
}

.timeline-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  min-width: 80px;
  position: relative;
}

.timeline-connector {
  position: absolute;
  top: 16px;
  left: calc(-50% + 14px);
  width: calc(100% - 28px);
  height: 1px;
  background: rgba(26,26,26,0.2);
}

.timeline-node:first-child .timeline-connector { display: none; }

.timeline-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid rgba(26,26,26,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fdfcfb;
  position: relative;
  z-index: 1;
  transition: border-color 0.2s, background 0.2s;
}

.timeline-node.timeline-current .timeline-circle {
  background: #1a1a1a;
  border-color: #1a1a1a;
}

.timeline-number {
  font-size: 11px;
  font-weight: 700;
  color: rgba(26,26,26,0.5);
}

.timeline-node.timeline-current .timeline-number {
  color: #fdfcfb;
}

.timeline-label {
  margin-top: 10px;
  text-align: center;
}

.timeline-name {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
  max-width: 100px;
}

.timeline-node.timeline-current .timeline-name {
  font-weight: 700;
}

.timeline-stage {
  font-size: 11px;
  color: #8a8a8a;
  margin-top: 2px;
}

.promo-details { margin-top: 20px; }

.promo-detail-item {
  padding: 12px 0;
  border-bottom: 1px solid rgba(26,26,26,0.06);
}

.promo-detail-item:last-child { border-bottom: none; }

.pdi-header { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.pdi-exp { font-size: 12px; color: #1a1a1a; border: 1px solid #1a1a1a; padding: 1px 8px; }
.pdi-arrow { font-size: 14px; color: #5a5a5a; }
.pdi-desc { font-size: 13px; color: #8a8a8a; margin-bottom: 6px; }
.pdi-skills { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.pdi-skills-label { font-size: 12px; color: #aaa; }

/* ===== Transfer Card Grid ===== */
.transfer-section { }

.transfer-center {
  text-align: center;
  padding: 16px;
  border: 2px solid #1a1a1a;
  margin-bottom: 20px;
}

.center-name { font-size: 16px; font-weight: 700; color: #1a1a1a; letter-spacing: 2px; }
.center-label { font-size: 12px; color: #8a8a8a; margin-top: 4px; }

.transfer-card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.transfer-paper-card {
  position: relative;
  padding: 14px 12px 12px;
  border: 1px solid rgba(26,26,26,0.1);
  background: #fdfcfb;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.transfer-paper-card:hover {
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
}

/* Paper fold corner effect */
.fold-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 16px;
  height: 16px;
  background: linear-gradient(225deg, #faf9f7 50%, rgba(26,26,26,0.08) 50%);
  border-bottom-left-radius: 2px;
}

.fold-corner::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 16px 16px 0;
  border-color: transparent #f0efed transparent transparent;
}

.transfer-paper-arrow {
  font-size: 11px;
  color: rgba(26,26,26,0.25);
  margin-bottom: 4px;
}

.transfer-paper-target {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6px;
  line-height: 1.3;
}

.transfer-paper-diff {
  font-size: 11px;
  color: #8a8a8a;
  display: flex;
  align-items: center;
  gap: 4px;
}

.diff-dots-inline {
  display: inline-flex;
  gap: 2px;
  margin-left: 2px;
}

.diff-dot-sm {
  width: 6px;
  height: 6px;
  border: 1px solid rgba(26,26,26,0.3);
  border-radius: 50%;
  display: inline-block;
}

.diff-dot-sm.filled {
  background: #1a1a1a;
  border-color: #1a1a1a;
}

/* 转岗详情 */
.td-row {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #5a5a5a;
}
.td-label { color: #1a1a1a; font-weight: 600; white-space: nowrap; }
.td-skills { display: flex; flex-wrap: wrap; gap: 4px; }

.diff-dots { display: flex; gap: 3px; }
.diff-dot {
  width: 10px;
  height: 10px;
  border: 1px solid #1a1a1a;
  border-radius: 50%;
}
.diff-dot.filled { background: #1a1a1a; }

/* ===== 空态 ===== */
.empty-state {
  position: relative;
  z-index: 1;
  max-width: 1400px;
  margin: 80px auto 0;
  text-align: center;
}

.empty-icon-paper {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 1px solid rgba(26,26,26,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  margin: 0 auto 20px;
}

.empty-title { font-size: 18px; font-weight: 700; color: #1a1a1a; letter-spacing: 2px; margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: #8a8a8a; margin-bottom: 24px; }

.empty-paper {
  text-align: center;
  padding: 40px 0;
  color: #aaa;
  font-size: 14px;
}

/* ===== 操作按钮 ===== */
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
.action-dash { display: inline-block; width: 50px; height: 1px; background: #1a1a1a; }
.action-text { font-size: 15px; font-weight: 600; color: #1a1a1a; letter-spacing: 3px; }

.footer {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-top: 24px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 11px;
  color: #aaa;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .papers { grid-template-columns: 1fr; max-width: 600px; }
  .info-row { grid-template-columns: 1fr; }
  .transfer-card-grid { grid-template-columns: repeat(2, 1fr); }
  .radar-wrap { flex-direction: column; }
  .radar-chart { width: 100%; max-width: 380px; min-width: unset; }
  .radar-legend { min-width: 100%; }
}

@media (max-width: 600px) {
  .detail-page { padding: 12px 8px; }
  .top-card-content { padding: 24px 18px; }
  .paper-content { padding: 24px 18px; }
  .radar-wrap { flex-direction: column; }
  .radar-chart { width: 100%; height: 260px; }
  .soft-grid { grid-template-columns: 1fr; }
  .top-title { font-size: 20px; }
  .section-title { font-size: 18px; }

  /* Timeline stacks vertically on mobile */
  .promo-timeline {
    flex-direction: column;
    align-items: flex-start;
    gap: 0;
    padding-left: 20px;
  }

  .timeline-node {
    flex-direction: row;
    min-width: unset;
    padding: 4px 0;
    gap: 12px;
  }

  .timeline-connector {
    top: auto;
    left: 13px;
    bottom: -4px;
    width: 1px;
    height: 12px;
    background: rgba(26,26,26,0.2);
  }

  .timeline-label {
    margin-top: 0;
    text-align: left;
  }

  .transfer-card-grid { grid-template-columns: 1fr; }
  .job-info-tags { gap: 6px; }
  .job-info-tag { padding: 3px 10px; }
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

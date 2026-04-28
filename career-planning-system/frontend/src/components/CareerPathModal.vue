<template>
  <teleport to="body">
    <div v-if="visible" class="overlay" @click.self="close">
      <div class="path-paper">
        <button class="close-btn" @click="close">✕</button>

        <!-- 加载中 -->
        <div v-if="loading" v-loading="true" element-loading-text="" class="loading-state">
          <p class="loading-main-text">正在加载路径图谱...</p>
          <p class="loading-hint">大模型正在聚合岗位画像与职业路径数据</p>
        </div>

        <!-- 内容 -->
        <div v-else-if="hasData" class="path-content">
          <!-- 标题 -->
          <h2 class="path-title">{{ jobName }}</h2>
          <p class="path-subtitle">职业发展路径图谱</p>
          <div class="divider"></div>

          <!-- 晋升路径 -->
          <div class="section-divider"></div>
          <h3 class="section-title">晋升路径</h3>

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
                <span class="skill-tag" v-for="(skill, sIdx) in splitSkills(item.required_skills || item.requiredSkills)" :key="sIdx">{{ skill }}</span>
              </div>
            </div>
          </div>

          <!-- 聚合画像中的 career_path 备用 -->
          <div v-if="!promotionPaths.length && careerPathSteps.length" class="promo-details">
            <div class="sub-title-row">
              <div class="sub-line"></div>
              <span class="sub-text">职业发展路径</span>
              <div class="sub-line"></div>
            </div>
            <div class="promo-timeline vertical">
              <div v-for="(step, idx) in careerPathSteps" :key="step.key" class="timeline-node vertical" :class="{ 'timeline-current': idx === 0 }">
                <div class="timeline-connector vertical" v-if="idx > 0"></div>
                <div class="timeline-circle">
                  <span class="timeline-number">{{ idx + 1 }}</span>
                </div>
                <div class="timeline-label">
                  <div class="timeline-name">{{ step.title }}</div>
                  <div class="timeline-stage">{{ step.desc }}</div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="!promotionNodes.length && !careerPathSteps.length" class="empty-block">
            <span>暂无晋升路径数据</span>
          </div>

          <!-- 转岗路径 -->
          <div class="section-divider"></div>
          <h3 class="section-title">转岗路径</h3>

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
                    <span v-for="d in 5" :key="d" class="diff-dot-sm" :class="{ filled: d <= (path.difficultyLevel || path.difficulty || 3) }"></span>
                  </span>
                </div>
              </div>
            </div>

            <div class="divider" style="margin: 20px 0;"></div>

            <!-- 详细列表 -->
            <div class="sub-title-row">
              <div class="sub-line"></div>
              <span class="sub-text">横向发展详情</span>
              <div class="sub-line"></div>
            </div>
            <div class="transfer-list">
              <div class="transfer-detail-item" v-for="(path, idx) in transferPaths" :key="idx">
                <div class="td-header">
                  <span class="td-title">{{ path.targetJobName || path.target_job_name }}</span>
                  <span class="td-diff-badge">
                    难度 {{ path.difficultyLevel || path.difficulty || 3 }}/5
                  </span>
                </div>
                <div class="td-row"><span class="td-label">目标岗位</span><span>{{ path.targetJobName || path.target_job_name }}</span></div>
                <div class="td-row"><span class="td-label">转型周期</span><span>{{ path.transferDuration || path.transfer_duration || '待确定' }}</span></div>
                <div class="td-row"><span class="td-label">转型难度</span>
                  <span class="diff-dots">
                    <span v-for="d in 5" :key="d" class="diff-dot" :class="{ filled: d <= (path.difficultyLevel || path.difficulty || 3) }"></span>
                  </span>
                </div>
                <div class="td-row"><span class="td-label">所需技能</span>
                  <div class="td-skills">
                    <span class="skill-tag small" v-for="(skill, sIdx) in splitSkills(path.requiredSkills || path.required_skills || '')" :key="sIdx">{{ skill }}</span>
                    <span v-if="!splitSkills(path.requiredSkills || path.required_skills || '').length" class="empty-text">待确定</span>
                  </div>
                </div>
                <div class="td-row"><span class="td-label">转型优势</span><span>{{ path.advantageAnalysis || path.advantage || '暂无说明' }}</span></div>
              </div>
            </div>
          </div>

          <div v-else class="empty-block">
            <span>暂无转岗路径数据</span>
          </div>

          <!-- 技能要求对比 -->
          <div v-if="allRequiredSkills.length" class="section-divider"></div>
          <h3 v-if="allRequiredSkills.length" class="section-title">技能要求概览</h3>
          <div v-if="allRequiredSkills.length" class="skills-overview">
            <div class="skills-cloud">
              <span class="skill-tag highlight" v-for="(skill, idx) in allRequiredSkills" :key="idx">{{ skill }}</span>
            </div>
          </div>
        </div>

        <!-- 无数据 -->
        <div v-if="!loading && !hasData" class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" width="48" height="48"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>
          <p class="empty-title">暂无路径图谱数据</p>
          <p class="empty-desc">该岗位暂无职业发展路径信息</p>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { getCareerPaths, getAggregatedProfile } from '@/api/job';

const props = defineProps<{
  visible: boolean;
  jobName: string;
}>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'close'): void;
}>();

const loading = ref(false);
const promotionPaths = ref<any[]>([]);
const transferPaths = ref<any[]>([]);
const profileData = ref<any>(null);
const promotionContext = ref('');
const transferContext = ref('');

const hasData = computed(() => {
  return promotionPaths.value.length > 0 || transferPaths.value.length > 0 || careerPathSteps.value.length > 0;
});

const promotionNodes = computed(() => {
  const nodes: string[] = [];
  for (const item of promotionPaths.value) {
    const current = item.current_job_name || item.currentJobName;
    const next = item.next_job_name || item.nextJobName;
    if (current && !nodes.includes(current)) nodes.push(current);
    if (next && !nodes.includes(next)) nodes.push(next);
  }
  return nodes;
});

const careerPathSteps = computed(() => {
  if (!profileData.value?.career_path) return [];
  const cp = profileData.value.career_path;
  return [
    { key: 'entry', title: cp.entry_level || '入门岗位', desc: '1-3年经验' },
    { key: 'mid', title: cp.mid_level || '中级岗位', desc: '3-5年经验' },
    { key: 'senior', title: cp.senior_level || '高级岗位', desc: '5-8年经验' },
    { key: 'expert', title: cp.expert_level || '专家/管理', desc: '8年以上' }
  ];
});

const allRequiredSkills = computed(() => {
  const skillSet = new Set<string>();
  for (const item of promotionPaths.value) {
    for (const s of splitSkills(item.required_skills || item.requiredSkills || '')) {
      skillSet.add(s);
    }
  }
  for (const item of transferPaths.value) {
    for (const s of splitSkills(item.requiredSkills || item.required_skills || '')) {
      skillSet.add(s);
    }
  }
  return Array.from(skillSet);
});

const getStageLabel = (idx: number) => {
  const labels = ['入门阶段', '成长阶段', '成熟阶段', '专家阶段', '管理阶段'];
  return labels[idx] || `阶段${idx + 1}`;
};

const splitSkills = (skills: string): string[] => {
  if (!skills) return [];
  return skills.split(/[,，、]/).map(s => s.trim()).filter(s => s);
};

const close = () => {
  emit('update:visible', false);
  emit('close');
};

const loadData = async () => {
  if (!props.jobName) return;
  loading.value = true;
  promotionPaths.value = [];
  transferPaths.value = [];
  profileData.value = null;

  try {
    // 加载职业路径数据
    try {
      const pathRes: any = await getCareerPaths(props.jobName);
      promotionPaths.value = pathRes?.promotionPaths || [];
      transferPaths.value = pathRes?.transferPaths || [];
      promotionContext.value = pathRes?.promotionContext || '';
      transferContext.value = pathRes?.transferContext || '';
    } catch (pathErr: any) {
      console.warn('[CareerPathModal] 加载路径数据失败:', pathErr?.message);
    }

    // 如果没有晋升路径数据，尝试从聚合画像获取 career_path
    if (!promotionPaths.value.length) {
      try {
        const profileRes: any = await getAggregatedProfile(props.jobName);
        let data = profileRes;
        if (typeof profileRes === 'string') {
          try {
            let jsonStr = profileRes;
            if (profileRes.includes('```json')) {
              jsonStr = profileRes.split('```json')[1].split('```')[0];
            } else if (profileRes.includes('```')) {
              jsonStr = profileRes.split('```')[1].split('```')[0];
            }
            data = JSON.parse(jsonStr);
          } catch {
            data = null;
          }
        }
        profileData.value = data;
      } catch (profileErr: any) {
        console.warn('[CareerPathModal] 加载画像数据失败:', profileErr?.message);
      }
    }
  } catch (e: any) {
    console.error('[CareerPathModal] 加载失败:', e);
  } finally {
    loading.value = false;
  }
};

watch(() => props.visible, async (val) => {
  if (val && props.jobName) {
    await loadData();
  }
});
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&display=swap');

/* ===== 覆盖层 ===== */
.overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(59, 36, 18, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

/* ===== 纸张容器 ===== */
.path-paper {
  background: var(--paper-surface);
  background-color: var(--paper-bg-solid);
  width: 100%;
  max-width: 960px;
  max-height: 90vh;
  overflow-y: auto;
  border-radius: 1px;
  box-shadow: 0 2px 4px rgba(59,36,18,0.06), 0 12px 32px rgba(59,36,18,0.12);
  position: relative;
  padding: 40px 48px;
  border: 1px solid var(--paper-border);
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #3b2412;
  line-height: 1.7;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 20px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 22px;
  color: #3b2412;
  padding: 6px 10px;
  transition: opacity 0.15s;
  z-index: 10;
}
.close-btn:hover { opacity: 0.5; }

/* ===== 加载中 ===== */
.loading-state {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.loading-main-text {
  font-size: 16px;
  margin: 0;
  color: #3b2412;
  letter-spacing: 2px;
}
.loading-hint {
  font-size: 13px;
  color: #7a6350;
  margin-top: 8px;
  letter-spacing: 1px;
}

/* ===== 标题 ===== */
.path-title {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 4px;
  margin: 0;
  color: #2a1a0e;
}
.path-subtitle {
  font-size: 15px;
  color: #5a3d28;
  margin: 8px 0 0;
  letter-spacing: 2px;
}

/* ===== 分隔线 ===== */
.divider {
  width: 100%;
  height: 1px;
  background: rgba(87, 64, 36, 0.15);
  margin: 24px 0;
}
.section-divider {
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, rgba(87, 64, 36, 0.2), rgba(87, 64, 36, 0.06), rgba(87, 64, 36, 0.2));
  margin: 28px 0 4px;
}
.section-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 24px 0 18px;
  padding-left: 14px;
  border-left: 4px solid #5a3d28;
  color: #2a1a0e;
}

/* ===== 子标题行 ===== */
.sub-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}
.sub-line { flex: 1; height: 1px; background: rgba(87, 64, 36, 0.18); }
.sub-text { font-size: 14px; color: #5a3d28; letter-spacing: 2px; white-space: nowrap; }

/* ===== 晋升时间线 ===== */
.promo-timeline {
  display: flex;
  align-items: flex-start;
  gap: 0;
  padding: 12px 0 8px;
  overflow-x: auto;
}
.promo-timeline.vertical {
  flex-direction: column;
  align-items: flex-start;
  gap: 0;
  padding-left: 16px;
}
.timeline-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  min-width: 90px;
  position: relative;
}
.timeline-node.vertical {
  flex-direction: row;
  min-width: unset;
  padding: 8px 0;
  gap: 16px;
}
.timeline-connector {
  position: absolute;
  top: 18px;
  left: calc(-50% + 18px);
  width: calc(100% - 36px);
  height: 2px;
  background: #8a7560;
}
.timeline-node.vertical .timeline-connector {
  position: absolute;
  top: auto;
  bottom: -4px;
  left: 17px;
  width: 2px;
  height: 16px;
  background: #8a7560;
}
.timeline-node:first-child .timeline-connector { display: none; }
.timeline-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid rgba(87, 64, 36, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--paper-bg-solid);
  position: relative;
  z-index: 1;
  transition: border-color 0.2s, background 0.2s;
  box-shadow: 0 1px 3px rgba(59, 36, 18, 0.08);
}
.timeline-node.timeline-current .timeline-circle {
  background: #3b2412;
  border-color: #3b2412;
  box-shadow: 0 2px 6px rgba(59, 36, 18, 0.2);
}
.timeline-number {
  font-size: 13px;
  font-weight: 700;
  color: #5a3d28;
}
.timeline-node.timeline-current .timeline-number {
  color: var(--paper-bg-solid);
}
.timeline-label {
  margin-top: 12px;
  text-align: center;
}
.timeline-node.vertical .timeline-label {
  margin-top: 0;
  text-align: left;
}
.timeline-name {
  font-size: 15px;
  font-weight: 700;
  color: #2a1a0e;
  line-height: 1.5;
  max-width: 120px;
}
.timeline-node.timeline-current .timeline-name {
  font-weight: 700;
}
.timeline-stage {
  font-size: 12px;
  color: #5a3d28;
  margin-top: 3px;
}

/* ===== 晋升详解 ===== */
.promo-details { margin-top: 24px; }
.promo-detail-item {
  padding: 16px 18px;
  margin-bottom: 10px;
  border: 1px solid rgba(87, 64, 36, 0.1);
  background: rgba(87, 64, 36, 0.015);
}
.promo-detail-item:last-child { margin-bottom: 0; }
.pdi-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.pdi-exp {
  font-size: 13px;
  color: #2a1a0e;
  border: 1px solid #5a3d28;
  padding: 2px 10px;
  font-weight: 600;
}
.pdi-arrow { font-size: 15px; color: #3b2412; font-weight: 600; }
.pdi-desc { font-size: 14px; color: #4a3020; margin-bottom: 8px; line-height: 1.8; }
.pdi-skills { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.pdi-skills-label { font-size: 13px; color: #5a3d28; }

/* ===== 技能标签 ===== */
.skill-tag {
  display: inline-block;
  padding: 4px 12px;
  font-size: 13px;
  color: #2a1a0e;
  border: 1px solid #5a3d28;
  border-radius: 2px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  background: rgba(87, 64, 36, 0.04);
}
.skill-tag.small { padding: 2px 8px; font-size: 12px; }
.skill-tag.highlight {
  border-color: #3b2412;
  background: rgba(59, 36, 18, 0.08);
  font-weight: 600;
  font-size: 13px;
  padding: 4px 12px;
  color: #2a1a0e;
}
.empty-text { font-size: 13px; color: #5a3d28; }

/* ===== 空数据块 ===== */
.empty-block {
  text-align: center;
  padding: 36px 0;
  color: #5a3d28;
  font-size: 15px;
}

/* ===== 转岗路径 ===== */
.transfer-section { }
.transfer-center {
  text-align: center;
  padding: 18px;
  border: 2px solid rgba(87, 64, 36, 0.25);
  margin-bottom: 24px;
  background: rgba(87, 64, 36, 0.03);
}
.center-name { font-size: 18px; font-weight: 700; color: #2a1a0e; letter-spacing: 3px; }
.center-label { font-size: 13px; color: #5a3d28; margin-top: 4px; }

.transfer-card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
.transfer-paper-card {
  position: relative;
  padding: 18px 16px 14px;
  border: 1px solid rgba(87, 64, 36, 0.12);
  background: var(--paper-bg-solid);
  box-shadow: 0 1px 3px rgba(59, 36, 18, 0.04);
  overflow: hidden;
  transition: box-shadow 0.2s, transform 0.2s;
}
.transfer-paper-card:hover {
  box-shadow: 0 3px 10px rgba(59, 36, 18, 0.08);
  transform: translateY(-2px);
}
.fold-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 18px;
  height: 18px;
  background: linear-gradient(225deg, rgba(250, 248, 244, 0.6) 50%, rgba(87, 64, 36, 0.08) 50%);
  border-bottom-left-radius: 2px;
}
.transfer-paper-arrow {
  font-size: 13px;
  color: #5a3d28;
  margin-bottom: 6px;
}
.transfer-paper-target {
  font-size: 15px;
  font-weight: 700;
  color: #2a1a0e;
  margin-bottom: 8px;
  line-height: 1.4;
}
.transfer-paper-diff {
  font-size: 12px;
  color: #5a3d28;
  display: flex;
  align-items: center;
  gap: 5px;
}
.diff-dots-inline {
  display: inline-flex;
  gap: 3px;
  margin-left: 3px;
}
.diff-dot-sm {
  width: 8px;
  height: 8px;
  border: 1px solid rgba(87, 64, 36, 0.3);
  border-radius: 50%;
  display: inline-block;
}
.diff-dot-sm.filled {
  background: #3b2412;
  border-color: #3b2412;
}

/* ===== 转岗详情列表 ===== */
.transfer-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.transfer-detail-item {
  padding: 18px 20px;
  border: 1px solid rgba(87, 64, 36, 0.1);
  background: rgba(87, 64, 36, 0.015);
}
.td-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(87, 64, 36, 0.1);
}
.td-title {
  font-size: 16px;
  font-weight: 700;
  color: #3b2412;
  letter-spacing: 1px;
}
.td-diff-badge {
  font-size: 12px;
  color: #3b2412;
  border: 1px solid #5a3d28;
  padding: 2px 10px;
}
.td-row {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #3b2412;
  line-height: 1.6;
}
.td-label {
  color: #2a1a0e;
  font-weight: 700;
  white-space: nowrap;
  min-width: 60px;
}
.td-skills { display: flex; flex-wrap: wrap; gap: 6px; }

.diff-dots { display: flex; gap: 4px; }
.diff-dot {
  width: 12px;
  height: 12px;
  border: 1px solid rgba(87, 64, 36, 0.3);
  border-radius: 50%;
}
.diff-dot.filled { background: #3b2412; border-color: #3b2412; }

/* ===== 技能概览 ===== */
.skills-overview { margin-top: 14px; }
.skills-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* ===== 空态 ===== */
.empty-state {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #5a3d28;
}
.empty-state svg {
  margin-bottom: 20px;
}
.empty-title {
  font-size: 20px;
  font-weight: 700;
  color: #2a1a0e;
  letter-spacing: 2px;
  margin: 0 0 10px;
}
.empty-desc {
  font-size: 15px;
  color: #5a3d28;
  margin: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .path-paper { padding: 28px 24px; }
  .path-title { font-size: 24px; }
  .section-title { font-size: 18px; }
  .transfer-card-grid { grid-template-columns: repeat(2, 1fr); }
  .promo-timeline { flex-direction: column; align-items: flex-start; gap: 0; padding-left: 12px; }
  .timeline-node { flex-direction: row; min-width: unset; padding: 6px 0; gap: 14px; }
  .timeline-connector { top: auto; left: 17px; bottom: -4px; width: 2px; height: 14px; }
  .timeline-label { margin-top: 0; text-align: left; }
}
@media (max-width: 500px) {
  .overlay { padding: 12px; }
  .path-paper { padding: 20px 16px; }
  .path-title { font-size: 20px; }
  .section-title { font-size: 16px; }
  .transfer-card-grid { grid-template-columns: 1fr; }
  .td-title { font-size: 14px; }
  .td-row { font-size: 13px; }
  .transfer-paper-target { font-size: 14px; }
}
</style>

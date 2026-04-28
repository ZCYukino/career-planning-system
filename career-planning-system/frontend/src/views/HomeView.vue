<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';
import { getTopJobs } from '@/api/job';

const router = useRouter();
const userStore = useUserStore();
const topJobs = ref<any[]>([]);
const loading = ref(true);
const introSequenceActive = ref(false);
const introSequenceAssembled = ref(false);
const introPrintStarted = ref(false);
const introPrintComplete = ref(false);
const printedHeadline = ref('');
const printedLines = ref(['', '', '']);
const introTimers: number[] = [];

const introPrintTargets = [
  '正在归档职业画像、岗位样本与成长轨迹。',
  '多张纸页已装订成册，准备生成你的专属规划入口。',
  '档案已就绪，点击下方卡片即可探索岗位、生成规划报告。'
];

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
        wordStyle: { fontSize: `${size}px`, opacity },
      });
    }
  }
  return items;
})();

const trendIndicators = ['↑', '↑', '↑', '→', '↑', '→'];

const queueIntroTimer = (callback: () => void, delay: number) => {
  const timerId = window.setTimeout(callback, delay);
  introTimers.push(timerId);
};

const streamText = (
  text: string,
  apply: (value: string) => void,
  startDelay: number,
  stepDelay: number
) => {
  let current = '';

  Array.from(text).forEach((char, index) => {
    queueIntroTimer(() => {
      current += char;
      apply(current);
    }, startDelay + index * stepDelay);
  });

  return startDelay + text.length * stepDelay;
};

const startIntroSequence = () => {
  queueIntroTimer(() => {
    introSequenceActive.value = true;
  }, 80);

  queueIntroTimer(() => {
    introSequenceAssembled.value = true;
  }, 980);

  queueIntroTimer(() => {
    introPrintStarted.value = true;
  }, 1320);

  let cursor = 1480;
  cursor = streamText('职业规划智能体', (value) => {
    printedHeadline.value = value;
  }, cursor, 72) + 200;

  introPrintTargets.forEach((line, index) => {
    cursor = streamText(line, (value) => {
      printedLines.value[index] = value;
    }, cursor, 28) + 170;
  });

  // 所有打字完成后隐藏光标
  queueIntroTimer(() => {
    introPrintComplete.value = true;
  }, cursor + 100);
};

onMounted(async () => {
  startIntroSequence();

  try {
    const res: any = await getTopJobs();
    topJobs.value = (res || []).slice(0, 6);
  } catch (e) { console.error(e); }
  finally { loading.value = false; }
});

onUnmounted(() => {
  introTimers.forEach((timerId) => {
    window.clearTimeout(timerId);
  });
});

const goToJobs = () => router.push('/jobs');
const goToReport = () => router.push('/report');
const goToJobDetail = (name: string) => router.push({ path: '/jobs/detail', query: { job: name } });
</script>

<template>
  <div class="home">
    <!-- 浮动职业名 -->
    <div class="careers-layer">
      <div v-for="(c, i) in floatingCareers" :key="i" class="career-drift" :style="c.driftStyle">
        <span class="career-word" :style="c.wordStyle">{{ c.name }}</span>
      </div>
    </div>

    <!-- 介绍栏 -->
    <section class="intro">
      <div class="intro-inner">
        <div class="intro-stage">
          <div
            class="folder-assembly"
            :class="{
              'is-active': introSequenceActive,
              'is-assembled': introSequenceAssembled
            }"
          >
            <div class="assembly-aura"></div>
            <div class="assembly-shadow"></div>
            <div class="assembly-sheet sheet-1"><span>能力画像</span></div>
            <div class="assembly-sheet sheet-2"><span>岗位样本</span></div>
            <div class="assembly-sheet sheet-3"><span>发展路径</span></div>
            <div class="assembly-sheet sheet-4"><span>技能差距</span></div>
            <div class="assembly-sheet sheet-5"><span>行动计划</span></div>
            <div class="assembly-folder">
              <span class="folder-tab">CAREER FILE</span>
              <span class="folder-title">职业规划档案册</span>
              <span class="folder-subtitle">PROFILE · JOBS · REPORT</span>
            </div>
          </div>

          <div class="intro-printer" :class="{ 'is-printing': introPrintStarted }">
            <div class="printer-head">
              <span class="printer-dot"></span>
              <span class="printer-dot"></span>
              <span class="printer-dot"></span>
            </div>
            <div class="printer-paper">
              <div class="intro-watermark">CAREER · PLANNING</div>
              <h1 class="intro-title">
                {{ printedHeadline || ' ' }}
                <span class="print-caret" :class="{ 'is-complete': introPrintComplete }"></span>
              </h1>
              <div class="intro-divider"></div>
              <p class="intro-desc print-line" v-for="(line, index) in printedLines" :key="index">{{ line || ' ' }}</p>
            </div>
          </div>
        </div>

        <div class="intro-meta">
          <p class="intro-meta-desc">基于阿里云百炼大模型，融合简历解析、多维画像、RAG 向量检索与 LangGraph 结构化报告生成，为大学生提供精准的职业规划与个性化成长路径。</p>
          <div class="intro-right">
            <div class="intro-stat">
              <span class="intro-stat-val">15</span>
              <span class="intro-stat-label">核心岗位</span>
            </div>
            <div class="intro-stat-sep"></div>
            <div class="intro-stat">
              <span class="intro-stat-val">5</span>
              <span class="intro-stat-label">能力维度</span>
            </div>
            <div class="intro-stat-sep"></div>
            <div class="intro-stat">
              <span class="intro-stat-val">3</span>
              <span class="intro-stat-label">核心模块</span>
            </div>
          </div>
        </div>

        <div class="intro-bottom">
          <div class="intro-flow">
            <div class="flow-step">
              <span class="flow-num">壹</span>
              <span class="flow-label">了解热门岗位</span>
            </div>
            <span class="flow-arrow">—</span>
            <div class="flow-step">
              <span class="flow-num">贰</span>
              <span class="flow-label">生成能力画像</span>
            </div>
            <span class="flow-arrow">—</span>
            <div class="flow-step">
              <span class="flow-num">叁</span>
              <span class="flow-label">查看职业报告</span>
            </div>
          </div>
          <div class="intro-bottom-row">
            <p class="intro-slogan">数据洞察 × 精准分析，让每一步职业选择都有据可依</p>
            <p class="intro-welcome" v-if="userStore.userInfo">欢迎回来，{{ userStore.userInfo.name || userStore.userInfo.username }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 两张 A4 纸并排 -->
    <div class="papers">
      <div class="paper" style="animation-delay:0s" @click="goToJobs">
        <div class="paper-inner">
          <!-- 右下角装饰 -->
          <svg class="card-deco" viewBox="0 0 40 40" fill="none">
            <circle cx="20" cy="20" r="14" stroke="currentColor" stroke-width="0.8"/>
            <circle cx="20" cy="20" r="8" stroke="currentColor" stroke-width="0.6"/>
            <circle cx="20" cy="20" r="2" fill="currentColor"/>
          </svg>

          <div class="p-header">
            <IconEpBriefcase class="p-icon" />
            <h2 class="p-title">岗位分析</h2>
          </div>
          <div class="p-divider"></div>

          <p class="p-desc">
            聚合 15 个核心岗位的真实招聘数据，生成七维度能力画像、技能要求、晋升与转岗路径图谱。结合向量检索技术，确保分析准确可靠。
          </p>

          <div class="p-section-label">
            <span class="p-label-line"></span>
            <span class="p-label-text">热门岗位预览</span>
            <span class="p-label-line"></span>
          </div>

          <div class="preview-jobs">
            <div v-for="(job, i) in topJobs.slice(0, 5)" :key="i" class="preview-job" @click.stop="goToJobDetail(job.jobName || job.job_name)">
              <span class="preview-rank" :class="{ top: i < 3 }">{{ i + 1 }}</span>
              <span class="preview-name">{{ job.jobName || job.job_name }}</span>
              <span class="preview-count">{{ job.count || job.job_count || 0 }}家</span>
              <span class="preview-trend" :class="trendIndicators[i] === '↑' ? 'up' : 'stable'">{{ trendIndicators[i] || '→' }}</span>
            </div>
            <div v-if="!loading && topJobs.length === 0" class="preview-empty">暂无岗位数据</div>
          </div>

          <div class="p-tags">
            <span class="ptag">七维画像</span>
            <span class="ptag">技能分析</span>
            <span class="ptag">晋升路径</span>
            <span class="ptag">转岗图谱</span>
          </div>

          <div class="p-cta">
            <button class="action-line" @click.stop="goToJobs">
              <span class="action-dash"></span>
              <span class="action-text">查看全部岗位</span>
              <span class="action-dash"></span>
            </button>
          </div>
        </div>
      </div>

      <div class="paper" style="animation-delay:0.08s" @click="goToReport">
        <div class="paper-inner">
          <!-- 右下角装饰 -->
          <svg class="card-deco" viewBox="0 0 40 40" fill="none">
            <path d="M8 8h28v28H8z" stroke="currentColor" stroke-width="0.8" opacity=".15"/>
            <path d="M14 14h16M14 20h16M14 26h10" stroke="currentColor" stroke-width="1" opacity=".12"/>
          </svg>

          <div class="p-header">
            <IconEpDocument class="p-icon" />
            <h2 class="p-title">职业报告</h2>
          </div>
          <div class="p-divider"></div>

          <p class="p-desc">
            基于 LangGraph 分阶段生成结构化职业规划报告，涵盖人岗匹配、职业路径、技能差距与行动计划，显著提升分析质量与生成效率。
          </p>

          <div class="p-section-label">
            <span class="p-label-line"></span>
            <span class="p-label-text">报告章节预览</span>
            <span class="p-label-line"></span>
          </div>

          <div class="preview-sections">
            <div class="preview-sec" v-for="(sec, i) in [
              { ch: '壹', title: '人岗匹配分析', desc: '四维加权评分：技能40% + 经验25% + 城市15% + 人格20%' },
              { ch: '贰', title: '职业发展路径', desc: '短中长期目标规划，含晋升与转岗双路径' },
              { ch: '叁', title: '技能差距与行动', desc: '分优先级技能提升方案与学习路径推荐' },
              { ch: '肆', title: '简历优化建议', desc: '结构化简历完善与亮点提炼指导' }
            ]" :key="i">
              <span class="sec-ch">{{ sec.ch }}</span>
              <div class="sec-body">
                <span class="sec-title">{{ sec.title }}</span>
                <span class="sec-desc">{{ sec.desc }}</span>
              </div>
            </div>
          </div>

          <div class="p-tags">
            <span class="ptag">四维匹配</span>
            <span class="ptag">结构化报告</span>
            <span class="ptag">行动计划</span>
            <span class="ptag">风险预判</span>
          </div>

          <div class="p-cta">
            <button class="action-line" @click.stop="goToReport">
              <span class="action-dash"></span>
              <span class="action-text">生成我的报告</span>
              <span class="action-dash"></span>
            </button>
          </div>
        </div>
      </div>

    </div>

    <p class="footer">&copy; 2026 职业规划智能体</p>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  background: var(--color-bg-page);
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow-x: hidden;
  padding: 0 24px 32px;
}

/* ===== 浮动职业名 ===== */
.careers-layer { position: absolute; inset: 0; overflow: hidden; z-index: 0; }
.career-drift { position: absolute; white-space: nowrap; will-change: transform; }
.career-drift:hover { animation-play-state: paused; }
.career-word {
  display: inline-block;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #1a1a1a;
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

/* ===== 介绍栏 ===== */
.intro {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1240px;
  margin: 28px 0 24px;
}
.intro-inner {
  background: #fffef9;
  border: 1px solid rgba(87, 64, 36, 0.09);
  border-radius: 2px;
  padding: 30px 34px 26px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 18px;
  box-shadow: 0 1px 1px rgba(0,0,0,0.015), 0 16px 30px rgba(64, 45, 20, 0.045);
  font-family: 'SimSun', 'Songti SC', serif;
  position: relative;
  overflow: hidden;
}
.intro-stage {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(340px, 1fr);
  gap: 28px;
  align-items: center;
}
.folder-assembly {
  position: relative;
  min-height: 320px;
  border: 1px solid rgba(87, 64, 36, 0.08);
  background: linear-gradient(180deg, rgba(252, 249, 241, 0.96) 0%, rgba(246, 238, 222, 0.92) 100%);
  overflow: hidden;
}
.assembly-aura {
  position: absolute;
  inset: 26px;
  background: radial-gradient(circle at center, rgba(255,255,255,0.72) 0%, rgba(255,255,255,0.12) 58%, rgba(255,255,255,0) 100%);
  opacity: 0;
  transition: opacity 0.45s ease;
}
.assembly-shadow {
  position: absolute;
  left: 50%;
  top: 66%;
  width: 240px;
  height: 42px;
  background: rgba(87, 64, 36, 0.16);
  filter: blur(22px);
  opacity: 0;
  transform: translateX(-50%) scale(0.7);
  transition: opacity 0.5s ease, transform 0.5s ease;
}
.assembly-sheet,
.assembly-folder {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 228px;
  height: 162px;
  border-radius: 2px;
  transform-origin: center;
}
.assembly-sheet {
  background: linear-gradient(180deg, rgba(255, 252, 245, 0.98) 0%, rgba(249, 242, 229, 0.96) 100%);
  border: 1px solid rgba(87, 64, 36, 0.12);
  box-shadow: 0 14px 28px rgba(64, 45, 20, 0.08), inset 0 0 0 1px rgba(255, 255, 255, 0.55);
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  padding: 16px 18px;
  color: rgba(87, 64, 36, 0.6);
  font-size: 13px;
  letter-spacing: 2px;
  opacity: 0;
  transform: translate(calc(-50% + var(--from-x)), calc(-50% + var(--from-y))) rotate(var(--from-rot)) scale(0.92);
}
.sheet-1 {
  --from-x: -250px;
  --from-y: -170px;
  --from-rot: -18deg;
  --to-x: -34px;
  --to-y: -18px;
  --to-rot: -9deg;
}
.sheet-2 {
  --from-x: 290px;
  --from-y: -148px;
  --from-rot: 20deg;
  --to-x: 30px;
  --to-y: -10px;
  --to-rot: 8deg;
}
.sheet-3 {
  --from-x: -286px;
  --from-y: 188px;
  --from-rot: 14deg;
  --to-x: -18px;
  --to-y: 6px;
  --to-rot: -3deg;
}
.sheet-4 {
  --from-x: 314px;
  --from-y: 176px;
  --from-rot: -16deg;
  --to-x: 16px;
  --to-y: 14px;
  --to-rot: 5deg;
}
.sheet-5 {
  --from-x: 0px;
  --from-y: -230px;
  --from-rot: 4deg;
  --to-x: 0px;
  --to-y: 0px;
  --to-rot: 0deg;
}
.assembly-folder {
  background: linear-gradient(180deg, #eadfbf 0%, #dccca4 100%);
  border: 1px solid rgba(107, 82, 45, 0.22);
  box-shadow: 0 24px 40px rgba(64, 45, 20, 0.12), inset 0 0 0 1px rgba(255,255,255,0.22);
  opacity: 0;
  transform: translate(calc(-50% + 220px), calc(-50% + 140px)) rotate(10deg) scale(0.94);
  z-index: 8;
}
.folder-tab,
.folder-title,
.folder-subtitle {
  position: absolute;
  left: 24px;
}
.folder-tab {
  top: -16px;
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 16px;
  background: #d6c08d;
  border: 1px solid rgba(107, 82, 45, 0.18);
  border-bottom: none;
  border-radius: 3px 3px 0 0;
  font-size: 11px;
  letter-spacing: 2px;
  color: rgba(72, 53, 24, 0.82);
}
.folder-title {
  top: 38px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 4px;
  color: #4c3a1e;
}
.folder-subtitle {
  top: 76px;
  font-size: 12px;
  color: rgba(72, 53, 24, 0.68);
  letter-spacing: 2px;
}
.folder-assembly.is-active .assembly-aura,
.folder-assembly.is-assembled .assembly-aura {
  opacity: 1;
}
.folder-assembly.is-active .assembly-shadow,
.folder-assembly.is-assembled .assembly-shadow {
  opacity: 1;
  transform: translateX(-50%) scale(1);
}
.folder-assembly.is-active .assembly-sheet {
  animation: assemble-sheet 0.95s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
}
.folder-assembly.is-active .sheet-2 { animation-delay: 0.08s; }
.folder-assembly.is-active .sheet-3 { animation-delay: 0.16s; }
.folder-assembly.is-active .sheet-4 { animation-delay: 0.24s; }
.folder-assembly.is-active .sheet-5 { animation-delay: 0.32s; }
.folder-assembly.is-active .assembly-folder {
  animation: assemble-folder 1.05s cubic-bezier(0.18, 0.88, 0.18, 1) 0.42s forwards;
}
.folder-assembly.is-assembled .assembly-folder {
  box-shadow: 0 28px 54px rgba(64, 45, 20, 0.16), inset 0 0 0 1px rgba(255,255,255,0.22);
}
.intro-printer {
  position: relative;
  min-height: 320px;
  padding: 16px 16px 0;
  background: rgba(250, 247, 240, 0.86);
  border: 1px solid rgba(87, 64, 36, 0.08);
}
.printer-head {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 18px;
  padding-left: 4px;
}
.printer-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: rgba(87, 64, 36, 0.28);
}
.printer-paper {
  min-height: 270px;
  background: #fffdfa;
  border: 1px solid rgba(87, 64, 36, 0.08);
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.65), 0 10px 20px rgba(64, 45, 20, 0.05);
  padding: 24px 26px 22px;
  transform: translateY(18px);
  opacity: 0;
  transition: transform 0.5s ease, opacity 0.5s ease;
}
.intro-printer.is-printing .printer-paper {
  transform: translateY(0);
  opacity: 1;
}
.intro-title {
  font-size: 30px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 5px;
  margin: 4px 0 0;
  min-height: 48px;
}
.intro-watermark {
  font-family: 'Georgia', serif;
  font-size: 10px;
  letter-spacing: 5px;
  color: rgba(26,26,26,0.07);
  text-transform: uppercase;
  margin-top: 2px;
  user-select: none;
}
.intro-divider {
  width: 54px;
  height: 1.5px;
  background: rgba(87, 64, 36, 0.2);
  margin: 14px 0 16px;
}
.intro-desc {
  font-size: 14px;
  color: #68583d;
  line-height: 1.85;
  margin: 0;
  letter-spacing: 0.3px;
}
.print-line {
  min-height: 26px;
}
.print-caret {
  display: inline-block;
  width: 8px;
  height: 1em;
  margin-left: 8px;
  background: rgba(87, 64, 36, 0.55);
  vertical-align: -2px;
  animation: blink-caret 1s steps(1) infinite;
  transition: opacity 0.3s ease;
}
.print-caret.is-complete {
  opacity: 0;
}
.intro-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 0 2px;
  border-top: 1px solid rgba(87, 64, 36, 0.08);
}
.intro-meta-desc {
  flex: 1;
  margin: 0;
  font-size: 13px;
  line-height: 1.9;
  color: #6e6148;
}
.intro-right {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
  padding: 10px 14px;
  background: rgba(255,255,255,0.68);
  border: 1px solid rgba(87, 64, 36, 0.07);
}
.intro-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 0 20px;
}
.intro-stat-val {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 1px;
}
.intro-stat-label {
  font-size: 11px;
  color: #8a8a8a;
  letter-spacing: 1px;
}
.intro-stat-sep {
  width: 1px;
  height: 28px;
  background: rgba(26,26,26,0.08);
}
.intro-bottom {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(87, 64, 36, 0.08);
}
.intro-flow {
  display: flex;
  align-items: center;
  gap: 0;
}
.flow-step {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  background: rgba(26,26,26,0.03);
  border: 1px solid rgba(87, 64, 36, 0.1);
  border-radius: 2px;
  transition: background 0.2s ease;
}
.flow-step:hover {
  background: rgba(26,26,26,0.06);
}
.flow-num {
  font-size: 13px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 1px;
}
.flow-label {
  font-size: 13px;
  color: #5f533f;
  letter-spacing: 0.5px;
}
.flow-arrow {
  color: rgba(87, 64, 36, 0.2);
  font-size: 12px;
  margin: 0 6px;
  letter-spacing: 2px;
}
.intro-bottom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.intro-slogan {
  margin: 0;
  font-size: 13px;
  color: #7b6b4d;
  letter-spacing: 0.8px;
}
.intro-welcome {
  margin: 0;
  font-size: 12px;
  color: #aaa;
  white-space: nowrap;
  flex-shrink: 0;
}

/* ===== 两张 A4 纸 ===== */
@keyframes paper-rise {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}

.papers {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 32px;
  width: 100%;
  max-width: 1240px;
  flex: 1;
}

.paper {
  animation: paper-rise 0.5s ease both;
  background: var(--paper-surface);
  background-color: var(--paper-bg-solid);
  border-radius: 2px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  box-shadow: var(--paper-shadow);
  border: 1px solid var(--paper-border);
  min-height: 480px;
}
.paper:hover {
  transform: translateY(-4px);
  box-shadow: var(--paper-shadow-hover);
}
.paper::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--paper-overlay);
  pointer-events: none;
}
/* 折角 */
.paper::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0;
  border-color: transparent rgba(255, 248, 233, 0.92) transparent transparent;
  transition: border-width 0.3s ease;
  z-index: 2;
}
.paper:hover::after { border-width: 18px; }

.paper-inner {
  padding: 44px 40px 36px;
  display: flex;
  flex-direction: column;
  font-family: 'SimSun', 'Songti SC', serif;
  color: #1a1a1a;
  position: relative;
  height: 100%;
  min-height: 480px;
}

.card-deco {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: 36px;
  height: 36px;
  color: #1a1a1a;
  opacity: 0.06;
  pointer-events: none;
}

/* 纸头 */
.p-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.p-icon { font-size: 22px; color: #1a1a1a; }
.p-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 6px;
  margin: 0;
}
.p-divider {
  height: 1px;
  background: rgba(26,26,26,0.15);
  margin: 16px 0 18px;
}
.p-desc {
  font-size: 15px;
  color: #5a5a5a;
  line-height: 2;
  margin: 0 0 22px;
  letter-spacing: 0.3px;
}

/* 段落标签 */
.p-section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.p-label-line {
  flex: 1;
  height: 1px;
  background: rgba(26,26,26,0.10);
}
.p-label-text {
  font-size: 12px;
  color: #8a8a8a;
  letter-spacing: 2px;
  white-space: nowrap;
}

/* 岗位预览 */
.preview-jobs {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.preview-job {
  display: flex;
  align-items: center;
  padding: 9px 0;
  border-bottom: 1px solid rgba(26,26,26,0.05);
  transition: background 0.15s ease, box-shadow 0.15s ease;
  cursor: pointer;
}
.preview-job:last-child { border-bottom: none; }
.preview-job:hover {
  background: rgba(255,255,255,0.84);
  box-shadow: inset 3px 0 0 rgba(87, 64, 36, 0.12);
}
.preview-rank {
  width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700;
  background: #ccc; color: #666;
  border-radius: 2px;
  margin-right: 10px;
  flex-shrink: 0;
}
.preview-rank.top {
  width: 26px; height: 26px;
  border-radius: 50%;
  font-size: 12px;
  background: #1a1a1a;
  color: #fdfcfb;
}
.preview-job:nth-child(2) .preview-rank.top { background: #444; }
.preview-job:nth-child(3) .preview-rank.top { background: #777; }
.preview-name {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.preview-count {
  font-size: 13px;
  color: #8a8a8a;
  margin-left: 8px;
}
.preview-trend { font-size: 12px; margin-left: 4px; }
.preview-trend.up { color: #8a8a8a; }
.preview-trend.stable { color: #bbb; }
.preview-empty {
  text-align: center;
  color: #aaa;
  font-size: 13px;
  padding: 20px 0;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 报告章节 */
.preview-sections {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.preview-sec {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(26,26,26,0.04);
}
.preview-sec:last-child { border-bottom: none; }
.sec-ch {
  font-size: 20px;
  font-weight: 700;
  color: rgba(26,26,26,0.12);
  letter-spacing: 1px;
  flex-shrink: 0;
  width: 28px;
  line-height: 1;
  padding-top: 2px;
}
.sec-body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  flex: 1;
}
.sec-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.sec-desc {
  font-size: 12px;
  color: #8a8a8a;
  line-height: 1.6;
}

/* 标签行 */
.p-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: auto;
  padding-top: 16px;
}
.ptag {
  display: inline-block;
  font-size: 11px;
  color: #6a6a6a;
  padding: 4px 12px;
  border: 1px solid rgba(26,26,26,0.08);
  border-radius: 2px;
  background: rgba(26,26,26,0.02);
  letter-spacing: 0.5px;
}

/* CTA */
.p-cta {
  display: flex;
  justify-content: center;
  padding-top: 14px;
}
.action-line {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 0;
  font-family: 'SimSun', 'Songti SC', serif;
  transition: opacity 0.15s;
}
.action-line:hover { opacity: 0.5; }
.action-dash {
  display: inline-block;
  width: 40px;
  height: 1px;
  background: #1a1a1a;
}
.action-text {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 3px;
}

/* ===== Footer ===== */
.footer {
  position: relative;
  z-index: 1;
  font-family: 'SimSun', 'Songti SC', serif;
  font-size: 12px;
  color: #aaa;
  letter-spacing: 0.5px;
  margin-top: 32px;
}

/* ===== 响应式 ===== */
@media (max-width: 1100px) {
  .intro-stage { grid-template-columns: 1fr; }
  .intro-meta { flex-direction: column; align-items: flex-start; }
  .intro-right { width: 100%; justify-content: space-between; }
  .papers { gap: 24px; max-width: 1120px; }
  .paper-inner { padding: 36px 28px 28px; }
  .p-title { font-size: 22px; letter-spacing: 4px; }
  .p-desc { font-size: 14px; }
}
@media (max-width: 900px) {
  .intro-inner { padding: 24px 20px 22px; }
  .folder-assembly,
  .intro-printer { min-height: 280px; }
  .assembly-sheet,
  .assembly-folder { width: 204px; height: 146px; }
  .folder-title { top: 34px; font-size: 21px; }
  .papers { grid-template-columns: 1fr; max-width: 640px; }
  .paper { min-height: auto; }
  .paper-inner { min-height: auto; }
}
@media (max-width: 600px) {
  .home { padding: 0 12px 24px; }
  .intro-title { font-size: 24px; letter-spacing: 3px; }
  .intro-right { padding: 8px 10px; }
  .intro-stat { padding: 0 10px; }
  .folder-assembly,
  .intro-printer { min-height: 248px; }
  .assembly-sheet,
  .assembly-folder { width: 176px; height: 126px; }
  .folder-tab { left: 16px; padding: 0 12px; }
  .folder-title { left: 18px; top: 28px; font-size: 18px; letter-spacing: 2px; }
  .folder-subtitle { left: 18px; top: 62px; font-size: 11px; letter-spacing: 1px; }
  .papers { gap: 20px; }
  .paper-inner { padding: 28px 20px 22px; }
  .p-title { font-size: 20px; letter-spacing: 3px; }
  .p-desc { font-size: 13px; }
  .preview-name { font-size: 15px; }
  .action-text { font-size: 14px; letter-spacing: 2px; }
  .action-dash { width: 30px; }
}
</style>

<style>
@keyframes assemble-sheet {
  0% {
    opacity: 0;
    transform: translate(calc(-50% + var(--from-x)), calc(-50% + var(--from-y))) rotate(var(--from-rot)) scale(0.92);
  }
  68% {
    opacity: 1;
    transform: translate(calc(-50% + var(--to-x)), calc(-50% + var(--to-y))) rotate(calc(var(--to-rot) * 1.15)) scale(1.02);
  }
  100% {
    opacity: 1;
    transform: translate(calc(-50% + var(--to-x)), calc(-50% + var(--to-y))) rotate(var(--to-rot)) scale(1);
  }
}
@keyframes assemble-folder {
  0% {
    opacity: 0;
    transform: translate(calc(-50% + 220px), calc(-50% + 140px)) rotate(10deg) scale(0.94);
  }
  70% {
    opacity: 1;
    transform: translate(-50%, -50%) rotate(-1deg) scale(1.02);
  }
  100% {
    opacity: 1;
    transform: translate(-50%, -50%) rotate(0deg) scale(1);
  }
}
@keyframes blink-caret {
  0%, 49% { opacity: 1; }
  50%, 100% { opacity: 0; }
}
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

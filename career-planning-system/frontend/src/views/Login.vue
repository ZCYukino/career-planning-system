<template>
  <div class="login-page">
    <div class="desk-glow glow-left"></div>
    <div class="desk-glow glow-right"></div>

    <div class="careers-layer">
      <div v-for="(item, index) in careers" :key="index" class="career-drift" :style="item.driftStyle">
        <span class="career-word" :style="item.wordStyle">{{ item.name }}</span>
      </div>
    </div>

    <!-- ===== 书本翻开动画 ===== -->
    <div class="book-overlay" :class="{ 'is-opening': bookOpening, 'is-opened': bookOpened, 'is-gone': overlayGone }">
      <div class="book-3d">
        <div class="book-spine"></div>
        <div class="book-pages-bg">
          <div class="bg-page bg-page-l"></div>
          <div class="bg-page bg-page-r"></div>
        </div>
        <div class="book-cover">
          <div class="cover-front">
            <div class="cover-frame">
              <div class="cover-watermark">CAREER · AI · PLANNING</div>
              <h1 class="cover-title">职业规划智能体</h1>
              <div class="cover-line"></div>
              <p class="cover-sub">基于大模型的职业生涯规划系统</p>
            </div>
          </div>
          <div class="cover-back"></div>
        </div>
      </div>
    </div>

    <!-- ===== 两页纸并排 ===== -->
    <div class="book-shell" :class="{ 'is-visible': contentVisible }">
      <!-- 左页：平台介绍 -->
      <section class="login-story">
        <div class="story-paper">
          <div class="paper-head">
            <span class="paper-dot"></span>
            <span class="paper-dot"></span>
            <span class="paper-dot"></span>
          </div>
          <div class="paper-body story-body">
            <div class="paper-watermark">CAREER · AI · PLANNING</div>
            <h2 class="story-title">{{ typedStoryTitle || ' ' }}<span class="cursor" v-if="typingStoryTitle">|</span></h2>
            <div class="story-divider" :class="{ drawn: storyTitleDone }"></div>
            <p class="story-desc">
              基于阿里云百炼大模型，融合简历解析、多维画像、RAG 向量检索与 LangGraph 结构化报告生成，为大学生提供精准的职业规划与个性化成长路径。
            </p>

            <div class="story-metrics">
              <div class="story-metric" v-for="m in platformMetrics" :key="m.label">
                <span class="metric-val">{{ m.value }}</span>
                <span class="metric-label">{{ m.label }}</span>
              </div>
            </div>

            <div class="story-features">
              <div class="feature-item" v-for="f in storyFeatures" :key="f.index">
                <span class="feature-index">{{ f.index }}</span>
                <div>
                  <p class="feature-title">{{ f.title }}</p>
                  <p class="feature-desc">{{ f.desc }}</p>
                </div>
              </div>
            </div>

            <div class="printer-feed" :class="{ visible: printerFeedVisible }">
              <div class="printer-feed-head">
                <span class="printer-feed-dot"></span>
                <span class="printer-feed-dot"></span>
                <span class="printer-feed-dot"></span>
                <span class="printer-feed-label">PRINT STREAM</span>
              </div>
              <div class="printer-feed-body">
                <p class="printer-line">{{ typedFeedLineOne || ' ' }}</p>
                <p class="printer-line">{{ typedFeedLineTwo || ' ' }}</p>
                <p class="printer-line">{{ typedFeedLineThree || ' ' }}</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 书脊 -->
      <div class="book-spine-visual"></div>

      <!-- 右页：登录/注册 -->
      <section class="login-panel">
        <div class="auth-paper">
          <div class="paper-head">
            <span class="paper-dot"></span>
            <span class="paper-dot"></span>
            <span class="paper-dot"></span>
          </div>
          <div class="paper-body auth-body-inner">
            <div class="auth-topline">
              <div class="folio-meta">
                <span class="folio-label">ENTRY TERMINAL</span>
                <span class="folio-sep"></span>
                <span class="folio-no">{{ activeTab === 'login' ? 'LOGIN' : 'REGISTER' }}</span>
              </div>
              <div class="mode-switch">
                <button type="button" class="mode-tab" :class="{ active: activeTab === 'login' }" @click="setTab('login')">登录</button>
                <button type="button" class="mode-tab" :class="{ active: activeTab === 'register' }" @click="setTab('register')">注册</button>
              </div>
            </div>

            <div class="auth-content">
              <div class="paper-heading">
                <p class="eyebrow">PERSONAL CAREER SHEET</p>
                <h1 class="main-title">{{ typedMainTitle }}<span class="cursor" v-if="typingMainTitle">|</span></h1>
              </div>

              <div class="divider" :class="{ drawn: mainTitleDone }"></div>

              <div class="subhead">
                <h2 class="sub-title">{{ typedSubTitle }}<span class="cursor" v-if="typingSubTitle">|</span></h2>
                <p class="sub-copy">{{ panelDescription }}</p>
              </div>

              <div class="process-ribbon">
                <span v-for="step in processSteps" :key="step" class="ribbon-item">{{ step }}</span>
              </div>

              <form v-if="activeTab === 'login'" class="form" :class="{ visible: formVisible }" @submit.prevent="handleLogin">
                <div class="field">
                  <div class="field-head">
                    <span class="field-label">{{ typedLabelUser }}<span class="cursor" v-if="typingLabelUser">|</span></span>
                    <span class="field-tag">账号</span>
                  </div>
                  <div class="field-line">
                    <input v-model="loginForm.username" type="text" class="field-input" :placeholder="inputsReady ? '请输入用户名' : ''" autocomplete="username" maxlength="20" />
                  </div>
                </div>
                <div class="field">
                  <div class="field-head">
                    <span class="field-label">{{ typedLabelPass }}<span class="cursor" v-if="typingLabelPass">|</span></span>
                    <span class="field-tag">密钥</span>
                  </div>
                  <div class="field-line">
                    <input v-model="loginForm.password" type="password" class="field-input" :placeholder="inputsReady ? '请输入6位数字密码' : ''" autocomplete="current-password" inputmode="numeric" pattern="[0-9]*" maxlength="6" @input="normalizeLoginPassword" @keyup.enter="handleLogin" />
                  </div>
                </div>
              </form>

              <form v-else class="form" :class="{ visible: formVisible }" @submit.prevent="handleLogin">
                <div class="field">
                  <div class="field-head">
                    <span class="field-label">{{ typedLabelUser }}<span class="cursor" v-if="typingLabelUser">|</span></span>
                    <span class="field-tag">新账号</span>
                  </div>
                  <div class="field-line">
                    <input v-model="registerForm.username" type="text" class="field-input" :placeholder="inputsReady ? '请输入用户名' : ''" autocomplete="username" maxlength="20" />
                  </div>
                </div>
                <div class="field">
                  <div class="field-head">
                    <span class="field-label">{{ typedLabelPass }}<span class="cursor" v-if="typingLabelPass">|</span></span>
                    <span class="field-tag">6位数字</span>
                  </div>
                  <div class="field-line">
                    <input v-model="registerForm.password" type="password" class="field-input" :placeholder="inputsReady ? '请输入6位数字密码' : ''" autocomplete="new-password" inputmode="numeric" pattern="[0-9]*" maxlength="6" @input="normalizeRegisterPassword('password')" />
                  </div>
                  <div class="field-meta">
                    <span class="field-helper">仅支持 6 位数字密码</span>
                    <span class="field-counter">{{ registerForm.password.length }}/6</span>
                  </div>
                </div>
                <div class="field">
                  <div class="field-head">
                    <span class="field-label">{{ typedLabelConfirm }}<span class="cursor" v-if="typingLabelConfirm">|</span></span>
                    <span class="field-tag">复核</span>
                  </div>
                  <div class="field-line">
                    <input v-model="registerForm.confirmPassword" type="password" class="field-input" :placeholder="inputsReady ? '再次输入6位数字密码' : ''" autocomplete="new-password" inputmode="numeric" pattern="[0-9]*" maxlength="6" @input="normalizeRegisterPassword('confirmPassword')" @keyup.enter="handleLogin" />
                  </div>
                </div>
              </form>

              <div class="bottom-area" :class="{ visible: inputsReady }">
                <button type="button" class="action-line" @click="handleLogin" :disabled="loading">
                  <span class="action-dash"></span>
                  <span class="action-text">{{ activeTab === 'login' ? '进入工作台' : '创建职业档案' }}</span>
                  <span class="action-dash"></span>
                </button>
                <button type="button" class="switch-link" @click="switchTab">
                  {{ activeTab === 'login' ? '改为注册模式' : '改为登录模式' }}
                </button>
                <p class="footer-tip">{{ footerTip }}</p>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <p class="footer">© 2026 职业规划智能体</p>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue';
import type { Ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { login as apiLogin, register as apiRegister } from '@/api/auth';
import { ElMessage } from 'element-plus';

const router = useRouter();
const userStore = useUserStore();

// ===== Book animation =====
const bookOpening = ref(false);
const bookOpened = ref(false);
const overlayGone = ref(false);
const contentVisible = ref(false);

// ===== Data =====
const platformMetrics = [
  { value: '100%', label: '智能检索引擎' },
  { value: '98%', label: '画像精准度' },
  { value: '100%', label: '报告完整率' },
  { value: '95%', label: '匹配推荐率' },
];

const storyFeatures = [
  { index: '01', title: '多级降级检索', desc: '向量检索优先，本地向量次级兜底，结构化知识库最终回退' },
  { index: '02', title: '三源能力画像', desc: '简历、测评、人格三类异构数据按权重融合生成动态画像' },
  { index: '03', title: 'LangGraph 报告', desc: '状态机按章节推进报告生成，实时反馈进度而非堆砌文本' },
  { index: '04', title: '四维人岗匹配', desc: '技能、经验、城市、人格联合评分与差距分析' },
];

// ===== Typing state =====
const SPEED = 50;
const typedStoryTitle = ref('');
const typingStoryTitle = ref(true);
const storyTitleDone = ref(false);

const typedMainTitle = ref('');
const typingMainTitle = ref(true);
const mainTitleDone = ref(false);

const typedSubTitle = ref('');
const typingSubTitle = ref(true);

const typedLabelUser = ref('');
const typingLabelUser = ref(false);

const typedLabelPass = ref('');
const typingLabelPass = ref(false);

const typedLabelConfirm = ref('');
const typingLabelConfirm = ref(false);

const typedFeedLineOne = ref('');
const typedFeedLineTwo = ref('');
const typedFeedLineThree = ref('');

const printerFeedVisible = ref(false);
const formVisible = ref(false);
const inputsReady = ref(false);

// ===== Auth state =====
const activeTab = ref<'login' | 'register'>('login');
const loading = ref(false);
const loginForm = reactive({ username: '', password: '' });
const registerForm = reactive({ username: '', password: '', confirmPassword: '' });

const sequenceTimeouts: number[] = [];
const sequenceIntervals: number[] = [];
let activeSequenceId = 0;

// ===== Careers floating =====
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
  const items: Array<{ name: string; driftStyle: Record<string, string>; wordStyle: Record<string, string | number> }> = [];
  for (let repeat = 0; repeat < 4; repeat++) {
    for (const name of careerNames) {
      const top = Math.random() * 94 + 3;
      const size = 11 + Math.random() * 10;
      const opacity = 0.1 + Math.random() * 0.18;
      const duration = 25 + Math.random() * 35;
      const delay = -(Math.random() * duration);
      const drift = Math.random() > 0.5 ? 'drift-left' : 'drift-right';
      items.push({ name, driftStyle: { top: `${top}%`, animation: `${drift} ${duration}s ${delay}s linear infinite` }, wordStyle: { fontSize: `${size}px`, opacity } });
    }
  }
  return items;
})();

// ===== Computed =====
const panelDescription = computed(() => {
  return activeTab.value === 'login'
    ? '登录后继续查看你的能力画像、岗位匹配结果与报告进度。'
    : '注册后系统会立即为你建立可信的职业档案底稿与分析入口。';
});

const processSteps = computed(() => {
  return activeTab.value === 'login'
    ? ['身份校验', '恢复画像', '进入平台']
    : ['创建账户', '激活画像', '解锁规划'];
});

const footerTip = computed(() => {
  return activeTab.value === 'login'
    ? '登录后会继续保留你的职业画像、岗位偏好、匹配记录与报告结果。'
    : '注册密码仍限制为 6 位数字，以便和当前接口校验规则保持一致。';
});

const printerFeedTargets = computed(() => {
  return activeTab.value === 'login'
    ? [
        '可信检索层已装载，异常时将自动切换本地向量与结构化知识库。',
        '三源能力画像、四维匹配结果与报告进度将在登录后恢复。',
        '完成身份校验后即可继续你的职业规划决策流程。',
      ]
    : [
        '正在建立新的职业档案编号，并初始化可信知识与画像引擎。',
        '注册成功后将解锁岗位匹配、分章节报告和行动计划流程。',
        '当前密码规则固定为 6 位数字，请完成平台接入。',
      ];
});

// ===== Typing helpers =====
function clearSequenceHandles() {
  sequenceTimeouts.splice(0).forEach(id => window.clearTimeout(id));
  sequenceIntervals.splice(0).forEach(id => window.clearInterval(id));
}

function queueTimeout(cb: () => void, delay: number, seqId: number): Promise<void> {
  return new Promise(resolve => {
    const id = window.setTimeout(() => { if (seqId === activeSequenceId) cb(); resolve(); }, delay);
    sequenceTimeouts.push(id);
  });
}

function typeString(target: Ref<string>, text: string, typingFlag: Ref<boolean>, seqId: number, speed = SPEED): Promise<void> {
  return new Promise(resolve => {
    if (seqId !== activeSequenceId) { resolve(); return; }
    typingFlag.value = true;
    let i = 0;
    const t = window.setInterval(() => {
      if (seqId !== activeSequenceId) { window.clearInterval(t); resolve(); return; }
      if (i < text.length) { target.value = text.slice(0, i + 1); i++; }
      else { window.clearInterval(t); typingFlag.value = false; resolve(); }
    }, speed);
    sequenceIntervals.push(t);
  });
}

function typeFeedLine(target: Ref<string>, text: string, seqId: number, speed = 26): Promise<void> {
  return new Promise(resolve => {
    if (seqId !== activeSequenceId) { resolve(); return; }
    let i = 0;
    const t = window.setInterval(() => {
      if (seqId !== activeSequenceId) { window.clearInterval(t); resolve(); return; }
      if (i < text.length) { target.value = text.slice(0, i + 1); i++; }
      else { window.clearInterval(t); resolve(); }
    }, speed);
    sequenceIntervals.push(t);
  });
}

// ===== Sequence =====
function resetSequenceState() {
  typedStoryTitle.value = '';
  typingStoryTitle.value = true;
  storyTitleDone.value = false;
  typedMainTitle.value = '';
  typingMainTitle.value = true;
  mainTitleDone.value = false;
  typedSubTitle.value = '';
  typingSubTitle.value = true;
  typedLabelUser.value = '';
  typedLabelPass.value = '';
  typedLabelConfirm.value = '';
  typedFeedLineOne.value = '';
  typedFeedLineTwo.value = '';
  typedFeedLineThree.value = '';
  typingLabelUser.value = false;
  typingLabelPass.value = false;
  typingLabelConfirm.value = false;
  printerFeedVisible.value = false;
  formVisible.value = false;
  inputsReady.value = false;
}

async function runSequence() {
  activeSequenceId += 1;
  const seqId = activeSequenceId;
  clearSequenceHandles();
  resetSequenceState();

  // Left page typing
  await typeString(typedStoryTitle, '职业规划智能体', typingStoryTitle, seqId, 65);
  storyTitleDone.value = true;

  // Printer feed on left page
  printerFeedVisible.value = true;
  await queueTimeout(() => {}, 200, seqId);
  const [lineOne, lineTwo, lineThree] = printerFeedTargets.value;
  await typeFeedLine(typedFeedLineOne, lineOne, seqId);
  await typeFeedLine(typedFeedLineTwo, lineTwo, seqId);
  await typeFeedLine(typedFeedLineThree, lineThree, seqId);

  // Right page typing
  await typeString(typedMainTitle, '职业规划智能体', typingMainTitle, seqId);
  mainTitleDone.value = true;
  await typeString(typedSubTitle, activeTab.value === 'login' ? '登录' : '注册', typingSubTitle, seqId);

  formVisible.value = true;
  await nextTick();

  await typeString(typedLabelUser, '用户名', typingLabelUser, seqId);
  await typeString(typedLabelPass, '密码', typingLabelPass, seqId);
  if (activeTab.value === 'register') {
    await typeString(typedLabelConfirm, '确认密码', typingLabelConfirm, seqId);
  }
  inputsReady.value = true;
}

function normalizeLoginPassword() {
  const normalized = loginForm.password.replace(/\D/g, '').slice(0, 6);
  if (loginForm.password !== normalized) loginForm.password = normalized;
}

function normalizeRegisterPassword(field: 'password' | 'confirmPassword') {
  const normalized = registerForm[field].replace(/\D/g, '').slice(0, 6);
  if (registerForm[field] !== normalized) registerForm[field] = normalized;
}

// ===== Lifecycle =====
onMounted(() => {
  // Book animation
  window.setTimeout(() => { bookOpening.value = true; }, 400);
  window.setTimeout(() => { bookOpened.value = true; }, 2200);
  window.setTimeout(() => { overlayGone.value = true; }, 2800);
  window.setTimeout(() => {
    contentVisible.value = true;
    void runSequence();
  }, 3200);
});

onUnmounted(() => {
  clearSequenceHandles();
});

const switchTab = async () => { await setTab(activeTab.value === 'login' ? 'register' : 'login'); };

const setTab = async (tab: 'login' | 'register') => {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
  loginForm.password = '';
  registerForm.password = '';
  registerForm.confirmPassword = '';
  await nextTick();
  void runSequence();
};

const handleLogin = async () => {
  if (activeTab.value === 'register') {
    if (!registerForm.username || !registerForm.password || !registerForm.confirmPassword) {
      ElMessage.warning('请填写完整信息'); return;
    }
    if (!/^\d{6}$/.test(registerForm.password)) {
      ElMessage.warning('注册密码必须为 6 位数字'); return;
    }
    if (registerForm.password !== registerForm.confirmPassword) {
      ElMessage.warning('两次密码不一致'); return;
    }
    loading.value = true;
    try {
      await apiRegister(registerForm.username, registerForm.password);
      ElMessage.success('注册成功，请登录');
      activeTab.value = 'login';
      loginForm.username = registerForm.username;
      loginForm.password = '';
      registerForm.password = '';
      registerForm.confirmPassword = '';
      resetSequenceState();
      await nextTick();
      void runSequence();
    } catch (_error: unknown) { /* interceptor handles */ }
    finally { loading.value = false; }
    return;
  }

  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码'); return;
  }
  if (!/^\d{6}$/.test(loginForm.password)) {
    ElMessage.warning('密码为6位数字'); return;
  }
  loading.value = true;
  try {
    const data = await apiLogin(loginForm.username, loginForm.password);
    userStore.login(data);
    ElMessage.success('登录成功');
    router.push('/');
  } catch (_error: unknown) { /* interceptor handles */ }
  finally { loading.value = false; }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.login-page {
  position: relative;
  min-height: 100vh;
  padding: 32px 24px 20px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #ffffff;
}

/* ===== Background ===== */
.desk-glow { position: absolute; pointer-events: none; width: 38vw; height: 38vw; border-radius: 50%; filter: blur(60px); opacity: 0.18; }
.glow-left { left: -10vw; top: -10vh; background: rgba(243,229,189,0.7); }
.glow-right { right: -12vw; bottom: -18vh; background: rgba(116,82,47,0.16); }

.careers-layer { position: absolute; inset: 0; overflow: hidden; z-index: 0; }
.career-drift { position: absolute; white-space: nowrap; will-change: transform; }
.career-drift:hover { animation-play-state: paused; }
.career-word {
  display: inline-block;
  color: rgba(40,36,29,0.72);
  font-family: 'Noto Serif SC','Songti SC','STSong',serif;
  padding: 2px 8px;
  cursor: pointer;
  transition: transform 0.35s cubic-bezier(0.23,1,0.32,1), opacity 0.35s ease, background 0.35s ease, box-shadow 0.35s ease, color 0.35s ease;
  border: 1px solid transparent;
}
.career-drift:hover .career-word {
  transform: scale(1.74);
  opacity: 0.92 !important;
  color: #1f1f1f;
  background: rgba(255,255,255,0.95);
  border-color: rgba(26,26,26,0.08);
  box-shadow: 0 10px 20px rgba(32,32,32,0.08);
}

/* ===== 书本翻开动画 ===== */
.book-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: #f5f0e8;
  display: flex;
  align-items: center;
  justify-content: center;
  perspective: 1800px;
  transition: opacity 0.7s ease;
}
.book-overlay.is-gone { opacity: 0; pointer-events: none; }
.book-3d {
  position: relative;
  width: 640px;
  height: 420px;
  transform-style: preserve-3d;
}
.book-spine {
  position: absolute;
  left: -4px; top: -4px; bottom: -4px;
  width: 12px;
  background: linear-gradient(90deg, #1e1810, #3a2f1f 40%, #2a2018);
  border-radius: 3px 0 0 3px;
  z-index: 12;
  box-shadow: -2px 0 8px rgba(0,0,0,0.2);
}
.book-pages-bg {
  position: absolute;
  inset: 2px 2px 2px 8px;
  display: flex;
  border-radius: 0 3px 3px 0;
  overflow: hidden;
  z-index: 1;
}
.bg-page { flex: 1; background: #fffef8; }
.bg-page-l { border-right: 1px solid rgba(87,64,36,0.06); box-shadow: inset -8px 0 20px rgba(0,0,0,0.03); }
.bg-page-r { box-shadow: inset 8px 0 20px rgba(0,0,0,0.03); }
.book-cover {
  position: absolute;
  inset: 0;
  transform-origin: left center;
  transform-style: preserve-3d;
  transition: transform 1.4s cubic-bezier(0.645,0.045,0.355,1);
  z-index: 10;
}
.book-overlay.is-opening .book-cover { transform: rotateY(-165deg); }
.book-overlay.is-opened .book-cover { transform: rotateY(-180deg); }
.cover-front, .cover-back {
  position: absolute; inset: 0;
  backface-visibility: hidden;
  border-radius: 2px 6px 6px 2px;
}
.cover-front {
  background: linear-gradient(145deg, #3a2f1f 0%, #5a4a32 35%, #4a3c28 65%, #3a2f1f 100%);
  box-shadow: 2px 4px 24px rgba(0,0,0,0.35), inset 0 0 60px rgba(0,0,0,0.15);
  display: flex; align-items: center; justify-content: center;
}
.cover-frame { border: 1px solid rgba(200,180,140,0.18); padding: 48px 56px; text-align: center; }
.cover-watermark { font-family: 'Georgia',serif; font-size: 10px; letter-spacing: 6px; color: rgba(200,180,140,0.25); text-transform: uppercase; }
.cover-title {
  font-family: 'SimSun','Songti SC',serif;
  font-size: 38px; font-weight: 700; letter-spacing: 10px; color: #d4c4a0; margin: 20px 0;
}
.cover-line { width: 64px; height: 1px; background: rgba(200,180,140,0.22); margin: 0 auto 18px; }
.cover-sub { font-family: 'SimSun','Songti SC',serif; font-size: 13px; color: rgba(200,180,140,0.38); letter-spacing: 2px; }
.cover-back { background: linear-gradient(145deg, #f0e8d4, #e8deca); transform: rotateY(180deg); }

/* ===== 两页纸布局 ===== */
.book-shell {
  position: relative;
  z-index: 1;
  width: min(1440px, calc(100vw - 48px));
  min-height: 680px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 6px minmax(440px, 1fr);
  gap: 0;
  align-items: stretch;
  opacity: 0;
  transform: translateY(12px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}
.book-shell.is-visible { opacity: 1; transform: translateY(0); }

/* 书脊 */
.book-spine-visual {
  background: linear-gradient(90deg, rgba(87,64,36,0.12), rgba(87,64,36,0.04), rgba(87,64,36,0.12));
  border-radius: 1px;
  position: relative;
}
.book-spine-visual::before {
  content: '';
  position: absolute;
  top: 16px; bottom: 16px; left: 50%; width: 1px;
  background: rgba(87,64,36,0.1);
  transform: translateX(-50%);
}

/* ===== 统一纸页样式 ===== */
.story-paper, .auth-paper {
  background: var(--paper-bg-solid, #fffef9);
  border: 1px solid var(--paper-border, rgba(87,64,36,0.09));
  box-shadow: var(--paper-shadow-hover, 0 18px 42px rgba(64,45,20,0.06), inset 0 0 0 1px rgba(255,255,255,0.55));
  display: flex;
  flex-direction: column;
  height: 100%;
}

.paper-head {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 22px;
  padding: 0 18px;
  background: rgba(87,64,36,0.03);
  border-bottom: 1px solid rgba(87,64,36,0.06);
  flex-shrink: 0;
}
.paper-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(87,64,36,0.25); }

.paper-body {
  flex: 1;
  padding: 28px 30px 26px;
  position: relative;
  overflow-y: auto;
}
.paper-watermark {
  font-family: 'Georgia',serif;
  font-size: 9px;
  letter-spacing: 4px;
  color: rgba(26,26,26,0.06);
  text-transform: uppercase;
  user-select: none;
}

/* ===== 左页：平台介绍 ===== */
.story-title {
  margin: 8px 0 0;
  font-family: 'Noto Serif SC','Songti SC','STSong',serif;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 5px;
  color: #332113;
  min-height: 42px;
}
.story-divider {
  width: 0;
  height: 2px;
  margin: 18px 0 16px;
  background: linear-gradient(90deg, rgba(84,55,23,0.7), rgba(152,119,79,0.18));
  transition: width 0.35s cubic-bezier(0.23,1,0.32,1);
}
.story-divider.drawn { width: 100%; }
.story-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: rgba(79,57,31,0.72);
}
.story-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin: 22px 0;
}
.story-metric {
  padding: 14px 12px;
  border: 1px solid rgba(87,64,36,0.08);
  background: rgba(255,255,255,0.54);
  text-align: center;
}
.metric-val {
  display: block;
  font-family: 'Cormorant Garamond','Noto Serif SC',serif;
  font-size: 26px;
  line-height: 1;
  color: #3b2412;
}
.metric-label {
  display: block;
  margin-top: 8px;
  font-size: 11px;
  letter-spacing: 1px;
  color: rgba(79,57,31,0.72);
}
.story-features {
  display: grid;
  gap: 12px;
  margin: 0 0 22px;
}
.feature-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  align-items: start;
  padding: 12px 14px;
  border: 1px solid rgba(87,64,36,0.08);
  background: rgba(255,255,255,0.54);
}
.feature-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px; height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid rgba(87,64,36,0.12);
  background: rgba(255,255,255,0.7);
  font-size: 11px;
  letter-spacing: 1.2px;
  color: rgba(83,57,28,0.72);
}
.feature-title { margin: 0 0 4px; font-size: 14px; font-weight: 700; color: #3e2816; }
.feature-desc { margin: 0; font-size: 12px; line-height: 1.8; color: rgba(79,57,31,0.72); }

/* ===== 打印机流 ===== */
.printer-feed {
  padding: 14px 16px 12px;
  border: 1px solid rgba(87,64,36,0.1);
  background: rgba(255,255,255,0.58);
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.42);
  opacity: 0;
  transform: translateY(10px);
  transition: opacity 0.32s ease, transform 0.32s ease;
}
.printer-feed.visible { opacity: 1; transform: translateY(0); }
.printer-feed-head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.printer-feed-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(103,73,37,0.34); }
.printer-feed-label { margin-left: 4px; font-size: 11px; letter-spacing: 1.4px; color: rgba(86,58,29,0.58); }
.printer-feed-body { display: flex; flex-direction: column; gap: 8px; }
.printer-line { margin: 0; min-height: 20px; font-size: 12px; line-height: 1.7; color: rgba(74,53,30,0.76); }

/* ===== 右页：登录/注册 ===== */
.auth-content {
  position: relative;
  padding-top: 18px;
}
.auth-content::before {
  content: '';
  position: absolute;
  inset: 8px 0 0;
  background: repeating-linear-gradient(180deg, transparent 0 44px, rgba(106,75,40,0.05) 44px 45px);
  opacity: 0.34;
  pointer-events: none;
}
.auth-content > * { position: relative; z-index: 1; }

.auth-topline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(87,64,36,0.08);
}
.folio-meta { display: flex; align-items: center; gap: 10px; color: rgba(75,49,25,0.7); font-size: 12px; letter-spacing: 1.8px; }
.folio-label, .folio-no { font-family: 'Cormorant Garamond','Noto Serif SC',serif; }
.folio-sep { width: 28px; height: 1px; background: rgba(86,58,28,0.24); }

.mode-switch {
  display: inline-flex;
  padding: 4px; gap: 4px;
  border: 1px solid rgba(87,64,36,0.1);
  background: rgba(255,255,255,0.54);
}
.mode-tab {
  min-width: 72px; padding: 8px 14px;
  border: none; background: transparent;
  font-size: 12px; letter-spacing: 1.2px;
  color: rgba(83,57,28,0.72);
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}
.mode-tab.active { background: rgba(255,255,255,0.86); color: #321f11; }

.paper-heading { display: flex; flex-direction: column; gap: 8px; }
.eyebrow { margin: 0; font-family: 'Cormorant Garamond','Noto Serif SC',serif; font-size: 18px; letter-spacing: 2.4px; color: rgba(91,60,27,0.56); }
.main-title {
  margin: 0; min-height: 56px;
  font-family: 'Noto Serif SC','Songti SC','STSong',serif;
  font-size: 36px; font-weight: 700; line-height: 1.35; letter-spacing: 8px; color: #3b2412;
}
.cursor { margin-left: 1px; font-weight: 300; color: #5f3418; animation: blink 0.45s step-end infinite; }

.divider {
  width: 0; height: 2px;
  margin: 24px 0 20px;
  background: linear-gradient(90deg, rgba(84,55,23,0.7), rgba(152,119,79,0.18));
  transition: width 0.35s cubic-bezier(0.23,1,0.32,1);
}
.divider.drawn { width: 100%; }

.subhead { display: flex; flex-direction: column; gap: 8px; }
.sub-title { margin: 0; min-height: 38px; font-size: 28px; font-weight: 700; letter-spacing: 5px; color: #4a2c15; }
.sub-copy { margin: 0; font-size: 14px; line-height: 1.9; color: rgba(74,53,30,0.74); }

.process-ribbon { display: flex; flex-wrap: wrap; gap: 10px; margin: 22px 0 30px; }
.ribbon-item {
  position: relative; padding: 8px 14px 8px 16px;
  background: rgba(255,255,255,0.54);
  border: 1px solid rgba(87,64,36,0.1);
  color: rgba(80,55,29,0.82);
  font-size: 12px; letter-spacing: 1.1px;
}
.ribbon-item::before {
  content: ''; position: absolute; left: 8px; top: 50%;
  width: 4px; height: 4px; border-radius: 50%; background: rgba(92,60,26,0.42); transform: translateY(-50%);
}

.form { opacity: 0; transform: translateY(12px); transition: opacity 0.28s ease, transform 0.28s ease; }
.form.visible { opacity: 1; transform: translateY(0); }

.field { margin-bottom: 28px; }
.field-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 10px; }
.field-label { min-height: 24px; font-size: 18px; letter-spacing: 2px; color: #3f2815; }
.field-tag { font-size: 11px; letter-spacing: 1.2px; color: rgba(98,69,38,0.66); padding: 4px 10px; border-radius: 999px; border: 1px solid rgba(87,64,36,0.1); background: rgba(255,255,255,0.54); }
.field-line {
  position: relative;
  border-bottom: 1px solid rgba(117,86,48,0.34);
  transition: border-color 0.2s ease;
}
.field-line::before {
  content: ''; position: absolute; left: 0; bottom: -1px;
  width: 0; height: 2px;
  background: linear-gradient(90deg, rgba(92,57,22,0.9), rgba(184,148,102,0.35));
  transition: width 0.28s cubic-bezier(0.23,1,0.32,1);
}
.field-line:focus-within { border-bottom-color: rgba(92,57,22,0.6); }
.field-line:focus-within::before { width: 100%; }
.field-input {
  width: 100%; padding: 12px 0 11px;
  border: none; outline: none; background: transparent;
  color: #2f1c0f;
  font-family: 'Noto Serif SC','Songti SC','STSong',serif;
  font-size: 18px; caret-color: #5a3317;
}
.field-input::placeholder { color: rgba(103,74,44,0.42); font-size: 15px; }
.field-meta { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-top: 8px; }
.field-helper, .field-counter { font-size: 12px; color: rgba(90,62,34,0.6); }
.field-counter { letter-spacing: 1px; }

.bottom-area {
  margin-top: 8px; padding-top: 20px;
  display: flex; flex-direction: column; align-items: flex-start; gap: 16px;
  opacity: 0; transition: opacity 0.5s ease;
}
.bottom-area.visible { opacity: 1; }

.action-line {
  display: inline-flex; align-items: center; gap: 14px;
  padding: 10px 0; border: none; background: none; cursor: pointer;
  color: #311d10; transition: transform 0.2s ease, opacity 0.2s ease;
}
.action-line:hover { transform: translateY(-1px); opacity: 0.82; }
.action-line:active { opacity: 0.68; }
.action-line:disabled { cursor: not-allowed; opacity: 0.4; transform: none; }
.action-dash { width: 72px; height: 1px; background: linear-gradient(90deg, rgba(100,68,35,0.22), rgba(69,42,17,0.72), rgba(100,68,35,0.22)); }
.action-text { font-size: 16px; font-weight: 700; letter-spacing: 5px; }
.switch-link {
  padding: 0; border: none; background: none; cursor: pointer;
  font-size: 13px; color: rgba(73,47,24,0.86); letter-spacing: 1px;
}
.switch-link:hover { opacity: 0.65; text-decoration: underline; text-underline-offset: 3px; }
.footer-tip { margin: 0; max-width: 360px; font-size: 12px; line-height: 1.7; color: rgba(97,68,36,0.62); }

.footer { position: relative; z-index: 1; margin-top: 14px; font-size: 11px; letter-spacing: 0.6px; color: rgba(86,61,35,0.48); }

/* ===== Responsive ===== */
@media (max-width: 1100px) {
  .book-shell { grid-template-columns: minmax(0, 1fr) 4px minmax(360px, 1fr); }
  .story-metrics { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 900px) {
  .login-page { padding: 18px 10px 16px; }
  .book-3d { width: 520px; height: 340px; }
  .cover-title { font-size: 30px; letter-spacing: 7px; }
  .book-shell {
    grid-template-columns: 1fr;
    gap: 20px;
    width: min(640px, calc(100vw - 24px));
  }
  .book-spine-visual { display: none; }
  .main-title { font-size: 32px; letter-spacing: 6px; }
}
@media (max-width: 600px) {
  .story-metrics { grid-template-columns: repeat(2, 1fr); }
  .story-title { font-size: 22px; letter-spacing: 3px; }
  .main-title { font-size: 28px; letter-spacing: 4px; }
  .sub-title { font-size: 22px; letter-spacing: 3px; }
  .field-label, .field-input { font-size: 16px; }
  .action-text { font-size: 14px; letter-spacing: 3px; }
  .action-dash { width: 44px; }
  .book-3d { width: 420px; height: 280px; }
  .cover-title { font-size: 26px; letter-spacing: 6px; }
  .cover-frame { padding: 28px 32px; }
}
</style>

<style>
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
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

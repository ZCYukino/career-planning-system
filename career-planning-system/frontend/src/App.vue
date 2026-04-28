<template>
  <el-container class="layout-container">
    <el-header v-if="userStore.isLoggedIn" class="header">
      <div class="header-inner">
        <div class="logo" @click="router.push('/')">
          <IconEpDataAnalysis class="logo-icon" />
          <span class="logo-text">职业规划智能体</span>
        </div>
        <el-menu mode="horizontal" router :default-active="route.path" class="menu">
          <el-menu-item index="/">
            <IconEpHomeFilled class="menu-icon" />
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/jobs">
            <IconEpBriefcase class="menu-icon" />
            <span>岗位分析</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <IconEpUser class="menu-icon" />
            <span>能力画像</span>
          </el-menu-item>
          <el-menu-item index="/report">
            <IconEpDocument class="menu-icon" />
            <span>职业报告</span>
          </el-menu-item>
        </el-menu>
        <div class="user-info">
          <el-dropdown trigger="click">
            <span class="user-dropdown">
              <el-avatar :size="32" class="user-avatar">
                {{ (userStore.userInfo?.name || userStore.userInfo?.username || '?')[0] }}
              </el-avatar>
              <span class="user-name-box">
                <span class="user-entry-label">档案</span>
                <span class="user-name">{{ userStore.userInfo?.name || userStore.userInfo?.username }}</span>
              </span>
              <IconEpArrowDown class="dropdown-arrow" />
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/account')">
                  <IconEpSetting class="dropdown-item-icon" />账号管理
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/profile')">
                  <IconEpUser class="dropdown-item-icon" />用户信息
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <IconEpSwitchButton class="dropdown-item-icon" />退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <el-main class="main-content">
      <div class="book-wrapper">
        <router-view v-slot="{ Component, route }">
          <component :is="Component" :key="route.path" />
        </router-view>
        <!-- 翻页遮罩：单个 div，极轻量，做 3D 翻转不卡顿 -->
        <div
          v-if="flipVisible"
          class="flip-page"
          :class="[flipPhase, flipDir]"
          @animationend="onFlipEnd"
        ></div>
      </div>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user';
import { useRoute, useRouter } from 'vue-router';

const userStore = useUserStore();
const router = useRouter();
const route = useRoute();

userStore.init();

const flipVisible = ref(false);
const flipPhase = ref('');      // 'flip-lift' | 'flip-settle'
const flipDir = ref('forward'); // 'forward' | 'backward'

router.beforeEach((to, from) => {
  if (!from.name) return; // 首次加载不翻页

  const toDepth = (to.meta.depth as number) ?? 0;
  const fromDepth = (from.meta.depth as number) ?? 0;
  flipDir.value = toDepth >= fromDepth ? 'forward' : 'backward';

  // 显示遮罩，用 lift 动画盖住旧页面
  flipVisible.value = true;
  flipPhase.value = 'flip-lift';
});

router.afterEach(() => {
  // 新页面已渲染，遮罩用 settle 动画翻走露出新页面
  nextTick(() => {
    flipPhase.value = 'flip-settle';
  });
});

function onFlipEnd() {
  if (flipPhase.value === 'flip-settle') {
    flipVisible.value = false;
    flipPhase.value = '';
  }
}

const handleLogout = () => {
  userStore.logout();
  router.push('/login');
};
</script>

<style>
/* ===== 钢笔光标 — 输入框内显示钢笔 SVG ===== */
:root {
  --pen-cursor: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none'%3E%3Cpath d='M3 21l1.5-4.5L17.3 3.7a1 1 0 011.4 0l1.6 1.6a1 1 0 010 1.4L7.5 19.5z' fill='%231a1a1a' opacity='0.7'/%3E%3Cpath d='M17 5l2 2' stroke='%231a1a1a' stroke-width='1.2' opacity='0.5'/%3E%3Cpath d='M3 21l2.5-1.5-1-1z' fill='%231a1a1a' opacity='0.4'/%3E%3C/svg%3E") 2 14, text;
}

/* 原生 input / textarea */
input:not([type="radio"]):not([type="checkbox"]):not([type="file"]):not([type="hidden"]):not([type="submit"]):not([type="button"]),
textarea {
  cursor: var(--pen-cursor) !important;
  caret-color: #1a1a1a;
}

/* Element Plus 输入框 */
.el-input__inner,
.el-textarea__inner,
.el-input textarea {
  cursor: var(--pen-cursor) !important;
  caret-color: #1a1a1a;
}

/* 可编辑区域 */
[contenteditable="true"] {
  cursor: var(--pen-cursor) !important;
  caret-color: #1a1a1a;
}

body {
  margin: 0;
  font-family: 'Inter', 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  background-color: var(--color-bg-page);
  -webkit-font-smoothing: antialiased;
}
.layout-container {
  min-height: 100vh;
}
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 56px !important;
  padding: 0;
  background: var(--paper-bg-solid);
  border-bottom: 1px solid rgba(87, 64, 36, 0.15);
  box-shadow:
    0 1px 2px rgba(64, 45, 20, 0.05),
    0 4px 12px rgba(64, 45, 20, 0.03);
}

/* 纸张纹理覆盖层 — 模拟信纸质感 */
.header::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      0deg,
      transparent,
      transparent 6px,
      rgba(87, 64, 36, 0.012) 6px,
      rgba(87, 64, 36, 0.012) 7px
    );
  pointer-events: none;
  z-index: 1;
}

/* 底部装饰线 — 双线信纸边框 */
.header::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(
    to right,
    transparent 5%,
    rgba(87, 64, 36, 0.06) 15%,
    rgba(87, 64, 36, 0.06) 85%,
    transparent 95%
  );
  pointer-events: none;
  z-index: 1;
}
.header-inner {
  display: flex;
  align-items: center;
  height: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0;
  position: relative;
  z-index: 2;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
  padding: 5px 14px;
  margin-left: 24px;
  margin-right: 32px;
  background: var(--paper-bg-solid);
  border: 1px solid rgba(87, 64, 36, 0.12);
  border-radius: 2px;
  box-shadow: 0 1px 3px rgba(64, 45, 20, 0.06);
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}
.logo:hover {
  box-shadow: 0 3px 8px rgba(64, 45, 20, 0.10);
  border-color: rgba(87, 64, 36, 0.20);
}
.logo-icon {
  font-size: 20px;
  color: #1a1a1a;
}
.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: #3b2412;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  letter-spacing: 2px;
  white-space: nowrap;
  background: none;
  -webkit-text-fill-color: initial;
}
.menu {
  flex: 1;
  margin: 0 24px;
  border-bottom: none !important;
  background: transparent !important;
  /* Element Plus 菜单变量覆盖 — 必须设在 .el-menu 层级 */
  --el-menu-active-color: #3b2412;
  --el-menu-hover-text-color: #3b2412;
  --el-menu-hover-bg-color: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #5a5a5a;
  --el-menu-item-font-size: 14px;
  --el-menu-item-height: 56px;
  --el-menu-horizontal-submenu-height: 56px;
}

/* ===== 菜单项 — 纸张标签页风格 ===== */
.menu :deep(.el-menu-item) {
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 14px;
  letter-spacing: 1px;
  color: #5a5a5a !important;
  border-bottom: none !important;
  position: relative;
  margin: 0 2px;
  padding: 0 16px !important;
  height: 40px !important;
  line-height: 40px !important;
  margin-top: 8px;
  border-radius: 2px 2px 0 0;
  border: 1px solid transparent;
  border-bottom: none;
  transition: all 0.22s ease;
  z-index: 2;
}

/* 默认态：极淡底纹，营造纸张质感 */
.menu :deep(.el-menu-item:not(.is-active)) {
  background: rgba(87, 64, 36, 0.02) !important;
  border-color: transparent;
}

/* 悬停态：纸张微微抬起，边框浮现 */
.menu :deep(.el-menu-item:hover) {
  color: #3b2412 !important;
  background: rgba(87, 64, 36, 0.05) !important;
  border-color: rgba(87, 64, 36, 0.10);
  border-bottom: none;
  box-shadow: 0 -1px 3px rgba(64, 45, 20, 0.04);
}

/* 激活态：像一张突出的纸张标签 */
.menu :deep(.el-menu-item.is-active) {
  color: #3b2412 !important;
  font-weight: 600;
  background: #fdfcfb !important;
  border-color: rgba(87, 64, 36, 0.12) !important;
  border-bottom: 1px solid #fdfcfb !important;
  box-shadow:
    0 -1px 4px rgba(64, 45, 20, 0.06),
    -1px 0 3px rgba(64, 45, 20, 0.03),
    1px 0 3px rgba(64, 45, 20, 0.03) !important;
}

/* 激活态下方的墨迹下划线 */
.menu :deep(.el-menu-item.is-active)::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  background: #3b2412;
  z-index: 3;
}

/* 彻底消除 Element Plus 所有蓝色 ripple / focus 效果 */
.menu :deep(.el-menu-item:focus),
.menu :deep(.el-menu-item:active) {
  background: inherit !important;
  outline: none !important;
  color: inherit !important;
}
.menu :deep(.el-menu-item *) {
  transition: none !important;
}
.menu-icon {
  font-size: 15px;
  margin-right: 3px;
  transition: transform 0.2s ease;
}
.menu :deep(.el-menu-item:hover) .menu-icon {
  transform: scale(1.08);
}
.menu :deep(.el-menu-item.is-active) .menu-icon {
  color: #3b2412;
}
.user-info {
  flex-shrink: 0;
}
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 2px;
  border: 1px solid rgba(87, 64, 36, 0.14);
  background: var(--paper-bg-solid);
  box-shadow: 0 1px 4px rgba(64, 45, 20, 0.06);
  transition: background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}
.user-dropdown:hover {
  background: #f5efe3;
  border-color: rgba(87, 64, 36, 0.22);
  box-shadow: 0 3px 8px rgba(64, 45, 20, 0.10);
}
.user-avatar {
  background: none !important;
  border: 1.5px solid rgba(87, 64, 36, 0.20);
  border-radius: 2px !important;
  color: #3b2412 !important;
  font-size: 14px;
  font-weight: 700;
  font-family: 'SimSun', 'Songti SC', serif;
}
.user-name-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px;
  border: 1px solid rgba(87, 64, 36, 0.12);
  background: rgba(87, 64, 36, 0.03);
}
.user-entry-label {
  font-size: 11px;
  letter-spacing: 1px;
  color: rgba(87, 64, 36, 0.55);
  padding-right: 8px;
  border-right: 1px solid rgba(87, 64, 36, 0.12);
  font-family: 'SimSun', 'Songti SC', serif;
}
.user-name {
  font-size: 13px;
  font-weight: 500;
  color: #3b2412;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: 'SimSun', 'Songti SC', serif;
}
.dropdown-arrow {
  font-size: 12px;
  color: #8a7560;
  transition: transform 0.2s ease;
}
.dropdown-item-icon {
  margin-right: 6px;
}

/* ===== Layout ===== */
.main-content {
  padding: 0;
  min-height: calc(100vh - 56px);
}
.book-wrapper {
  position: relative;
  perspective: 1800px;
  min-height: calc(100vh - 56px);
}

/* ===== 翻页遮罩 — 单个 div，模拟纸张翻转 ===== */
.flip-page {
  position: absolute;
  inset: 0;
  z-index: 200;
  transform-origin: left center;
  backface-visibility: hidden;
  /* 纸张外观 */
  background:
    linear-gradient(to right,
      rgba(0,0,0,0.03) 0%,
      transparent 4%,
      transparent 92%,
      rgba(0,0,0,0.02) 100%
    ),
    #fdfcfb;
  box-shadow: inset 0 0 60px rgba(0,0,0,0.03);
  pointer-events: none;
  will-change: transform;
}

/* 纸张纹理：细横线模拟纸张纤维 */
.flip-page::after {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 28px,
    rgba(0,0,0,0.012) 28px,
    rgba(0,0,0,0.012) 29px
  );
  pointer-events: none;
}

/* ===== Phase 1: Lift — 遮罩翻起来盖住旧页面 ===== */
.flip-page.forward.flip-lift {
  animation: lift-forward 0.32s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}
.flip-page.backward.flip-lift {
  animation: lift-backward 0.32s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}

/* ===== Phase 2: Settle — 新页已就位，遮罩翻走 ===== */
.flip-page.forward.flip-settle {
  animation: settle-forward 0.42s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}
.flip-page.backward.flip-settle {
  animation: settle-backward 0.42s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

/* ——— Forward 关键帧 ——— */

/* Lift: 从右边缘掀起，纸面抬起弯曲 */
@keyframes lift-forward {
  0%   { transform: rotateY(0deg); }
  30%  { transform: rotateY(-6deg) translateZ(8px); }
  60%  { transform: rotateY(-18deg) translateZ(16px) skewY(0.4deg); }
  100% { transform: rotateY(-40deg) translateZ(20px) skewY(-0.3deg); }
}

/* Settle: 纸张翻过去消失 */
@keyframes settle-forward {
  0%   { transform: rotateY(-40deg) translateZ(20px) skewY(-0.3deg);
         box-shadow: 12px 4px 32px rgba(0,0,0,0.14); }
  20%  { transform: rotateY(-72deg) translateZ(14px) skewY(0.3deg); }
  45%  { transform: rotateY(-110deg) translateZ(8px) skewY(-0.2deg); }
  70%  { transform: rotateY(-148deg) translateZ(3px); }
  90%  { transform: rotateY(-174deg) translateZ(0px); }
  100% { transform: rotateY(-180deg); }
}

/* ——— Backward 关键帧 ——— */

/* Lift: 纸张从完全翻过的位置开始翻回来 */
@keyframes lift-backward {
  0%   { transform: rotateY(-180deg); }
  30%  { transform: rotateY(-170deg) translateZ(8px); }
  60%  { transform: rotateY(-155deg) translateZ(16px) skewY(-0.4deg); }
  100% { transform: rotateY(-130deg) translateZ(20px) skewY(0.3deg); }
}

/* Settle: 纸张盖回去 */
@keyframes settle-backward {
  0%   { transform: rotateY(-130deg) translateZ(20px) skewY(0.3deg);
         box-shadow: -8px 4px 28px rgba(0,0,0,0.12); }
  20%  { transform: rotateY(-100deg) translateZ(14px) skewY(-0.3deg); }
  45%  { transform: rotateY(-62deg) translateZ(8px) skewY(0.2deg); }
  70%  { transform: rotateY(-28deg) translateZ(3px); }
  90%  { transform: rotateY(-6deg) translateZ(0px); }
  100% { transform: rotateY(0deg); }
}

/* ============================================================
   墨迹标记 — 交互元素设计系统
   核心思路：用纸张自身的语言（墨底、笔线、书签边、墨蓝）区分可点击元素
   使用 .book-wrapper 前缀确保优先级高于各页面局部样式
   ============================================================ */

/* ── 1. 主操作按钮 .action-line ──────────────────────────────
   纸张上贴的书签标签：浅墨底 + 细笔边框 + 左侧粗线标记 */
.book-wrapper .action-line {
  background: rgba(26, 26, 26, 0.03) !important;
  border: 1px solid rgba(26, 26, 26, 0.12) !important;
  border-left: 3px solid rgba(26, 26, 26, 0.18) !important;
  border-radius: 2px !important;
  padding: 10px 24px !important;
  transition: all 0.22s ease !important;
  opacity: 1 !important;
}
.book-wrapper .action-line:hover {
  background: rgba(26, 26, 26, 0.07) !important;
  border-color: rgba(26, 26, 26, 0.22) !important;
  border-left-color: rgba(26, 26, 26, 0.35) !important;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  opacity: 1 !important;
}
.book-wrapper .action-line:active {
  background: rgba(26, 26, 26, 0.10) !important;
  transform: translateY(0);
  box-shadow: none;
  opacity: 1 !important;
}

/* ── 2. 次级/小按钮 ────────────────────────────────────────
   虚线边框，更轻盈 */
.book-wrapper .action-line.secondary {
  background: none !important;
  border: 1px dashed rgba(26, 26, 26, 0.14) !important;
  border-left: 2px solid rgba(26, 26, 26, 0.14) !important;
  padding: 7px 16px !important;
  border-radius: 2px !important;
}
.book-wrapper .action-line.secondary:hover {
  background: rgba(26, 26, 26, 0.03) !important;
  border-left-color: rgba(26, 26, 26, 0.28) !important;
  opacity: 1 !important;
}

/* ── 3. 墨蓝文字链接 ───────────────────────────────────────
   #2a3a5c 模拟钢笔墨蓝色，与正文纯黑拉开距离 */
.book-wrapper .card-link,
.book-wrapper .text-btn,
.book-wrapper .paper-link,
.book-wrapper .switch-link,
.book-wrapper .header-link {
  color: #2a3a5c !important;
  border-bottom-color: rgba(42, 58, 92, 0.25) !important;
  text-decoration: none !important;
  transition: all 0.2s ease !important;
  opacity: 1 !important;
}
.book-wrapper .card-link:hover,
.book-wrapper .text-btn:hover,
.book-wrapper .paper-link:hover,
.book-wrapper .switch-link:hover,
.book-wrapper .header-link:hover {
  color: #1a2a4a !important;
  border-bottom-color: rgba(42, 58, 92, 0.55) !important;
  opacity: 1 !important;
}

/* ── 4. 标签页 .paper-tab ───────────────────────────────────
   提升未选中态可见度（0.5 → 0.65） */
.book-wrapper .paper-tab {
  opacity: 0.65 !important;
  transition: all 0.2s ease !important;
}
.book-wrapper .paper-tab:hover {
  opacity: 0.9 !important;
  border-bottom-color: rgba(26, 26, 26, 0.2) !important;
}
.book-wrapper .paper-tab.active {
  opacity: 1 !important;
}

/* ── 5. 可点击卡片 .name-card ──────────────────────────────
   右下角折角暗示 + 增强默认阴影 */
.book-wrapper .name-card {
  border: 1px solid rgba(26, 26, 26, 0.06);
}
.book-wrapper .name-card::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: 18px;
  height: 18px;
  background: linear-gradient(135deg, transparent 50%, rgba(26, 26, 26, 0.04) 50%);
  transition: all 0.2s ease;
}
.book-wrapper .name-card:hover {
  border-color: rgba(26, 26, 26, 0.10);
}
.book-wrapper .name-card:hover::after {
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, transparent 50%, rgba(26, 26, 26, 0.07) 50%);
}

/* ── 6. 列表可点击项 .job-item ─────────────────────────────
   hover 时右侧出现 → 箭头 */
.book-wrapper .job-item {
  position: relative;
  padding-right: 24px !important;
  transition: all 0.2s ease !important;
}
.book-wrapper .job-item::after {
  content: '→';
  position: absolute;
  right: 6px;
  font-size: 12px;
  color: rgba(42, 58, 92, 0.4);
  opacity: 0;
  transition: all 0.2s ease;
}
.book-wrapper .job-item:hover {
  background: rgba(42, 58, 92, 0.03) !important;
}
.book-wrapper .job-item:hover::after {
  opacity: 1;
}

/* ── 7. 返回导航 .nav-back ──────────────────────────────────
   hover 下划线 */
.book-wrapper .nav-back {
  transition: all 0.2s ease !important;
}
.book-wrapper .nav-back:hover {
  text-decoration: underline !important;
  text-underline-offset: 3px !important;
  color: #2a3a5c !important;
}

/* ===== Global Design System Utilities ===== */

/* ── Skeleton / Shimmer Animation ────────────────────────── */
@keyframes skeleton-shimmer {
  0% { background-position: -200px 0; }
  100% { background-position: calc(200px + 100%) 0; }
}
.book-wrapper .skeleton-block {
  background: linear-gradient(90deg, #f5f0e8 25%, #ede8df 50%, #f5f0e8 75%);
  background-size: 400px 100%;
  animation: skeleton-shimmer 1.5s ease infinite;
  border-radius: 2px;
}

/* ── Paper-Rise Animation ────────────────────────────────── */
@keyframes paper-rise {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ── Section Title ───────────────────────────────────────── */
.book-wrapper .section-title {
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  padding-left: 12px;
  border-left: 3px solid rgba(26, 26, 26, 0.5);
  letter-spacing: 2px;
  margin: 24px 0 16px 0;
}

/* ── Section Divider ─────────────────────────────────────── */
.book-wrapper .section-divider {
  height: 1px;
  background: linear-gradient(to right, rgba(26, 26, 26, 0.12), rgba(26, 26, 26, 0.03), transparent);
  margin: 20px 0;
  border: none;
}

/* ── Empty State ─────────────────────────────────────────── */
.book-wrapper .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #8a8a8a;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}
.book-wrapper .empty-state-icon {
  width: 64px;
  height: 80px;
  border: 1.5px solid rgba(26, 26, 26, 0.12);
  border-radius: 2px;
  position: relative;
  margin-bottom: 16px;
}
.book-wrapper .empty-state-icon::before {
  content: '';
  position: absolute;
  top: 8px;
  left: 8px;
  right: 8px;
  height: 1px;
  background: rgba(26, 26, 26, 0.08);
  box-shadow: 0 8px 0 rgba(26, 26, 26, 0.08), 0 16px 0 rgba(26, 26, 26, 0.08), 0 24px 0 rgba(26, 26, 26, 0.08);
}
.book-wrapper .empty-state-icon::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 12px;
  height: 12px;
  background: linear-gradient(135deg, #faf9f7 50%, rgba(26,26,26,0.06) 50%);
}
.book-wrapper .empty-state-text {
  font-size: 14px;
  letter-spacing: 1px;
}

/* ── Paper Tag ───────────────────────────────────────────── */
.book-wrapper .paper-tag {
  display: inline-block;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 12px;
  color: #5a5a5a;
  padding: 3px 10px;
  border: 1px solid rgba(26, 26, 26, 0.08);
  border-radius: 2px;
  background: rgba(26, 26, 26, 0.02);
  letter-spacing: 0.5px;
}

/* ── Element Plus Menu 纸张风格覆盖 — 消除默认蓝色 ── */
.menu :deep(.el-menu-item) {
  --el-menu-active-color: #3b2412;
  --el-menu-hover-bg-color: transparent;
  --el-menu-hover-text-color: #3b2412;
}
.menu :deep(.el-menu-item:focus),
.menu :deep(.el-menu-item:active) {
  background: transparent !important;
  outline: none;
}

/* 消除 el-menu 底部蓝色下划线 */
.menu > :deep(.el-menu--horizontal > .el-menu-item.is-active),
.menu :deep(.el-menu-item.is-active) {
  border-bottom-color: transparent !important;
}

/* 全局消除 Element Plus focus 蓝色 outline */
.el-menu-item:focus,
.el-menu-item:active,
.el-menu-item.is-active {
  outline: none !important;
  box-shadow: none !important;
  -webkit-tap-highlight-color: transparent;
}

/* ── Element Plus Dropdown Paper Override ─────────────────── */
.el-dropdown-menu {
  background: #fdfcfb !important;
  border: 1px solid rgba(26, 26, 26, 0.10) !important;
  border-radius: 2px !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06) !important;
  padding: 4px 0 !important;
}
.el-dropdown-menu__item {
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif !important;
  font-size: 13px !important;
  color: #1a1a1a !important;
  letter-spacing: 0.5px !important;
}
.el-dropdown-menu__item:hover {
  background: rgba(26, 26, 26, 0.03) !important;
  color: #1a1a1a !important;
}
</style>

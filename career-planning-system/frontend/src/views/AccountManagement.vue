<template>
  <div class="account-page">
    <!-- 浮动职业名称 -->
    <div class="careers-layer">
      <div v-for="(item, i) in careers" :key="i" class="career-drift" :style="item.driftStyle">
        <span class="career-word" :style="item.wordStyle">{{ item.name }}</span>
      </div>
    </div>

    <!-- 居中纸张容器 -->
    <div class="account-center">
      <div class="paper">
        <svg class="icon-deco" style="bottom:18px;right:16px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
        </svg>

        <div class="paper-content">
          <h3 class="section-title section-title--accent">账号管理</h3>
          <div class="divider"></div>

          <el-form :model="accountForm" :rules="accountRules" ref="accountFormRef" label-width="72px" class="paper-form">
            <el-form-item label="用户名">
              <el-input v-model="accountForm.username" disabled />
            </el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="accountForm.gender">
                <el-radio value="男">男</el-radio>
                <el-radio value="女">女</el-radio>
                <el-radio value="">未知</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="accountForm.email" placeholder="请输入邮箱" clearable />
            </el-form-item>
            <div class="form-actions">
              <button type="button" class="action-line" @click="handleUpdateAccount" :disabled="savingAccount">
                <span class="action-dash"></span>
                <span class="action-text">{{ savingAccount ? '保存中...' : '保存修改' }}</span>
                <span class="action-dash"></span>
              </button>
              <button type="button" class="action-line secondary" @click="openPasswordDialog">
                <span class="action-text-sm">修改密码</span>
              </button>
            </div>
          </el-form>

          <div class="section-wrapper stagger-item" :style="{ animationDelay: '0.1s' }">
            <h3 class="section-title section-title--accent" style="margin-top:36px">安全设置</h3>
            <div class="divider"></div>
          </div>

          <div class="security-info">
            <div class="security-item">
              <span class="security-label">登录密码</span>
              <span class="security-value">已设置（6位数字密码）</span>
              <button type="button" class="action-line secondary" @click="openPasswordDialog">
                <span class="action-text-sm">修改</span>
              </button>
            </div>
            <div class="security-item">
              <span class="security-label">注册时间</span>
              <span class="security-value">{{ accountForm.username ? '已注册' : '未知' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <teleport to="body">
      <div class="overlay" v-if="passwordDialogVisible" @click.self="passwordDialogVisible = false">
        <div class="password-paper">
          <h3 class="section-title">修改密码</h3>
          <div class="divider"></div>
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="96px" class="paper-form">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码（6位）" maxlength="6" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码（6位）" maxlength="6" />
            </el-form-item>
          </el-form>
          <div class="form-actions" style="justify-content:flex-end;gap:16px">
            <button class="action-line secondary" @click="passwordDialogVisible = false">
              <span class="action-text-sm">取消</span>
            </button>
            <button class="action-line" @click="handleChangePassword" :disabled="changingPassword">
              <span class="action-dash"></span>
              <span class="action-text">{{ changingPassword ? '修改中...' : '确认修改' }}</span>
              <span class="action-dash"></span>
            </button>
          </div>
        </div>
      </div>
    </teleport>

    <p class="footer">&copy; 2026 职业规划智能体</p>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { getUserInfo, updateUserInfo, changePassword as apiChangePassword } from '@/api/auth';
import { ElMessage, FormInstance, FormRules } from 'element-plus';

const router = useRouter();
const userStore = useUserStore();

// 职业名称飘动
const careerNames = [
  '前端工程师','后端工程师','全栈工程师','算法工程师','机器学习工程师','数据工程师','数据分析师','数据科学家','人工智能工程师','深度学习工程师',
  'NLP工程师','计算机视觉工程师','大模型工程师','提示词工程师','搜索工程师','推荐算法工程师','DevOps工程师','SRE工程师','云计算工程师','云原生工程师',
  '系统架构师','解决方案架构师','软件架构师','嵌入式工程师','测试开发工程师','自动化测试工程师','QA工程师','数据库工程师','DBA','产品经理',
];

const careers = (() => {
  const items: Array<{name: string; driftStyle: Record<string,string>; wordStyle: Record<string,string|number>}> = [];
  for (let repeat = 0; repeat < 4; repeat++) {
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

// 账号表单
const accountFormRef = ref<FormInstance>();
const savingAccount = ref(false);
const accountForm = reactive({ username: '', gender: '', email: '' });
const accountRules: FormRules = {
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }]
};

// 密码修改表单
const passwordFormRef = ref<FormInstance>();
const passwordDialogVisible = ref(false);
const changingPassword = ref(false);
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { len: 6, message: '新密码长度必须为6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

const handleUpdateAccount = async () => {
  try {
    await accountFormRef.value?.validate();
  } catch { return; }
  savingAccount.value = true;
  try {
    await updateUserInfo(accountForm.gender, accountForm.email);
    userStore.setUserInfo({ ...userStore.userInfo, gender: accountForm.gender, email: accountForm.email });
    ElMessage.success('账号信息更新成功');
  } catch (e: any) {
  } finally {
    savingAccount.value = false;
  }
};

const openPasswordDialog = () => {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  passwordFormRef.value?.clearValidate();
  passwordDialogVisible.value = true;
};

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return;
  try {
    await passwordFormRef.value.validate();
  } catch { return; }
  changingPassword.value = true;
  try {
    await apiChangePassword(passwordForm.oldPassword, passwordForm.newPassword, passwordForm.confirmPassword);
    passwordDialogVisible.value = false;
    userStore.logout();
    ElMessage.success('密码修改成功，请使用新密码重新登录');
    router.push('/login');
  } catch (e: any) {
  } finally {
    changingPassword.value = false;
  }
};

onMounted(async () => {
  accountForm.username = userStore.userInfo?.username || '';
  accountForm.gender = userStore.userInfo?.gender || '';
  accountForm.email = userStore.userInfo?.email || '';

  try {
    const info: any = await getUserInfo();
    accountForm.gender = info.gender || '';
    accountForm.email = info.email || '';
    userStore.setUserInfo({ ...userStore.userInfo, ...info });
  } catch {}
});
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&display=swap');

.account-page {
  min-height: 100vh;
  background: #faf9f7;
  padding: 20px;
  position: relative;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
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
}

/* ===== 居中纸张 ===== */
.account-center {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 640px;
  margin: 28px auto 0;
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
  padding: 36px 40px 32px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #1a1a1a;
  display: flex;
  flex-direction: column;
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

/* ===== 标题与分隔线 ===== */
.section-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 3px;
  margin: 0;
}
.section-title--accent {
  padding-left: 12px;
  border-left: 3px solid rgba(26, 26, 26, 0.6);
}
.divider {
  width: 100%;
  height: 1px;
  background: #1a1a1a;
  opacity: 0.15;
  margin: 14px 0 20px;
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
.paper-form :deep(.el-radio__label) {
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #1a1a1a;
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
.action-line.secondary:hover .action-text-sm { color: #1a1a1a; }

/* ===== 安全设置 ===== */
.security-info {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.security-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(26, 26, 26, 0.06);
}
.security-item:last-child { border-bottom: none; }
.security-label {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  min-width: 72px;
  letter-spacing: 1px;
}
.security-value {
  flex: 1;
  font-size: 13px;
  color: #8a8a8a;
}

/* ===== 密码弹窗 ===== */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.password-paper {
  width: 420px;
  background: var(--paper-surface);
  background-color: var(--paper-bg-solid);
  border-radius: 1px;
  box-shadow: var(--paper-shadow-hover);
  padding: 36px 36px 28px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  border: 1px solid var(--paper-border);
}

/* ===== Footer ===== */
.footer {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-top: auto;
  padding-top: 24px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 11px;
  color: #aaa;
  letter-spacing: 0.5px;
}

/* ===== 动画 ===== */
.section-wrapper { position: relative; }
.stagger-item {
  opacity: 0;
  animation: paper-rise 0.4s ease forwards;
}
@keyframes paper-rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 响应式 ===== */
@media (max-width: 700px) {
  .account-page { padding: 12px 8px; }
  .account-center { max-width: 100%; }
  .paper-content { padding: 24px 18px 20px; }
  .section-title { font-size: 18px; }
  .action-dash { width: 36px; }
  .action-text { font-size: 14px; }
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

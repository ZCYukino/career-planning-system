<template>
  <div class="career-report">
    <!-- 浮动职业名称 -->
    <div class="careers-layer">
      <div v-for="(item, i) in careers" :key="i" class="career-drift" :style="item.driftStyle">
        <span class="career-word" :style="item.wordStyle">{{ item.name }}</span>
      </div>
    </div>

    <!-- 页头横条 -->
    <div class="page-header paper">
      <h1 class="page-title">职业生涯发展报告</h1>
      <div class="header-actions" v-if="report && !isGenerating">
        <button class="action-line" @click="confirmRegenerate" :disabled="loading">
          <span class="action-dash"></span>
          <span class="action-text">重新生成</span>
          <span class="action-dash"></span>
        </button>
        <button class="action-line" @click="exportPDF" :disabled="loading">
          <span class="action-dash"></span>
          <span class="action-text">导出PDF</span>
          <span class="action-dash"></span>
        </button>
      </div>
    </div>

    <!-- 生成中状态 -->
    <div v-if="isGenerating" class="gen-paper paper">
      <!-- SVG装饰：书本 -->
      <svg class="icon-deco bottom-right" viewBox="0 0 44 44" fill="none"><path d="M8 8h28v28H8z" stroke="#1a1a1a" stroke-width="1" opacity=".15"/><path d="M14 14h16M14 20h16M14 26h10" stroke="#1a1a1a" stroke-width="1.2" opacity=".12"/></svg>

      <div class="gen-content">
        <h2 class="gen-title">正在为您生成职业规划报告</h2>
        <p class="gen-hint">报告生成需要1-2分钟，请稍候...</p>

        <div class="gen-progress-ring">
          <svg viewBox="0 0 120 120" class="ring-svg">
            <circle cx="60" cy="60" r="52" stroke="rgba(26,26,26,0.06)" stroke-width="6" fill="none"/>
            <circle cx="60" cy="60" r="52" stroke="#1a1a1a" stroke-width="6" fill="none"
              :stroke-dasharray="2 * Math.PI * 52"
              :stroke-dashoffset="2 * Math.PI * 52 * (1 - progress / 100)"
              stroke-linecap="round"
              transform="rotate(-90 60 60)"
              class="ring-fill"
            />
          </svg>
          <div class="ring-text">{{ progress }}%</div>
        </div>

        <div class="gen-steps">
          <div v-for="(step, index) in generatingSteps" :key="index"
            class="gen-step"
            :class="{ done: progress > step.threshold, active: progress >= step.threshold && progress < (generatingSteps[index + 1]?.threshold || 100) }"
          >
            <div class="gen-dot">
              <span v-if="progress > step.threshold">✓</span>
              <span v-else>{{ index + 1 }}</span>
            </div>
            <div class="gen-step-bar">
              <div class="gen-step-fill" :style="{ width: getStepProgress(index) + '%' }"></div>
            </div>
            <span class="gen-step-name">{{ step.name }}</span>
            <span class="gen-step-status">
              <template v-if="progress > step.threshold">已完成</template>
              <template v-else-if="progress >= step.threshold">进行中</template>
              <template v-else>等待中</template>
            </span>
          </div>
        </div>

        <p class="gen-current">{{ currentStep }}</p>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!report" class="empty-wrapper">
      <!-- 加载中 -->
      <div v-if="loadingLatest" class="empty-paper paper">
        <p class="empty-title" style="text-align:center;">正在加载历史报告...</p>
      </div>
      <!-- 实际空态 -->
      <div v-else class="empty-paper paper">
        <!-- SVG装饰：麦穗 -->
        <svg class="icon-deco bottom-right" viewBox="0 0 44 44" fill="none"><path d="M22 4c0 0-8 6-8 16s8 16 8 16" stroke="#1a1a1a" stroke-width="1" opacity=".12"/><path d="M22 4c0 0 8 6 8 16s-8 16-8 16" stroke="#1a1a1a" stroke-width="1" opacity=".12"/><circle cx="22" cy="14" r="2" fill="#1a1a1a" opacity=".1"/><circle cx="18" cy="22" r="2" fill="#1a1a1a" opacity=".1"/><circle cx="26" cy="22" r="2" fill="#1a1a1a" opacity=".1"/></svg>

        <div class="empty-content">
          <div v-if="loadError" class="paper-alert warn" style="margin-bottom:20px;">{{ loadError }}</div>

          <h2 class="empty-title">您还没有生成职业报告</h2>
          <div class="divider"></div>
          <p class="empty-desc">选择目标岗位，系统将为您生成个性化的职业发展报告</p>

          <div class="empty-form">
            <div class="field">
              <span class="field-label">目标岗位</span>
              <div class="field-line">
                <el-select v-model="targetJobName" placeholder="请选择岗位" style="width:100%;">
                  <el-option v-for="(job, idx) in jobs" :key="idx" :label="job.jobName || job.job_name" :value="job.jobName || job.job_name" />
                </el-select>
              </div>
            </div>
            <button class="action-line" @click="generateReport" :disabled="loading || !targetJobName">
              <span class="action-dash"></span>
              <span class="action-text">生成报告</span>
              <span class="action-dash"></span>
            </button>
          </div>

          <div v-if="!hasProfile && !checkingProfile" class="paper-alert warn" style="margin-top:20px;">
            建议先前往 <a href="#" @click.prevent="goToProfile" class="paper-link">用户中心</a> 生成个人能力画像，报告会更精准完整。
          </div>
        </div>
      </div>
    </div>

    <!-- 报告内容 -->
    <div v-else-if="isReportReady && report" class="report-scroll report-card">
      <!-- 报告为空提示 -->
      <div v-if="isReportContentEmpty" class="paper-alert warn" style="margin:0 auto 20px;">
        {{ reportData._message || '报告生成后内容为空，请重新生成报告' }}
        <div style="margin-top:12px;">
          <button class="action-line" @click="confirmRegenerate">
            <span class="action-dash"></span>
            <span class="action-text">重新生成</span>
            <span class="action-dash"></span>
          </button>
        </div>
      </div>

      <div v-if="!isReportContentEmpty" class="report-paper paper">
        <!-- SVG装饰 -->
        <svg class="icon-deco bottom-right" viewBox="0 0 44 44" fill="none"><path d="M22 4c0 0-8 6-8 16s8 16 8 16" stroke="#1a1a1a" stroke-width="1" opacity=".12"/><path d="M22 4c0 0 8 6 8 16s-8 16-8 16" stroke="#1a1a1a" stroke-width="1" opacity=".12"/><circle cx="22" cy="14" r="2" fill="#1a1a1a" opacity=".1"/><circle cx="18" cy="22" r="2" fill="#1a1a1a" opacity=".1"/><circle cx="26" cy="22" r="2" fill="#1a1a1a" opacity=".1"/></svg>

        <!-- 报告头部 -->
        <div class="report-head">
          <h2 class="report-head-title">{{ userStore.userInfo?.name || '用户' }} 的职业发展报告</h2>
          <div class="gradient-divider"></div>
          <div class="report-meta">
            <span class="meta-label">目标岗位</span>
            <span class="meta-value">{{ targetJobName }}</span>
            <span class="meta-sep">·</span>
            <span class="meta-label">综合匹配度</span>
            <span class="meta-value score" :style="{ color: getScoreColor(report.matchScore) }">{{ report.matchScore }}</span>
            <span class="meta-unit">分</span>
            <span class="meta-level">（{{ getMatchLevel(report.matchScore) }}）</span>
          </div>
        </div>

        <!-- 自定义标签栏 -->
        <div class="paper-tabs">
          <div v-for="(tab, i) in sectionTabs" :key="tab.key"
            class="paper-tab" :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <span class="tab-num">{{ ['一','二','三','四'][i] }}</span>
            <span class="tab-text">{{ tab.label }}</span>
          </div>
        </div>

        <!-- 章节内容 -->
        <div class="section-panels">

          <!-- ===== 一、岗位匹配 ===== -->
          <div v-show="activeTab === 'section_a'" class="section-panel section-animate" data-section="a">
            <div class="chapter-watermark" aria-hidden="true">壹</div>
            <div v-if="reportData.section_a">
              <!-- 资料不足提示 -->
              <div v-if="isProfileInsufficient" class="paper-alert warn" style="margin-bottom:20px;">
                您的简历信息不足，建议前往「用户中心」补充详细的实习经历、项目经验等信息，以便获得更精准的职业规划分析。
              </div>

              <!-- 总体匹配度 -->
              <div class="psection">
                <div class="psection-head">
                  <span class="psection-title">总体匹配度</span>
                </div>
                <div class="overall-paper">
                  <div class="overall-left">
                    <div class="score-ring-wrap">
                      <svg viewBox="0 0 120 120" class="score-ring-svg">
                        <circle cx="60" cy="60" r="50" stroke="rgba(26,26,26,0.08)" stroke-width="6" fill="none"/>
                        <circle cx="60" cy="60" r="50" stroke="#1a1a1a" stroke-width="6" fill="none"
                          :stroke-dasharray="2 * Math.PI * 50"
                          :stroke-dashoffset="2 * Math.PI * 50 * (1 - (reportData.section_a.overall_match?.total_score || 0) / 100)"
                          stroke-linecap="round"
                          transform="rotate(-90 60 60)"
                          class="score-ring-fill"
                        />
                      </svg>
                      <div class="score-ring-text">
                        <span class="score-ring-num" :style="{ color: getScoreColor(reportData.section_a.overall_match?.total_score) }">
                          {{ reportData.section_a.overall_match?.total_score || 0 }}
                        </span>
                        <span class="score-ring-unit">分</span>
                      </div>
                    </div>
                    <div class="overall-level">
                      <span class="ptag dark">{{ reportData.section_a.overall_match?.level || '待评估' }}</span>
                      <span class="ptag">{{ reportData.section_a.overall_match?.recommendation || '待评估' }}</span>
                    </div>
                  </div>
                  <div class="overall-right">
                    <div class="breakdown-row">
                      <span class="br-label">基础要求</span>
                      <span class="br-score" :style="{ color: getScoreColor(reportData.section_a.overall_match?.basic_requirement_score) }">
                        {{ reportData.section_a.overall_match?.basic_requirement_score || 0 }}分
                      </span>
                    </div>
                    <div class="breakdown-row">
                      <span class="br-label">发展潜力</span>
                      <span class="br-score" :style="{ color: getScoreColor(reportData.section_a.overall_match?.development_potential_score) }">
                        {{ reportData.section_a.overall_match?.development_potential_score || 0 }}分
                      </span>
                    </div>
                  </div>
                </div>
                <div class="overall-analysis">{{ reportData.section_a.overall_match?.analysis || '暂无综合评价' }}</div>
              </div>

              <!-- 四维度概览 -->
              <div class="four-dim-grid">
                <div class="dim-card" v-for="(dim, key) in fourDimensions" :key="key">
                  <div class="dim-card-ring">
                    <svg viewBox="0 0 60 60" class="dim-card-svg">
                      <circle cx="30" cy="30" r="24" fill="none" stroke="rgba(26,26,26,0.06)" stroke-width="4"/>
                      <circle cx="30" cy="30" r="24" fill="none" :stroke="getScoreColor(dim.score)" stroke-width="4"
                        :stroke-dasharray="(dim.score || 0) / 100 * 150.8 + ' 150.8'"
                        stroke-linecap="round" transform="rotate(-90 30 30)" class="dim-card-ring-fill"
                      />
                    </svg>
                    <span class="dim-card-ring-num" :style="{ color: getScoreColor(dim.score) }">{{ dim.score || 0 }}</span>
                  </div>
                  <div class="dim-name">{{ dim.name }}</div>
                  <div class="dim-desc">{{ dim.desc }}</div>
                </div>
              </div>

              <!-- 专业技能匹配 -->
              <div class="psection">
                <div class="psection-head">
                  <span class="psection-title">专业技能匹配</span>
                  <span class="psection-score" :style="{ color: getScoreColor(reportData.section_a.professional_skill_match?.score) }">
                    {{ reportData.section_a.professional_skill_match?.score || 0 }}分
                  </span>
                  <span class="ptag small">{{ reportData.section_a.professional_skill_match?.match_rate || '0%' }}</span>
                </div>
                <div class="pbar"><div class="pbar-fill" :style="{ width: (reportData.section_a.professional_skill_match?.score || 0) + '%', background: getScoreColor(reportData.section_a.professional_skill_match?.score) }"></div></div>

                <div class="analysis-note" v-if="reportData.section_a.professional_skill_match?.analysis">
                  {{ reportData.section_a.professional_skill_match.analysis }}
                </div>

                <!-- 技能详细对比表 -->
                <div class="skill-table-wrap" v-if="reportData.section_a.professional_skill_match?.skill_details?.length">
                  <div class="sub-title">技能详细对比</div>
                  <div class="skill-legend">
                    <span class="skill-legend-item"><span class="skill-legend-dot required"></span>岗位要求</span>
                    <span class="skill-legend-item"><span class="skill-legend-dot current"></span>当前水平</span>
                  </div>
                  <div class="skill-bars">
                    <div class="skill-bar-row" v-for="(row, ri) in reportData.section_a.professional_skill_match.skill_details" :key="ri">
                      <div class="skill-bar-name">{{ row.skill }}</div>
                      <div class="skill-bar-chart">
                        <div class="skill-bar-track">
                          <div class="skill-bar-fill required" :style="{ width: parseSkillLevel(row.required_level) + '%' }"></div>
                        </div>
                        <div class="skill-bar-track">
                          <div class="skill-bar-fill current" :style="{ width: parseSkillLevel(row.student_level) + '%' }"></div>
                        </div>
                      </div>
                      <div class="skill-bar-meta">
                        <span class="skill-bar-label">岗位要求 <span class="ptag small">{{ row.required_level }}</span></span>
                        <span class="skill-bar-label">当前水平 <span class="ptag small">{{ row.student_level }}</span></span>
                        <span class="skill-gap-indicator" :class="{ 'no-gap': row.gap === '无' }">{{ row.gap === '无' ? '✓ 已达标' : '△ 差距：' + row.gap }}</span>
                      </div>
                      <div class="skill-bar-suggestion" v-if="row.suggestion">{{ row.suggestion }}</div>
                    </div>
                  </div>
                </div>

                <!-- 已匹配/差距技能 -->
                <div class="skills-split">
                  <div class="skills-col">
                    <div class="col-title">已匹配技能 <span class="count-badge">{{ getMatchedSkillsCount() }}</span></div>
                    <div class="col-tags">
                      <span class="ptag small" v-for="(skill, idx) in reportData.section_a.professional_skill_match?.matched_skills" :key="idx">
                        {{ typeof skill === 'object' ? skill.name : skill }}
                        <span v-if="typeof skill === 'object' && skill.level" class="tag-sub">（{{ skill.level }}）</span>
                      </span>
                      <span v-if="!reportData.section_a.professional_skill_match?.matched_skills?.length" class="muted-text">暂无匹配技能</span>
                    </div>
                  </div>
                  <div class="skills-col">
                    <div class="col-title">差距技能 <span class="count-badge warn">{{ getGapSkillsCount() }}</span></div>
                    <div class="col-tags">
                      <span class="ptag small dark" v-for="(skill, idx) in reportData.section_a.professional_skill_match?.gap_skills" :key="idx">
                        {{ typeof skill === 'object' ? skill.name : skill }}
                        <span v-if="typeof skill === 'object' && skill.priority" class="tag-sub">（{{ skill.priority }}）</span>
                      </span>
                      <span v-if="!reportData.section_a.professional_skill_match?.gap_skills?.length" class="muted-text">暂无差距技能</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 通用素质 -->
              <div class="psection">
                <div class="psection-head">
                  <span class="psection-title">通用素质</span>
                  <span class="psection-score" :style="{ color: getScoreColor(reportData.section_a.general_quality_match?.score) }">
                    {{ reportData.section_a.general_quality_match?.score || 0 }}分
                  </span>
                </div>
                <div class="soft-list">
                  <div class="soft-row" v-for="(skill, idx) in reportData.section_a.general_quality_match?.soft_skills" :key="idx">
                    <div class="soft-left">
                      <span class="soft-check" :class="{ ok: skill.status === '满足' || skill.gap <= 0 }">{{ skill.status === '满足' || skill.gap <= 0 ? '✓' : '△' }}</span>
                      <span class="soft-name">{{ skill.name }}</span>
                    </div>
                    <div class="soft-right">
                      <div class="soft-bar-wrap">
                        <div class="soft-bar-track">
                          <div class="soft-bar-fill" :style="{ width: Math.min(skill.student_score, 100) + '%' }"></div>
                        </div>
                      </div>
                      <span class="ptag small">{{ skill.student_score }}分</span>
                      <span class="soft-gap" :class="{ pos: skill.gap <= 0, neg: skill.gap > 0 }">
                        {{ skill.gap >= 0 ? '+' : '' }}{{ skill.gap }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 差距分析 -->
              <div class="psection">
                <div class="psection-head">
                  <span class="psection-title">差距分析</span>
                </div>

                <!-- 关键差距 -->
                <div class="gap-group" v-if="filterPlaceholderSkills(reportData.section_a.gap_analysis?.critical_gaps)?.length">
                  <div class="gap-group-title critical">关键差距（影响入职）</div>
                  <div class="gap-cards">
                    <div class="gap-card" v-for="(gap, idx) in filterPlaceholderSkills(reportData.section_a.gap_analysis?.critical_gaps)" :key="idx">
                      <div class="gap-card-head">
                        <span class="ptag small dark">关键</span>
                        <span class="gap-skill-name">{{ typeof gap === 'object' ? (gap?.skill || gap) : (typeof gap === 'string' ? gap : '') }}</span>
                      </div>
                      <div class="gap-card-body" v-if="typeof gap === 'object'">
                        <p v-if="gap.reason" class="gap-reason">{{ gap.reason }}</p>
                        <div class="gap-meta" v-if="gap.priority || gap.difficulty || gap.suggested_duration">
                          <span v-if="gap.priority" class="ptag small">优先级：{{ gap.priority }}</span>
                          <span v-if="gap.difficulty" class="ptag small">难度：{{ gap.difficulty }}星</span>
                          <span v-if="gap.suggested_duration" class="ptag small">建议时长：{{ gap.suggested_duration }}</span>
                        </div>
                        <div class="gap-suggestion" v-if="gap.learning_path">
                          <span class="gap-sug-label">学习建议</span>
                          <p>{{ gap.learning_path }}</p>
                        </div>
                      </div>
                      <p v-else-if="typeof gap === 'string'" class="gap-reason">{{ gap }}</p>
                    </div>
                  </div>
                </div>

                <!-- 中等差距 -->
                <div class="gap-group" v-if="filterPlaceholderSkills(reportData.section_a.gap_analysis?.moderate_gaps)?.length">
                  <div class="gap-group-title moderate">中等差距（影响晋升）</div>
                  <div class="gap-cards">
                    <div class="gap-card moderate" v-for="(gap, idx) in filterPlaceholderSkills(reportData.section_a.gap_analysis?.moderate_gaps)" :key="idx">
                      <div class="gap-card-head">
                        <span class="gap-skill-name">{{ typeof gap === 'object' ? (gap?.skill || gap) : (typeof gap === 'string' ? gap : '') }}</span>
                      </div>
                      <div class="gap-card-body" v-if="typeof gap === 'object'">
                        <p v-if="gap.reason" class="gap-reason">{{ gap.reason }}</p>
                        <div class="gap-meta" v-if="gap.priority || gap.difficulty || gap.suggested_duration">
                          <span v-if="gap.priority" class="ptag small">优先级：{{ gap.priority }}</span>
                          <span v-if="gap.difficulty" class="ptag small">难度：{{ gap.difficulty }}星</span>
                          <span v-if="gap.suggested_duration" class="ptag small">建议时长：{{ gap.suggested_duration }}</span>
                        </div>
                        <div class="gap-suggestion" v-if="gap.learning_path">
                          <span class="gap-sug-label">学习建议</span>
                          <p>{{ gap.learning_path }}</p>
                        </div>
                      </div>
                      <p v-else-if="typeof gap === 'string'" class="gap-reason">{{ gap }}</p>
                    </div>
                  </div>
                </div>

                <!-- 轻微差距 -->
                <div class="gap-group" v-if="filterPlaceholderSkills(reportData.section_a.gap_analysis?.minor_gaps)?.length">
                  <div class="gap-group-title minor">轻微差距</div>
                  <div class="gap-cards">
                    <div class="gap-card minor" v-for="(gap, idx) in filterPlaceholderSkills(reportData.section_a.gap_analysis?.minor_gaps)" :key="idx">
                      <div class="gap-card-head">
                        <span class="gap-skill-name">{{ typeof gap === 'object' ? (gap?.skill || gap) : (typeof gap === 'string' ? gap : '') }}</span>
                      </div>
                      <div class="gap-card-body" v-if="typeof gap === 'object'">
                        <p v-if="gap.reason" class="gap-reason">{{ gap.reason }}</p>
                        <div class="gap-meta" v-if="gap.suggested_duration">
                          <span class="ptag small">建议时长：{{ gap.suggested_duration }}</span>
                        </div>
                        <div class="gap-suggestion" v-if="gap.learning_path">
                          <span class="gap-sug-label">学习建议</span>
                          <p>{{ gap.learning_path }}</p>
                        </div>
                      </div>
                      <p v-else-if="typeof gap === 'string'" class="gap-reason">{{ gap }}</p>
                    </div>
                  </div>
                </div>

                <div class="gap-empty" v-if="!filterPlaceholderSkills(reportData.section_a.gap_analysis?.critical_gaps)?.length && !filterPlaceholderSkills(reportData.section_a.gap_analysis?.moderate_gaps)?.length && !filterPlaceholderSkills(reportData.section_a.gap_analysis?.minor_gaps)?.length">
                  <span class="muted-text">暂无差距分析数据</span>
                </div>
              </div>
            </div>
          </div>

          <!-- ===== 二、目标路径 ===== -->
          <div v-show="activeTab === 'section_b'" class="section-panel section-animate" data-section="b">
            <div class="chapter-watermark" aria-hidden="true">贰</div>
            <div v-if="reportData.section_b">
              <h3 class="section-title" v-if="reportData.section_b.title">{{ reportData.section_b.title }}</h3>

              <!-- 职业目标 -->
              <div class="psection">
                <div class="psection-head">
                  <span class="psection-title">职业目标设定</span>
                </div>
                <div class="goals-grid">
                  <div class="goal-card">
                    <div class="goal-top">
                      <span class="goal-num">Ⅰ</span>
                      <div class="goal-badge">短期目标</div>
                    </div>
                    <div class="goal-period">1-2年</div>
                    <div class="goal-content">{{ reportData.section_b.career_goals?.short_term || '暂无' }}</div>
                  </div>
                  <div class="goal-card">
                    <div class="goal-top">
                      <span class="goal-num">Ⅱ</span>
                      <div class="goal-badge">中期目标</div>
                    </div>
                    <div class="goal-period">3-5年</div>
                    <div class="goal-content">{{ reportData.section_b.career_goals?.mid_term || '暂无' }}</div>
                  </div>
                  <div class="goal-card">
                    <div class="goal-top">
                      <span class="goal-num">Ⅲ</span>
                      <div class="goal-badge">长期目标</div>
                    </div>
                    <div class="goal-period">5年以上</div>
                    <div class="goal-content">{{ reportData.section_b.career_goals?.long_term || '暂无' }}</div>
                  </div>
                </div>
              </div>

              <!-- 行业分析 -->
              <div class="psection">
                <div class="psection-head">
                  <span class="psection-title">行业分析</span>
                </div>
                <div class="analysis-grid">
                  <div class="ana-card">
                    <div class="ana-label">社会需求</div>
                    <div class="ana-content">{{ reportData.section_b.industry_analysis?.social_demand || '暂无分析' }}</div>
                  </div>
                  <div class="ana-card">
                    <div class="ana-label">行业发展趋势</div>
                    <div class="ana-content">{{ reportData.section_b.industry_analysis?.industry_trend || '暂无分析' }}</div>
                  </div>
                  <div class="ana-card">
                    <div class="ana-label">岗位稳定性</div>
                    <div class="ana-content">{{ reportData.section_b.industry_analysis?.job_security || '暂无分析' }}</div>
                  </div>
                </div>
              </div>

              <!-- 垂直晋升路径 -->
              <div class="psection" v-if="reportData.section_b.career_path?.length">
                <div class="psection-head">
                  <span class="psection-title">垂直晋升路径</span>
                </div>
                <div class="node-chain">
                  <div class="node-item" v-for="(path, idx) in reportData.section_b.career_path" :key="idx">
                    <div class="node-dot-wrap">
                      <div class="node-dot" :class="{ first: idx === 0 }"></div>
                      <div v-if="idx < reportData.section_b.career_path.length - 1" class="node-line"></div>
                    </div>
                    <div class="node-body">
                      <div class="node-head">
                        <span class="node-stage">第{{ idx + 1 }}阶段</span>
                        <span class="node-position">{{ path.position }}</span>
                      </div>
                      <div class="node-meta">
                        <span v-if="path.duration">{{ path.duration }}</span>
                        <span v-if="path.salary_range"> · {{ path.salary_range }}</span>
                      </div>
                      <div class="node-skills" v-if="path.required_skills?.length">
                        <span class="node-skills-label">所需技能：</span>
                        <span class="ptag small" v-for="(skill, sIdx) in (Array.isArray(path.required_skills) ? path.required_skills : path.required_skills.split(/[,，、]/))" :key="sIdx">
                          {{ typeof skill === 'string' ? skill.trim() : skill }}
                        </span>
                      </div>
                      <div class="node-milestone" v-if="path.milestone">里程碑：{{ path.milestone }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 横向发展路径 -->
              <div class="psection">
                <div class="psection-head">
                  <span class="psection-title">横向发展路径</span>
                </div>
                <div class="transfer-list" v-if="reportData.section_b.transfer_paths?.length">
                  <div class="transfer-card" v-for="(tp, idx) in reportData.section_b.transfer_paths" :key="idx">
                    <div class="transfer-head">
                      <span class="transfer-src">{{ tp.source || '当前岗位' }}</span>
                      <span class="transfer-arrow">→</span>
                      <span class="transfer-tgt">{{ tp.target }}</span>
                      <span class="ptag small">难度 {{ tp.difficulty || '?' }}/5</span>
                    </div>
                    <div class="transfer-body">
                      <div class="transfer-meta" v-if="tp.duration || tp.salary_change">
                        <span v-if="tp.duration" class="ptag small">转型耗时: {{ tp.duration }}</span>
                        <span v-if="tp.salary_change" class="ptag small">{{ tp.salary_change }}</span>
                      </div>

                      <div class="transfer-skills-section" v-if="tp.required_skills?.length">
                        <div class="transfer-sub-title">所需技能</div>
                        <div class="transfer-skills-list">
                          <div class="transfer-skill-item" v-for="(sk, sIdx) in tp.required_skills" :key="sIdx">
                            <span class="ptag small dark">{{ typeof sk === 'object' ? sk.name : sk }}</span>
                            <span v-if="typeof sk === 'object' && sk.current_gap" class="ptag small">差距：{{ sk.current_gap }}</span>
                            <p v-if="typeof sk === 'object' && sk.requirement" class="skill-req">要求：{{ sk.requirement }}</p>
                          </div>
                        </div>
                      </div>

                      <div class="transfer-adv" v-if="tp.advantage">
                        <div class="transfer-sub-title">转型优势</div>
                        <p>{{ tp.advantage }}</p>
                      </div>

                      <div class="transfer-risk" v-if="tp.risk">
                        <div class="transfer-sub-title">潜在风险</div>
                        <p>{{ tp.risk }}</p>
                      </div>

                      <div class="transfer-plan" v-if="tp.action_plan?.length">
                        <div class="transfer-sub-title">分阶段行动计划</div>
                        <div class="plan-timeline">
                          <div class="plan-phase" v-for="(phase, pIdx) in tp.action_plan" :key="pIdx">
                            <div class="phase-dot" :class="{ first: pIdx === 0 }"></div>
                            <div class="phase-body">
                              <div class="phase-title">{{ phase.phase }}</div>
                              <div class="phase-actions">
                                <div class="phase-action" v-for="(action, aIdx) in phase.actions" :key="aIdx">· {{ action }}</div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="gap-empty"><span class="muted-text">暂无换岗路径信息</span></div>
              </div>
            </div>
          </div>

          <!-- ===== 三、行动计划 ===== -->
          <div v-show="activeTab === 'section_c'" class="section-panel section-animate" data-section="c">
            <div class="chapter-watermark" aria-hidden="true">叁</div>
            <div v-if="reportData.section_c">
              <h3 class="section-title" v-if="reportData.section_c.title">{{ reportData.section_c.title }}</h3>

              <!-- Action Plan Timeline -->
              <div class="action-timeline">
                <!-- Node: 短期 -->
                <div class="timeline-node">
                  <div class="timeline-marker">
                    <div class="timeline-dot"></div>
                    <div class="timeline-line"></div>
                  </div>
                  <div class="timeline-content">
                    <div class="timeline-phase">
                      <span class="phase-label">短期计划</span>
                      <span class="phase-duration">{{ reportData.section_c.short_term_plan?.duration || '1-6个月' }}</span>
                    </div>
                    <div class="timeline-cards">
                      <div class="timeline-card" v-if="reportData.section_c.short_term_plan?.learning_path?.length">
                        <div class="timeline-card-title">学习路径</div>
                        <div class="timeline-checklist">
                          <label class="timeline-check-item" v-for="(item, idx) in reportData.section_c.short_term_plan.learning_path" :key="idx">
                            <span class="timeline-checkbox"><input type="checkbox" /><span class="checkmark"></span></span>
                            <span class="check-text">{{ typeof item === 'string' ? item : '' }}</span>
                          </label>
                        </div>
                      </div>
                      <div class="timeline-card" v-if="reportData.section_c.short_term_plan?.practice_arrangement?.length">
                        <div class="timeline-card-title">实践安排</div>
                        <div class="timeline-checklist">
                          <label class="timeline-check-item" v-for="(item, idx) in reportData.section_c.short_term_plan.practice_arrangement" :key="idx">
                            <span class="timeline-checkbox"><input type="checkbox" /><span class="checkmark"></span></span>
                            <span class="check-text">{{ typeof item === 'string' ? item : '' }}</span>
                          </label>
                        </div>
                      </div>
                      <div class="timeline-card" v-if="reportData.section_c.short_term_plan?.evaluation_indicators?.length">
                        <div class="timeline-card-title">评估指标</div>
                        <div class="timeline-checklist">
                          <label class="timeline-check-item" v-for="(item, idx) in reportData.section_c.short_term_plan.evaluation_indicators" :key="idx">
                            <span class="timeline-checkbox"><input type="checkbox" /><span class="checkmark"></span></span>
                            <span class="check-text">{{ typeof item === 'string' ? item : '' }}</span>
                          </label>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Node: 中期 -->
                <div class="timeline-node">
                  <div class="timeline-marker">
                    <div class="timeline-dot mid"></div>
                    <div class="timeline-line"></div>
                  </div>
                  <div class="timeline-content">
                    <div class="timeline-phase">
                      <span class="phase-label">中期计划</span>
                      <span class="phase-duration">{{ reportData.section_c.mid_term_plan?.duration || '6-18个月' }}</span>
                    </div>
                    <div class="timeline-cards">
                      <div class="timeline-card" v-if="reportData.section_c.mid_term_plan?.learning_path?.length">
                        <div class="timeline-card-title">进阶学习</div>
                        <div class="timeline-checklist">
                          <label class="timeline-check-item" v-for="(item, idx) in reportData.section_c.mid_term_plan.learning_path" :key="idx">
                            <span class="timeline-checkbox"><input type="checkbox" /><span class="checkmark"></span></span>
                            <span class="check-text">{{ typeof item === 'string' ? item : '' }}</span>
                          </label>
                        </div>
                      </div>
                      <div class="timeline-card" v-if="reportData.section_c.mid_term_plan?.practice_arrangement?.length">
                        <div class="timeline-card-title">项目实践</div>
                        <div class="timeline-checklist">
                          <label class="timeline-check-item" v-for="(item, idx) in reportData.section_c.mid_term_plan.practice_arrangement" :key="idx">
                            <span class="timeline-checkbox"><input type="checkbox" /><span class="checkmark"></span></span>
                            <span class="check-text">{{ typeof item === 'string' ? item : '' }}</span>
                          </label>
                        </div>
                      </div>
                      <div class="timeline-card" v-if="reportData.section_c.mid_term_plan?.evaluation_indicators?.length">
                        <div class="timeline-card-title">评估指标</div>
                        <div class="timeline-checklist">
                          <label class="timeline-check-item" v-for="(item, idx) in reportData.section_c.mid_term_plan.evaluation_indicators" :key="idx">
                            <span class="timeline-checkbox"><input type="checkbox" /><span class="checkmark"></span></span>
                            <span class="check-text">{{ typeof item === 'string' ? item : '' }}</span>
                          </label>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Node: 评估周期 -->
                <div class="timeline-node">
                  <div class="timeline-marker">
                    <div class="timeline-dot long"></div>
                  </div>
                  <div class="timeline-content">
                    <div class="timeline-phase">
                      <span class="phase-label">评估周期</span>
                    </div>
                    <div class="timeline-cards">
                      <div class="timeline-card">
                        <div class="timeline-card-title">月度评估</div>
                        <p class="timeline-card-text">{{ reportData.section_c.evaluation_cycle?.monthly || '暂无' }}</p>
                      </div>
                      <div class="timeline-card">
                        <div class="timeline-card-title">季度评估</div>
                        <p class="timeline-card-text">{{ reportData.section_c.evaluation_cycle?.quarterly || '暂无' }}</p>
                      </div>
                      <div class="timeline-card">
                        <div class="timeline-card-title">年度评估</div>
                        <p class="timeline-card-text">{{ reportData.section_c.evaluation_cycle?.yearly || '暂无' }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- ===== 四、编辑优化 ===== -->
          <div v-show="activeTab === 'section_d'" class="section-panel section-animate" data-section="d">
            <div class="chapter-watermark" aria-hidden="true">肆</div>
            <div v-if="reportData.section_d">
              <h3 class="section-title" v-if="reportData.section_d.title">{{ reportData.section_d.title }}</h3>

              <!-- 完整性检查 -->
              <div class="psection">
                <div class="psection-head">
                  <span class="psection-title">内容完整性检查</span>
                </div>
                <div v-if="filteredCompletenessItems?.length" class="check-list">
                  <div class="check-item" v-for="(item, idx) in filteredCompletenessItems" :key="idx">
                    <span class="check-dot">△</span>
                    <span>{{ typeof item === 'string' ? item : '' }}</span>
                  </div>
                </div>
                <div v-else class="check-ok">
                  <span class="check-dot ok">✓</span> 内容完整，无需额外补充
                </div>
              </div>

              <!-- 润色建议 -->
              <div class="psection">
                <div class="psection-head">
                  <span class="psection-title">润色建议</span>
                </div>
                <div class="sugg-grid" v-if="reportData.section_d.polishing_suggestions?.length">
                  <div class="sugg-card" v-for="(item, idx) in reportData.section_d.polishing_suggestions" :key="idx">
                    <div class="sugg-head">润色建议 {{ idx + 1 }}</div>
                    <div class="sugg-body">{{ typeof item === 'string' ? item : '' }}</div>
                  </div>
                </div>
                <div v-else class="gap-empty"><span class="muted-text">暂无建议</span></div>
              </div>
            </div>
          </div>

        </div><!-- /section-panels -->
      </div><!-- /report-paper -->
    </div><!-- /report-scroll -->

    <!-- 岗位选择对话框 -->
    <el-dialog v-model="jobSelectDialogVisible" title="重新生成报告 — 选择目标岗位" width="460px" :close-on-click-modal="false" class="paper-dialog">
      <div class="dialog-body">
        <div class="field">
          <span class="field-label">目标岗位</span>
          <div class="field-line">
            <el-select v-model="tempSelectedJob" placeholder="请选择目标岗位" style="width:100%;" filterable clearable>
              <el-option v-for="(job, idx) in jobs" :key="idx" :label="job.jobName || job.job_name" :value="job.jobName || job.job_name" />
            </el-select>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="action-line" @click="jobSelectDialogVisible = false">
          <span class="action-dash"></span><span class="action-text">取消</span><span class="action-dash"></span>
        </button>
        <button class="action-line" @click="confirmJobAndRegenerate" :disabled="!tempSelectedJob">
          <span class="action-dash"></span><span class="action-text">确认生成</span><span class="action-dash"></span>
        </button>
      </template>
    </el-dialog>

    <!-- 编辑报告对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑报告" width="70%" :close-on-click-modal="false" class="paper-dialog">
      <div class="dialog-body">
        <el-input v-model="editContent" type="textarea" :rows="25" placeholder="在此编辑报告内容..." />
      </div>
      <template #footer>
        <button class="action-line" @click="editDialogVisible = false">
          <span class="action-dash"></span><span class="action-text">取消</span><span class="action-dash"></span>
        </button>
        <button class="action-line" @click="saveEditedReport">
          <span class="action-dash"></span><span class="action-text">保存</span><span class="action-dash"></span>
        </button>
      </template>
    </el-dialog>

    <!-- PDF专用隐藏容器 -->
    <div ref="printContainerRef"></div>

    <p class="footer">© 2026 职业规划智能体</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { getTopJobs, getJobById } from '@/api/job';
import { createReportTask, getReportStatus, getLatestReport as apiGetLatestReport, updateReport as apiUpdateReport } from '@/api/report';
import { getLatestAbilityProfile } from '@/api/student';
import { ElMessage, ElMessageBox } from 'element-plus';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';
import {
  Document, Download, TrendCharts, DataLine,
  Reading, Tools, Check, EditPen, DataAnalysis, Loading, Refresh,
  Briefcase, Aim, Guide, Top, Switch,
  Clock, Coin, Lock, ShoppingCart, Calendar, Timer,
  SuccessFilled, WarningFilled, Right, InfoFilled, List, Memo, Edit, CircleCheckFilled
} from '@element-plus/icons-vue';
void [Document, Download, TrendCharts, DataLine, Reading, Tools, Check, EditPen, DataAnalysis, Loading, Refresh, Briefcase, Aim, Guide, Top, Switch, Clock, Coin, Lock, ShoppingCart, Calendar, Timer, SuccessFilled, WarningFilled, Right, InfoFilled, List, Memo, Edit, CircleCheckFilled];

const router = useRouter();
const userStore = useUserStore();
const jobs = ref<any[]>([]);
const targetJobName = ref('');
const report = ref<any>(null);
const isReportReady = ref(false);
const loading = ref(false);
const loadingLatest = ref(false);
const loadError = ref('');
const activeTab = ref('section_a');
const jobSelectDialogVisible = ref(false);
const tempSelectedJob = ref('');
const editDialogVisible = ref(false);
const editContent = ref('');
const printContainerRef = ref<HTMLElement | null>(null);

const isGenerating = ref(false);
const progress = ref(0);
const currentStep = ref('正在初始化...');
const pollTimer = ref<number | undefined>(undefined);
const hasProfile = ref(false);
const checkingProfile = ref(false);
const reportTaskId = ref<number | null>(null);

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

// --- 自定义标签栏 ---
const sectionTabs = [
  { key: 'section_a', label: '岗位匹配' },
  { key: 'section_b', label: '目标路径' },
  { key: 'section_c', label: '行动计划' },
  { key: 'section_d', label: '编辑优化' },
];

// --- 生成进度辅助 ---
const generatingSteps = [
  { name: '学生画像', threshold: 15 },
  { name: '岗位画像', threshold: 45 },
  { name: '人岗匹配', threshold: 55 },
  { name: '生成报告', threshold: 70 }
];

const getStepProgress = (index: number) => {
  const current = progress.value;
  const start = generatingSteps[index].threshold;
  const end = generatingSteps[index + 1]?.threshold || 100;
  if (current >= end) return 100;
  if (current <= start) return 0;
  return Math.round(((current - start) / (end - start)) * 100);
};

const isProfileInsufficient = computed(() => {
  const a = reportData.value;
  if (!a) return false;
  const level = a.overall_match?.level;
  return level === '信息不足' || level === '差距较大';
});

const fourDimensions = computed(() => {
  const dimA = reportData.value?.section_a;
  if (!dimA) return [];

  const overallMatch = dimA.overall_match || {};
  const totalScore = overallMatch.total_score || 0;
  const basicScore = overallMatch.basic_requirement_score;

  const computedBasicScore = (() => {
    if (basicScore !== undefined && basicScore !== null) return basicScore;
    return Math.round(totalScore * 0.95);
  })();

  const computedPotentialScore = (() => {
    if (overallMatch.development_potential_score !== undefined) return overallMatch.development_potential_score;
    return Math.round(totalScore * 0.85);
  })();

  return [
    { key: 'basic', name: '基础要求', score: computedBasicScore, desc: '学历、专业、经验匹配' },
    { key: 'skill', name: '职业技能', score: dimA.professional_skill_match?.score || 0, desc: '专业技能掌握程度' },
    { key: 'literacy', name: '职业素养', score: dimA.general_quality_match?.score || 0, desc: '创新、学习、抗压、沟通' },
    { key: 'potential', name: '发展潜力', score: computedPotentialScore, desc: '学习能力与成长空间' }
  ];
});

const filterPlaceholderSkills = (skills: any[]) => {
  if (!Array.isArray(skills)) return [];
  const placeholderPatterns = [
    /^兜底$/, /^待评估$/, /^参考.*$/, /^暂无$/, /^待补充$/,
    /^待定$/, /^示例$/, /^placeholder$/, /^无相关$/,
    /^根据.*$/, /^待.*$/, /^暂无.*$/
  ];
  return skills.filter((s: any) => {
    const skillStr = typeof s === 'object' ? (s?.skill || '') : String(s || '');
    return !placeholderPatterns.some((p: RegExp) => p.test(skillStr.trim()));
  });
};

const parseSkillLevel = (level: string | number | undefined): number => {
  if (!level) return 0;
  const str = String(level).trim();
  const numMatch = str.match(/(\d+)/);
  if (numMatch) {
    const val = parseInt(numMatch[1]);
    if (val > 10) return Math.min(val, 100);
    return val * 10;
  }
  const levelMap: Record<string, number> = {
    '精通': 95, '熟练': 80, '掌握': 70, '了解': 50,
    '入门': 30, '无': 0, '暂无': 0,
  };
  for (const [key, val] of Object.entries(levelMap)) {
    if (str.includes(key)) return val;
  }
  return 50;
};

const getMatchedSkillsCount = () => {
  const skills = reportData.value?.section_a?.professional_skill_match?.matched_skills;
  return Array.isArray(skills) ? skills.length : 0;
};

const getGapSkillsCount = () => {
  const skills = reportData.value?.section_a?.professional_skill_match?.gap_skills;
  return Array.isArray(skills) ? skills.length : 0;
};

const parseReportContent = (rawContent: any) => {
  if (rawContent === null || rawContent === undefined) {
    return { ok: false, data: null, message: '报告内容为空' };
  }
  if (typeof rawContent === 'object') {
    if (Object.keys(rawContent).length === 0) {
      return { ok: false, data: null, message: '报告内容为空' };
    }
    return { ok: true, data: rawContent, message: '' };
  }
  const content = String(rawContent).trim();
  if (!content) {
    return { ok: false, data: null, message: '报告内容为空' };
  }
  const tryParse = (text: string) => {
    try { return JSON.parse(text); } catch { return null; }
  };
  const direct = tryParse(content);
  if (direct && Object.keys(direct).length > 0) {
    return { ok: true, data: direct, message: '' };
  }
  const blockMatch = content.match(/```(?:json)?\s*([\s\S]*?)```/i);
  if (blockMatch?.[1]) {
    const blockParsed = tryParse(blockMatch[1].trim());
    if (blockParsed && Object.keys(blockParsed).length > 0) {
      return { ok: true, data: blockParsed, message: '' };
    }
  }
  const firstBrace = content.indexOf('{');
  const lastBrace = content.lastIndexOf('}');
  if (firstBrace !== -1 && lastBrace > firstBrace) {
    const fragment = content.slice(firstBrace, lastBrace + 1);
    const fragmentParsed = tryParse(fragment);
    if (fragmentParsed && Object.keys(fragmentParsed).length > 0) {
      return { ok: true, data: fragmentParsed, message: '' };
    }
  }
  return { ok: false, data: null, message: '报告格式异常' };
};

const reportData = computed(() => {
  if (!report.value) return {};
  const content = report.value.reportContent ?? report.value.report_content;
  const parsedResult = parseReportContent(content);
  if (!parsedResult.ok) {
    return { _empty: true, _message: parsedResult.message };
  }
  return parsedResult.data || { _empty: true, _message: '报告内容为空' };
});

const isReportContentEmpty = computed(() => {
  return reportData.value._empty === true;
});

const filteredCompletenessItems = computed(() => {
  const items = reportData.value.section_d?.completeness_check;
  if (!items || !Array.isArray(items)) return [];
  const invalidPatterns = [
    /^(未知|空|null|n\/a|无|缺失|未找到|未提供)$/i,
    /^(高|中|低)风险$/,
    /^兜底$/, /^待评估$/, /^示例$/, /^placeholder$/,
    /^待定$/, /^待补充$/, /^暂无$/, /^无相关$/,
    /^根据.*$/, /^待.*$/, /^暂无.*$/, /^无$/i,
  ];
  return items.filter((item: string) => {
    if (!item || typeof item !== 'string') return false;
    const trimmed = item.trim();
    if (!trimmed || trimmed.length < 4) return false;
    for (const pattern of invalidPatterns) {
      if (pattern.test(trimmed)) return false;
    }
    return true;
  });
});

const hydrateTargetJobName = (latestReport: any, allJobs: any[]) => {
  const reportJobName = latestReport.targetJobName || latestReport.target_job_name || latestReport.jobName || latestReport.job_name;
  if (reportJobName) {
    targetJobName.value = reportJobName;
    return;
  }
  const reportJobId = latestReport.targetJobId || latestReport.target_job_id || latestReport.jobId || latestReport.job_id;
  if (reportJobId && allJobs?.length > 0) {
    const byId = allJobs.find((j: any) => Number(j.id || j.jobId || j.job_id) === Number(reportJobId));
    if (byId) {
      targetJobName.value = byId.jobName || byId.job_name || byId.title || '';
      return;
    }
  }
  if (reportJobId) {
    getJobById(Number(reportJobId)).then((job: any) => {
      if (job) targetJobName.value = job.jobName || job.job_name || '';
    }).catch(() => {});
  }
};

onMounted(async () => {
  const studentId = userStore.userInfo?.id;

  const savedReportGen = sessionStorage.getItem('reportGenerating');
  if (savedReportGen) {
    try {
      const savedState = JSON.parse(savedReportGen);
      const isExpired = savedState.timestamp && (Date.now() - savedState.timestamp > 30 * 60 * 1000);
      if (isExpired) {
        sessionStorage.removeItem('reportGenerating');
      } else if (savedState.isGenerating && savedState.taskId) {
        reportTaskId.value = savedState.taskId;
        progress.value = savedState.progress || 0;
        currentStep.value = savedState.stepName || '处理中...';
        targetJobName.value = savedState.targetJobName || '';
        isGenerating.value = true;
        report.value = null;

        try {
          const res: any = await getTopJobs();
          jobs.value = res || [];
        } catch (e) {}
        await checkProfileStatus();

        try {
          const status: any = await getReportStatus(savedState.taskId);
          if (status.status === 'COMPLETED' || status.status === 'FINAL') {
            const reportData = status.report || await apiGetLatestReport(studentId);
            if (reportData) {
              report.value = reportData;
              isGenerating.value = false;
              isReportReady.value = true;
              progress.value = 100;
              currentStep.value = '报告生成完成';
              reportTaskId.value = null;
              sessionStorage.removeItem('reportGenerating');
              hydrateTargetJobName(reportData, jobs.value);
              ElMessage.success('报告加载成功');
              return;
            }
          }
        } catch (checkError) {
          console.warn('检查报告状态失败，将继续轮询:', checkError);
        }

        startPolling(savedState.taskId);
        return;
      }
    } catch (e) {
      sessionStorage.removeItem('reportGenerating');
    }
  }

  try {
    const res: any = await getTopJobs();
    jobs.value = res || [];

    if (studentId) {
      loadingLatest.value = true;
      try {
        const latest: any = await apiGetLatestReport(studentId);
        report.value = latest;
        if (latest && latest.reportContent) {
          isReportReady.value = true;
        }
        if (latest) {
          hydrateTargetJobName(latest, jobs.value);
        }
      } catch (reportError) {
        loadError.value = '历史报告加载失败，可重新生成';
      }
    }
    await checkProfileStatus();
  } catch (error) {
    loadError.value = '页面加载失败，请刷新重试';
  } finally {
    loadingLatest.value = false;
  }
});

onUnmounted(() => {
  if (pollTimer.value !== undefined) {
    clearInterval(pollTimer.value);
    pollTimer.value = undefined;
  }
});

const checkProfileStatus = async () => {
  const studentId = userStore.userInfo?.id;
  if (!studentId) {
    hasProfile.value = false;
    return;
  }
  checkingProfile.value = true;
  try {
    const profile: any = await getLatestAbilityProfile(studentId);
    hasProfile.value = profile && Object.keys(profile).length > 0;
  } catch {
    hasProfile.value = false;
  } finally {
    checkingProfile.value = false;
  }
};

const startGenerateReport = async () => {
  if (!targetJobName.value) {
    ElMessage.warning('请选择目标岗位');
    return;
  }
  if (!hasProfile.value) {
    ElMessageBox.confirm(
      '您还没有生成个人能力画像，建议先前往「用户中心」生成画像后再生成报告。\n\n是否仍要继续生成报告？',
      '提示',
      { confirmButtonText: '仍要继续', cancelButtonText: '去生成画像', type: 'warning' }
    ).then(() => doGenerateReport()).catch(() => router.push('/profile'));
    return;
  }
  doGenerateReport();
};

const doGenerateReport = () => {
  report.value = null;
  isGenerating.value = true;
  progress.value = 0;
  currentStep.value = '正在创建任务...';

  sessionStorage.setItem('reportGenerating', JSON.stringify({
    isGenerating: true, taskId: null, progress: 0,
    stepName: '正在创建任务...', targetJobName: targetJobName.value,
    timestamp: Date.now()
  }));

  createReportTaskAndPoll();
};

const createReportTaskAndPoll = async () => {
  try {
    const res: any = await createReportTask(userStore.userInfo?.id || 1, targetJobName.value);
    if (!res || !res.reportId) {
      isGenerating.value = false;
      sessionStorage.removeItem('reportGenerating');
      ElMessage.error('任务创建失败：服务器返回异常');
      return;
    }
    const reportId = res.reportId;
    reportTaskId.value = reportId;

    const saved = sessionStorage.getItem('reportGenerating');
    if (saved) {
      try {
        const savedState = JSON.parse(saved);
        savedState.taskId = reportId;
        sessionStorage.setItem('reportGenerating', JSON.stringify(savedState));
      } catch (e) {}
    }
    startPolling(reportId);
  } catch (error: any) {
    isGenerating.value = false;
    sessionStorage.removeItem('reportGenerating');
    ElMessage.error('任务创建失败: ' + (error?.message || '未知错误'));
  }
};

const validateAndFinish = (reportData: any) => {
  if (!reportData) {
    ElMessage.warning('报告数据为空，请刷新页面查看');
    isGenerating.value = false;
    reportTaskId.value = null;
    sessionStorage.removeItem('reportGenerating');
    return;
  }
  const content = reportData?.reportContent ?? reportData?.report_content;
  const parsedResult = parseReportContent(content);
  if (!parsedResult.ok) {
    ElMessage.warning(parsedResult.message + '，请刷新页面查看');
    report.value = reportData;
    isGenerating.value = false;
    progress.value = 100;
    currentStep.value = '报告生成完成';
    reportTaskId.value = null;
    sessionStorage.removeItem('reportGenerating');
    return;
  }
  report.value = reportData;
  isGenerating.value = false;
  isReportReady.value = true;
  progress.value = 100;
  currentStep.value = '报告生成完成';
  reportTaskId.value = null;
  sessionStorage.removeItem('reportGenerating');
  nextTick(() => { ElMessage.success('报告生成成功'); });
};

const startPolling = (reportId: number) => {
  if (pollTimer.value !== undefined) {
    clearInterval(pollTimer.value);
  }
  const startTime = Date.now();
  const POLL_TIMEOUT = 15 * 60 * 1000;

  pollTimer.value = window.setInterval(async () => {
    if (Date.now() - startTime > POLL_TIMEOUT) {
      clearInterval(pollTimer.value);
      pollTimer.value = undefined;
      try {
        const latestReport: any = await apiGetLatestReport(userStore.userInfo?.id || 1);
        if (latestReport && latestReport.reportContent) {
          report.value = latestReport;
          isGenerating.value = false;
          isReportReady.value = true;
          progress.value = 100;
          currentStep.value = '报告生成完成';
          reportTaskId.value = null;
          sessionStorage.removeItem('reportGenerating');
          nextTick(() => { ElMessage.success('报告生成成功'); });
          return;
        }
      } catch (e) {
        console.error('[POLL] 超时后最后一次获取报告失败:', e);
      }
      isGenerating.value = false;
      reportTaskId.value = null;
      sessionStorage.removeItem('reportGenerating');
      ElMessage.warning('报告生成时间较长，已在后台完成。请刷新页面查看。');
      return;
    }

    try {
      const status: any = await getReportStatus(reportId);
      progress.value = status.progress || 0;
      currentStep.value = status.stepName || '处理中...';

      sessionStorage.setItem('reportGenerating', JSON.stringify({
        isGenerating: true, taskId: reportId, progress: progress.value,
        stepName: currentStep.value, targetJobName: targetJobName.value,
        timestamp: Date.now()
      }));

      if (status.status === 'COMPLETED' || status.status === 'FINAL') {
        clearInterval(pollTimer.value);
        pollTimer.value = undefined;
        const reportFromStatus: any = status.report;
        if (reportFromStatus) {
          validateAndFinish(reportFromStatus);
          return;
        }
        try {
          const latestReport: any = await apiGetLatestReport(userStore.userInfo?.id || 1);
          if (latestReport) {
            validateAndFinish(latestReport);
            return;
          }
        } catch (e) {
          console.error('[POLL] 获取最新报告失败:', e);
        }
        ElMessage.warning('报告已生成完成，但获取内容失败，请刷新页面查看');
        isGenerating.value = false;
        reportTaskId.value = null;
        sessionStorage.removeItem('reportGenerating');
        return;
      } else if (status.status === 'FAILED') {
        clearInterval(pollTimer.value);
        pollTimer.value = undefined;
        isGenerating.value = false;
        progress.value = 0;
        currentStep.value = '生成失败';
        reportTaskId.value = null;
        sessionStorage.removeItem('reportGenerating');
        ElMessage.error('报告生成失败: ' + (status.errorMessage || '未知错误'));
      }
    } catch (error) {
      console.error('[POLL] 轮询状态失败:', error);
    }
  }, 2000);
};

const generateReport = () => startGenerateReport();

const goToProfile = () => { router.push('/profile'); };

const getMatchLevel = (score: number) => {
  if (score >= 80) return '优秀匹配';
  if (score >= 65) return '良好匹配';
  if (score >= 50) return '基本匹配';
  if (score >= 35) return '需要提升';
  return '差距较大';
};

const getScoreColor = (score: number) => {
  if (!score) return '#909399';
  if (score >= 90) return '#2a1a08';
  if (score >= 75) return '#3b2412';
  if (score >= 60) return '#6e5538';
  return '#8a7a68';
};

const editReport = () => {
  editContent.value = report.value?.reportContent || report.value?.report_content || '';
  editDialogVisible.value = true;
};
void editReport;

const saveEditedReport = async () => {
  if (!report.value) { ElMessage.error('暂无报告可保存'); return; }
  const studentId = userStore.userInfo?.id;
  if (!studentId) { ElMessage.error('未登录或学生信息缺失'); return; }
  if (!report.value.id) { ElMessage.error('报告ID缺失，请重新生成报告后再保存'); return; }

  try {
    const updated = await apiUpdateReport({
      reportId: report.value.id, studentId, reportContent: editContent.value
    });
    report.value = updated;
    editDialogVisible.value = false;
    ElMessage.success('报告已保存到服务器');
  } catch (e) {
    ElMessage.error('保存失败，请稍后重试');
  }
};

const exportPDF = async () => {
  ElMessage.info('正在生成 PDF，请稍候...');
  // 自定义标签栏使用 v-show，所有内容始终在 DOM 中，无需循环激活
  setTimeout(doExportPDF, 300);
};

const doExportPDF = async () => {
  const rd = reportData.value;
  if (!rd || !rd.section_a) {
    ElMessage.error('报告数据不完整，无法导出');
    return;
  }

  const container = printContainerRef.value;
  if (!container) return;

  const name = userStore.userInfo?.name || '用户';
  const job = targetJobName.value;
  const score = report.value?.matchScore || 0;
  const level = getMatchLevel(score);
  const scoreColor = getScoreColor(score);

  const esc = (s: string) => String(s || '').replace(/</g, '&lt;').replace(/>/g, '&gt;');

  const fourDim = fourDimensions.value;
  const fourDimHtml = fourDim.map(d => `
    <div class="dim-card">
      <div class="dim-score" style="color:${getScoreColor(d.score)}">${d.score || 0}</div>
      <div class="dim-name">${esc(d.name)}</div>
    </div>
  `).join('');

  const skillMatch = rd.section_a.professional_skill_match || {};
  const skillTableRows = (skillMatch.skill_details || []).map((r: any) => `
    <tr><td>${esc(r.skill)}</td><td>${esc(r.required_level)}</td><td>${esc(r.student_level)}</td><td>${esc(r.gap)}</td><td>${esc(r.suggestion || '')}</td></tr>
  `).join('');
  const skillTableHtml = skillTableRows ? `
    <table class="skill-table"><thead><tr><th>技能</th><th>岗位要求</th><th>您的水平</th><th>差距</th><th>学习建议</th></tr></thead><tbody>${skillTableRows}</tbody></table>
  ` : '';

  const matchedSkills = (skillMatch.matched_skills || []).map((s: any) => {
    const label = typeof s === 'object' ? s.name : s;
    return `<span class="tag tag-success">${esc(label)}</span>`;
  }).join('');
  const gapSkills = (skillMatch.gap_skills || []).map((s: any) => {
    const label = typeof s === 'object' ? s.name : s;
    return `<span class="tag tag-danger">${esc(label)}</span>`;
  }).join('');

  const softSkills = (rd.section_a.general_quality_match?.soft_skills || []).map((s: any) => `
    <div class="soft-row">
      <span>${esc(s.name)}</span><span>${s.student_score}分</span>
      <span>要求Lv.${s.required_level}</span>
      <span class="${s.gap <= 0 ? 'pos' : 'neg'}">${s.gap >= 0 ? '+' : ''}${s.gap}</span>
    </div>
  `).join('');

  const criticalGaps = filterPlaceholderSkills(rd.section_a.gap_analysis?.critical_gaps) || [];
  const moderateGaps = filterPlaceholderSkills(rd.section_a.gap_analysis?.moderate_gaps) || [];
  const minorGaps = filterPlaceholderSkills(rd.section_a.gap_analysis?.minor_gaps) || [];

  const renderGapCard = (g: any) => {
    const skillName = typeof g === 'object' ? (g.skill || '') : g;
    const reason = typeof g === 'object' ? (g.reason || '') : '';
    const learningPath = typeof g === 'object' ? (g.learning_path || '') : '';
    const meta: string[] = [];
    if (typeof g === 'object') {
      if (g.priority) meta.push(`优先级：${g.priority}`);
      if (g.difficulty) meta.push(`难度：${g.difficulty}星`);
      if (g.suggested_duration) meta.push(`建议时长：${g.suggested_duration}`);
    }
    return `<div class="gap-card">
      <div class="gap-title">${esc(skillName)}</div>
      ${reason ? `<div class="gap-reason">${esc(reason)}</div>` : ''}
      ${meta.length ? `<div class="gap-meta">${meta.map(m => `<span class="tag tag-info">${esc(m)}</span>`).join('')}</div>` : ''}
      ${learningPath ? `<div class="gap-learn"><b>学习建议：</b>${esc(learningPath)}</div>` : ''}
    </div>`;
  };

  const sectionA = `
    <div class="section">
      <div class="section-head"><span class="sec-num">01</span><span class="sec-title">职业探索与岗位匹配</span></div>
      <div class="overall-card">
        <div class="overall-left">
          <div class="big-score" style="color:${scoreColor}">${score}</div>
          <div class="score-tags"><span class="tag tag-outline">${esc(level)}</span></div>
        </div>
        <div class="overall-right">
          <div class="br-row"><span>基础要求</span><strong style="color:${scoreColor}">${rd.section_a.overall_match?.basic_requirement_score || 0}分</strong></div>
          <div class="br-row"><span>发展潜力</span><strong style="color:${scoreColor}">${rd.section_a.overall_match?.development_potential_score || 0}分</strong></div>
        </div>
      </div>
      <div class="overall-desc">${esc(rd.section_a.overall_match?.analysis || '暂无综合评价')}</div>
      <div class="four-dim">${fourDimHtml}</div>
      <div class="block">
        <div class="block-title">专业技能匹配 <span style="color:${scoreColor};font-size:20px">${skillMatch.score || 0}</span>分</div>
        ${skillTableHtml}
        <div class="skills-row">
          <div class="skills-col"><div class="col-title success">已匹配技能</div><div>${matchedSkills || '<span class="muted">暂无</span>'}</div></div>
          <div class="skills-col"><div class="col-title danger">差距技能</div><div>${gapSkills || '<span class="muted">暂无</span>'}</div></div>
        </div>
      </div>
      <div class="block">
        <div class="block-title">通用素质 <span style="color:${scoreColor}">${rd.section_a.general_quality_match?.score || 0}</span>分</div>
        <div class="soft-table">${softSkills || '<div class="muted">暂无</div>'}</div>
      </div>
      <div class="block">
        <div class="block-title">差距分析</div>
        ${criticalGaps.length ? `<div class="gap-group-title critical">关键差距（影响入职）</div><div class="gap-cards">${criticalGaps.map(renderGapCard).join('')}</div>` : ''}
        ${moderateGaps.length ? `<div class="gap-group-title moderate">中等差距（影响晋升）</div><div class="gap-cards">${moderateGaps.map(renderGapCard).join('')}</div>` : ''}
        ${minorGaps.length ? `<div class="gap-group-title minor">轻微差距</div><div class="gap-cards">${minorGaps.map(renderGapCard).join('')}</div>` : ''}
        ${!criticalGaps.length && !moderateGaps.length && !minorGaps.length ? '<div class="muted">暂无差距分析</div>' : ''}
      </div>
    </div>`;

  const sectionB = (() => {
    const sb = rd.section_b || {};
    const goalsHtml = [
      { label: '短期目标（1-2年）', content: sb.career_goals?.short_term },
      { label: '中期目标（3-5年）', content: sb.career_goals?.mid_term },
      { label: '长期目标（5年以上）', content: sb.career_goals?.long_term },
    ].map(g => `<div class="goal-card"><div class="goal-label">${g.label}</div><div>${esc(g.content || '暂无')}</div></div>`).join('');

    const analysisHtml = [
      { label: '社会需求', content: sb.industry_analysis?.social_demand },
      { label: '行业发展趋势', content: sb.industry_analysis?.industry_trend },
      { label: '岗位稳定性', content: sb.industry_analysis?.job_security },
    ].map(a => `<div class="ana-card"><div class="ana-label">${esc(a.label)}</div><div>${esc(a.content || '暂无')}</div></div>`).join('');

    const pathRows = (sb.career_path || []).map((p: any, i: number) => `
      <div class="path-step">
        <span class="path-stage">${i + 1}阶段</span>
        <span class="path-pos">${esc(p.position || '')}</span>
        <span>${esc(p.duration || '时间待定')}</span>
        ${p.salary_range ? `<span>${esc(p.salary_range)}</span>` : ''}
        ${(p.required_skills || []).length ? `<span class="path-skills">所需技能：${(Array.isArray(p.required_skills) ? p.required_skills : String(p.required_skills).split(/[,，、]/)).map((s: string) => esc(typeof s === 'string' ? s.trim() : s)).join('、')}</span>` : ''}
      </div>
    `).join('');

    const transferHtml = (sb.transfer_paths || []).map((t: any) => `
      <div class="transfer-card">
        <div class="transfer-head">
          <span class="blue">${esc(t.source || '当前岗位')}</span><span> → </span>
          <span class="green">${esc(t.target || '')}</span>
          <span class="tag tag-outline">难度 ${t.difficulty || '?'}/5</span>
        </div>
        <div>${t.duration ? `<span class="tag tag-info">转型耗时: ${esc(t.duration)}</span>` : ''} ${t.salary_change ? `<span class="tag tag-info">${esc(t.salary_change)}</span>` : ''}</div>
        ${(t.required_skills || []).length ? `<div class="transfer-skills">所需技能：${(t.required_skills || []).map((s: any) => esc(typeof s === 'object' ? s.name : s)).join('、')}</div>` : ''}
        ${t.advantage ? `<div class="transfer-adv">转型优势：${esc(t.advantage)}</div>` : ''}
        ${t.risk ? `<div class="transfer-risk">潜在风险：${esc(t.risk)}</div>` : ''}
      </div>
    `).join('');

    return `
    <div class="section">
      <div class="section-head"><span class="sec-num">02</span><span class="sec-title">职业目标与路径</span></div>
      ${sb.title ? `<div class="sub-title">${esc(sb.title)}</div>` : ''}
      <div class="block-title">职业目标设定</div>
      <div class="goals-grid">${goalsHtml}</div>
      <div class="block-title">行业分析</div>
      <div class="analysis-grid">${analysisHtml}</div>
      ${pathRows ? `<div class="block-title">垂直晋升路径</div><div class="path-list">${pathRows}</div>` : ''}
      ${transferHtml ? `<div class="block-title">横向发展路径（换岗血缘图谱）</div><div class="transfer-list">${transferHtml}</div>` : ''}
    </div>`;
  })();

  const sectionC = (() => {
    const sc = rd.section_c || {};
    const renderPlan = (plan: any, label: string) => {
      if (!plan) return '';
      return `
      <div class="plan-block">
        <div class="plan-label">${esc(label)}</div>
        <div class="plan-items">
          <div class="plan-item"><span class="plan-sub">学习路径</span>${(plan.learning_path || []).map((i: any) => `<div>· ${esc(typeof i === 'string' ? i : '')}</div>`).join('')}</div>
          <div class="plan-item"><span class="plan-sub">实践安排</span>${(plan.practice_arrangement || []).map((i: any) => `<div>· ${esc(typeof i === 'string' ? i : '')}</div>`).join('')}</div>
          <div class="plan-item"><span class="plan-sub">评估指标</span>${(plan.evaluation_indicators || []).map((i: any) => `<div>· ${esc(typeof i === 'string' ? i : '')}</div>`).join('')}</div>
        </div>
      </div>`;
    };
    const cycleHtml = [
      { label: '月度评估', content: sc.evaluation_cycle?.monthly },
      { label: '季度评估', content: sc.evaluation_cycle?.quarterly },
      { label: '年度评估', content: sc.evaluation_cycle?.yearly },
    ].map(c => `<div class="cycle-card"><div class="cycle-label">${esc(c.label)}</div><div>${esc(c.content || '暂无')}</div></div>`).join('');

    return `
    <div class="section">
      <div class="section-head"><span class="sec-num">03</span><span class="sec-title">行动计划</span></div>
      ${sc.title ? `<div class="sub-title">${esc(sc.title)}</div>` : ''}
      ${renderPlan(sc.short_term_plan, '短期计划')}
      ${renderPlan(sc.mid_term_plan, '中期计划')}
      <div class="block-title">评估周期</div>
      <div class="cycle-grid">${cycleHtml}</div>
    </div>`;
  })();

  const sectionD = (() => {
    const sd = rd.section_d || {};
    const suggHtml = (sd.polishing_suggestions || []).map((s: any, i: number) => `
      <div class="sugg-card"><div class="sugg-title">润色建议 ${i + 1}</div><div>${esc(typeof s === 'string' ? s : '')}</div></div>
    `).join('');
    const completenessItems = filteredCompletenessItems.value;
    const completenessHtml = completenessItems.map((c: string) => `<div class="warn-item">· ${esc(c)}</div>`).join('');

    return `
    <div class="section">
      <div class="section-head"><span class="sec-num">04</span><span class="sec-title">编辑优化</span></div>
      ${sd.title ? `<div class="sub-title">${esc(sd.title)}</div>` : ''}
      <div class="block-title">内容完整性检查</div>
      ${completenessHtml ? `<div class="warn-list">${completenessHtml}</div>` : '<div class="muted">内容完整</div>'}
      <div class="block-title">润色建议</div>
      <div class="sugg-list">${suggHtml || '<div class="muted">暂无建议</div>'}</div>
    </div>`;
  })();

  const html = `
<style>
*{box-sizing:border-box;margin:0;padding:0}
.section{margin-bottom:28px}
.section-head{display:flex;align-items:center;gap:12px;margin-bottom:18px;padding-bottom:10px;border-bottom:2px solid #3b2412}
.sec-num{font-size:30px;font-weight:700;font-family:Georgia,serif;color:rgba(87,64,36,0.25)}
.sec-title{font-size:20px;font-weight:700;color:#3b2412}
.sub-title{font-size:15px;color:rgba(87,64,36,0.65);margin-bottom:14px}
.block{margin-bottom:18px}
.block-title{font-size:15px;font-weight:700;color:#3b2412;margin-bottom:12px;padding:5px 0 5px 10px;border-left:4px solid #3b2412}
.overall-card{display:flex;justify-content:space-between;align-items:center;background:#faf8f4;border:1px solid rgba(87,64,36,0.1);padding:20px 24px;border-radius:2px;margin-bottom:14px}
.overall-left{display:flex;align-items:center;gap:20px}
.big-score{font-size:56px;font-weight:700;line-height:1}
.score-tags{display:flex;flex-direction:column;gap:6px}
.overall-right{display:flex;flex-direction:column;gap:8px}
.br-row{display:flex;justify-content:space-between;gap:28px;font-size:15px}
.br-row span{color:rgba(87,64,36,0.65)}
.overall-desc{font-size:14px;color:rgba(87,64,36,0.65);line-height:1.7;padding:10px 14px;background:#faf8f4;border-radius:1px;margin-bottom:16px}
.four-dim{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:20px}
.dim-card{background:#faf8f4;border:1px solid rgba(87,64,36,0.1);padding:16px 10px;border-radius:2px;text-align:center}
.dim-score{font-size:30px;font-weight:700;margin-bottom:6px}
.dim-name{font-size:13px;font-weight:600;color:#3b2412}
.skills-row{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:14px}
.skills-col{background:#faf8f4;border-radius:2px;padding:14px}
.col-title{font-size:13px;font-weight:700;margin-bottom:10px}
.col-title.success{color:#3b2412}
.col-title.danger{color:#8a7a68}
.skill-table{width:100%;border-collapse:collapse;font-size:13px;margin-top:12px}
.skill-table th,.skill-table td{border:1px solid #ddd;padding:8px 10px;text-align:left}
.skill-table th{background:rgba(87,64,36,0.04);font-weight:700}
.soft-table{display:flex;flex-direction:column;gap:8px}
.soft-row{display:flex;justify-content:space-between;background:#faf8f4;padding:10px 14px;border-radius:2px;font-size:14px;gap:20px}
.soft-row .neg{color:#8a7a68;font-weight:700}
.soft-row .pos{color:#2a1a08;font-weight:700}
.gap-cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:12px;margin-bottom:12px}
.gap-card{background:#fffdf8;border:1px solid rgba(87,64,36,0.12);border-radius:2px;padding:14px 16px}
.gap-title{font-weight:700;font-size:15px;color:#3b2412;margin-bottom:8px}
.gap-reason{font-size:13px;color:rgba(87,64,36,0.65);margin-bottom:8px}
.gap-meta{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:6px}
.gap-learn{font-size:13px;color:#3b2412;background:#faf8f4;padding:8px 10px;border-radius:1px;margin-top:8px}
.gap-group-title{font-size:13px;font-weight:700;margin-bottom:10px;margin-top:14px}
.gap-group-title.critical{color:#3b2412}
.gap-group-title.moderate{color:#6e5538}
.gap-group-title.minor{color:#909399}
.goals-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:18px}
.goal-card{background:#faf8f4;border-radius:2px;padding:16px;border-top:4px solid}
.goal-card:nth-child(1){border-color:#2a1a08}
.goal-card:nth-child(2){border-color:#3b2412}
.goal-card:nth-child(3){border-color:#6e5538}
.goal-label{font-size:12px;font-weight:700;margin-bottom:8px;color:rgba(87,64,36,0.65)}
.analysis-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:18px}
.ana-card{background:#faf8f4;border-radius:2px;padding:16px}
.ana-label{font-size:13px;font-weight:600;margin-bottom:8px;color:#3b2412}
.path-list{display:flex;flex-direction:column;gap:10px;margin-bottom:18px}
.path-step{display:flex;align-items:center;gap:14px;background:#faf8f4;padding:12px 16px;border-radius:2px;font-size:14px;flex-wrap:wrap}
.path-stage{font-weight:700;color:#3b2412;font-size:12px;flex-shrink:0}
.path-pos{font-weight:600;color:#3b2412}
.path-skills{color:rgba(87,64,36,0.65);font-size:13px;flex-basis:100%}
.transfer-list{display:flex;flex-direction:column;gap:12px;margin-bottom:18px}
.transfer-card{background:#faf8f4;border-radius:2px;padding:14px}
.transfer-head{display:flex;align-items:center;gap:10px;font-size:14px;margin-bottom:8px;flex-wrap:wrap}
.blue{color:#3b2412;font-weight:700}
.green{color:#2a1a08;font-weight:700}
.transfer-skills{font-size:13px;color:rgba(87,64,36,0.65);margin-top:6px}
.transfer-adv{font-size:13px;color:#2a1a08;margin-top:6px}
.transfer-risk{font-size:13px;color:#8a7a68;margin-top:6px}
.plan-block{background:#faf8f4;border-radius:2px;padding:18px;margin-bottom:16px}
.plan-label{font-size:14px;font-weight:700;color:#3b2412;margin-bottom:12px;padding-left:10px;border-left:4px solid #3b2412}
.plan-items{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}
.plan-item{font-size:13px;color:rgba(87,64,36,0.65)}
.plan-sub{font-weight:700;color:#3b2412;display:block;margin-bottom:6px}
.cycle-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}
.cycle-card{background:#faf8f4;border-radius:2px;padding:16px}
.cycle-label{font-size:13px;font-weight:700;margin-bottom:8px;color:#3b2412}
.warn-list{display:flex;flex-direction:column;gap:8px;margin-bottom:16px}
.warn-item{background:#fffdf8;border:1px solid rgba(139,122,104,0.2);padding:10px 14px;border-radius:1px;font-size:13px}
.sugg-list{display:grid;grid-template-columns:1fr 1fr;gap:12px}
.sugg-card{background:#faf8f4;border-radius:2px;padding:14px}
.sugg-title{font-size:13px;font-weight:700;margin-bottom:8px;color:#3b2412}
.tag{display:inline-block;padding:3px 10px;border-radius:4px;font-size:12px;margin:3px}
.tag-success{background:#faf8f4;color:#2a1a08;border:1px solid rgba(42,26,8,0.2)}
.tag-danger{background:#fffdf8;color:#8a7a68;border:1px solid rgba(138,122,104,0.2)}
.tag-info{background:#faf8f4;color:#6e5538;border:1px solid rgba(110,85,56,0.15)}
.tag-outline{border:1px solid rgba(87,64,36,0.3);color:#3b2412;background:#fff}
.muted{color:#999;font-size:13px}
</style>
<div style="width:800px;padding:24px;font-family:'Microsoft YaHei','PingFang SC',sans-serif;font-size:15px;color:#1a1a1a;background:#fff;box-sizing:border-box">
  <div style="border-bottom:3px solid #3b2412;padding-bottom:14px;margin-bottom:22px">
    <h1 style="font-size:24px;font-weight:700;margin:0 0 8px 0;letter-spacing:2px">${esc(name)} 的职业发展报告</h1>
    <div style="color:rgba(87,64,36,0.65);font-size:13px;display:flex;gap:24px;flex-wrap:wrap">
      <span>目标岗位：<strong>${esc(job)}</strong></span>
      <span>综合匹配度：<span style="font-size:30px;font-weight:700">${score}</span>分（${esc(level)}）</span>
    </div>
  </div>
  ${sectionA}
  ${sectionB}
  ${sectionC}
  ${sectionD}
</div>`;

  container.innerHTML = html;
  document.body.appendChild(container);

  const originalCard = document.querySelector('.report-card') as HTMLElement;
  if (originalCard) originalCard.style.display = 'none';

  try {
    const savedBodyMargin = document.body.style.margin;
    const savedBodyPadding = document.body.style.padding;
    document.body.style.margin = '0';
    document.body.style.padding = '0';

    container.style.position = 'absolute';
    container.style.left = '0';
    container.style.top = '0';
    container.style.zIndex = '-1';
    container.style.display = 'block';

    const scale = 2;
    const canvas = await html2canvas(container, {
      scale, windowWidth: 800, useCORS: true, allowTaint: true,
      backgroundColor: '#ffffff', logging: false,
      onclone: (clonedDoc) => {
        const clonedContainer = clonedDoc.querySelector('[data-html2canvas-container]') || clonedDoc.body.lastElementChild;
        if (clonedContainer) {
          (clonedContainer as HTMLElement).style.display = 'block';
          (clonedContainer as HTMLElement).style.width = '800px';
        }
      },
    });

    document.body.style.margin = savedBodyMargin;
    document.body.style.padding = savedBodyPadding;

    const pageWidth = 210;
    const pageHeight = 297;
    const margin = 10;
    const printWidthMm = pageWidth - margin * 2;
    const printHeightMm = pageHeight - margin * 2;
    const pxPerMm = canvas.width / printWidthMm;
    const totalContentHeightMm = canvas.height / pxPerMm;
    const totalPages = Math.ceil(totalContentHeightMm / printHeightMm);

    const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });

    for (let i = 0; i < totalPages; i++) {
      if (i > 0) pdf.addPage();
      const srcY = Math.round(i * printHeightMm * pxPerMm);
      const srcH = Math.min(Math.round(printHeightMm * pxPerMm), canvas.height - srcY);
      const cropCanvas = document.createElement('canvas');
      cropCanvas.width = canvas.width;
      cropCanvas.height = srcH;
      const ctx = cropCanvas.getContext('2d')!;
      ctx.drawImage(canvas, 0, srcY, canvas.width, srcH, 0, 0, canvas.width, srcH);
      const pageImg = cropCanvas.toDataURL('image/jpeg', 0.92);
      pdf.addImage(pageImg, 'JPEG', margin, margin, printWidthMm, srcH / pxPerMm);
    }

    const fileName = `${name}_职业发展报告_${Date.now()}.pdf`;
    pdf.save(fileName);
    ElMessage.success('PDF 导出成功');
  } catch (err) {
    console.error('PDF 导出失败:', err);
    ElMessage.error('PDF 导出失败，请重试');
  } finally {
    if (container.parentNode) document.body.removeChild(container);
    if (originalCard) originalCard.style.display = '';
    activeTab.value = 'section_a';
  }
};

const confirmRegenerate = () => {
  if (jobs.value.length === 0) {
    ElMessage.warning('岗位列表加载中，请稍后再试');
    return;
  }
  tempSelectedJob.value = targetJobName.value || (report.value?.targetJobName || report.value?.target_job_name) || '';
  jobSelectDialogVisible.value = true;
};

const confirmJobAndRegenerate = () => {
  if (!tempSelectedJob.value) {
    ElMessage.warning('请选择目标岗位');
    return;
  }
  targetJobName.value = tempSelectedJob.value;
  jobSelectDialogVisible.value = false;
  startGenerateReport();
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&display=swap');

/* =============== Page =============== */
.career-report {
  min-height: 100vh;
  background: #faf9f7;
  padding: 16px 32px;
  position: relative;
  overflow-x: hidden;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #1a1a1a;
}

/* ---- 浮动职业名称 ---- */
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
  pointer-events: auto;
}

.career-drift:hover { animation-play-state: paused; }

.career-word {
  display: inline-block;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: #1a1a1a;
  padding: 2px 6px;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1), opacity 0.3s ease, background 0.3s ease, border-radius 0.3s ease;
  border-radius: 0;
}

.career-drift:hover .career-word {
  transform: scale(2);
  opacity: 0.85 !important;
  background: rgba(253, 252, 251, 0.95);
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.10);
}

/* ---- Paper base ---- */
.paper {
  position: relative;
  z-index: 1;
  background: var(--paper-surface);
  background-color: var(--paper-bg-solid);
  box-shadow: var(--paper-shadow);
  border-radius: 1px;
  border: 1px solid var(--paper-border);
}

.paper::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--paper-overlay);
  pointer-events: none;
}

/* ---- SVG decorations ---- */
.icon-deco {
  position: absolute;
  width: 44px;
  height: 44px;
  opacity: 0.15;
  pointer-events: none;
  z-index: 0;
}
.icon-deco.bottom-right { bottom: 16px; right: 16px; }

/* ---- Divider ---- */
.divider {
  height: 1px;
  background: rgba(87, 64, 36, 0.15);
  margin: 20px 0;
}

/* ---- Action line button ---- */
.action-line {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px 0;
  font-family: inherit;
  transition: opacity 0.15s ease;
  color: #1a1a1a;
}
.action-line:hover { opacity: 0.5; }
.action-line:active { opacity: 0.3; }
.action-line:disabled { cursor: not-allowed; opacity: 0.3; }

.action-dash {
  display: inline-block;
  width: 60px;
  height: 1px;
  background: #1a1a1a;
}

.action-text {
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 4px;
}

/* ---- Field ---- */
.field {
  margin-bottom: 20px;
}
.field-label {
  display: block;
  font-size: 15px;
  font-weight: 400;
  color: #1a1a1a;
  letter-spacing: 2px;
  margin-bottom: 8px;
}
.field-line {
  border-bottom: 1px solid rgba(87, 64, 36, 0.25);
  transition: border-color 0.2s ease;
}
.field-line:focus-within { border-color: #3b2412; }
.field-line :deep(.el-input__wrapper) {
  box-shadow: none !important;
  background: transparent !important;
}
.field-line :deep(.el-select) {
  width: 100%;
}

/* ---- Paper alert ---- */
.paper-alert {
  padding: 12px 16px;
  border: 1px solid rgba(87, 64, 36, 0.12);
  border-left: 3px solid rgba(87, 64, 36, 0.4);
  font-size: 14px;
  line-height: 1.6;
  color: #3b2412;
  background: rgba(87, 64, 36, 0.03);
}
.paper-alert.warn {
  border-left-color: rgba(87, 64, 36, 0.4);
  background: rgba(87, 64, 36, 0.04);
}
.paper-link {
  color: #1a1a1a;
  text-decoration: underline;
  text-underline-offset: 2px;
  font-weight: 700;
}

/* ---- Paper tag ---- */
.ptag {
  display: inline-block;
  padding: 2px 10px;
  border: 1px solid rgba(87, 64, 36, 0.2);
  border-radius: 2px;
  font-size: 13px;
  color: #3b2412;
  margin: 3px;
  letter-spacing: 0.5px;
  background: rgba(87, 64, 36, 0.06);
}
.ptag.dark {
  background: rgba(87, 64, 36, 0.06);
  color: #3b2412;
  border-color: rgba(87, 64, 36, 0.2);
}
.ptag.small {
  font-size: 12px;
  padding: 1px 8px;
}
.tag-sub {
  font-size: 11px;
  color: rgba(87, 64, 36, 0.55);
}
.ptag.dark .tag-sub {
  color: rgba(87, 64, 36, 0.6);
}

/* ---- Muted text ---- */
.muted-text {
  color: rgba(87, 64, 36, 0.4);
  font-size: 13px;
}

/* ================================================================
   Page Header
   ================================================================ */
.page-header {
  max-width: 960px;
  margin: 0 auto 20px;
  padding: 14px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 4px;
  margin: 0;
  color: #1a1a1a;
}
.header-actions {
  display: flex;
  gap: 16px;
}

/* ================================================================
   Generating State
   ================================================================ */
.gen-paper {
  max-width: 740px;
  margin: 40px auto;
  padding: 40px 36px;
}
.gen-content {
  text-align: center;
}
.gen-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 3px;
  margin: 0 0 8px;
}
.gen-hint {
  font-size: 14px;
  color: rgba(87, 64, 36, 0.55);
  margin: 0 0 36px;
}

/* Progress ring */
.gen-progress-ring {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 28px;
}
.ring-svg { width: 100%; height: 100%; }
.ring-fill { transition: stroke-dashoffset 0.5s ease; }
.ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #3b2412;
}

/* Steps */
.gen-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 360px;
  margin: 0 auto 24px;
  text-align: left;
}
.gen-step {
  display: flex;
  align-items: center;
  gap: 12px;
  opacity: 0.4;
  transition: opacity 0.3s;
}
.gen-step.active, .gen-step.done { opacity: 1; }

.gen-dot {
  width: 28px;
  height: 28px;
  border: 1.5px solid rgba(87, 64, 36, 0.25);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: rgba(87, 64, 36, 0.4);
  flex-shrink: 0;
  transition: all 0.3s;
}
.gen-step.active .gen-dot {
  border-color: #3b2412;
  color: #3b2412;
}
.gen-step.done .gen-dot {
  background: #3b2412;
  border-color: #3b2412;
  color: #fdfcfb;
}

.gen-step-bar {
  flex: 1;
  height: 4px;
  background: rgba(87, 64, 36, 0.08);
  border-radius: 2px;
  overflow: hidden;
}
.gen-step-fill {
  height: 100%;
  background: #3b2412;
  transition: width 0.4s ease;
  border-radius: 2px;
}

.gen-step-name {
  font-size: 14px;
  color: #3b2412;
  min-width: 72px;
}

.gen-step-status {
  font-size: 11px;
  color: rgba(87, 64, 36, 0.5);
  min-width: 50px;
  text-align: right;
}
.gen-step.active .gen-step-status {
  color: #3b2412;
  font-weight: 600;
}
.gen-step.done .gen-step-status {
  color: rgba(87, 64, 36, 0.4);
}

.gen-current {
  font-size: 13px;
  color: rgba(87, 64, 36, 0.5);
  margin: 0;
}

/* ================================================================
   Empty State
   ================================================================ */
.empty-wrapper {
  max-width: 700px;
  margin: 40px auto;
}
.empty-paper {
  padding: 40px 36px;
}
.empty-content {
  text-align: center;
}
.empty-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0 0 4px;
  color: #3b2412;
}
.empty-desc {
  font-size: 14px;
  color: rgba(87, 64, 36, 0.55);
  margin: 0 0 28px;
  line-height: 1.6;
}
.empty-form {
  max-width: 340px;
  margin: 0 auto;
  text-align: center;
}
.empty-form .action-line {
  margin-top: 16px;
}

/* ================================================================
   Report Content
   ================================================================ */
.report-scroll {
  max-width: 960px;
  margin: 0 auto;
}

.report-paper {
  position: relative;
  padding: 48px 56px 40px;
  border-top: 1px solid rgba(87, 64, 36, 0.08);
}

/* Report header */
.report-head {
  margin-bottom: 8px;
  text-align: center;
  border-bottom: 1px solid rgba(87,64,36,0.08);
  padding-bottom: 20px;
}
.report-head-title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 4px;
  margin: 0 0 0;
  color: #3b2412;
}
.report-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  font-size: 14px;
  color: rgba(87, 64, 36, 0.55);
  margin-top: 12px;
}
.report-meta .meta-label { color: rgba(87, 64, 36, 0.5); }
.report-meta .meta-value { font-weight: 700; color: #3b2412; }
.report-meta .meta-value.score { font-size: 20px; }
.report-meta .meta-sep { margin: 0 8px; }
.report-meta .meta-level { color: rgba(87, 64, 36, 0.5); }

/* ---- Custom tab bar ---- */
.paper-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid rgba(87, 64, 36, 0.15);
  margin: 20px 0 28px;
  background: transparent;
  padding: 0;
  border-radius: 0;
}
.paper-tab {
  padding: 12px 24px;
  cursor: pointer;
  font-size: 14px;
  color: rgba(87, 64, 36, 0.5);
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
  user-select: none;
  background: none;
  letter-spacing: 1px;
  position: relative;
}
.paper-tab:hover { color: #3b2412; background: rgba(87,64,36,0.02); }
.paper-tab.active {
  color: #3b2412;
  border-bottom-color: #3b2412;
  font-weight: 700;
}
.tab-num {
  font-size: 20px;
  font-weight: 700;
  font-family: Georgia, serif;
  color: rgba(87, 64, 36, 0.2);
  transition: color 0.2s;
}
.paper-tab.active .tab-num { color: #3b2412; }

/* ---- Section panels ---- */
.section-panels {
  min-height: 200px;
}
.section-panel {
  padding: 4px 0;
}
.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #3b2412;
  margin: 0 0 20px;
  letter-spacing: 2px;
}

/* ---- Paper section ---- */
.psection {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
}
.psection:last-child { border-bottom: none; }
.psection-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}
.psection-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 1px;
  border-left: 3px solid rgba(87, 64, 36, 0.15);
  padding-left: 12px;
}
.psection-score {
  font-size: 22px;
  font-weight: 700;
}
.psection-duration {
  font-size: 13px;
  color: rgba(87, 64, 36, 0.5);
}

/* ---- Overall match ---- */
/* Overall match section */
.overall-paper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  background: rgba(87, 64, 36, 0.025);
  border: 1px solid rgba(87, 64, 36, 0.08);
  margin-bottom: 14px;
  gap: 24px;
}
.overall-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
  min-width: 0;
}
.overall-score {
  font-size: 52px;
  font-weight: 700;
  line-height: 1;
}
.overall-level {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.overall-right {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  flex: 1;
}
.breakdown-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  font-size: 14px;
}
.br-label { color: rgba(87, 64, 36, 0.55); }
.br-score { font-weight: 700; }
.overall-analysis {
  font-size: 14px;
  color: rgba(87, 64, 36, 0.65);
  line-height: 1.7;
  padding: 12px 16px;
  background: rgba(87, 64, 36, 0.03);
  border-radius: 1px;
}

/* ---- Four dimensions ---- */
.four-dim-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 28px;
}
.dim-card {
  padding: 18px 12px 14px;
  text-align: center;
  background: rgba(87, 64, 36, 0.02);
  border: 1px solid rgba(87, 64, 36, 0.08);
  transition: box-shadow 0.2s, transform 0.2s;
}
.dim-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(87,64,36,0.06);
}

.dim-card-ring {
  position: relative;
  width: 56px;
  height: 56px;
  margin: 0 auto 8px;
}
.dim-card-svg {
  width: 100%;
  height: 100%;
}
.dim-card-ring-fill {
  transition: stroke-dasharray 0.8s ease;
}
.dim-card-ring-num {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.dim-name {
  font-size: 13px;
  font-weight: 600;
  color: #3b2412;
  margin-bottom: 4px;
}
.dim-desc {
  font-size: 11px;
  color: rgba(87, 64, 36, 0.5);
}

/* ---- Progress bar ---- */
.pbar {
  height: 6px;
  background: rgba(87, 64, 36, 0.08);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 16px;
}
.pbar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

/* ---- Analysis note ---- */
.analysis-note {
  padding: 12px 16px;
  border-left: 3px solid rgba(87, 64, 36, 0.2);
  font-size: 14px;
  color: rgba(87, 64, 36, 0.65);
  line-height: 1.6;
  margin-bottom: 16px;
}

/* ---- Paper table ---- */
.sub-title {
  font-size: 14px;
  font-weight: 700;
  color: #3b2412;
  margin-bottom: 12px;
}
.paper-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-bottom: 16px;
}
.paper-table th, .paper-table td {
  border: 1px solid rgba(26, 26, 26, 0.12);
  padding: 8px 10px;
  text-align: left;
}
.paper-table th {
  background: rgba(26, 26, 26, 0.04);
  font-weight: 700;
  font-size: 12px;
  color: #1a1a1a;
}
.skill-table-wrap {
  margin-bottom: 16px;
}

/* Skill legend */
.skill-legend {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
  font-size: 12px;
  color: rgba(87, 64, 36, 0.55);
}
.skill-legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.skill-legend-dot {
  display: inline-block;
  width: 16px;
  height: 6px;
  border-radius: 1px;
}
.skill-legend-dot.required {
  background: rgba(87, 64, 36, 0.45);
}
.skill-legend-dot.current {
  background: rgba(87, 64, 36, 0.18);
}

/* Skill gap indicator */
.skill-gap-indicator {
  font-size: 12px;
  font-weight: 600;
  color: rgba(87, 64, 36, 0.65);
  letter-spacing: 0.5px;
}
.skill-gap-indicator.no-gap {
  color: #3b2412;
}

/* ---- Skills split ---- */
.skills-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.skills-col {
  padding: 14px;
  border: 1px solid rgba(87, 64, 36, 0.08);
}
.col-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 10px;
  color: #3b2412;
}
.count-badge {
  display: inline-block;
  background: rgba(87, 64, 36, 0.08);
  color: #3b2412;
  font-size: 11px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 2px;
  margin-left: 4px;
}
.count-badge.warn {
  background: rgba(87, 64, 36, 0.15);
}
.col-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

/* ---- Soft skills ---- */
.soft-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.soft-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: rgba(87, 64, 36, 0.02);
  transition: background 0.15s;
}
.soft-row:hover {
  background: rgba(87, 64, 36, 0.04);
}
.soft-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 100px;
}
.soft-check {
  font-size: 14px;
  color: rgba(87, 64, 36, 0.4);
}
.soft-check.ok { color: #3b2412; }
.soft-name {
  font-size: 14px;
  font-weight: 500;
  color: #3b2412;
}
.soft-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  justify-content: flex-end;
}
.soft-bar-wrap {
  width: 100px;
}
.soft-bar-track {
  height: 4px;
  background: rgba(87,64,36,0.06);
  border-radius: 2px;
  overflow: hidden;
}
.soft-bar-fill {
  height: 100%;
  background: #3b2412;
  border-radius: 2px;
  transition: width 0.5s ease;
}
.soft-gap {
  font-weight: 700;
  font-size: 13px;
  min-width: 32px;
  text-align: right;
}
.soft-gap.pos { color: #3b2412; }
.soft-gap.neg { color: rgba(87, 64, 36, 0.4); }

/* ---- Gap analysis ---- */
.gap-group {
  margin-bottom: 18px;
}
.gap-group-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 10px;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 6px;
}
.gap-group-title::before {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  flex-shrink: 0;
}
.gap-group-title.critical { color: #3b2412; }
.gap-group-title.critical::before { background: #3b2412; }
.gap-group-title.moderate { color: #6e5538; }
.gap-group-title.moderate::before { background: #6e5538; }
.gap-group-title.minor { color: rgba(87, 64, 36, 0.45); }
.gap-group-title.minor::before { background: rgba(87, 64, 36, 0.3); }

.gap-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 10px;
}
.gap-card {
  padding: 14px;
  border: 1px solid rgba(87, 64, 36, 0.08);
  border-left: 3px solid #3b2412;
  transition: box-shadow 0.2s;
}
.gap-card:hover {
  box-shadow: 0 2px 8px rgba(87,64,36,0.06);
}
.gap-card.moderate { border-left-color: #6e5538; }
.gap-card.minor { border-left-color: rgba(87, 64, 36, 0.3); }

.gap-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.gap-skill-name {
  font-weight: 700;
  font-size: 14px;
  color: #3b2412;
}
.gap-card-body {
  padding-left: 2px;
}
.gap-reason {
  font-size: 13px;
  color: rgba(87, 64, 36, 0.65);
  line-height: 1.5;
  margin: 0;
}
.gap-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.gap-suggestion {
  margin-top: 10px;
  padding: 10px 12px;
  background: rgba(87, 64, 36, 0.03);
  border-left: 2px solid rgba(87, 64, 36, 0.2);
}
.gap-sug-label {
  font-size: 12px;
  font-weight: 700;
  color: #3b2412;
  margin-bottom: 4px;
  display: block;
}
.gap-suggestion p {
  font-size: 12px;
  color: rgba(87, 64, 36, 0.65);
  line-height: 1.5;
  margin: 0;
}
.gap-empty {
  padding: 24px;
  text-align: center;
}

/* ---- Goals ---- */
.goals-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.goal-card {
  padding: 20px;
  border: 1px solid rgba(87, 64, 36, 0.08);
  border-top: 3px solid #3b2412;
  text-align: left;
  transition: box-shadow 0.2s;
}
.goal-card:hover {
  box-shadow: 0 4px 12px rgba(87,64,36,0.06);
}
.goal-card:nth-child(2) { border-top-color: #6e5538; }
.goal-card:nth-child(3) { border-top-color: rgba(87, 64, 36, 0.3); }

.goal-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.goal-num {
  font-size: 18px;
  font-weight: 700;
  color: rgba(26,26,26,0.15);
  font-family: Georgia, serif;
}

.goal-badge {
  display: inline-block;
  font-size: 12px;
  font-weight: 700;
  color: #3b2412;
  letter-spacing: 1px;
}
.goal-period {
  font-size: 13px;
  color: rgba(87, 64, 36, 0.55);
  margin-bottom: 8px;
}
.goal-content {
  font-size: 14px;
  color: #3b2412;
  line-height: 1.6;
}

/* ---- Analysis grid ---- */
.analysis-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.ana-card {
  padding: 16px;
  border: 1px solid rgba(87, 64, 36, 0.08);
}
.ana-label {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
  color: #3b2412;
}
.ana-content {
  font-size: 14px;
  color: rgba(87, 64, 36, 0.65);
  line-height: 1.6;
}

/* ---- Node chain (career path) ---- */
.node-chain {
  display: flex;
  flex-direction: column;
}
.node-item {
  display: flex;
  gap: 16px;
}
.node-dot-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}
.node-dot {
  width: 14px;
  height: 14px;
  border: 2px solid #3b2412;
  border-radius: 50%;
  background: #fdfcfb;
  flex-shrink: 0;
  margin-top: 4px;
  transition: all 0.2s;
}
.node-dot.first { background: #3b2412; }
.node-line {
  width: 1.5px;
  flex: 1;
  min-height: 20px;
  background: linear-gradient(to bottom, rgba(87,64,36,0.25), rgba(87,64,36,0.08));
}
.node-body {
  padding-bottom: 20px;
  flex: 1;
}
.node-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 4px;
}
.node-stage {
  font-size: 11px;
  font-weight: 700;
  color: rgba(87, 64, 36, 0.55);
  padding: 1px 6px;
  border: 1px solid rgba(87,64,36,0.15);
  border-radius: 2px;
}
.node-position {
  font-size: 15px;
  font-weight: 700;
  color: #3b2412;
}
.node-meta {
  font-size: 13px;
  color: rgba(87, 64, 36, 0.55);
  margin-bottom: 6px;
}
.node-skills {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}
.node-skills-label {
  font-size: 12px;
  color: rgba(87, 64, 36, 0.55);
}
.node-milestone {
  font-size: 12px;
  color: rgba(87, 64, 36, 0.55);
  margin-top: 4px;
}

/* ---- Transfer cards ---- */
.transfer-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.transfer-card {
  padding: 16px;
  border: 1px solid rgba(87, 64, 36, 0.08);
}
.transfer-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 700;
  font-size: 14px;
}
.transfer-src { color: #3b2412; }
.transfer-arrow { color: rgba(87, 64, 36, 0.4); }
.transfer-tgt { color: #6e5538; }
.transfer-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.transfer-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.transfer-sub-title {
  font-size: 12px;
  font-weight: 700;
  color: #3b2412;
  margin-bottom: 6px;
}
.transfer-skills-section {
  padding: 10px;
  border: 1px solid rgba(87, 64, 36, 0.06);
}
.transfer-skills-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.transfer-skill-item {
  padding-bottom: 6px;
  border-bottom: 1px dashed rgba(87, 64, 36, 0.1);
}
.transfer-skill-item:last-child { border-bottom: none; padding-bottom: 0; }
.skill-req {
  font-size: 11px;
  color: rgba(87, 64, 36, 0.55);
  margin: 2px 0 0;
  line-height: 1.4;
}
.transfer-adv {
  padding: 10px;
  border-left: 2px solid rgba(87, 64, 36, 0.2);
}
.transfer-adv p { font-size: 13px; color: #6e5538; margin: 0; line-height: 1.5; }
.transfer-risk {
  padding: 10px;
  border-left: 2px solid rgba(87, 64, 36, 0.3);
}
.transfer-risk p { font-size: 13px; color: #6e5538; margin: 0; line-height: 1.5; }
.transfer-plan {
  padding: 10px;
  border: 1px solid rgba(87, 64, 36, 0.06);
}

/* Plan timeline inside transfers */
.plan-timeline {
  display: flex;
  flex-direction: column;
}
.plan-phase {
  display: flex;
  gap: 12px;
}
.phase-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(87, 64, 36, 0.35);
  margin-top: 5px;
  flex-shrink: 0;
}
.phase-dot.first { background: #3b2412; }
.phase-body {
  padding-bottom: 12px;
}
.phase-title {
  font-size: 12px;
  font-weight: 700;
  color: #3b2412;
  margin-bottom: 4px;
}
.phase-actions {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.phase-action {
  font-size: 12px;
  color: rgba(87, 64, 36, 0.65);
  line-height: 1.4;
}

/* ---- Plan grid (section C) ---- */
.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.plan-col {
  padding: 16px;
  border: 1px solid rgba(87, 64, 36, 0.08);
}
.plan-col-title {
  font-size: 13px;
  font-weight: 700;
  color: #3b2412;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(87, 64, 36, 0.1);
}
.plan-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.plan-list li {
  font-size: 13px;
  color: rgba(87, 64, 36, 0.65);
  line-height: 1.5;
  margin-bottom: 6px;
}
.plan-list li:last-child { margin-bottom: 0; }

/* ---- Cycle grid ---- */
.cycle-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.cycle-card {
  padding: 16px;
  border: 1px solid rgba(87, 64, 36, 0.08);
}
.cycle-label {
  font-size: 13px;
  font-weight: 700;
  color: #3b2412;
  margin-bottom: 8px;
}
.cycle-content {
  font-size: 14px;
  color: rgba(87, 64, 36, 0.65);
  line-height: 1.6;
}

/* ---- Completeness check ---- */
.check-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}
.check-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid rgba(87, 64, 36, 0.08);
  font-size: 13px;
  color: #3b2412;
}
.check-dot {
  color: rgba(87, 64, 36, 0.4);
  flex-shrink: 0;
}
.check-dot.ok { color: #3b2412; }
.check-ok {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #3b2412;
  padding: 12px 0;
}

/* ---- Suggestion grid ---- */
.sugg-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.sugg-card {
  padding: 16px;
  border: 1px solid rgba(87, 64, 36, 0.08);
  transition: box-shadow 0.2s;
}
.sugg-card:hover {
  box-shadow: 0 2px 8px rgba(87,64,36,0.05);
}
.sugg-head {
  font-size: 13px;
  font-weight: 700;
  color: #3b2412;
  margin-bottom: 8px;
}
.sugg-body {
  font-size: 14px;
  color: rgba(87, 64, 36, 0.65);
  line-height: 1.6;
}

/* ================================================================
   Dialogs — paper overlay style
   ================================================================ */
:deep(.paper-dialog .el-dialog) {
  background: #fdfcfb;
  border-radius: 1px;
  box-shadow: 0 1px 1px rgba(0,0,0,0.03), 0 4px 8px rgba(0,0,0,0.03), 0 12px 24px rgba(0,0,0,0.04), 0 28px 52px rgba(0,0,0,0.05);
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  max-width: 90vw;
}
:deep(.paper-dialog .el-dialog__header) {
  border-bottom: 1px solid rgba(26, 26, 26, 0.1);
  padding: 16px 24px;
}
:deep(.paper-dialog .el-dialog__title) {
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-weight: 700;
  letter-spacing: 2px;
  color: #1a1a1a;
  font-size: 16px;
}
:deep(.paper-dialog .el-dialog__body) {
  padding: 24px;
}
:deep(.paper-dialog .el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid rgba(26, 26, 26, 0.08);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.dialog-body {
  padding: 4px 0;
}

/* Dialog action buttons — match global action-line style */
:deep(.paper-dialog .el-dialog__footer) .action-line {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: rgba(26, 26, 26, 0.03);
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-left: 3px solid rgba(26, 26, 26, 0.18);
  border-radius: 2px;
  padding: 10px 24px;
  cursor: pointer;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 2px;
  transition: all 0.22s ease;
  margin: 0;
}
:deep(.paper-dialog .el-dialog__footer) .action-line:hover {
  background: rgba(26, 26, 26, 0.07);
  border-color: rgba(26, 26, 26, 0.22);
  border-left-color: rgba(26, 26, 26, 0.35);
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}
:deep(.paper-dialog .el-dialog__footer) .action-line:active {
  background: rgba(26, 26, 26, 0.10);
  transform: translateY(0);
  box-shadow: none;
}
:deep(.paper-dialog .el-dialog__footer) .action-line:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background: rgba(26, 26, 26, 0.02);
}
:deep(.paper-dialog .el-dialog__footer) .action-line .action-dash {
  display: inline-block;
  width: 40px;
  height: 1px;
  background: #1a1a1a;
}
:deep(.paper-dialog .el-dialog__footer) .action-line .action-text {
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 2px;
}

/* ================================================================
   Footer
   ================================================================ */
.footer {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-top: 28px;
  font-size: 11px;
  color: rgba(87, 64, 36, 0.35);
  letter-spacing: 0.5px;
}

/* ================================================================
   Responsive
   ================================================================ */
@media (max-width: 900px) {
  .overall-paper { flex-wrap: wrap; }
  .overall-left { flex: 1 1 auto; }
  .overall-right { flex: 1 1 200px; }
  .four-dim-grid { grid-template-columns: repeat(2, 1fr); }
  .skills-split, .plan-grid, .goals-grid, .analysis-grid, .cycle-grid, .sugg-grid { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .career-report { padding: 12px; }
  .page-header { padding: 14px 20px; flex-direction: column; gap: 12px; }
  .page-title { font-size: 18px; }
  .gen-paper { padding: 32px 20px; }
  .gen-title { font-size: 18px; }
  .empty-paper { padding: 32px 20px; }
  .report-paper { padding: 32px 20px 24px; }
  .report-head-title { font-size: 20px; letter-spacing: 2px; }
  .paper-tabs { overflow-x: auto; }
  .paper-tab { padding: 8px 16px; font-size: 13px; }
  .tab-num { font-size: 15px; }
  .four-dim-grid { grid-template-columns: repeat(2, 1fr); }
  .skills-split, .plan-grid, .goals-grid, .analysis-grid, .cycle-grid, .sugg-grid { grid-template-columns: 1fr; }
  .overall-paper { flex-direction: column; gap: 16px; align-items: stretch; }
  .overall-left { flex-direction: column; align-items: center; }
  .gap-cards { grid-template-columns: 1fr; }
  .action-dash { width: 40px; }
  .chapter-watermark { font-size: 60px; }
  .skill-bar-chart { flex-direction: column; }
  .timeline-content { padding-left: 12px; }
}

/* ================================================================
   NEW: Paper-rise Stagger Animation
   ================================================================ */
@keyframes paper-rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.section-animate {
  animation: paper-rise 0.5s ease both;
}
.section-animate[data-section="b"] { animation-delay: 0.08s; }
.section-animate[data-section="c"] { animation-delay: 0.16s; }
.section-animate[data-section="d"] { animation-delay: 0.24s; }

/* Stagger psections within each section panel */
.section-animate .psection {
  animation: paper-rise 0.4s ease both;
}
.section-animate .psection:nth-child(2) { animation-delay: 0.1s; }
.section-animate .psection:nth-child(3) { animation-delay: 0.2s; }
.section-animate .psection:nth-child(4) { animation-delay: 0.3s; }
.section-animate .psection:nth-child(5) { animation-delay: 0.4s; }

/* ================================================================
   NEW: Chapter Watermark Decoration
   ================================================================ */
.chapter-watermark {
  position: absolute;
  top: 8px;
  right: 16px;
  font-size: 90px;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  color: rgba(26, 26, 26, 0.04);
  font-weight: 700;
  line-height: 1;
  pointer-events: none;
  user-select: none;
  z-index: 0;
}

.section-panel {
  position: relative;
}

/* ================================================================
   NEW: SVG Score Ring
   ================================================================ */
.score-ring-wrap {
  position: relative;
  width: 120px;
  height: 120px;
  min-width: 120px;
  flex-shrink: 0;
}
.score-ring-svg {
  width: 100%;
  height: 100%;
}
.score-ring-fill {
  transition: stroke-dashoffset 1s ease;
}
.score-ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  line-height: 1;
}
.score-ring-num {
  font-size: 32px;
  font-weight: 700;
  font-family: 'SimSun', 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}
.score-ring-unit {
  font-size: 12px;
  color: rgba(87, 64, 36, 0.5);
  margin-top: 2px;
}

/* ================================================================
   NEW: Skill Gap Comparison Bars
   ================================================================ */
.skill-bars {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.skill-bar-row {
  padding: 14px 16px;
  border: 1px solid rgba(87, 64, 36, 0.08);
  background: rgba(87, 64, 36, 0.01);
}
.skill-bar-name {
  font-size: 14px;
  font-weight: 700;
  color: #3b2412;
  margin-bottom: 10px;
}
.skill-bar-chart {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  min-width: 0;
}
.skill-bar-track {
  flex: 1;
  height: 8px;
  background: rgba(87, 64, 36, 0.06);
  border-radius: 1px;
  overflow: hidden;
}
.skill-bar-fill {
  height: 100%;
  border-radius: 1px;
  transition: width 0.6s ease;
}
.skill-bar-fill.required {
  background: rgba(87, 64, 36, 0.45);
}
.skill-bar-fill.current {
  background: rgba(87, 64, 36, 0.18);
}
.skill-bar-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}
.skill-bar-label {
  font-size: 12px;
  color: rgba(87, 64, 36, 0.55);
}
.skill-bar-suggestion {
  font-size: 12px;
  color: rgba(87, 64, 36, 0.65);
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed rgba(87, 64, 36, 0.1);
  line-height: 1.5;
}

/* ================================================================
   NEW: Action Plan Timeline
   ================================================================ */
.action-timeline {
  display: flex;
  flex-direction: column;
}

.timeline-node {
  display: flex;
  gap: 20px;
}

.timeline-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 24px;
}

.timeline-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #1a1a1a;
  flex-shrink: 0;
  margin-top: 4px;
  position: relative;
}
.timeline-dot::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 6px;
  height: 6px;
  background: #fdfcfb;
  border-radius: 50%;
  transform: translate(-50%, -50%);
}
.timeline-dot.mid {
  background: #6e5538;
}
.timeline-dot.mid::after {
  background: #fdfcfb;
}
.timeline-dot.long {
  background: rgba(87, 64, 36, 0.4);
}
.timeline-dot.long::after {
  background: #fdfcfb;
}

.timeline-line {
  width: 1px;
  flex: 1;
  min-height: 30px;
  background: linear-gradient(to bottom, rgba(87, 64, 36, 0.25), rgba(87, 64, 36, 0.08));
}

.timeline-content {
  flex: 1;
  padding-bottom: 28px;
}

.timeline-phase {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 14px;
  padding-left: 10px;
  border-left: 3px solid #3b2412;
}
.timeline-phase .phase-label {
  font-size: 16px;
  font-weight: 700;
  color: #3b2412;
  letter-spacing: 1px;
}
.timeline-phase .phase-duration {
  font-size: 13px;
  color: rgba(87, 64, 36, 0.55);
}

.timeline-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-left: 13px;
}

.timeline-card {
  padding: 16px 18px;
  border: 1px solid rgba(87, 64, 36, 0.08);
  background: rgba(87, 64, 36, 0.015);
  transition: box-shadow 0.2s;
}
.timeline-card:hover {
  box-shadow: 0 2px 8px rgba(87,64,36,0.05);
}
.timeline-card-title {
  font-size: 13px;
  font-weight: 700;
  color: #3b2412;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(87, 64, 36, 0.08);
}
.timeline-card-text {
  font-size: 14px;
  color: rgba(87, 64, 36, 0.65);
  line-height: 1.6;
  margin: 0;
}

.timeline-checklist {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.timeline-check-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
  font-size: 13px;
  color: rgba(87, 64, 36, 0.65);
  line-height: 1.5;
}

.timeline-checkbox {
  position: relative;
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  margin-top: 2px;
}

.timeline-checkbox input[type="checkbox"] {
  position: absolute;
  opacity: 0;
  width: 16px;
  height: 16px;
  cursor: pointer;
  margin: 0;
}

.timeline-checkbox .checkmark {
  position: absolute;
  top: 0;
  left: 0;
  width: 16px;
  height: 16px;
  border: 1.5px solid rgba(87, 64, 36, 0.3);
  background: transparent;
  transition: all 0.2s ease;
}

.timeline-checkbox input[type="checkbox"]:checked ~ .checkmark {
  background: #3b2412;
  border-color: #3b2412;
}

.timeline-checkbox input[type="checkbox"]:checked ~ .checkmark::after {
  content: '';
  position: absolute;
  left: 4.5px;
  top: 1px;
  width: 4px;
  height: 8px;
  border: solid #fdfcfb;
  border-width: 0 1.5px 1.5px 0;
  transform: rotate(45deg);
}

.timeline-check-item:hover .checkmark {
  border-color: rgba(87, 64, 36, 0.5);
}

.check-text {
  transition: opacity 0.2s ease;
}
.timeline-checkbox input[type="checkbox"]:checked ~ .checkmark + .check-text,
.timeline-check-item:has(input:checked) .check-text {
  color: rgba(87, 64, 36, 0.3);
  text-decoration: line-through;
  text-decoration-color: rgba(87, 64, 36, 0.2);
}

/* ================================================================
   NEW: Gradient Divider & Decorative Elements
   ================================================================ */
.gradient-divider {
  height: 1px;
  background: linear-gradient(to right, transparent, rgba(87, 64, 36, 0.2), rgba(87, 64, 36, 0.15), transparent);
  margin: 24px 0;
}

/* Section header accent bars */
.psection-head {
  position: relative;
}
.psection-title {
  position: relative;
}
.psection-title::before {
  content: '';
  position: absolute;
  left: -12px;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(to bottom, #3b2412, rgba(87, 64, 36, 0.3));
}

/* Subtle paper texture overlay — ruled lines */
.report-paper::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    to bottom,
    transparent,
    transparent 31px,
    rgba(26, 26, 26, 0.015) 31px,
    rgba(26, 26, 26, 0.015) 32px
  );
  pointer-events: none;
  z-index: 0;
}
.report-paper > * {
  position: relative;
  z-index: 1;
}
</style>

<!-- Non-scoped: drift keyframes + paper-rise (Vue would hash @keyframes names in scoped) -->
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
@keyframes paper-rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

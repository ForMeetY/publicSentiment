<!--
用户推荐列表
-->

<template>
  <div class="rec-page">
    <!-- 统计卡片 -->
    <el-row :gutter="12" class="stats-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">推荐总条数</div>
          <div class="stat-val">{{ total }}</div>
          <div class="stat-sub">本次推荐批次</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">地域召回</div>
          <div class="stat-val" style="color: #1d9e75">{{ regionCount }}</div>
          <div class="stat-sub">来源：region</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">时段召回</div>
          <div class="stat-val" style="color: #ef9f27">{{ timeCount }}</div>
          <div class="stat-sub">来源：time_period</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">最高热度分</div>
          <div class="stat-val">{{ topScore.toFixed(0) }}</div>
          <div class="stat-sub">score 排名第一</div>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-group">
        <span class="filter-label">话题：</span>
        <el-radio-group v-model="topicFilter" size="small" @change="handleFilterChange">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button v-for="t in topics" :key="t" :label="t">{{ t }}</el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-group">
        <span class="filter-label">召回：</span>
        <el-radio-group v-model="recallFilter" size="small" @change="handleFilterChange">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="region">地域</el-radio-button>
          <el-radio-button label="time_period">时段</el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-group">
        <el-button size="small" class="reset-btn" @click="resetAllFilters">查看全部</el-button>
      </div>
    </div>

    <!-- 列表 -->
    <div v-loading="loading" element-loading-background="rgba(11,15,25,0.8)">
      <template v-if="paged.length">
        <div
          v-for="item in paged"
          :key="item.weiboId"
          class="rec-card"
          :class="{ 'rec-card--top': isTopScore(item.score) }"
        >
          <div class="rec-header">
            <div class="rec-author">
              <div class="avatar">{{ avatarChar(item.screenName) }}</div>
              <div class="author-info">
                <div class="author-name">{{ item.screenName }}</div>
                <div class="author-meta">{{ item.createTime }}</div>
              </div>
            </div>
            <div class="rec-badges">
              <el-tag
                :class="['topic-tag', topicBadgeClass(item.topicLabel)]"
                size="small"
                effect="plain"
              >
                {{ item.topicLabel }}
              </el-tag>
              <el-tag
                :class="['recall-tag', recallClass(item.recallReason)]"
                size="small"
                effect="plain"
              >
                {{ recallLabel(item.recallReason) }}
              </el-tag>
            </div>
          </div>

          <p
            :ref="
              (el) => {
                if (el) textRefs.set(item.weiboId, el);
              }
            "
            class="rec-text"
            :class="{ 'is-expanded': expandedItems.has(item.weiboId) }"
          >
            {{ item.text }}
          </p>

          <div v-if="overflowStatus.has(item.weiboId)" class="toggle-wrap">
            <el-button type="text" class="toggle-btn" @click="toggleExpand(item.weiboId)">
              {{ expandedItems.has(item.weiboId) ? "收起" : "查看全部" }}
            </el-button>
          </div>

          <div class="rec-footer">
            <span class="rec-location"> {{ item.location || "全国" }}</span>
            <div class="score-wrap">
              <span class="score-label">热度分</span>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: scoreBarW(item.score) + '%' }" />
              </div>
              <span class="score-num">{{ item.score.toFixed(1) }}</span>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无推荐内容" />
    </div>

    <div v-if="filtered.length > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filtered.length"
        layout="total, prev, pager, next"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick } from "vue";

const props = defineProps({
  list: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
});

const topicFilter = ref("all");
const recallFilter = ref("all");
const pageSize = ref(10);
const currentPage = ref(1);
const topics = ["政务发布", "民生服务", "公众反馈/举报", "城市问题"];

const expandedItems = reactive(new Set());
const textRefs = reactive(new Map());
const overflowStatus = reactive(new Set());

const toggleExpand = (id) => {
  if (expandedItems.has(id)) expandedItems.delete(id);
  else expandedItems.add(id);
};

const checkOverflow = () => {
  overflowStatus.clear();
  nextTick(() => {
    textRefs.forEach((el, id) => {
      if (el && el.scrollHeight > el.offsetHeight) overflowStatus.add(id);
    });
  });
};

watch([() => props.list, currentPage], checkOverflow, { immediate: true, deep: true });

const filtered = computed(() =>
  props.list.filter(
    (r) =>
      (topicFilter.value === "all" || r.topicLabel === topicFilter.value) &&
      (recallFilter.value === "all" || r.recallReason === recallFilter.value),
  ),
);

const paged = computed(() =>
  filtered.value.slice(
    (currentPage.value - 1) * pageSize.value,
    currentPage.value * pageSize.value,
  ),
);

const total = computed(() => props.list.length);
const maxScore = computed(() =>
  filtered.value.length ? Math.max(...filtered.value.map((r) => r.score)) : 1,
);
const regionCount = computed(() => props.list.filter((r) => r.recallReason === "region").length);
const timeCount = computed(() => props.list.filter((r) => r.recallReason === "time_period").length);
const topScore = computed(() =>
  props.list.length ? Math.max(...props.list.map((r) => r.score)) : 0,
);

const resetAllFilters = () => {
  topicFilter.value = "all";
  recallFilter.value = "all";
  currentPage.value = 1;
};
const handleFilterChange = () => {
  currentPage.value = 1;
};

const topicBadgeClass = (label) =>
  ({
    政务发布: "badge-purple",
    民生服务: "badge-teal",
    "公众反馈/举报": "badge-pink",
    城市问题: "badge-coral",
  })[label] || "badge-gray";
const recallLabel = (reason) => (reason === "region" ? "地域召回" : "时段召回");
const recallClass = (reason) => (reason === "region" ? "badge-cyan" : "badge-amber");
const avatarChar = (name) => (name || "?").charAt(0);
const scoreBarW = (score) => Math.round(Math.min((score / maxScore.value) * 100, 100));
const isTopScore = (score) => score === topScore.value;
</script>

<style scoped>
.rec-page {
  padding: 24px;
  background: #0b0f19;
  min-height: calc(100vh - 40px);
  color: #e2e8f0;
}
.stats-row {
  margin-bottom: 20px;
}
.stat-card {
  background: #131a26;
  border: 1px solid #1e293b;
  border-radius: 10px;
  padding: 14px 16px;
}
.stat-label {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}
.stat-val {
  font-size: 22px;
  font-weight: 700;
  color: #00ffcc;
}
.stat-sub {
  font-size: 11px;
  color: #475569;
  margin-top: 3px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-label {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}

:deep(.el-radio-button__inner) {
  background: #131a26 !important;
  border-color: #1e293b !important;
  color: #94a3b8 !important;
}
:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: rgba(0, 191, 255, 0.12) !important;
  border-color: #00bfff !important;
  color: #00bfff !important;
  box-shadow: none !important;
}

.reset-btn {
  background: #131a26 !important;
  border-color: #1e293b !important;
  color: #94a3b8 !important;
  margin-left: 8px;
}
.reset-btn:hover {
  border-color: #00bfff !important;
  color: #00bfff !important;
}

.rec-card {
  background: #131a26;
  border: 1px solid #1e293b;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 10px;
}
.rec-card--top {
  border-left: 3px solid #00bfff;
  border-radius: 0 10px 10px 0;
}
.rec-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 10px;
  gap: 12px;
}
.rec-author {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #1e3a4f;
  border: 1px solid #00bfff33;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #00bfff;
}
.author-name {
  font-size: 13px;
  font-weight: 600;
  color: #e2e8f0;
}
.author-meta {
  font-size: 11px;
  color: #475569;
  margin-top: 2px;
}
.rec-badges {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

:deep(.topic-tag.badge-purple) {
  background: rgba(83, 74, 183, 0.15) !important;
  border-color: #7f77dd33 !important;
  color: #7f77dd !important;
}
:deep(.topic-tag.badge-teal) {
  background: rgba(0, 158, 117, 0.15) !important;
  border-color: #1d9e7533 !important;
  color: #1d9e75 !important;
}
:deep(.topic-tag.badge-pink) {
  background: rgba(212, 83, 126, 0.15) !important;
  border-color: #d4537e33 !important;
  color: #d4537e !important;
}
:deep(.topic-tag.badge-coral) {
  background: rgba(216, 90, 48, 0.15) !important;
  border-color: #d85a3033 !important;
  color: #d85a30 !important;
}
:deep(.recall-tag.badge-cyan) {
  background: rgba(0, 191, 255, 0.1) !important;
  border-color: #00bfff33 !important;
  color: #00bfff !important;
}
:deep(.recall-tag.badge-amber) {
  background: rgba(186, 117, 23, 0.1) !important;
  border-color: #ef9f2733 !important;
  color: #ef9f27 !important;
}

.rec-text {
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 10px;
  transition: all 0.3s;
}
.rec-text.is-expanded {
  -webkit-line-clamp: unset;
}
.toggle-wrap {
  margin-bottom: 10px;
}
.toggle-btn {
  font-size: 12px;
  padding: 0;
  color: #00bfff;
}
.toggle-btn:hover {
  color: #fff;
}

.rec-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.score-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}
.score-label {
  font-size: 11px;
  color: #475569;
}
.score-bar {
  width: 80px;
  height: 4px;
  background: #1e293b;
  border-radius: 2px;
  overflow: hidden;
}
.score-fill {
  height: 100%;
  background: #00bfff;
  border-radius: 2px;
  transition: width 0.3s;
}
.score-num {
  font-size: 11px;
  color: #00bfff;
  font-weight: 600;
  min-width: 48px;
  text-align: right;
}
.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>

<template>
  <div class="container">
    <div ref="pieAndLine" class="chart-box"></div>
  </div>
</template>

<script setup>
import * as echarts from "echarts";
import { onMounted, onUnmounted, ref, watch, computed, nextTick } from "vue";

defineOptions({
  name: "PieAndLineComponent",
});

// 接收父组件传入的原始行矩阵数据与认证数据
const prop = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
  userAuthData: {
    type: Array,
    default: () => [],
  },
});

const pieAndLine = ref(null);
let myChart = null;
let axisPointerHandler = null;

// 领先型节流函数：确保大屏高频动态联动时不会发生严重的卡顿或堆栈溢出
const throttle = (func, delay) => {
  let lastTime = 0;
  return function (...args) {
    const now = Date.now();
    if (now - lastTime >= delay) {
      lastTime = now;
      func.apply(this, args);
    }
  };
};

// 计算折线图 Y 轴最大边界值
const maxY = computed(() => {
  const authData = prop.data || [];
  const allValues = [];
  for (let i = 1; i < authData.length; i++) {
    const row = authData[i] || [];
    for (let j = 1; j < row.length; j++) {
      const num = Number(row[j]);
      if (!isNaN(num)) allValues.push(num);
    }
  }
  return allValues.length > 0 ? Math.max(...allValues) : 100;
});

// 4. 计算属性：深度清洗合并后端的复合维度 userAuthData
const cleanedUserAuthData = computed(() => {
  const raw = prop.userAuthData || [];
  const map = {};

  raw.forEach((item) => {
    const auth = item.userAuthentication;
    const count = Number(item.count || 0);
    if (auth) {
      map[auth] = (map[auth] || 0) + count;
    }
  });

  return Object.keys(map).map((key) => ({
    "用户认证": key,
    "数量": map[key],
  }));
});

// 格式标准化：兼容处理对象数组或二维数组，转化为 dataset 所需的标准格式
const normalizeDataset = (raw) => {
  if (!raw || raw.length === 0) return [];
  if (Array.isArray(raw[0])) return raw;
  const keys = Object.keys(raw[0]);
  const header = keys;
  const rows = raw.map((item) => keys.map((k) => item[k]));
  return [header, ...rows];
};

// 核心初始化与样式精调函数
const initChart = () => {
  if (!myChart) return;
  if (!prop.data?.length || !prop.userAuthData?.length) return;

  const dataset0 = normalizeDataset(prop.data);
  const dataset1 = normalizeDataset(cleanedUserAuthData.value);

  myChart.setOption({
    backgroundColor: "transparent", // 依托大屏半透明玻璃面板
    animation: true,
    legend: {
      orient: "horizontal",
      top: "2%",
      left: "center",
      textStyle: { color: "#a0aec0" },
      itemGap: 15
    },
    title: [
      {
        text: "24小时时段用户活跃趋势",
        top: "43%",
        left: "5%",
        textStyle: { color: "#00e5ff", fontSize: 14, fontWeight: '600' },
      },
      {
        text: "活跃网民认证类型构成",
        top: "43%",
        left: "52%",
        textStyle: { color: "#00e5ff", fontSize: 14, fontWeight: '600' },
      },
    ],
    tooltip: {
      trigger: "axis",
      backgroundColor: "rgba(11, 19, 36, 0.9)",
      borderColor: "#00bfff",
      borderWidth: 1,
      textStyle: { color: "#fff" }
    },
    xAxis: {
      type: "category",
      axisLabel: { color: "#8a99ad" },
      axisLine: { lineStyle: { color: "#1e293b" } }
    },
    yAxis: {
      gridIndex: 0,
      max: maxY.value,
      axisLabel: { color: "#8a99ad" },
      splitLine: {
        lineStyle: { color: "rgba(255,255,255,0.05)" },
      },
    },
    grid: {
      top: "52%",
      bottom: "6%",
      left: "6%",
      right: "4%",
    },
    color: ['#ff4365', '#ffbc42', '#00ffcc', '#00bfff', '#3f51b5', '#9c27b0'],
    dataset: [
      { source: dataset0 },
      { source: dataset1 },
    ],
    series: [
      // 6路折线监控线
      { name: "个人红V",      datasetIndex: 0, type: "line", smooth: true, seriesLayoutBy: "row", symbol: 'none' },
      { name: "个人金V",      datasetIndex: 0, type: "line", smooth: true, seriesLayoutBy: "row", symbol: 'none' },
      { name: "个人黄V",      datasetIndex: 0, type: "line", smooth: true, seriesLayoutBy: "row", symbol: 'none' },
      { name: "官方媒体(蓝V)", datasetIndex: 0, type: "line", smooth: true, seriesLayoutBy: "row", symbol: 'none' },
      { name: "普通用户",      datasetIndex: 0, type: "line", smooth: true, seriesLayoutBy: "row", symbol: 'none' },
      { name: "未认证用户",    datasetIndex: 0, type: "line", smooth: true, seriesLayoutBy: "row", symbol: 'none' },

      // 左上玫瑰图：时间联动
      {
        type: "pie",
        id: "pie",
        datasetIndex: 0,
        radius: ["0%", "30%"],
        center: ["26%", "24%"],
        roseType: "radius",
        emphasis: { focus: "self" },
        label: {
          formatter: "{b}\n{d}%",
          color: "#cbd5e1",
          fontSize: 10,
        },
      },

      // 右上环形图：用户类型总占比
      {
        type: "pie",
        id: "pie2",
        datasetIndex: 1,
        radius: ["18%", "30%"],
        center: ["74%", "24%"],
        emphasis: { focus: "self" },
        label: {
          formatter: "{b}\n{d}%",
          color: "#cbd5e1",
          fontSize: 10,
        },
        encode: { itemName: 0, value: 1, tooltip: 1 },
      },
    ],
  });

  // 重点联动逻辑修复：安全清理并重新注册 updateAxisPointer 轴联动监听
  if (axisPointerHandler) {
    myChart.off("updateAxisPointer", axisPointerHandler);
  }

  axisPointerHandler = throttle((event) => {
    const xAxisInfo = event.axesInfo?.[0];
    if (!xAxisInfo) return;
    // 基于时段动态抓取当前对应的列索引
    const dim = xAxisInfo.value + 1;

    myChart.setOption({
      series: [
        {
          id: "pie",
          label: { formatter: `{b}\n占比：{d}%` },
          encode: { value: dim, tooltip: dim },
        },
      ],
    });
  }, 60);

  myChart.on("updateAxisPointer", axisPointerHandler);
};

// 8. 挂载与动态监测销毁
onMounted(() => {
  if (pieAndLine.value) {
    myChart = echarts.init(pieAndLine.value, "dark");
    if (prop.data?.length && prop.userAuthData?.length) {
      initChart();
    }
  }

  window.addEventListener('resize', () => myChart?.resize());
});

onUnmounted(() => {
  if (myChart) {
    if (axisPointerHandler) myChart.off("updateAxisPointer", axisPointerHandler);
    myChart.dispose();
  }
  myChart = null;
  axisPointerHandler = null;
});

watch(
  () => [prop.data, prop.userAuthData],
  ([newData, newUserAuthData]) => {
    if (!newData?.length || !newUserAuthData?.length) return;
    nextTick(() => {
      initChart();
    });
  },
  { deep: true }
);
</script>

<style scoped>
.container {
  width: 100%;
  background: rgba(16, 24, 48, 0.6);
  border: 1px solid #1e293b;
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.1);
  border-radius: 12px;
  padding: 15px;
  box-sizing: border-box;
}
.chart-box {
  width: 100%;
  height: 670px;
}
</style>

<!--
情感分析各个时段折线图

-->

<template>
  <div
    ref="sentimentLinear"
    class="sentimentLinear"
  ></div>
</template>

<script setup>
import * as echarts from "echarts";
import { onMounted, ref, onUnmounted, watch, nextTick } from "vue";

defineOptions({
  name: "SentimentLinearComponent",
});

const props = defineProps({
  sentimentLinearData: {
    type: Object,
    default: () => ({}),
  },
});

const sentimentLinear = ref(null);
let myChart = null;

const renderChart = () => {
  if (!myChart || !props.sentimentLinearData) return;

  const { xAxis = [], data = [[], [], []] } = props.sentimentLinearData;

  const option = {
    backgroundColor: "transparent",
    color: ["#8a99ad", "#00ffcc", "#ff4365"],

    title: {
      text: "不同时段情感倾向分布趋势",
      top: "4%",
      left: "3%",
      textStyle: {
        color: "#00e5ff",
        fontSize: 14,
        fontWeight: "600"
      },
    },

    tooltip: {
      trigger: "axis",
      backgroundColor: "rgba(11, 19, 36, 0.9)",
      borderColor: "#00bfff",
      borderWidth: 1,
      textStyle: { color: "#fff" },
      /* ====== 核心修改：配置随鼠标移动的纵向指示线 ====== */
      axisPointer: {
        type: "line", // 直线指示器
        lineStyle: {
          color: "rgba(0, 191, 255, 0.5)", // 青色半透明线，对齐大屏风格
          width: 1.5,
          type: "dashed", // 虚线更具科技感，如果想要实线可以改为 'solid'
        },
      },
      /* ============================================== */
    },

    legend: {
      data: ["中性", "正面", "负面"],
      top: "4%",
      right: "5%",
      itemWidth: 12,
      itemHeight: 12,
      textStyle: { color: "#a0aec0" },
    },

    grid: {
      top: "22%",
      left: "4%",
      right: "4%",
      bottom: "6%",
      containLabel: true,
    },

    xAxis: {
      type: "category",
      boundaryGap: false,
      data: xAxis,
      axisLine: { lineStyle: { color: "#1e293b" } },
      axisLabel: { color: "#8a99ad" },
    },

    yAxis: {
      type: "value",
      name: "数量",
      nameTextStyle: {
        color: "#8a99ad",
        padding: [0, 20, 0, 0]
      },
      axisLine: {
        show: true,
        lineStyle: { color: "#1e293b" }
      },
      axisLabel: { color: "#8a99ad" },
      splitLine: {
        lineStyle: { color: "rgba(255,255,255,0.05)" },
      },
    },

    series: [
      {
        name: "中性",
        type: "line",
        smooth: true,
        symbol: "none",
        data: data[0],
        lineStyle: { width: 3 },
      },
      {
        name: "正面",
        type: "line",
        smooth: true,
        symbol: "none",
        data: data[1],
        lineStyle: { width: 3 },
      },
      {
        name: "负面",
        type: "line",
        smooth: true,
        symbol: "none",
        data: data[2],
        lineStyle: { width: 3 },
      },
    ],
  };

  myChart.setOption(option, true);
};

const handleResize = () => {
  myChart?.resize();
};

onMounted(() => {
  if (sentimentLinear.value) {
    myChart = echarts.init(sentimentLinear.value, "dark");
    renderChart();
  }
  window.addEventListener("resize", handleResize);
});

onUnmounted(() => {
  window.removeEventListener("resize", handleResize);
  if (myChart) {
    myChart.dispose();
  }
  myChart = null;
});

watch(
  () => props.sentimentLinearData,
  () => {
    nextTick(() => {
      renderChart();
    });
  },
  { deep: true }
);
</script>

<style scoped>
.sentimentLinear {
  width: 75%;
  height: 400px;
  margin-right: 5px;
  background: rgba(16, 24, 48, 0.4);
  border: 1px solid #1e293b;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.08);
  border-radius: 8px;
  padding: 15px;
  box-sizing: border-box;
  display: inline-block;
  vertical-align: top;
}
</style>

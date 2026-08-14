<template>
  <div
    ref="chartDom"
    class="cyber-chart-box"
  ></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from "vue";
import * as echarts from "echarts";

defineOptions({
  name: "SentimentPieComponent",
});

const props = defineProps({
  labelCount: {
    type: Array,
    default: () => [],
  },
});

const chartDom = ref(null);
let myChart = null;

const initChart = () => {
  if (!myChart || !props.labelCount?.length) return;

  const option = {
    // 依然保持背景透明，完美融入你的大屏底色
    backgroundColor: "transparent",

    // 完全对齐第二张图的科技感冷暖色配比
    color: ["#8a99ad", "#00ffcc", "#ff4365"], // 中性 / 正面 / 负面

    title: {
      text: "舆情内容情感分布统计",
      top: "4%",
      left: "4%",
      textStyle: {
        color: "#00e5ff",
        fontSize: 14, // 配合 25% 的小体量图表，字号微调至 14px 更精致
        fontWeight: "600",
      },
    },

    tooltip: {
      trigger: "item",
      backgroundColor: "rgba(11, 19, 36, 0.9)",
      borderColor: "#00bfff",
      borderWidth: 1,
      textStyle: {
        color: "#fff",
      },
      formatter: "{b}：{c}条 <br/>占比：{d}%",
    },

    legend: {
      orient: "horizontal",
      bottom: "4%",
      left: "center",
      itemWidth: 10,
      itemHeight: 10,
      textStyle: {
        color: "#a0aec0",
        fontSize: 12,
      },
    },

    series: [
      {
        name: "情感统计",
        type: "pie",
        // 保持 40%-70% 的传统环形比例，或者跟随第二张图改成更细的 45%-65%
        radius: ["40%", "65%"],
        center: ["50%", "48%"],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 4, // 略微圆角
          borderColor: "#111726", // 与深色背景呼应的掐丝边缘
          borderWidth: 2,
        },
        label: {
          show: true,
          position: "outside",
          formatter: "{b}\n{d}%",
          color: "#cbd5e1",
          fontSize: 11, // 小图里标签字号稍作缩小，防止 25% 宽度下文字溢出
        },
        labelLine: {
          show: true,
          length: 10,
          length2: 8,
          lineStyle: {
            color: "#475569",
          },
        },
        emphasis: {
          scale: true,
          scaleSize: 6,
          itemStyle: {
            shadowBlur: 10,
            shadowColor: "rgba(0, 229, 255, 0.5)",
          },
        },
        data: props.labelCount.map((item) => ({
          name: item.label,
          value: item.value,
        })),
      },
    ],
  };

  myChart.setOption(option, true);
};

// 尺寸自适应及生命周期
onMounted(() => {
  if (chartDom.value) {
    myChart = echarts.init(chartDom.value, "dark");
    initChart();
  }
  window.addEventListener("resize", handleResize);
});

const handleResize = () => {
  myChart?.resize();
};

onUnmounted(() => {
  window.removeEventListener("resize", handleResize);
  if (myChart) {
    myChart.dispose();
  }
  myChart = null;
});

watch(
  () => props.labelCount,
  (newVal) => {
    if (newVal?.length) {
      nextTick(() => {
        initChart();
      });
    }
  },
  { deep: true }
);
</script>

<style scoped>
.cyber-chart-box {
  /* 严格遵循第一张图的原始尺寸要求 */
  width: 25%;
  height: 400px;

  /* 融合第二张图的科幻质感：深色半透明微发光边框 */
  background: rgba(16, 24, 48, 0.4);
  border: 1px solid #1e293b;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.08);
  border-radius: 8px;
  box-sizing: border-box;

  /* 确保小组件在行内块或弹性盒中正常排列 */
  display: inline-block;
  vertical-align: top;
}
</style>

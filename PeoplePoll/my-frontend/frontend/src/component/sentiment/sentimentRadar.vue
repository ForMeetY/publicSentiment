<template>
  <div class="radar-wrapper">
    <div ref="r1" class="radar-box"></div>
    <div ref="r2" class="radar-box"></div>
    <div ref="r3" class="radar-box"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from "vue";
import * as echarts from "echarts";

defineOptions({ name: "SentimentRadarComponent" });

const r1 = ref(null);
const r2 = ref(null);
const r3 = ref(null);

let chart1, chart2, chart3;

const props = defineProps({
  data: { type: Array, default: () => [] },
});

const render = () => {
  const d = props.data || [];
  if (!d.length) return;

  const positive = d.find((i) => i.label === "正面");
  const neutral = d.find((i) => i.label === "中性");
  const negative = d.find((i) => i.label === "负面");

  const indicator = [
    { name: "转发", max: 400 },
    { name: "评论", max: 400 },
    { name: "点赞", max: 400 },
  ];

  // 公共基础配置
  const opt = {
    backgroundColor: "transparent",
    tooltip: {
      trigger: "item",
      backgroundColor: "rgba(11, 19, 36, 0.95)",
      borderColor: "#00bfff",
      borderWidth: 1,
      textStyle: { color: "#fff" }
    },
    radar: {
      indicator,
      // 【关键优化】缩小半径并上移中心，避开底部截断
      radius: "50%",
      center: ["50%", "58%"],
      startAngle: 90, // 指标轴指向正上方，底部对称分布
      axisName: {
        color: "#fff",
        fontSize: 12,
        fontWeight: "bold",
        padding: [0, 0]
      },
      axisNameGap: 10, // 标签与雷达外圈的间距
      splitLine: { lineStyle: { color: "rgba(0, 229, 255, 0.25)", width: 1 } },
      splitArea: { areaStyle: { color: ["rgba(255, 255, 255, 0.02)", "rgba(255, 255, 255, 0.06)"] } },
      axisLine: { lineStyle: { color: "rgba(0, 229, 255, 0.3)", width: 1.5 } }
    },
  };

  const createSeries = (dataItem, color) => ({
    type: "radar",
    data: [{
      value: [dataItem?.avgRepost || 0, dataItem?.avgComment || 0, dataItem?.avgAttitude || 0],
      areaStyle: { color: color.replace("1)", "0.35)") }
    }],
    itemStyle: { color: color },
    lineStyle: { width: 3, color: color, opacity: 1 },
    symbol: "circle",
    symbolSize: 6
  });

  chart1.setOption({ ...opt, title: { text: "正面情感互动", top: "5%", left: "center", textStyle: { color: "#52c41a", fontSize: 13 } }, series: [createSeries(positive, "#52c41a")] }, true);
  chart2.setOption({ ...opt, title: { text: "中性情感互动", top: "5%", left: "center", textStyle: { color: "#cbd5e1", fontSize: 13 } }, series: [createSeries(neutral, "#cbd5e1")] }, true);
  chart3.setOption({ ...opt, title: { text: "负面情感互动", top: "5%", left: "center", textStyle: { color: "#ff4d4f", fontSize: 13 } }, series: [createSeries(negative, "#ff4d4f")] }, true);
};

const resizeHandler = () => {
  chart1?.resize();
  chart2?.resize();
  chart3?.resize();
};

onMounted(() => {
  chart1 = echarts.init(r1.value, "dark");
  chart2 = echarts.init(r2.value, "dark");
  chart3 = echarts.init(r3.value, "dark");
  render();
  window.addEventListener("resize", resizeHandler);
});

watch(() => props.data, render, { deep: true });

onUnmounted(() => {
  window.removeEventListener("resize", resizeHandler);
  chart1?.dispose();
  chart2?.dispose();
  chart3?.dispose();
});
</script>

<style scoped>
.radar-wrapper {
  display: flex;
  gap: 10px;
  width: 100%;
  height: 340px;
  background: rgba(11, 19, 36, 0.9);
  border: 1px solid #1e293b;
  border-radius: 12px;
  padding: 10px 5px; /* 减小左右padding，增加内部雷达图可用空间 */
  box-sizing: border-box;
}
.radar-box { flex: 1; height: 100%; }
</style>

<script setup>
import {
  getLabelCount,
  getSentimentLinear,
  getSentimentMap,
  getSentimentInteraction,
  getSentimentUserAuth,
  getSentimentStack,
} from "@/api/SentimentStatistic";
import { ref, onMounted, watch } from 'vue';
import SentimentPie from "@/component/sentiment/sentimentPie.vue";
import SentimentLinear from "@/component/sentiment/sentimentLinear.vue";
import SentimentMap from "@/component/sentiment/sentimentMap.vue";
import SentimentRadar from "@/component/sentiment/sentimentRadar.vue";
import SentimentSankey from "@/component/sentiment/sentimentSankey.vue";
import SentimentStack from "@/component/sentiment/sentimentStack.vue";
import Footer from "@/component/footer.vue";

const labelCount = ref(null);
const labelCountByTime = ref(null);
// 最终传给子组件的格式化+排序后的数据
const sentimentLinearData = ref({
  xAxis: [], // 排序后的时段轴：["凌晨", "早晨", "上午", ...]
  data: [[], [], []] // 对应：[中性数据, 正面数据, 负面数据]
});

// 定义时段的标准时间顺序（按实际时间从早到晚）
const timePeriodOrder = ["凌晨", "早晨", "上午", "下午", "晚间", "午夜"];

// 数据处理核心函数：格式化+排序
const formatSentimentData = (rawData) => {
  // 容错：原始数据为空时返回空结构
  if (!rawData || rawData.length === 0) {
    return { xAxis: [], data: [[], [], []] };
  }

  // 提取所有不重复的时段并按标准顺序排序
  const rawTimePeriods = [...new Set(rawData.map(item => item.timePeriod))];
  const sortedTimePeriods = rawTimePeriods.sort((a, b) => {
    // 按自定义时间顺序排序，未知时段排最后
    const indexA = timePeriodOrder.indexOf(a) !== -1 ? timePeriodOrder.indexOf(a) : 99;
    const indexB = timePeriodOrder.indexOf(b) !== -1 ? timePeriodOrder.indexOf(b) : 99;
    return indexA - indexB;
  });

  // 按排序后的时段，提取对应情感数据（中性、正面、负面）
  const neutralData = sortedTimePeriods.map(period => {
    const item = rawData.find(d => d.timePeriod === period && d.label === "中性");
    return item ? item.value : 0; // 无数据时填0，避免图表断层
  });
  const positiveData = sortedTimePeriods.map(period => {
    const item = rawData.find(d => d.timePeriod === period && d.label === "正面");
    return item ? item.value : 0;
  });
  const negativeData = sortedTimePeriods.map(period => {
    const item = rawData.find(d => d.timePeriod === period && d.label === "负面");
    return item ? item.value : 0;
  });

  // 组装成目标格式
  return {
    xAxis: sortedTimePeriods,
    data: [neutralData, positiveData, negativeData]
  };
};

const mapHotData = ref(null);
// 交互度情感分析
const interactionEmotion = ref(null);

// 用户认证情感统计
const userAuthEmotion = ref(null);

// 情感堆叠图数据
const stackData = ref(null);



const loader = async () => {
  try {
    const res = await getLabelCount();
    const res1 = await getSentimentLinear();
    const res2 = await getSentimentMap();
    const res3 = await getSentimentInteraction();
    const res4 = await getSentimentUserAuth();
    const res5 = await getSentimentStack();


    console.log("sentimentLinear原始数据:", res1);
    console.log("labelCount:", res);
    console.log("mapHotData原始数据:", res2);
    console.log("interactionEmotion原始数据:", res3);
    console.log("userAuthEmotion原始数据:", res4);
    console.log("stackData原始数据:", res5);
    labelCount.value = res;
    labelCountByTime.value = res1;
    mapHotData.value = res2;
    interactionEmotion.value = res3;
    userAuthEmotion.value = res4;
    stackData.value = res5;

    // 数据请求完成后，立即格式化+排序
    sentimentLinearData.value = formatSentimentData(res1);
  } catch (error) {
    console.error("数据请求失败：", error);
  }
};

// 监听原始数据变化
watch(
  () => labelCountByTime.value,
  (newVal) => {
    if (newVal) {
      sentimentLinearData.value = formatSentimentData(newVal);
    }
  },
  { deep: true }
);

onMounted(() => {
  loader();
});
</script>

<template>
  <div class = "box">
    <SentimentPie :labelCount="labelCount" />
    <!-- 传给子组件的是处理好的sentimentLinearData，而非原始数据 -->
    <SentimentLinear :sentimentLinearData="sentimentLinearData" />
  </div>
  <div class="box1">
    <SentimentMap :mapHotData="mapHotData" />
  </div>
  <div class="box2">
    <SentimentRadar :data="interactionEmotion|| []" />
  </div>
  <SentimentSankey :data="userAuthEmotion || []" />
  <SentimentStack :data="stackData || []" />
  <Footer/>
</template>

<style scoped>
.box {
  display: flex;
  justify-content: space-between;
}
</style>

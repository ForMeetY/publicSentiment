<script setup>
import PieComponent from "@/component/pieAndLine.vue";
import UpTotal from "@/component/upTotal.vue";
import MapHotComponent from "@/component/mapHot.vue";
import {
  getWeiboTotalStatistic,
  getWeiboTotalStatisticByAuth,
  getMapHotData,
} from "@/api/totalStatistic";
import { onMounted, ref, onUnmounted } from "vue";
import FooterComponent from "@/component/footer.vue";

defineOptions({
  name: "TotalWeiboStatistic",
});

const data = ref(null);
const userAuthData = ref(null);
const mapHotData = ref(null);
const totalData = ref(null);
let timer = null;

const load = async () => {
  const res = await getWeiboTotalStatistic();
  const res2 = await getWeiboTotalStatisticByAuth();
  const res3 = await getMapHotData();
  data.value = res.data;
  userAuthData.value = res2.data;
  mapHotData.value = res3.data;
  totalData.value = res2.data.map((item) => item.count).reduce((a, b) => a + b, 0);
};

onMounted(() => {
  if (timer) clearInterval(timer);
  load();

  timer = setInterval(
    () => {
      load();
    },
    60000 * 5 - 100,
  );
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<template>
  <div class="screen-bg">
    <header class="screen-header">
      <h1 class="screen-title">微博网络舆情大数据整体统计中心</h1>
      <div class="header-line"></div>
    </header>

    <div class="screen-container">
      <div class="row-total">
        <UpTotal :totalData="totalData"></UpTotal>
      </div>

      <div class="row-content">
        <div class="grid-left">
          <MapHotComponent :mapHotData="mapHotData"></MapHotComponent>
        </div>
        <div class="grid-right">
          <PieComponent :data="data" :userAuthData="userAuthData"></PieComponent>
        </div>
      </div>
    </div>

    <FooterComponent></FooterComponent>
  </div>
</template>

<style scoped>
.screen-bg {
  background-color: #040810;
  min-height: 100vh;
  padding: 16px;
  color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  box-sizing: border-box;
}

.screen-header {
  text-align: center;
  position: relative;
  margin-bottom: 24px;
}

.screen-title {
  margin: 0;
  padding: 10px 0;
  font-size: 26px;
  font-weight: bold;
  letter-spacing: 2px;
  background: linear-gradient(180deg, #ffffff 30%, #00bfff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 2px 10px rgba(0, 191, 255, 0.3);
}

.header-line {
  height: 2px;
  background: linear-gradient(90deg, transparent 10%, #00bfff 50%, transparent 90%);
  width: 100%;
  opacity: 0.6;
  margin-top: 5px;
}

.screen-container {
  max-width: 1920px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.row-total {
  width: 100%;
}

.row-content {
  display: flex;
  gap: 20px;
  width: 100%;
}

.grid-left {
  flex: 4.5; /* 地图占 45% 宽度 */
  min-width: 0;
}

.grid-right {
  flex: 5.5; /* 图表占 55% 宽度 */
  min-width: 0;
}

/* 适配中等屏幕，防止挤压 */
@media (max-width: 1200px) {
  .row-content {
    flex-direction: column;
  }
}
</style>

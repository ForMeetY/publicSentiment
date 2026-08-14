<template>
  <div class="map-card-wrapper">
    <div ref="chartDomRef" class="china-map-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'


import geoJson from '@/assets/map/100000_full.json'

// 1. 声明 Ref 容器和 ECharts 实例变量
const chartDomRef = ref(null)
let myChart = null
let resizeHandler = null

// 2. 定义从父组件接收的数据
const props = defineProps({
  mapHotData: {
    type: Array,
    default: () => [] // 默认为空数组防止渲染报错
  }
})

// 将后端接口数据转化为 ECharts 所需的标准格式 {name: 省份, value: 热度}
const processData = (data) => {
  if (!data || data.length === 0) return []
  return data.map(item => ({
    name: item.region, // 如果接口字段不是 region，在这里对齐
    value: item.hot  || 0 // 兼容 hot 或 count 字段
  }))
}

// 定义防抖节流函数：防止父组件异步加载或定时器导致频繁刷新
const throttle = (func, delay) => {
  let lastTime = 0
  return function (...args) {
    const now = Date.now()
    if (now - lastTime >= delay) {
      lastTime = now
      func.apply(this, args)
    }
  }
}

// 核心渲染函数：这里放置提供的暗黑风格配置
const renderChart = () => {
  if (!myChart) return
  const washedData = processData(props.mapHotData) || []

  // 检查数据，如果为空，可以打印日志排查
  if(washedData.length === 0) {
    console.warn("[MapHot] 接收到的地图热度数据为空，将渲染基础底图。");
  } else {
    console.log("[MapHot] 渲染数据:", washedData);
  }

  const option = {
    animation: true,
    title: {
      text: '全国舆情活跃热度映射大屏',
      left: '20px',
      top: '15px',
      textStyle: {
        fontSize: 16,
        fontWeight: '600',
        color: '#00e5ff' // 亮色
      }
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(11, 19, 36, 0.9)', // 深色背景
      borderColor: '#00bfff', // 蓝
      borderWidth: 1,
      textStyle: { color: '#fff' },
      formatter: function (params) {
        if (!params.data) return `<span style="color:#999">${params.name}：暂无热度数据</span>`
        return `<b style="color:#00ffcc">${params.name}</b><br/>舆情热度：<span style="color:#ffb74d">${params.value}</span>`
      }
    },
    visualMap: {
      min: 0,
      max: 100, // 热度上限动态调整
      left: '25px',
      bottom: '25px',
      calculable: true,
      orient: 'horizontal', // 横向排列，适配大屏底部空间
      inRange: {
        // 赛博暗黑炫彩渐变色卡：幽静蓝 -> 工业绿 -> 警示橙 -> 霓虹红
        color: ['rgba(30,50,90,0.4)', '#00bfff', '#ffb74d', '#ff3d00']
      },
      textStyle: { color: '#a0aec0' },
    },
    geo: {
      map: 'china',
      roam: false, // 禁用鼠标缩放，保持大屏稳定
      zoom: 1.15,
      center: [104.300, 36.000], // 微微往上对齐中心，防止底部文字盖住南海诸岛
      label: {
        show: true,
        fontSize: 10,
        color: '#abc7f5', // 柔和的亮蓝色标签
      },
      itemStyle: {
        areaColor: '#121b2d', // 深邃暗黑色底图
        borderColor: '#26497d', // 亮蓝色省界线
        borderWidth: 1.1,
      },
      emphasis: {
        itemStyle: {
          areaColor: '#1e3d6b', // 悬浮高亮颜色
          shadowBlur: 10,
          shadowColor: '#00bfff'
        },
        label: { color: '#00ffcc' },
      }
    },
    series: [{
      type: 'map',
      map: 'china',
      geoIndex: 0,
      data: washedData,
    }]
  }
  myChart.setOption(option, false)
}

// 定义节流版的渲染
const renderChartThrottle = throttle(renderChart, 500)

// 生命周期钩子：初始化地图
onMounted(() => {
  if (!chartDomRef.value) return

  myChart = echarts.init(chartDomRef.value, 'dark')

  //注册中国地图 GeoJSON
  echarts.registerMap('china', geoJson)

  renderChart()

  // 适配屏幕拉伸
  resizeHandler = () => {
    if(myChart) {
      myChart.resize()
    }
  }
  window.addEventListener('resize', resizeHandler)
})

// 7. 监听数据变化：数据更新时自动刷新图表
watch(() => props.mapHotData, () => {
  nextTick(() => {
    if (myChart) {
      renderChartThrottle()
    }
  })
}, { deep: true })

// 8. 生命周期钩子：销毁组件时释放内存
onUnmounted(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
  if (myChart) {
    myChart.dispose()
    myChart = null
  }
})
</script>

<style scoped>
.map-card-wrapper {
  background: rgba(16, 24, 48, 0.6); /* 玻璃拟态半透明 */
  border: 1px solid #1e293b;
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.1);
  border-radius: 12px;
  padding: 10px;
  box-sizing: border-box;
}
.china-map-container {
  width: 100%;
  height: 680px; /* 对齐父组件 Bento Box 网格高度 */
  background-color: transparent;
}
</style>

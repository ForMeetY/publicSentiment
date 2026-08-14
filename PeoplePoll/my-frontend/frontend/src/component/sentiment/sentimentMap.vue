<template>
  <div class="chart-wrapper">
    <div ref="chartMap" class="map-chart"></div>
    <div ref="chartBar" class="bar-chart"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import geoJson from '@/assets/map/100000_full.json'

const chartMap = ref(null)
const chartBar = ref(null)
let myMap = null
let myBar = null
let resizeHandler = null

const props = defineProps({
  mapHotData: {
    type: Array,
    default: () => []
  }
})

// 数据处理
const processMapData = (data) => {
  if (!data) return []
  return data.map(item => ({
    name: item.region,
    value: item.avgScore,
    count: item.count
  }))
}

// 条形图数据（按得分排序）
const processBarData = (data) => {
  if (!data) return []
  return [...data]
    .sort((a, b) => a.avgScore - b.avgScore)
    .map(item => ({
      name: item.region,
      value: item.avgScore
    }))
}

// 节流
const throttle = (func, delay) => {
  let timer = null
  return (...args) => {
    if (timer) return
    func(...args)
    timer = setTimeout(() => timer = null, delay)
  }
}

// 渲染地图
const renderMap = () => {
  if (!myMap) return
  const data = processMapData(props.mapHotData)

  const option = {
    // 保持透明，完美继承外层 wrapper 的科技蓝背景
    backgroundColor: "transparent",
    title: {
      text: '地区情感倾向分布',
      left: 'center',
      top: 20,
      textStyle: {
        fontSize: 18,
        color: '#00e5ff', // 标题使用科技青，更具极客感
        fontWeight: '600'
      }
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: "rgba(11, 19, 36, 0.9)", // 提示框同步科技蓝
      borderColor: "#00bfff",
      borderWidth: 1,
      textStyle: { color: "#fff" },
      formatter: params => {
        if (!params.data) return `${params.name}：暂无数据`
        return `省份：${params.name}<br/>情感分：${params.value.toFixed(2)}`
      }
    },
    visualMap: {
      min: -1,
      max: 1,
      left: 30,
      bottom: '15%',
      calculable: true,
      // 保持你原本的红、灰、绿经典原生色配比
      inRange: { color: ['#ff4d4f', '#8a99ad', '#52c41a'] },
      textStyle: { color: '#cbd5e1' }
    },
    geo: {
      map: 'china',
      roam: false,
      zoom: 1.1,
      center: [105, 36],
      label: {
        show: true,
        color: '#cbd5e1', // 柔和字色，在科技蓝底色上非常清晰且不刺眼
        fontSize: 10
      },
      itemStyle: {
        // 无数据省份填充深海科技蓝，边框采用隐蔽的暗蓝，整体感极强
        areaColor: '#161e35',
        borderColor: '#242f42',
        borderWidth: 1
      },
      emphasis: {
        label: { color: '#fff' },
        itemStyle: {
          areaColor: '#1e293b' // 悬浮高亮时稍微调亮
        }
      }
    },
    series: [{
      type: 'map',
      map: 'china',
      geoIndex: 0,
      data
    }]
  }

  myMap.setOption(option)
}

// 渲染条形图
const renderBar = () => {
  if (!myBar) return
  const list = processBarData(props.mapHotData)

  const option = {
    backgroundColor: "transparent",
    tooltip: {
      trigger: 'axis',
      backgroundColor: "rgba(11, 19, 36, 0.9)",
      borderColor: "#00bfff",
      borderWidth: 1,
      textStyle: { color: "#fff" },
      axisPointer: { type: 'shadow' }
    },
    grid: { left: '10%', right: '12%', top: '15%', bottom: '8%', containLabel: true },
    xAxis: {
      type: 'value',
      min: -1,
      max: 1,
      axisLine: { lineStyle: { color: '#1e293b' } }, // 坐标轴精细化，换成深色线
      axisLabel: { color: '#8a99ad' },
      splitLine: { lineStyle: { color: "rgba(255,255,255,0.05)" } } // 极淡的分格网格线
    },
    yAxis: {
      type: 'category',
      data: list.map(i => i.name),
      axisLine: { lineStyle: { color: '#1e293b' } },
      axisLabel: { color: '#8a99ad', fontSize: 11 }
    },
    series: [{
      type: 'bar',
      barWidth: '55%',
      data: list.map(i => i.value),
      itemStyle: {
        borderRadius: [0, 4, 4, 0], // 给柱状图加点微圆角显精致
        borderColor: '#111726',
        borderWidth: 1,
        // 保持你原本的条形图红绿灰色块逻辑
        color: params => {
          const val = params.data
          if (val < -0.05) return '#ff4d4f'
          if (val > 0.05) return '#52c41a'
          return '#8a99ad'
        }
      }
    }]
  }

  myBar.setOption(option)
}

const connectHover = () => {
  myMap.on('mouseover', (params) => {
    const idx = processBarData(props.mapHotData).findIndex(d => d.name === params.name)
    if (idx !== -1) {
      myBar.dispatchAction({ type: 'downplay' })
      myBar.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: idx })
    }
  })

  myMap.on('mouseout', () => {
    myBar.dispatchAction({ type: 'downplay' })
  })

  myBar.on('mouseover', (params) => {
    const idx = processMapData(props.mapHotData).findIndex(d => d.name === params.name)
    if (idx !== -1) {
      myMap.dispatchAction({ type: 'downplay' })
      myMap.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: idx })
    }
  })

  myBar.on('mouseout', () => {
    myMap.dispatchAction({ type: 'downplay' })
  })
}

// 初始化
onMounted(() => {
  // 显式指定使用内置的 "dark" 主题基底，更贴合科技蓝大屏
  myMap = echarts.init(chartMap.value, "dark")
  myBar = echarts.init(chartBar.value, "dark")
  echarts.registerMap('china', geoJson)

  renderMap()
  renderBar()
  connectHover()

  resizeHandler = throttle(() => {
    myMap.resize()
    myBar.resize()
  }, 100)
  window.addEventListener('resize', resizeHandler)
})

watch(() => props.mapHotData, () => {
  renderMap()
  renderBar()
}, { deep: true })

onUnmounted(() => {
  window.removeEventListener('resize', resizeHandler)
  myMap.dispose()
  myBar.dispose()
})
</script>

<style scoped>
.chart-wrapper {
  display: flex;
  width: 100%;
  height: 800px;
  /* 核心修改：整体底色换成经典的深邃大屏科技蓝 */
  background: rgba(11, 19, 36, 0.9);
  /* 搭配一条精致的深科技感暗框线 */
  border: 1px solid #1e293b;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.05);
  border-radius: 12px;
  overflow: hidden;
  padding: 20px;
  gap: 15px;
  box-sizing: border-box;
}

.map-chart {
  flex: 7;
  height: 100%;
}

.bar-chart {
  flex: 3;
  height: 100%;
}
</style>

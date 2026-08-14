<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let myChart = null

const renderChart = () => {
  if (!myChart || !props.data.length) return

  const dateList = [...new Set(props.data.map(item => item.dt))].sort()

  const positiveRatio = []
  const neutralRatio = []
  const negativeRatio = []

  dateList.forEach(date => {
    const dayData = props.data.filter(d => d.dt === date)
    positiveRatio.push(dayData.find(i => i.label === '正面')?.ratio || 0)
    neutralRatio.push(dayData.find(i => i.label === '中性')?.ratio || 0)
    negativeRatio.push(dayData.find(i => i.label === '负面')?.ratio || 0)
  })

  const option = {
    backgroundColor: 'transparent',
    color: ['#8a99ad', '#00ffcc', '#ff4365'],

    title: {
      text: '情感倾向占比趋势',
      top: '4%',
      left: '3%',
      textStyle: {
        color: '#00e5ff',
        fontSize: 14,
        fontWeight: '600'
      }
    },

    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(11,19,36,0.9)',
      borderColor: '#00bfff',
      borderWidth: 1,
      textStyle: { color: '#fff' },
      formatter: function (params) {
        let tip = params[0].axisValue + '<br/>'
        params.forEach(p => {
          tip += `${p.marker}${p.seriesName}：${p.data}%<br/>`
        })
        return tip
      },
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: 'rgba(0,191,255,0.5)',
          width: 1.5,
          type: 'dashed'
        }
      }
    },

    legend: {
      data: ['中性', '正面', '负面'],
      top: '4%',
      right: '5%',
      itemWidth: 12,
      itemHeight: 12,
      textStyle: { color: '#a0aec0' }
    },

    dataZoom: [
      {
        type: 'slider',
        show: true,
        xAxisIndex: [0],
        start: 0,
        end: 100,
        bottom: 8,
        height: 18,
        borderColor: 'rgba(255,255,255,0.1)',
        fillerColor: 'rgba(0,191,255,0.15)',
        handleStyle: { color: '#00bfff' },
        textStyle: { color: '#8a99ad' },
        dataBackground: {
          lineStyle: { color: 'rgba(0,191,255,0.3)' },
          areaStyle: { color: 'rgba(0,191,255,0.05)' }
        }
      },
      {
        type: 'inside',
        xAxisIndex: [0],
        start: 0,
        end: 100
      }
    ],

    grid: {
      top: '22%',
      left: '4%',
      right: '4%',
      bottom: '16%',
      containLabel: true
    },

    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dateList,
      axisLine: { lineStyle: { color: '#1e293b' } },
      axisLabel: { color: '#8a99ad', rotate: 25 }
    },

    yAxis: {
      type: 'value',
      max: 100,
      name: '占比(%)',
      nameTextStyle: {
        color: '#8a99ad',
        padding: [0, 20, 0, 0]
      },
      axisLine: {
        show: true,
        lineStyle: { color: '#1e293b' }
      },
      axisLabel: {
        color: '#8a99ad',
        formatter: '{value}%'
      },
      splitLine: {
        lineStyle: { color: 'rgba(255,255,255,0.05)' }
      }
    },

    series: [
      {
        name: '中性',
        type: 'line',
        stack: 'total',
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(138,153,173,0.5)' },
            { offset: 1, color: 'rgba(138,153,173,0.05)' }
          ])
        },
        data: neutralRatio
      },
      {
        name: '正面',
        type: 'line',
        stack: 'total',
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,255,204,0.5)' },
            { offset: 1, color: 'rgba(0,255,204,0.05)' }
          ])
        },
        data: positiveRatio
      },
      {
        name: '负面',
        type: 'line',
        stack: 'total',
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255,67,101,0.5)' },
            { offset: 1, color: 'rgba(255,67,101,0.05)' }
          ])
        },
        data: negativeRatio
      }
    ]
  }

  myChart.setOption(option, true)
}

const handleResize = () => {
  myChart?.resize()
}

onMounted(() => {
  if (chartRef.value) {
    myChart = echarts.init(chartRef.value, 'dark')
    renderChart()
  }
  window.addEventListener('resize', handleResize)
})

watch(() => props.data, () => {
  nextTick(() => renderChart())
}, { deep: true })

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  myChart?.dispose()
  myChart = null
})
</script>

<template>
  <div ref="chartRef" class="trend-chart"></div>
</template>

<style scoped>
.trend-chart {
  width: 100%;
  height: 420px;
  background: rgba(16, 24, 48, 0.4);
  border: 1px solid #1e293b;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.08);
  border-radius: 8px;
  padding: 15px;
  box-sizing: border-box;
}
</style>

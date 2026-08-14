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

  const list = props.data
  const authList = [...new Set(list.map(v => v.userAuthentication))]

  const posArr = [], neuArr = [], negArr = []
  authList.forEach(auth => {
    const group = list.filter(v => v.userAuthentication === auth)
    const total = group.reduce((s, i) => s + i.value, 0)
    if (total === 0) { posArr.push(0); neuArr.push(0); negArr.push(0); return }
    const pos = group.find(i => i.label === '正面')?.value || 0
    const neu = group.find(i => i.label === '中性')?.value || 0
    const neg = group.find(i => i.label === '负面')?.value || 0
    posArr.push(Number((pos / total * 100).toFixed(1)))
    neuArr.push(Number((neu / total * 100).toFixed(1)))
    negArr.push(Number((neg / total * 100).toFixed(1)))
  })

  const option = {
    backgroundColor: 'transparent',
    color: ['#00ffcc', '#8a99ad', '#ff4365'],

    title: {
      text: '不同认证用户情感分布',
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
      axisPointer: {
        type: 'shadow',
        shadowStyle: { color: 'rgba(0,191,255,0.05)' }
      },
      backgroundColor: 'rgba(11,19,36,0.9)',
      borderColor: '#00bfff',
      borderWidth: 1,
      textStyle: { color: '#fff' },
      formatter: p => {
        let str = p[0].axisValue + '<br>'
        p.forEach(item => str += `${item.marker}${item.seriesName}：${item.data}%<br>`)
        return str
      }
    },

    legend: {
      data: ['正面', '中性', '负面'],
      top: '4%',
      right: '5%',
      itemWidth: 12,
      itemHeight: 12,
      textStyle: { color: '#a0aec0' }
    },

    grid: {
      top: '22%',
      left: '4%',
      right: '6%',
      bottom: '6%',
      containLabel: true
    },

    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { color: '#8a99ad', formatter: '{value}%' },
      axisLine: { show: true, lineStyle: { color: '#1e293b' } },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
    },

    yAxis: {
      type: 'category',
      data: authList,
      axisLabel: { color: '#8a99ad' },
      axisLine: { lineStyle: { color: '#1e293b' } }
    },

    series: [
      {
        name: '正面',
        type: 'bar',
        stack: 'total',
        barMaxWidth: 28,
        data: posArr,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: '#00ffcc' },
            { offset: 1, color: 'rgba(0,255,204,0.5)' }
          ])
        },
        label: {
          show: true,
          position: 'inside',
          fontSize: 11,
          color: '#0b1324',
          formatter: p => p.data > 5 ? p.data + '%' : ''
        }
      },
      {
        name: '中性',
        type: 'bar',
        stack: 'total',
        barMaxWidth: 28,
        data: neuArr,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: '#8a99ad' },
            { offset: 1, color: 'rgba(138,153,173,0.5)' }
          ])
        },
        label: {
          show: true,
          position: 'inside',
          fontSize: 11,
          color: '#fff',
          formatter: p => p.data > 5 ? p.data + '%' : ''
        }
      },
      {
        name: '负面',
        type: 'bar',
        stack: 'total',
        barMaxWidth: 28,
        data: negArr,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: '#ff4365' },
            { offset: 1, color: 'rgba(255,67,101,0.5)' }
          ])
        },
        label: {
          show: true,
          position: 'inside',
          fontSize: 11,
          color: '#fff',
          formatter: p => p.data > 5 ? p.data + '%' : ''
        }
      }
    ]
  }

  myChart.setOption(option, true)
}

const handleResize = () => myChart?.resize()

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
  <div ref="chartRef" class="chart-box"></div>
</template>

<style scoped>
.chart-box {
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

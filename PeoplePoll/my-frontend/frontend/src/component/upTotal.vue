<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  totalData: {
    type: Number,
    default: 0,
  },
})

const lastValue = ref('0')
const digits = ref([])

// 固定位数 + 固定 KEY → 绝对不重建 DOM
function updateDigits(curr) {
  const prev = lastValue.value.padStart(curr.length, ' ')

  // 不重新生成数组！只修改内容
  for (let i = 0; i < curr.length; i++) {
    const isNew = !digits.value[i]
    const p = prev[i]
    const c = curr[i]

    if (isNew) {
      digits.value.push({
        num: c,
        changed: true,
        fixedKey: 'digit-' + i, // 固定 KEY！核心！
      })
    } else {
      digits.value[i].num = c
      digits.value[i].changed = p !== c
    }
  }

  // 长度超过就删除
  if (digits.value.length > curr.length) {
    digits.value.splice(curr.length)
  }

  // 动画后重置状态
  setTimeout(() => {
    digits.value.forEach(d => d.changed = false)
  }, 300)

  lastValue.value = curr
}

watch(
  () => props.totalData,
  (val) => {
    updateDigits(String(val || 0))
  },
  { immediate: true }
)
</script>

<template>
  <el-card class="total-card" shadow="never">
    <div class="card-title">当前统计微博总数</div>
    <div class="flip-board">
      <div
        class="flip-item"
        v-for="item in digits"
        :key="item.fixedKey"
      >
        <div class="digital" :class="{ flip: item.changed }">
          {{ item.num }}
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.total-card {
  --el-card-bg-color: linear-gradient(135deg, #0b1528 0%, #060d1a 100%) !important;
  --el-card-border-color: #1e293b !important;
  text-align: center;
  padding: 12px 10px;
  border-radius: 12px !important;
  box-shadow: 0 4px 20px rgba(0,0,0,0.5);
}

:deep(.el-card__body) { padding: 0; }

.card-title {
  font-size: 15px;
  color: #00ffcc; /* 改为炫酷荧光绿 */
  font-weight: 600;
  letter-spacing: 1px;
  margin-bottom: 12px;
  text-transform: uppercase;
}

.flip-board {
  display: flex;
  justify-content: center;
  gap: 6px;
}

.flip-item {
  width: 46px; /* 适当收窄，使得整体紧凑精致 */
  height: 56px;
  background: #09101d;
  border-radius: 6px;
  border: 1px solid #00bfff; /* 科技蓝科技边界 */
  box-shadow: inset 0 0 10px rgba(0, 191, 255, 0.3);
  overflow: hidden;
}

.digital {
  width: 100%;
  height: 100%;
  font-size: 36px;
  font-weight: 800;
  color: #ffffff;
  text-shadow: 0 0 8px rgba(0, 242, 254, 0.8); /* 赋予数字LED灯发光特效 */
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Courier New', Courier, monospace; /* 替换为等宽电子字体 */
}

.flip {
  animation: flip 0.4s ease-in-out;
}

@keyframes flip {
  0% { transform: rotateX(80deg); opacity: 0.5; }
  100% { transform: rotateX(0); opacity: 1; }
}


.total-card {
  --el-card-bg-color: #141414;
  --el-card-border-color: #2a2f3f;
  text-align: center;
  padding: 20px 10px;
}

:deep(.el-card__body) { padding: 0; }

.card-title {
  font-size: 18px;
  color: #d1d5db;
  margin-bottom: 20px;
}

.flip-board {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.flip-item {
  width: 58px;
  height: 63px;
  background: #0f141f;
  border-radius: 8px;
  border: 1px solid #374151;
  overflow: hidden;
}

.digital {
  width: 100%;
  height: 100%;
  font-size: 42px;
  font-weight: bold;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.flip {
  animation: flip 0.5s ease-in-out;
}

@keyframes flip {
  0% { transform: rotateX(90deg); }
  100% { transform: rotateX(0); }
}
</style>

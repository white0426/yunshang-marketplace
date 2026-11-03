<script setup>
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
  height: {
    type: Number,
    default: 230,
  },
})

const chartEl = ref(null)
const colors = ['#2d6a4f', '#315d7a', '#c66b57', '#c4a66a', '#7b6d8d', '#6e8b74']
let chart
let resizeObserver

function renderChart() {
  if (!chart) return
  chart.setOption({
    animationDuration: 650,
    color: colors,
    tooltip: {
      trigger: 'item',
      backgroundColor: '#17201c',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 },
      formatter: '{b}<br/>{c} 件（{d}%）',
    },
    legend: {
      left: 'center',
      bottom: 0,
      icon: 'circle',
      itemWidth: 7,
      itemHeight: 7,
      itemGap: 13,
      textStyle: { color: '#77827c', fontSize: 10 },
    },
    series: [
      {
        type: 'pie',
        radius: ['46%', '70%'],
        center: ['50%', '43%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 3,
          borderRadius: 4,
        },
        label: { show: false },
        emphasis: { scale: true, scaleSize: 4 },
        data: props.items,
      },
    ],
  })
}

onMounted(() => {
  chart = echarts.init(chartEl.value)
  renderChart()
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartEl.value)
})

watch(() => props.items, renderChart, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div ref="chartEl" class="demand-donut" :style="{ height: `${height}px` }" />
</template>

<script setup>
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  values: {
    type: Array,
    required: true,
  },
  labels: {
    type: Array,
    default: () => ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  },
  color: {
    type: String,
    default: '#2d6a4f',
  },
  type: {
    type: String,
    default: 'line',
  },
  height: {
    type: Number,
    default: 220,
  },
})

const chartEl = ref(null)
let chart
let resizeObserver

function renderChart() {
  if (!chart) return
  const isLine = props.type === 'line'
  chart.setOption({
    animationDuration: 700,
    grid: { left: 8, right: 8, top: 20, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#17201c',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 },
      axisPointer: {
        type: isLine ? 'line' : 'shadow',
        lineStyle: { color: '#a7b7ae', type: 'dashed' },
      },
    },
    xAxis: {
      type: 'category',
      data: props.labels,
      axisLine: { lineStyle: { color: '#dfe5e1' } },
      axisTick: { show: false },
      axisLabel: { color: '#77827c', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#edf0ee' } },
      axisLabel: {
        color: '#8a948e',
        fontSize: 11,
        formatter: (value) => (value >= 10000 ? `${value / 10000}万` : value),
      },
    },
    series: [
      {
        type: props.type,
        data: props.values,
        smooth: isLine,
        symbol: isLine ? 'circle' : 'none',
        symbolSize: 6,
        showSymbol: false,
        lineStyle: { width: 3, color: props.color },
        itemStyle: {
          color: props.color,
          borderRadius: isLine ? 0 : [4, 4, 0, 0],
        },
        areaStyle: isLine
          ? {
              color: props.color,
              opacity: 0.08,
            }
          : undefined,
        barMaxWidth: 22,
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

watch(
  () => [props.values, props.labels, props.color, props.type],
  renderChart,
  { deep: true },
)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div ref="chartEl" class="sales-chart" :style="{ height: `${height}px` }" />
</template>

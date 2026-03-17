<template>
  <div class="page">
    <div class="statistics-layout">
      <div class="main-column">
        <div class="main-header">
           <h1 class="page-title" style="margin-bottom: 0;">文章统计</h1>
        </div>

        <div v-if="errorMsg" class="error-message">
          {{ errorMsg }}
        </div>

        <div class="statistic-card card">
            <div class="chart-container">
               <div class="chart-item">
                  <div class="chart-title">分类占比</div>
                  <div ref="categoryChartRef" style="height: 400px; width:100%;"></div>
               </div>
               <div class="chart-item">
                  <div class="chart-title">Top 10 标签</div>
                  <div ref="tagChartRef" style="height: 400px; width:100%;"></div>
               </div>
               <div class="chart-item">
                  <div class="chart-title">Top 10 浏览量</div>
                  <div ref="viewChartRef" style="height: 450px; width:100%;"></div>
               </div>
               <div class="chart-item">
                  <div class="chart-title">近14天发布趋势</div>
                  <div ref="trendChartRef" style="height: 450px; width:100%;"></div>
               </div>
            </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, nextTick, onBeforeUnmount } from 'vue';
import axios from 'axios';
import * as echarts from 'echarts';

defineOptions({
  name: 'Statistics'
});

const errorMsg = ref('');
const categoryChartRef = ref(null);
const tagChartRef = ref(null);
const viewChartRef = ref(null);
const trendChartRef = ref(null);
let categoryChart = null;
let tagChart = null;
let viewChart = null;
let trendChart = null;

const resizeCharts = () => {
  if (categoryChart) categoryChart.resize();
  if (tagChart) tagChart.resize();
  if (viewChart) viewChart.resize();
  if (trendChart) trendChart.resize();
};

const handleResize = () => resizeCharts();

const fetchStatistics = async () => {
  errorMsg.value = '';
  try {
    const [resCat, resTag, resView, resTrend] = await Promise.all([
      axios.get('/api/statistics/categories'),
      axios.get('/api/statistics/tags'),
      axios.get('/api/statistics/views'),
      axios.get('/api/statistics/timeline')
    ]);

    if (resCat.data.code === 0 && categoryChartRef.value) {
      if (!categoryChart) {
        categoryChart = echarts.init(categoryChartRef.value);
      }
      const data = resCat.data.data || [];
      categoryChart.setOption({
        tooltip: {
          trigger: 'item'
        },
        legend: {
          top: '6%',
          left: 'center',
          itemGap: 12,
          textStyle: { color: '#e5e7eb' }
        },
        series: [
          {
            name: '文章分类',
            type: 'pie',
            center: ['50%', '58%'],
            radius: ['45%', '68%'],
            avoidLabelOverlap: true,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#020617',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 20,
                fontWeight: 'bold',
                color: '#fff'
              }
            },
            labelLine: {
              show: false
            },
            data
          }
        ]
      });
      resizeCharts();
    }

    if (resTag.data.code === 0 && tagChartRef.value) {
      if (!tagChart) {
        tagChart = echarts.init(tagChartRef.value);
      }
      const data = resTag.data.data || [];
      const top10 = data.slice(0, 10);
      tagChart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: [
          {
            type: 'category',
            data: top10.map(d => d.name),
            axisTick: { alignWithLabel: true },
            axisLabel: { color: '#9ca3af', interval: 0, rotate: 30 }
          }
        ],
        yAxis: [
          {
            type: 'value',
            axisLabel: { color: '#9ca3af' },
            splitLine: { lineStyle: { color: '#334155' } }
          }
        ],
        series: [
          {
            name: '文章数',
            type: 'bar',
            barWidth: '60%',
            data: top10.map(d => d.value),
            itemStyle: { color: '#38bdf8' }
          }
        ]
      });
      resizeCharts();
    }

    if (resView.data.code === 0 && viewChartRef.value) {
      if (!viewChart) {
        viewChart = echarts.init(viewChartRef.value);
      }
      const data = resView.data.data || [];
      viewChart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: {
          left: '12%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          axisLabel: { color: '#9ca3af' },
          splitLine: { lineStyle: { color: '#334155' } }
        },
        yAxis: {
          type: 'category',
          data: data.map(d => d.name),
          axisLabel: {
            color: '#9ca3af',
            formatter: function (value) {
              if (value.length > 8) {
                return value.substring(0, 8) + '...';
              }
              return value;
            }
          }
        },
        series: [
          {
            name: '浏览量',
            type: 'bar',
            barWidth: '60%',
            data: data.map(d => d.value),
            itemStyle: { color: '#a78bfa' }
          }
        ]
      });
      resizeCharts();
    }

    if (resTrend.data.code === 0 && trendChartRef.value) {
      if (!trendChart) {
        trendChart = echarts.init(trendChartRef.value);
      }
      const data = resTrend.data.data || [];
      const trendXData = data.map(d => {
        // Simple date formatting to MM-DD if string is YYYY-MM-DD
        const dateStr = d.date;
        if (dateStr && dateStr.length >= 10) {
           return dateStr.substring(5);
        }
        return dateStr;
      });

      trendChart.setOption({
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: trendXData,
          axisLabel: { color: '#9ca3af', rotate: 0 }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: '#9ca3af' },
          splitLine: { lineStyle: { color: '#334155' } }
        },
        series: [
          {
            name: '发布量',
            type: 'line',
            smooth: true,
            data: data.map(d => d.value),
            lineStyle: { color: '#38bdf8' },
            areaStyle: { color: 'rgba(56, 189, 248, 0.18)' },
            symbol: 'circle',
            symbolSize: 6
          }
        ]
      });
      resizeCharts();
    }
  } catch (err) {
    console.error('获取统计数据失败', err);
    errorMsg.value = '无法获取统计数据，请检查后端服务是否正在运行。';
  }
};

onMounted(() => {
  nextTick(() => {
    fetchStatistics();
    window.addEventListener('resize', handleResize);
  });
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  if (categoryChart) {
    categoryChart.dispose();
    categoryChart = null;
  }
  if (tagChart) {
    tagChart.dispose();
    tagChart = null;
  }
  if (viewChart) {
    viewChart.dispose();
    viewChart = null;
  }
  if (trendChart) {
    trendChart.dispose();
    trendChart = null;
  }
});
</script>

<style scoped>
.error-message {
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #fca5a5;
  padding: 1rem;
  margin: 1rem;
  border-radius: 0.5rem;
  text-align: center;
}

.page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.statistics-layout {
  display: flex;
  flex: 1;
  width: 100%;
  margin: 0;
  padding: 1rem;
}

.main-column {
  width: 100%;
  flex: 1;
  background: radial-gradient(circle at top left, #1e293b, #020617);
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.7);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.main-header {
  padding: 1.2rem 1.4rem;
  background: rgba(255, 255, 255, 0.02);
  border-bottom: 1px solid rgba(148, 163, 184, 0.25);
}

.page-title {
  font-size: 1.6rem;
  font-weight: 700;
  color: #e5e7eb;
}

.card {
  background: radial-gradient(circle at top left, #0b1224, #020617);
  border-radius: 1rem;
  padding: 1.6rem;
  margin: 1rem 0 1.4rem;
  border: 1px solid rgba(148, 163, 184, 0.3);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.7);
  width: 100%;
}

.chart-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.2rem;
}

@media (max-width: 1024px) {
  .chart-container {
    grid-template-columns: 1fr;
  }
}

.chart-item {
  background: rgba(15, 23, 42, 0.75);
  border-radius: 1rem;
  padding: 1.6rem 1.8rem 1.4rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
  min-height: 480px;
}

.chart-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #e5e7eb;
  margin-bottom: 0.75rem;
  text-align: center;
}
</style>

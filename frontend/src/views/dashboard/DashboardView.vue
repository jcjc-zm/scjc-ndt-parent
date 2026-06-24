<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { dashboardApi } from '@/api/dashboard'
import VChart from 'vue-echarts'
import 'echarts'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const stats = ref({
  totalInspections: 0,
  qualifiedRate: 100,
  pendingCount: 0,
  activeProjects: 0,
  weeklyNew: 0,
  trend: [],
  methodDistribution: [],
  weeklyWorkload: [],
  projectLocations: [],
})

// ─── Map ───
const mapContainer = ref(null)
let mapInstance = null
let markerLayer = null
let labelLayer = null

// ⭐ icon — Leaflet divIcon with star emoji
const starIcon = L.divIcon({
  html: '<div style="font-size:11px;line-height:1;text-align:center;">⭐</div>',
  className: '',
  iconSize: [14, 14],
  iconAnchor: [7, 7],
  popupAnchor: [0, -7],
})

// 省/直辖市/自治区 中心坐标 + 标签
const PROVINCE_LABELS = [
  { name: '黑龙江省', lat: 47.80, lng: 127.50 },
  { name: '吉林省', lat: 43.90, lng: 125.30 },
  { name: '辽宁省', lat: 41.80, lng: 123.40 },
  { name: '内蒙古', lat: 43.50, lng: 113.00 },
  { name: '新疆', lat: 41.50, lng: 85.00 },
  { name: '西藏', lat: 31.00, lng: 89.00 },
  { name: '青海省', lat: 35.50, lng: 96.00 },
  { name: '甘肃省', lat: 37.00, lng: 103.50 },
  { name: '宁夏', lat: 37.20, lng: 106.00 },
  { name: '陕西省', lat: 35.50, lng: 109.00 },
  { name: '山西省', lat: 37.50, lng: 112.00 },
  { name: '河北省', lat: 38.00, lng: 115.00 },
  { name: '北京市', lat: 39.90, lng: 116.40 },
  { name: '天津市', lat: 39.13, lng: 117.19 },
  { name: '山东省', lat: 36.20, lng: 118.00 },
  { name: '河南省', lat: 34.00, lng: 113.50 },
  { name: '江苏省', lat: 32.50, lng: 119.50 },
  { name: '安徽省', lat: 31.50, lng: 117.00 },
  { name: '上海市', lat: 31.23, lng: 121.47 },
  { name: '湖北省', lat: 31.00, lng: 113.00 },
  { name: '浙江省', lat: 29.50, lng: 120.00 },
  { name: '湖南省', lat: 27.50, lng: 112.00 },
  { name: '江西省', lat: 27.50, lng: 116.00 },
  { name: '四川省', lat: 30.50, lng: 103.00 },
  { name: '重庆市', lat: 29.56, lng: 106.55 },
  { name: '贵州省', lat: 26.80, lng: 106.50 },
  { name: '云南省', lat: 25.00, lng: 102.00 },
  { name: '广西', lat: 23.50, lng: 108.50 },
  { name: '广东省', lat: 23.00, lng: 113.50 },
  { name: '福建省', lat: 25.50, lng: 118.00 },
  { name: '海南省', lat: 19.50, lng: 109.50 },
]

// 省级标签图标工厂
function makeProvinceLabelIcon(name) {
  return L.divIcon({
    html: `<div style="
      font-size:13px;
      font-weight:700;
      color:#374151;
      text-shadow:1px 1px 0 #fff, -1px -1px 0 #fff, 1px -1px 0 #fff, -1px 1px 0 #fff, 0 0 4px rgba(255,255,255,0.8);
      white-space:nowrap;
      pointer-events:none;
      letter-spacing:1px;
    ">${name}</div>`,
    className: '',
    iconSize: [0, 0],
    iconAnchor: [0, 0],
  })
}

// 中国主要城市坐标库
const CITY_COORDS = {
  '北京': [39.90, 116.40], '上海': [31.23, 121.47], '天津': [39.13, 117.19],
  '重庆': [29.56, 106.55], '成都': [30.57, 104.07], '绵阳': [31.47, 104.68],
  '西安': [34.26, 108.94], '武汉': [30.59, 114.30], '南京': [32.06, 118.80],
  '杭州': [30.27, 120.15], '广州': [23.13, 113.26], '深圳': [22.54, 114.06],
  '长沙': [28.23, 112.94], '郑州': [34.75, 113.63], '济南': [36.67, 116.98],
  '青岛': [36.07, 120.38], '大连': [38.91, 121.61], '沈阳': [41.80, 123.43],
  '哈尔滨': [45.80, 126.53], '长春': [43.88, 125.32], '昆明': [25.04, 102.71],
  '贵阳': [26.65, 106.63], '南宁': [22.82, 108.32], '海口': [20.02, 110.35],
  '兰州': [36.06, 103.83], '西宁': [36.62, 101.78], '银川': [38.47, 106.27],
  '乌鲁木齐': [43.82, 87.62], '克拉玛依': [45.60, 84.87], '库尔勒': [41.73, 86.15],
  '喀什': [39.47, 75.99], '伊犁': [43.92, 81.32], '伊宁': [43.91, 81.28],
  '阿克苏': [41.17, 80.26], '哈密': [42.83, 93.51], '昌吉': [44.01, 87.30],
  '石河子': [44.30, 86.08], '吐鲁番': [42.95, 89.19], '和田': [37.11, 79.91],
  '塔城': [46.75, 82.98], '阿勒泰': [47.84, 88.14], '博乐': [44.90, 82.07],
  '拉萨': [29.65, 91.11], '日喀则': [29.27, 88.88], '林芝': [29.65, 94.36],
  '合肥': [31.86, 117.28], '南昌': [28.68, 115.89], '福州': [26.07, 119.30],
  '厦门': [24.48, 118.09], '石家庄': [38.04, 114.51], '唐山': [39.63, 118.18],
  '太原': [37.87, 112.55], '大同': [40.09, 113.30], '呼和浩特': [40.84, 111.75],
  '包头': [40.65, 109.84], '鄂尔多斯': [39.61, 109.78], '苏州': [31.30, 120.58],
  '无锡': [31.57, 120.30], '宁波': [29.87, 121.55], '温州': [28.00, 120.70],
  '大庆': [46.59, 125.03], '齐齐哈尔': [47.35, 123.92], '攀枝花': [26.58, 101.72],
  '洛阳': [34.66, 112.45], '开封': [34.80, 114.31], '宜昌': [30.69, 111.29],
  '襄阳': [32.04, 112.14], '岳阳': [29.37, 113.10], '株洲': [27.83, 113.13],
  '桂林': [27.83, 113.13], '柳州': [24.31, 109.41], '三亚': [18.25, 109.50],
  '珠海': [22.27, 113.57], '东莞': [23.05, 113.75], '佛山': [23.02, 113.12],
  '烟台': [37.53, 121.39], '威海': [37.51, 122.12], '徐州': [34.26, 117.19],
  '南通': [32.02, 120.86], '常州': [31.79, 119.97], '绍兴': [30.03, 120.58],
  '泉州': [24.91, 118.59], '汕头': [23.37, 116.68], '湛江': [21.27, 110.36],
  '北海': [21.47, 109.12], '遵义': [27.72, 106.93], '宜宾': [28.77, 104.62],
  '德阳': [31.13, 104.40], '南充': [30.80, 106.08],
}

// 省级关键词 → 省会坐标（geocoding 回退）
const PROVINCE_COORDS = {
  '新疆': [41.50, 85.00], '西藏': [31.00, 89.00], '青海': [35.50, 96.00],
  '甘肃': [37.00, 103.50], '宁夏': [37.20, 106.00], '内蒙古': [43.00, 113.00],
  '黑龙江': [47.50, 127.00], '吉林': [43.50, 126.00], '辽宁': [41.50, 122.50],
  '四川': [30.50, 103.00], '云南': [25.00, 102.00], '贵州': [26.80, 106.50],
  '广西': [23.50, 108.50], '广东': [23.00, 113.50], '海南': [19.50, 109.50],
  '福建': [25.50, 118.00], '江西': [27.50, 116.00], '湖南': [27.50, 112.00],
  '湖北': [31.00, 113.00], '河南': [34.00, 113.50], '河北': [38.00, 115.00],
  '山东': [36.00, 118.00], '山西': [37.50, 112.00], '陕西': [35.50, 109.00],
  '江苏': [32.50, 119.50], '浙江': [29.50, 120.00], '安徽': [31.50, 117.00],
}

function geocodeAddress(address) {
  if (!address) return null
  // 先精确匹配城市名
  for (const [city, coords] of Object.entries(CITY_COORDS)) {
    if (address.includes(city)) {
      return { lat: coords[0], lng: coords[1] }
    }
  }
  // 再匹配省级
  for (const [prov, coords] of Object.entries(PROVINCE_COORDS)) {
    if (address.includes(prov)) {
      return { lat: coords[0], lng: coords[1] }
    }
  }
  return null
}

function initMapBase() {
  if (!mapContainer.value) {
    console.warn('地图容器未就绪，200ms 后重试')
    setTimeout(initMapBase, 200)
    return
  }
  if (mapInstance) return // 已初始化
  try {
    mapInstance = L.map(mapContainer.value, {
      center: [35.86, 104.19],
      zoom: 5,
      minZoom: 4,
      maxBounds: [[10, 65], [58, 145]],
      attributionControl: false,
    })
    // 高德标准瓦片 — 基础底图
    L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x={x}&y={y}&z={z}', {
      maxZoom: 18,
      subdomains: ['1', '2', '3', '4'],
    }).addTo(mapInstance)

    // 自定义省名/直辖市标签层 — 保证最小缩放时也可见
    addProvinceLabels()

    // 确保 Leaflet 感知容器实际尺寸
    setTimeout(() => {
      if (mapInstance) mapInstance.invalidateSize()
    }, 500)
  } catch (e) {
    console.error('地图初始化失败:', e)
  }
}

function addProvinceLabels() {
  if (!mapInstance) return
  if (labelLayer) mapInstance.removeLayer(labelLayer)
  labelLayer = L.layerGroup().addTo(mapInstance)
  for (const p of PROVINCE_LABELS) {
    L.marker([p.lat, p.lng], { icon: makeProvinceLabelIcon(p.name), interactive: false })
      .addTo(labelLayer)
  }
}

async function updateMapMarkers() {
  if (!mapInstance) {
    await new Promise(r => setTimeout(r, 500))
    if (!mapInstance) return
  }
  if (markerLayer) mapInstance.removeLayer(markerLayer)
  markerLayer = L.layerGroup().addTo(mapInstance)

  const locations = stats.value.projectLocations || []
  if (locations.length === 0) return

  const bounds = []
  for (const loc of locations) {
    if (!loc.location) continue
    const coords = geocodeAddress(loc.location)
    if (coords) {
      L.marker([coords.lat, coords.lng], { icon: starIcon })
        .bindPopup(`<strong>${loc.projectName}</strong><br>${loc.location}`)
        .addTo(markerLayer)
      bounds.push([coords.lat, coords.lng])
    }
  }

  if (bounds.length > 0) {
    mapInstance.fitBounds(bounds, { padding: [30, 30], maxZoom: 10 })
  }
}

// ECharts trend option (30-day inspection trend)
const trendOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
  },
  grid: { top: 10, right: 16, bottom: 20, left: 40 },
  xAxis: {
    type: 'category',
    data: Array.from({ length: 30 }, (_, i) => {
      const d = new Date()
      d.setDate(d.getDate() - 29 + i)
      return `${d.getMonth() + 1}/${d.getDate()}`
    }),
    axisLabel: { fontSize: 11, color: '#94a3b8' },
  },
  yAxis: {
    type: 'value',
    axisLabel: { fontSize: 11, color: '#94a3b8' },
    splitLine: { lineStyle: { color: '#f1f5f9' } },
  },
  series: [
    {
      name: '检测数量',
      type: 'bar',
      data: stats.value.trend || Array.from({ length: 30 }, () => 0),
      itemStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#10b981' },
            { offset: 1, color: '#059669' },
          ],
        },
        borderRadius: [4, 4, 0, 0],
      },
      barWidth: '60%',
    },
  ],
}))

// Method distribution chart option
const methodChartOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)',
  },
  legend: {
    orient: 'vertical',
    left: 'left',
    top: 'center',
    textStyle: { fontSize: 12, color: '#64748b' },
  },
  series: [
    {
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['55%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 4,
        borderColor: '#fff',
        borderWidth: 2,
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' },
      },
      data: (stats.value.methodDistribution || []).map((m) => ({
        name: m.method,
        value: m.count,
      })),
      color: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316', '#84cc16'],
    },
  ],
}))

// KPI card config
const kpiCards = computed(() => [
  {
    label: '检测总任务',
    value: stats.value.totalInspections || 0,
    color: '#3b82f6',
    bg: '#eff6ff',
    icon: 'Document',
  },
  {
    label: '合格率',
    value: (stats.value.qualifiedRate || 100).toFixed(1) + '%',
    color: '#10b981',
    bg: '#ecfdf5',
    icon: 'CircleCheck',
  },
  {
    label: '待处理任务',
    value: stats.value.pendingCount || 0,
    color: '#f59e0b',
    bg: '#fffbeb',
    icon: 'Clock',
  },
  {
    label: '本周新增',
    value: stats.value.weeklyNew || 0,
    color: '#8b5cf6',
    bg: '#f5f3ff',
    icon: 'Plus',
  },
  {
    label: '活跃项目',
    value: stats.value.activeProjects || 0,
    color: '#059669',
    bg: '#ecfdf5',
    icon: 'FolderOpened',
  },
])

async function loadData() {
  loading.value = true
  try {
    const res = await dashboardApi.overview()
    if (res?.data) stats.value = res.data
    await nextTick()
    updateMapMarkers()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await nextTick()
  initMapBase()
  loadData()
})
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <!-- Page header -->
    <div class="page-header">
      <div>
        <h2 class="page-title">数据总览</h2>
        <p class="page-subtitle">无损检测数据总览</p>
      </div>
      <div class="header-actions">
        <el-button v-if="userStore.canCreateProject" :icon="Plus" size="small" @click="router.push('/project/create')">创建立项</el-button>
        <el-button :icon="Edit" size="small" @click="router.push('/approval')">审批</el-button>
        <el-button :icon="Document" size="small" @click="router.push('/report')">报告</el-button>
        <el-button v-if="userStore.canManageUsers" :icon="User" size="small" @click="router.push('/user')">用户</el-button>
        <el-button :icon="Refresh" size="small" @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- KPI Cards -->
    <div class="kpi-grid">
      <div
        v-for="card in kpiCards"
        :key="card.label"
        class="kpi-card"
        :style="{ '--kpi-color': card.color, '--kpi-bg': card.bg }"
      >
        <div class="kpi-icon-box">
          <el-icon :size="24"><component :is="card.icon" /></el-icon>
        </div>
        <div class="kpi-body">
          <span class="kpi-value">{{ card.value }}</span>
          <span class="kpi-label">{{ card.label }}</span>
        </div>
      </div>
    </div>

    <!-- Charts row -->
    <el-row :gutter="16" class="charts-row">
      <!-- Trend chart -->
      <el-col :span="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span class="card-title">近30天检测趋势</span>
          </template>
          <div v-if="stats.trend?.length" class="chart-body">
            <v-chart :option="trendOption" autoresize />
          </div>
          <el-empty v-else description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>

      <!-- Method distribution -->
      <el-col :span="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span class="card-title">检测方法分布</span>
          </template>
          <div v-if="stats.methodDistribution?.length" class="chart-body">
            <v-chart :option="methodChartOption" autoresize />
          </div>
          <el-empty v-else description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Weekly workload + Map -->
    <el-row :gutter="16" class="bottom-row">
      <el-col :span="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header-row">
              <span class="card-title">本周工作量统计</span>
              <span class="card-subtitle">按事业部 × 检测方法</span>
            </div>
          </template>
          <el-table
            :data="stats.weeklyWorkload || []"
            border
            stripe
            size="small"
            :empty-text="'暂无数据'"
          >
            <el-table-column prop="deptName" label="事业部 / 公司级项目" fixed width="160" />
            <el-table-column label="RT" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.rt }">{{ row.rt || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="UT" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.ut }">{{ row.ut || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="PT" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.pt }">{{ row.pt || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="MT" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.mt }">{{ row.mt || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="AUT" width="70" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.aut }">{{ row.aut || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="PA" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.pa }">{{ row.pa || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="TOFD" width="75" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.tofd }">{{ row.tofd || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="DR" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.dr }">{{ row.dr || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="合计" width="75" align="center" fixed="right">
              <template #default="{ row }">
                <el-tag :type="row.total > 20 ? 'danger' : row.total > 10 ? 'warning' : 'primary'" size="small">
                  {{ row.total || 0 }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never" class="chart-card map-card">
          <template #header>
            <span class="card-title">项目分布地图 ⭐</span>
          </template>
          <div class="map-wrap">
            <div ref="mapContainer" class="map-container"></div>
            <div v-if="!stats.projectLocations?.length" class="map-empty-overlay">
              <el-empty description="暂无项目位置数据" :image-size="60" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Page header */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 2px;
}

.page-subtitle {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* KPI cards grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 10px;
}

.kpi-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
  cursor: default;
}

.kpi-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.kpi-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--kpi-bg, #f1f5f9);
  color: var(--kpi-color, #3b82f6);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kpi-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.kpi-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.kpi-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 1px;
}

/* Chart cards */
.charts-row {
  margin-bottom: 10px;
}

.chart-card {
  border-radius: var(--radius-md);
  height: 100%;
}

.chart-card :deep(.el-card__header) {
  padding: 10px 16px;
  border-bottom: 1px solid var(--color-border-light);
}

.chart-body {
  height: 240px;
  width: 100%;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.card-header-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.card-subtitle {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.bottom-row {
  margin-bottom: 10px;
}

/* Table zero values */
.zero-count {
  color: var(--color-text-placeholder);
}

/* Map */
.map-card {
  overflow: hidden;
}

.map-wrap {
  position: relative;
  height: 280px;
  width: 100%;
}

.map-container {
  height: 100%;
  width: 100%;
  border-radius: var(--radius-sm);
}

.map-empty-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  z-index: 1000;
  border-radius: var(--radius-sm);
}

/* Responsive */
@media (max-width: 1400px) {
  .kpi-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

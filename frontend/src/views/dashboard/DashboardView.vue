<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue'
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
  pendingSignCount: 0,
  pendingEntryCount: 0,
  activeProjects: 0,
  weeklyNew: 0,
  trend: [],
  methodDistribution: [],
  weeklyWorkload: [],
  projectLocations: [],
})

// ─── Greeting ───
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

// ─── Map ───
const mapContainer = ref(null)
let mapInstance = null
let markerLayer = null
let labelLayer = null

const markerIcon = L.divIcon({
  html: `<div style="
    width:12px;height:12px;
    border-radius:50%;
    background:var(--color-accent, #059669);
    border:2px solid #fff;
    box-shadow: 0 2px 6px rgba(5,150,105,0.4);
  "></div>`,
  className: '',
  iconSize: [12, 12],
  iconAnchor: [6, 6],
  popupAnchor: [0, -6],
})

const PROVINCE_LABELS = [
  { name: '黑龙江省', lat: 47.80, lng: 127.50 }, { name: '吉林省', lat: 43.90, lng: 125.30 },
  { name: '辽宁省', lat: 41.80, lng: 123.40 }, { name: '内蒙古', lat: 43.50, lng: 113.00 },
  { name: '新疆', lat: 41.50, lng: 85.00 }, { name: '西藏', lat: 31.00, lng: 89.00 },
  { name: '青海省', lat: 35.50, lng: 96.00 }, { name: '甘肃省', lat: 37.00, lng: 103.50 },
  { name: '宁夏', lat: 37.20, lng: 106.00 }, { name: '陕西省', lat: 35.50, lng: 109.00 },
  { name: '山西省', lat: 37.50, lng: 112.00 }, { name: '河北省', lat: 38.00, lng: 115.00 },
  { name: '北京市', lat: 39.90, lng: 116.40 }, { name: '天津市', lat: 39.13, lng: 117.19 },
  { name: '山东省', lat: 36.20, lng: 118.00 }, { name: '河南省', lat: 34.00, lng: 113.50 },
  { name: '江苏省', lat: 32.50, lng: 119.50 }, { name: '安徽省', lat: 31.50, lng: 117.00 },
  { name: '上海市', lat: 31.23, lng: 121.47 }, { name: '湖北省', lat: 31.00, lng: 113.00 },
  { name: '浙江省', lat: 29.50, lng: 120.00 }, { name: '湖南省', lat: 27.50, lng: 112.00 },
  { name: '江西省', lat: 27.50, lng: 116.00 }, { name: '四川省', lat: 30.50, lng: 103.00 },
  { name: '重庆市', lat: 29.56, lng: 106.55 }, { name: '贵州省', lat: 26.80, lng: 106.50 },
  { name: '云南省', lat: 25.00, lng: 102.00 }, { name: '广西', lat: 23.50, lng: 108.50 },
  { name: '广东省', lat: 23.00, lng: 113.50 }, { name: '福建省', lat: 25.50, lng: 118.00 },
  { name: '海南省', lat: 19.50, lng: 109.50 },
]

function makeProvinceLabelIcon(name) {
  return L.divIcon({
    html: `<div style="
      font-size:13px;font-weight:700;color:#374151;
      text-shadow:1px 1px 0 #fff,-1px -1px 0 #fff,1px -1px 0 #fff,-1px 1px 0 #fff,0 0 4px rgba(255,255,255,0.8);
      white-space:nowrap;pointer-events:none;letter-spacing:1px;
    ">${name}</div>`,
    className: '',
    iconSize: [0, 0],
    iconAnchor: [0, 0],
  })
}

const CITY_COORDS = {
  '北京': [39.90,116.40],'上海': [31.23,121.47],'天津': [39.13,117.19],'重庆': [29.56,106.55],
  '成都': [30.57,104.07],'绵阳': [31.47,104.68],'西安': [34.26,108.94],'武汉': [30.59,114.30],
  '南京': [32.06,118.80],'杭州': [30.27,120.15],'广州': [23.13,113.26],'深圳': [22.54,114.06],
  '长沙': [28.23,112.94],'郑州': [34.75,113.63],'济南': [36.67,116.98],'青岛': [36.07,120.38],
  '大连': [38.91,121.61],'沈阳': [41.80,123.43],'哈尔滨': [45.80,126.53],'长春': [43.88,125.32],
  '昆明': [25.04,102.71],'贵阳': [26.65,106.63],'南宁': [22.82,108.32],'海口': [20.02,110.35],
  '兰州': [36.06,103.83],'西宁': [36.62,101.78],'银川': [38.47,106.27],'乌鲁木齐': [43.82,87.62],
  '克拉玛依': [45.60,84.87],'库尔勒': [41.73,86.15],'喀什': [39.47,75.99],'伊犁': [43.92,81.32],
  '伊宁': [43.91,81.28],'阿克苏': [41.17,80.26],'哈密': [42.83,93.51],'昌吉': [44.01,87.30],
  '石河子': [44.30,86.08],'吐鲁番': [42.95,89.19],'和田': [37.11,79.91],'塔城': [46.75,82.98],
  '阿勒泰': [47.84,88.14],'博乐': [44.90,82.07],'拉萨': [29.65,91.11],'日喀则': [29.27,88.88],
  '林芝': [29.65,94.36],'合肥': [31.86,117.28],'南昌': [28.68,115.89],'福州': [26.07,119.30],
  '厦门': [24.48,118.09],'石家庄': [38.04,114.51],'唐山': [39.63,118.18],'太原': [37.87,112.55],
  '大同': [40.09,113.30],'呼和浩特': [40.84,111.75],'包头': [40.65,109.84],'鄂尔多斯': [39.61,109.78],
  '苏州': [31.30,120.58],'无锡': [31.57,120.30],'宁波': [29.87,121.55],'温州': [28.00,120.70],
  '大庆': [46.59,125.03],'齐齐哈尔': [47.35,123.92],'攀枝花': [26.58,101.72],'洛阳': [34.66,112.45],
  '开封': [34.80,114.31],'宜昌': [30.69,111.29],'襄阳': [32.04,112.14],'岳阳': [29.37,113.10],
  '株洲': [27.83,113.13],'桂林': [27.83,113.13],'柳州': [24.31,109.41],'三亚': [18.25,109.50],
  '珠海': [22.27,113.57],'东莞': [23.05,113.75],'佛山': [23.02,113.12],'烟台': [37.53,121.39],
  '威海': [37.51,122.12],'徐州': [34.26,117.19],'南通': [32.02,120.86],'常州': [31.79,119.97],
  '绍兴': [30.03,120.58],'泉州': [24.91,118.59],'汕头': [23.37,116.68],'湛江': [21.27,110.36],
  '北海': [21.47,109.12],'遵义': [27.72,106.93],'宜宾': [28.77,104.62],'德阳': [31.13,104.40],
  '南充': [30.80,106.08],
}

const PROVINCE_COORDS = {
  '新疆': [41.50,85.00],'西藏': [31.00,89.00],'青海': [35.50,96.00],'甘肃': [37.00,103.50],
  '宁夏': [37.20,106.00],'内蒙古': [43.00,113.00],'黑龙江': [47.50,127.00],'吉林': [43.50,126.00],
  '辽宁': [41.50,122.50],'四川': [30.50,103.00],'云南': [25.00,102.00],'贵州': [26.80,106.50],
  '广西': [23.50,108.50],'广东': [23.00,113.50],'海南': [19.50,109.50],'福建': [25.50,118.00],
  '江西': [27.50,116.00],'湖南': [27.50,112.00],'湖北': [31.00,113.00],'河南': [34.00,113.50],
  '河北': [38.00,115.00],'山东': [36.00,118.00],'山西': [37.50,112.00],'陕西': [35.50,109.00],
  '江苏': [32.50,119.50],'浙江': [29.50,120.00],'安徽': [31.50,117.00],
}

function geocodeAddress(address) {
  if (!address) return null
  for (const [city, coords] of Object.entries(CITY_COORDS)) {
    if (address.includes(city)) return { lat: coords[0], lng: coords[1] }
  }
  for (const [prov, coords] of Object.entries(PROVINCE_COORDS)) {
    if (address.includes(prov)) return { lat: coords[0], lng: coords[1] }
  }
  return null
}

function initMapBase() {
  if (!mapContainer.value) { setTimeout(initMapBase, 200); return }
  if (mapInstance) return
  try {
    mapInstance = L.map(mapContainer.value, {
      center: [35.86, 104.19],
      zoom: 5,
      minZoom: 4,
      maxBounds: [[10, 65], [58, 145]],
      attributionControl: false,
    })
    L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x={x}&y={y}&z={z}', {
      maxZoom: 18,
      subdomains: ['1', '2', '3', '4'],
    }).addTo(mapInstance)
    addProvinceLabels()
    setTimeout(() => { if (mapInstance) mapInstance.invalidateSize() }, 500)
  } catch (e) {
    console.error('地图初始化失败:', e)
  }
}

function addProvinceLabels() {
  if (!mapInstance) return
  if (labelLayer) mapInstance.removeLayer(labelLayer)
  labelLayer = L.layerGroup().addTo(mapInstance)
  for (const p of PROVINCE_LABELS) {
    L.marker([p.lat, p.lng], { icon: makeProvinceLabelIcon(p.name), interactive: false }).addTo(labelLayer)
  }
}

async function updateMapMarkers() {
  if (!mapInstance) { await new Promise(r => setTimeout(r, 500)); if (!mapInstance) return }
  if (markerLayer) mapInstance.removeLayer(markerLayer)
  markerLayer = L.layerGroup().addTo(mapInstance)
  const locations = stats.value.projectLocations || []
  if (locations.length === 0) return
  const bounds = []
  for (const loc of locations) {
    if (!loc.location) continue
    const coords = geocodeAddress(loc.location)
    if (coords) {
      L.marker([coords.lat, coords.lng], { icon: markerIcon })
        .bindPopup(`<strong>${loc.projectName}</strong><br>${loc.location}`)
        .addTo(markerLayer)
      bounds.push([coords.lat, coords.lng])
    }
  }
  if (bounds.length > 0) {
    mapInstance.fitBounds(bounds, { padding: [30, 30], maxZoom: 10 })
  }
}

// ─── ECharts: 30-day trend ───
const trendOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#fff',
    borderColor: '#e2e8f0',
    textStyle: { color: '#1e293b', fontSize: 12 },
    axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(0,0,0,0.03)' } },
  },
  grid: { top: 20, right: 20, bottom: 30, left: 44 },
  xAxis: {
    type: 'category',
    data: Array.from({ length: 30 }, (_, i) => {
      const d = new Date(); d.setDate(d.getDate() - 29 + i)
      return `${d.getMonth() + 1}/${d.getDate()}`
    }),
    axisLabel: { fontSize: 11, color: '#94a3b8' },
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#e2e8f0' } },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLabel: { fontSize: 11, color: '#94a3b8' },
    splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
    axisLine: { show: false },
    axisTick: { show: false },
  },
  series: [{
    name: '检测数量',
    type: 'bar',
    data: stats.value.trend?.length ? stats.value.trend : Array.from({ length: 30 }, () => 0),
    itemStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: '#34d399' },
          { offset: 1, color: '#059669' },
        ],
      },
      borderRadius: [6, 6, 0, 0],
    },
    barWidth: '55%',
    emphasis: {
      itemStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#6ee7b7' },
            { offset: 1, color: '#10b981' },
          ],
        },
      },
    },
  }],
}))

// ─── ECharts: method distribution ───
const methodColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316', '#84cc16']

const methodChartOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} 条 ({d}%)',
    backgroundColor: '#fff',
    borderColor: '#e2e8f0',
    textStyle: { color: '#1e293b', fontSize: 12 },
  },
  legend: {
    orient: 'vertical',
    left: 12,
    top: 'center',
    textStyle: { fontSize: 12, color: '#64748b' },
    itemWidth: 8,
    itemHeight: 8,
    itemGap: 12,
  },
  series: [{
    type: 'pie',
    radius: ['55%', '78%'],
    center: ['58%', '50%'],
    avoidLabelOverlap: false,
    itemStyle: {
      borderRadius: 5,
      borderColor: '#fff',
      borderWidth: 3,
    },
    label: { show: false },
    emphasis: {
      scaleSize: 8,
      label: { show: true, fontSize: 14, fontWeight: 'bold' },
    },
    data: (stats.value.methodDistribution || []).map(m => ({ name: m.method, value: m.count })),
    color: methodColors,
  }],
}))

// ─── KPI cards ───
const kpiCards = computed(() => [
  {
    key: 'total',
    label: '检测总任务',
    value: (stats.value.totalInspections || 0).toLocaleString(),
    icon: 'Document',
    gradient: 'linear-gradient(135deg, #3b82f6, #2563eb)',
    bg: '#eff6ff',
  },
  {
    key: 'rate',
    label: '合格率',
    value: (stats.value.qualifiedRate || 100).toFixed(1) + '%',
    icon: 'CircleCheck',
    gradient: 'linear-gradient(135deg, #10b981, #059669)',
    bg: '#ecfdf5',
  },
  {
    key: 'pending',
    label: '待处理任务',
    value: (stats.value.pendingCount || 0).toLocaleString(),
    icon: 'Clock',
    gradient: 'linear-gradient(135deg, #f59e0b, #d97706)',
    bg: '#fffbeb',
  },
  {
    key: 'weekly',
    label: '本周新增',
    value: (stats.value.weeklyNew || 0).toLocaleString(),
    icon: 'TrendCharts',
    gradient: 'linear-gradient(135deg, #8b5cf6, #7c3aed)',
    bg: '#f5f3ff',
  },
  {
    key: 'projects',
    label: '活跃项目',
    value: (stats.value.activeProjects || 0).toLocaleString(),
    icon: 'FolderOpened',
    gradient: 'linear-gradient(135deg, #06b6d4, #0891b2)',
    bg: '#ecfeff',
  },
])

// ─── Quick actions ───
const quickActions = [
  { key: 'create', label: '创建立项', icon: 'Plus', visible: userStore.canCreateProject, path: '/project/create' },
  { key: 'entry', label: '数据录入', icon: 'EditPen', visible: true, path: '/project' },
  { key: 'approval', label: '签字审批', icon: 'Edit', visible: userStore.canSign, path: '/approval' },
  { key: 'report', label: '报告管理', icon: 'Document', visible: true, path: '/report' },
]

// ─── Workload heatmap ───
const workloadMax = computed(() => {
  const list = stats.value.weeklyWorkload || []
  if (!list.length) return 1
  return Math.max(1, ...list.map(r => r.total || 0))
})

function heatClass(count, max) {
  if (!count || !max) return 'heat-0'
  const ratio = count / max
  if (ratio >= 0.8) return 'heat-4'
  if (ratio >= 0.5) return 'heat-3'
  if (ratio >= 0.2) return 'heat-2'
  if (ratio > 0) return 'heat-1'
  return 'heat-0'
}

// ─── Data loading ───
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
    <!-- ── Welcome row ── -->
    <div class="welcome-row">
      <div class="welcome-info">
        <h2 class="welcome-greeting">{{ greeting }}，{{ userStore.realName || userStore.username }}</h2>
        <p class="welcome-date">{{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }}</p>
      </div>
      <div class="header-actions">
        <template v-for="action in quickActions" :key="action.key">
          <el-button
            v-if="action.visible"
            size="default"
            class="quick-btn"
            @click="router.push(action.path)"
          >
            <el-icon><component :is="action.icon" /></el-icon>
            {{ action.label }}
          </el-button>
        </template>
        <el-button size="default" class="quick-btn quick-btn--refresh" @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- ── KPI cards ── -->
    <div class="kpi-grid">
      <div
        v-for="card in kpiCards"
        :key="card.key"
        class="kpi-card"
        :style="{ '--kpi-gradient': card.gradient, '--kpi-bg': card.bg }"
      >
        <div class="kpi-icon-box">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="kpi-body">
          <span class="kpi-value">{{ card.value }}</span>
          <span class="kpi-label">{{ card.label }}</span>
        </div>
        <div class="kpi-shimmer"></div>
      </div>
    </div>

    <!-- ── Main charts row ── -->
    <div class="charts-row">
      <!-- Trend chart -->
      <div class="panel panel--chart panel--span-7">
        <div class="panel-header">
          <div class="panel-title-group">
            <el-icon :size="16" class="panel-title-icon"><TrendCharts /></el-icon>
            <span class="panel-title">近30天检测趋势</span>
          </div>
          <span class="panel-badge">{{ stats.trend?.reduce((a, b) => a + b, 0) || 0 }} 条</span>
        </div>
        <div class="panel-body">
          <div v-if="stats.trend?.length" class="chart-body">
            <v-chart :option="trendOption" autoresize />
          </div>
          <div v-else class="chart-empty">
            <el-icon :size="40" color="#cbd5e1"><TrendCharts /></el-icon>
            <p>暂无检测趋势数据</p>
          </div>
        </div>
      </div>

      <!-- Method distribution -->
      <div class="panel panel--chart panel--span-5">
        <div class="panel-header">
          <div class="panel-title-group">
            <el-icon :size="16" class="panel-title-icon"><PieChart /></el-icon>
            <span class="panel-title">检测方法分布</span>
          </div>
        </div>
        <div class="panel-body">
          <div v-if="stats.methodDistribution?.length" class="chart-body">
            <v-chart :option="methodChartOption" autoresize />
          </div>
          <div v-else class="chart-empty">
            <el-icon :size="40" color="#cbd5e1"><PieChart /></el-icon>
            <p>暂无方法分布数据</p>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Bottom row: Workload + Map ── -->
    <div class="bottom-row">
      <!-- Weekly workload -->
      <div class="panel panel--table panel--span-7">
        <div class="panel-header">
          <div class="panel-title-group">
            <el-icon :size="16" class="panel-title-icon"><Grid /></el-icon>
            <span class="panel-title">本周工作量统计</span>
          </div>
          <span class="panel-subtitle">按事业部 × 检测方法</span>
        </div>
        <div class="panel-body panel-body--flush">
          <div class="workload-table-wrap">
            <table class="workload-table" v-if="(stats.weeklyWorkload || []).length">
              <thead>
                <tr>
                  <th class="col-dept">事业部 / 公司级项目</th>
                  <th>RT</th><th>UT</th><th>PT</th><th>MT</th>
                  <th>AUT</th><th>PA</th><th>TOFD</th><th>DR</th>
                  <th class="col-total">合计</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in stats.weeklyWorkload" :key="row.deptName">
                  <td class="col-dept">{{ row.deptName }}</td>
                  <td :class="heatClass(row.rt, workloadMax)">{{ row.rt || 0 }}</td>
                  <td :class="heatClass(row.ut, workloadMax)">{{ row.ut || 0 }}</td>
                  <td :class="heatClass(row.pt, workloadMax)">{{ row.pt || 0 }}</td>
                  <td :class="heatClass(row.mt, workloadMax)">{{ row.mt || 0 }}</td>
                  <td :class="heatClass(row.aut, workloadMax)">{{ row.aut || 0 }}</td>
                  <td :class="heatClass(row.pa, workloadMax)">{{ row.pa || 0 }}</td>
                  <td :class="heatClass(row.tofd, workloadMax)">{{ row.tofd || 0 }}</td>
                  <td :class="heatClass(row.dr, workloadMax)">{{ row.dr || 0 }}</td>
                  <td class="col-total">
                    <span class="total-badge" :class="row.total > 20 ? 'total--high' : row.total > 10 ? 'total--mid' : 'total--low'">
                      {{ row.total || 0 }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-else class="table-empty">
              <el-icon :size="36" color="#cbd5e1"><Grid /></el-icon>
              <p>暂无本周工作量数据</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Map -->
      <div class="panel panel--map panel--span-5">
        <div class="panel-header">
          <div class="panel-title-group">
            <el-icon :size="16" class="panel-title-icon"><Location /></el-icon>
            <span class="panel-title">项目分布地图</span>
          </div>
          <span class="panel-badge panel-badge--muted">{{ stats.projectLocations?.length || 0 }} 个地点</span>
        </div>
        <div class="panel-body panel-body--map">
          <div class="map-wrap">
            <div ref="mapContainer" class="map-container"></div>
            <div v-if="!stats.projectLocations?.length && !loading" class="map-empty-overlay">
              <el-icon :size="36" color="#cbd5e1"><Location /></el-icon>
              <p>暂无项目位置数据</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Dashboard container ── */
.dashboard {
  animation: fadeSlideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ── Welcome row ── */
.welcome-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-5);
  gap: var(--space-4);
}

.welcome-greeting {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 2px;
  letter-spacing: -0.3px;
}

.welcome-date {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
  align-items: center;
}

.quick-btn {
  border-radius: var(--radius-md);
  font-weight: 500;
  transition: all var(--transition-fast);
}

.quick-btn:hover {
  transform: translateY(-1px);
}

.quick-btn--refresh {
  color: var(--color-text-secondary);
}

/* ── KPI grid ── */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.kpi-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
  cursor: default;
  overflow: hidden;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: transparent;
}

.kpi-card:hover .kpi-shimmer {
  opacity: 1;
}

.kpi-shimmer {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  transition: opacity var(--transition-fast);
  opacity: 0;
  pointer-events: none;
}

.kpi-icon-box {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--kpi-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
  transition: transform var(--transition-fast);
}

.kpi-card:hover .kpi-icon-box {
  transform: scale(1.06);
}

.kpi-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.kpi-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.15;
  font-family: var(--font-mono);
  letter-spacing: -0.5px;
}

.kpi-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 2px;
  font-weight: 500;
}

/* ── Panel (unified card) ── */
.panel {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: box-shadow var(--transition-fast);
}

.panel:hover {
  box-shadow: var(--shadow-sm);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-5);
  background: var(--color-bg-hover);
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.panel-title-group {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.panel-title-icon {
  color: var(--color-primary);
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.panel-subtitle {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.panel-badge {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  background: rgba(51, 65, 85, 0.06);
  padding: 2px 10px;
  border-radius: 999px;
}

.panel-badge--muted {
  color: var(--color-text-secondary);
}

.panel-body {
  flex: 1 1 auto;
  padding: var(--space-4);
}

.panel-body--flush {
  padding: 0;
}

.panel-body--map {
  padding: 0;
}

/* ── Charts row ── */
.charts-row {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.panel--span-7 { grid-column: span 7; }
.panel--span-5 { grid-column: span 5; }

.panel--chart .panel-body {
  flex: none;
  height: 280px;
}

.chart-body {
  height: 100%;
  width: 100%;
}

.chart-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  color: var(--color-text-placeholder);
  font-size: 13px;
}

.chart-empty p { margin: 0; }

/* ── Bottom row ── */
.bottom-row {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: var(--space-4);
}

.panel--table .panel-body {
  flex: none;
  height: 340px;
  overflow: hidden;
}

.panel--map .panel-body {
  flex: none;
  height: 340px;
}

/* ── Workload table ── */
.workload-table-wrap {
  height: 100%;
  overflow-y: auto;
}

.workload-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.workload-table thead {
  position: sticky;
  top: 0;
  z-index: 1;
}

.workload-table th {
  background: var(--color-bg-hover);
  color: var(--color-text-secondary);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  padding: 10px 8px;
  text-align: center;
  border-bottom: 1px solid var(--color-border);
}

.workload-table th.col-dept {
  text-align: left;
  padding-left: var(--space-5);
  width: 180px;
}

.workload-table th.col-total {
  width: 70px;
}

.workload-table td {
  padding: 9px 8px;
  text-align: center;
  border-bottom: 1px solid var(--color-border-light);
  font-weight: 500;
  color: var(--color-text-regular);
  transition: background var(--transition-fast);
}

.workload-table td.col-dept {
  text-align: left;
  padding-left: var(--space-5);
  font-weight: 500;
  color: var(--color-text-primary);
}

.workload-table td.col-total {
  font-weight: 600;
}

.workload-table tbody tr {
  transition: background var(--transition-fast);
}

.workload-table tbody tr:hover {
  background: var(--color-bg-hover);
}

/* Heatmap */
.heat-0 { color: var(--color-text-placeholder) !important; }
.heat-1 { color: var(--color-text-secondary); }
.heat-2 { color: var(--color-primary); }
.heat-3 { background: rgba(5, 150, 105, 0.06); color: var(--color-accent); font-weight: 600; }
.heat-4 { background: rgba(5, 150, 105, 0.12); color: var(--color-accent-dark); font-weight: 700; }

.total-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.total--low {
  background: rgba(59, 130, 246, 0.08);
  color: #3b82f6;
}

.total--mid {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}

.total--high {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.table-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  color: var(--color-text-placeholder);
  font-size: 13px;
}

.table-empty p { margin: 0; }

/* ── Map ── */
.map-wrap {
  position: relative;
  height: 100%;
  width: 100%;
}

.map-container {
  height: 100%;
  width: 100%;
}

.map-empty-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  background: #fafbfc;
  z-index: 1000;
  color: var(--color-text-placeholder);
  font-size: 13px;
}

.map-empty-overlay p { margin: 0; }

/* ── Responsive ── */
@media (max-width: 1400px) {
  .kpi-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .charts-row,
  .bottom-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .panel--span-7,
  .panel--span-5 {
    grid-column: span 1;
  }
}

@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .welcome-row {
    flex-direction: column;
  }

  .charts-row,
  .bottom-row {
    grid-template-columns: 1fr;
  }

  .panel--span-7,
  .panel--span-5 {
    grid-column: span 1;
  }
}
</style>

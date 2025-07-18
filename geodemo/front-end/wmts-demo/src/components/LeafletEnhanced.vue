<template>
  <div class="leaflet-enhanced">
    <h3>🚀 Leaflet 增强版 WMTS</h3>
    <p class="description">
      这个版本提供了更好的 WMTS 错误处理、多投影支持和调试功能
    </p>
    
    <div class="enhanced-controls">
      <div class="control-section">
        <h4>📋 预设配置</h4>
        <div class="preset-buttons">
          <button @click="loadPreset('china-buildings')" class="preset-btn">
            🏢 中国建筑
          </button>
          <button @click="loadPreset('beijing-detail')" class="preset-btn">
            🏛️ 北京详图
          </button>
          <button @click="loadPreset('osm-overlay')" class="preset-btn">
            🌍 OSM 叠加
          </button>
        </div>
      </div>
      
      <div class="control-section">
        <h4>🔧 高级设置</h4>
        <div class="advanced-controls">
          <label>
            服务器地址:
            <input v-model="serverUrl" class="control-input" />
          </label>
          
          <label>
            图层名称:
            <input v-model="layerName" class="control-input" />
          </label>
          
          <label>
            瓦片格式:
            <select v-model="format" class="control-select">
              <option value="image/png">PNG (推荐)</option>
              <option value="image/jpeg">JPEG</option>
              <option value="image/webp">WebP</option>
            </select>
          </label>
          
          <label>
            投影坐标系:
            <select v-model="projection" class="control-select">
              <option value="EPSG:4326">EPSG:4326 (WGS84)</option>
              <option value="EPSG:3857">EPSG:3857 (Web Mercator)</option>
            </select>
          </label>
        </div>
      </div>
    </div>
    
    <div class="map-actions">
      <button @click="createMap" class="action-btn primary">
        🗺️ 创建地图
      </button>
      <button @click="addWMTSLayer" class="action-btn success" :disabled="!leafletMap">
        ➕ 添加 WMTS
      </button>
      <button @click="toggleDebugMode" class="action-btn info" :disabled="!leafletMap">
        {{ debugMode ? '🔍 关闭调试' : '🔍 开启调试' }}
      </button>
      <button @click="exportConfig" class="action-btn warning" :disabled="!leafletMap">
        📤 导出配置
      </button>
    </div>
    
    <!-- 地图容器 -->
    <div ref="mapElement" class="enhanced-map" :class="{ 'debug-mode': debugMode }"></div>
    
    <!-- 调试信息面板 -->
    <div v-if="debugMode" class="debug-panel">
      <h4>🔍 调试信息</h4>
      <div class="debug-content">
        <div class="debug-section">
          <h5>地图状态</h5>
          <ul>
            <li>中心坐标: {{ mapCenter }}</li>
            <li>缩放级别: {{ mapZoom }}</li>
            <li>投影: {{ projection }}</li>
            <li>图层数量: {{ layerCount }}</li>
          </ul>
        </div>
        
        <div class="debug-section">
          <h5>最后请求的瓦片</h5>
          <div v-if="lastTileInfo" class="tile-info">
            <p><strong>URL:</strong> <a :href="lastTileInfo.url" target="_blank">{{ lastTileInfo.url }}</a></p>
            <p><strong>坐标:</strong> Z{{ lastTileInfo.z }}/X{{ lastTileInfo.x }}/Y{{ lastTileInfo.y }}</p>
            <p><strong>状态:</strong> 
              <span :class="lastTileInfo.success ? 'success' : 'error'">
                {{ lastTileInfo.success ? '✅ 成功' : '❌ 失败' }}
              </span>
            </p>
          </div>
        </div>
        
        <div class="debug-section">
          <h5>错误日志</h5>
          <div class="error-log">
            <div v-for="(error, index) in errorLog" :key="index" class="error-entry">
              <span class="error-time">{{ error.time }}</span>
              <span class="error-message">{{ error.message }}</span>
            </div>
            <div v-if="errorLog.length === 0" class="no-errors">暂无错误</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 配置导出模态框 -->
    <div v-if="showExportModal" class="modal-overlay" @click="showExportModal = false">
      <div class="modal-content" @click.stop>
        <h4>📤 导出配置</h4>
        <textarea v-model="exportedConfig" readonly class="export-textarea"></textarea>
        <div class="modal-actions">
          <button @click="copyConfig" class="action-btn success">📋 复制</button>
          <button @click="showExportModal = false" class="action-btn">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'

// 响应式数据
const mapElement = ref(null)
const leafletMap = ref(null)
const debugMode = ref(false)
const showExportModal = ref(false)

// 配置参数
const serverUrl = ref('http://localhost:8080/geoserver/gwc/service/wmts')
const layerName = ref('chinademo1:buildings')
const format = ref('image/png')
const projection = ref('EPSG:4326')

// 地图状态
const mapCenter = ref('')
const mapZoom = ref(0)
const layerCount = ref(0)

// 调试信息
const lastTileInfo = ref(null)
const errorLog = ref([])
const exportedConfig = ref('')

// 图层存储
let currentLayers = []

// 创建地图
const createMap = () => {
  try {
    // 清理现有地图
    if (leafletMap.value) {
      leafletMap.value.remove()
    }
    
    // 根据投影创建不同的地图配置
    const mapOptions = {
      center: projection.value === 'EPSG:4326' ? [35, 105] : [39.9, 116.4],
      zoom: 6,
      zoomControl: true,
      attributionControl: true
    }
    
    if (projection.value === 'EPSG:4326') {
      mapOptions.crs = L.CRS.EPSG4326
    }
    
    leafletMap.value = L.map(mapElement.value, mapOptions)
    
    // 添加事件监听
    leafletMap.value.on('moveend zoomend', updateMapStatus)
    
    // 添加控件
    addMapControls()
    
    updateMapStatus()
    addToLog('地图创建成功')
    
  } catch (error) {
    addToLog(`地图创建失败: ${error.message}`)
  }
}

// 添加 WMTS 图层
const addWMTSLayer = () => {
  try {
    // 构建 WMTS URL
    const wmtsUrl = buildWMTSUrl()
    
    // 创建图层
    const wmtsLayer = L.tileLayer(wmtsUrl, {
      attribution: `WMTS: ${layerName.value}`,
      opacity: 0.8,
      maxZoom: 18,
      errorTileUrl: createErrorTile()
    })
    
    // 添加事件监听
    wmtsLayer.on('loading', () => {
      addToLog('WMTS 图层开始加载')
    })
    
    wmtsLayer.on('load', () => {
      addToLog('WMTS 图层加载完成')
    })
    
    wmtsLayer.on('tileerror', (e) => {
      const tileInfo = extractTileInfo(e.coords)
      lastTileInfo.value = {
        url: e.tile.src,
        x: e.coords.x,
        y: e.coords.y,
        z: e.coords.z,
        success: false
      }
      addToLog(`瓦片加载失败: Z${e.coords.z}/X${e.coords.x}/Y${e.coords.y}`)
    })
    
    wmtsLayer.on('tileload', (e) => {
      const tileInfo = extractTileInfo(e.coords)
      lastTileInfo.value = {
        url: e.tile.src,
        x: e.coords.x,
        y: e.coords.y,
        z: e.coords.z,
        success: true
      }
    })
    
    // 添加到地图
    wmtsLayer.addTo(leafletMap.value)
    currentLayers.push(wmtsLayer)
    
    updateLayerCount()
    addToLog(`WMTS 图层已添加: ${layerName.value}`)
    
  } catch (error) {
    addToLog(`WMTS 图层添加失败: ${error.message}`)
  }
}

// 构建 WMTS URL
const buildWMTSUrl = () => {
  const params = new URLSearchParams({
    SERVICE: 'WMTS',
    REQUEST: 'GetTile',
    VERSION: '1.0.0',
    LAYER: layerName.value,
    STYLE: '',
    TILEMATRIXSET: projection.value,
    FORMAT: format.value,
    TILEMATRIX: `${projection.value}:{z}`,
    TILEROW: '{y}',
    TILECOL: '{x}'
  })
  
  return `${serverUrl.value}?${params.toString()}`
}

// 创建错误瓦片
const createErrorTile = () => {
  const canvas = document.createElement('canvas')
  canvas.width = 256
  canvas.height = 256
  const ctx = canvas.getContext('2d')
  
  ctx.fillStyle = '#f8f9fa'
  ctx.fillRect(0, 0, 256, 256)
  
  ctx.strokeStyle = '#dee2e6'
  ctx.strokeRect(0, 0, 256, 256)
  
  ctx.fillStyle = '#6c757d'
  ctx.font = '14px Arial'
  ctx.textAlign = 'center'
  ctx.fillText('加载失败', 128, 120)
  ctx.fillText('Load Failed', 128, 140)
  
  return canvas.toDataURL()
}

// 添加地图控件
const addMapControls = () => {
  // 坐标显示控件
  const coordControl = L.control({ position: 'bottomleft' })
  coordControl.onAdd = function() {
    const div = L.DomUtil.create('div', 'coord-display')
    div.style.cssText = `
      background: rgba(255,255,255,0.9);
      padding: 5px 10px;
      border-radius: 4px;
      font-family: monospace;
      font-size: 12px;
      box-shadow: 0 2px 5px rgba(0,0,0,0.2);
    `
    div.innerHTML = '移动鼠标查看坐标'
    return div
  }
  coordControl.addTo(leafletMap.value)
  
  // 鼠标坐标更新
  leafletMap.value.on('mousemove', (e) => {
    const coordDiv = leafletMap.value.getContainer().querySelector('.coord-display')
    if (coordDiv) {
      coordDiv.innerHTML = `${e.latlng.lng.toFixed(6)}, ${e.latlng.lat.toFixed(6)}`
    }
  })
}

// 预设配置
const loadPreset = (presetName) => {
  const presets = {
    'china-buildings': {
      layerName: 'chinademo1:buildings',
      format: 'image/png',
      projection: 'EPSG:4326',
      center: [105, 35],
      zoom: 6
    },
    'beijing-detail': {
      layerName: 'chinademo1:buildings',
      format: 'image/png',
      projection: 'EPSG:4326',
      center: [116.4, 39.9],
      zoom: 10
    },
    'osm-overlay': {
      layerName: 'topp:states',
      format: 'image/png',
      projection: 'EPSG:4326',
      center: [-100, 40],
      zoom: 4
    }
  }
  
  const preset = presets[presetName]
  if (preset) {
    layerName.value = preset.layerName
    format.value = preset.format
    projection.value = preset.projection
    
    if (leafletMap.value) {
      leafletMap.value.setView([preset.center[1], preset.center[0]], preset.zoom)
    }
    
    addToLog(`已加载预设: ${presetName}`)
  }
}

// 更新地图状态
const updateMapStatus = () => {
  if (leafletMap.value) {
    const center = leafletMap.value.getCenter()
    mapCenter.value = `${center.lng.toFixed(4)}, ${center.lat.toFixed(4)}`
    mapZoom.value = leafletMap.value.getZoom()
  }
}

// 更新图层数量
const updateLayerCount = () => {
  layerCount.value = currentLayers.length
}

// 切换调试模式
const toggleDebugMode = () => {
  debugMode.value = !debugMode.value
  addToLog(debugMode.value ? '调试模式已开启' : '调试模式已关闭')
}

// 导出配置
const exportConfig = () => {
  const config = {
    serverUrl: serverUrl.value,
    layerName: layerName.value,
    format: format.value,
    projection: projection.value,
    currentView: {
      center: mapCenter.value,
      zoom: mapZoom.value
    },
    wmtsUrl: buildWMTSUrl(),
    timestamp: new Date().toISOString()
  }
  
  exportedConfig.value = JSON.stringify(config, null, 2)
  showExportModal.value = true
}

// 复制配置
const copyConfig = async () => {
  try {
    await navigator.clipboard.writeText(exportedConfig.value)
    addToLog('配置已复制到剪贴板')
  } catch (error) {
    addToLog('复制失败，请手动复制')
  }
}

// 添加日志
const addToLog = (message) => {
  errorLog.value.unshift({
    time: new Date().toLocaleTimeString(),
    message
  })
  
  // 只保留最近 20 条日志
  if (errorLog.value.length > 20) {
    errorLog.value = errorLog.value.slice(0, 20)
  }
}

// 提取瓦片信息
const extractTileInfo = (coords) => {
  return {
    x: coords.x,
    y: coords.y,
    z: coords.z
  }
}

// 组件挂载
onMounted(() => {
  setTimeout(createMap, 100)
})

// 组件卸载
onUnmounted(() => {
  if (leafletMap.value) {
    leafletMap.value.remove()
  }
})
</script>

<style scoped>
.leaflet-enhanced {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  border-radius: 10px;
  color: white;
}

.description {
  text-align: center;
  margin-bottom: 20px;
  opacity: 0.9;
}

.enhanced-controls {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.control-section {
  background: rgba(255,255,255,0.1);
  padding: 15px;
  border-radius: 8px;
  backdrop-filter: blur(10px);
}

.control-section h4 {
  margin: 0 0 15px 0;
  color: #fff;
}

.preset-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preset-btn {
  padding: 8px 12px;
  background: rgba(255,255,255,0.2);
  color: white;
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.preset-btn:hover {
  background: rgba(255,255,255,0.3);
}

.advanced-controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.advanced-controls label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
}

.control-input, .control-select {
  padding: 6px 8px;
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 4px;
  background: rgba(255,255,255,0.1);
  color: white;
  backdrop-filter: blur(5px);
}

.control-input::placeholder {
  color: rgba(255,255,255,0.6);
}

.map-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.action-btn {
  padding: 10px 15px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.primary { background: #007bff; color: white; }
.action-btn.success { background: #28a745; color: white; }
.action-btn.info { background: #17a2b8; color: white; }
.action-btn.warning { background: #ffc107; color: #212529; }

.action-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.2);
}

.enhanced-map {
  width: 100%;
  height: 500px;
  border-radius: 8px;
  box-shadow: 0 8px 25px rgba(0,0,0,0.3);
  margin-bottom: 20px;
  transition: all 0.3s;
}

.enhanced-map.debug-mode {
  border: 3px solid #ffc107;
  box-shadow: 0 0 20px rgba(255, 193, 7, 0.5);
}

.debug-panel {
  background: rgba(255,255,255,0.1);
  padding: 20px;
  border-radius: 8px;
  backdrop-filter: blur(10px);
}

.debug-panel h4 {
  margin: 0 0 15px 0;
}

.debug-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
}

.debug-section {
  background: rgba(255,255,255,0.1);
  padding: 12px;
  border-radius: 6px;
}

.debug-section h5 {
  margin: 0 0 8px 0;
  color: #ffc107;
}

.debug-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.debug-section li {
  padding: 2px 0;
  font-size: 12px;
}

.tile-info p {
  margin: 4px 0;
  font-size: 12px;
}

.tile-info a {
  color: #90EE90;
  word-break: break-all;
}

.success { color: #90EE90; }
.error { color: #ff6b6b; }

.error-log {
  max-height: 150px;
  overflow-y: auto;
}

.error-entry {
  display: flex;
  gap: 10px;
  padding: 4px 0;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  font-size: 11px;
}

.error-time {
  color: #ffc107;
  min-width: 60px;
}

.error-message {
  flex: 1;
}

.no-errors {
  color: rgba(255,255,255,0.6);
  font-style: italic;
  font-size: 12px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  max-width: 600px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content h4 {
  margin: 0 0 15px 0;
  color: #2c3e50;
}

.export-textarea {
  width: 100%;
  height: 300px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  resize: vertical;
}

.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .enhanced-controls {
    grid-template-columns: 1fr;
  }
  
  .map-actions {
    flex-direction: column;
  }
  
  .debug-content {
    grid-template-columns: 1fr;
  }
}
</style>

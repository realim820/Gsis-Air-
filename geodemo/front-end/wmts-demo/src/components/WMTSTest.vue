<template>
  <div>
    <h3>🗺️ WMTS 地图展示</h3>
    <div ref="mapContainer" class="wmts-map"></div>
    
    <div class="status">
      <p>状态: {{ status }}</p>
      <div class="map-info">
        <p><strong>当前坐标:</strong> {{ currentCoords }}</p>
        <p><strong>缩放级别:</strong> {{ currentZoom }}</p>
        <p><strong>投影坐标系:</strong> {{ currentProjection }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import WMTS from 'ol/source/WMTS'
import TileGrid from 'ol/tilegrid/WMTS'
import OSM from 'ol/source/OSM'
import { get as getProjection } from 'ol/proj'
import { getTopLeft, getWidth } from 'ol/extent'
import { ScaleLine, MousePosition, defaults as defaultControls } from 'ol/control'
import { createStringXY } from 'ol/coordinate'

const mapContainer = ref(null)
const status = ref('初始化中...')
const currentCoords = ref('0.0000, 0.0000')
const currentZoom = ref(4)
const currentProjection = ref('EPSG:4326')

let map = null
let wmtsLayer = null

const createGeoServerWMTS = (layerName = 'ne:countries', format = 'image/png', matrixSet = 'EPSG:4326') => {
  console.log(`创建 WMTS 源: ${layerName}, 格式: ${format}, 矩阵集: ${matrixSet}`)
  
  const projection = getProjection('EPSG:4326')
  const extent = projection.getExtent()
  const size = getWidth(extent) / 256
  
  const resolutions = []
  const matrixIds = []
  
  // 从第4级开始，适合中国区域数据
  const startZoom = 4
  const endZoom = 18
  
  for (let z = startZoom; z <= endZoom; ++z) {
    resolutions.push(size / Math.pow(2, z))
    matrixIds.push(`EPSG:4326:${z}`)
  }
  
  const tileGrid = new TileGrid({
    origin: getTopLeft(extent),
    resolutions,
    matrixIds,
  })
  
  return new WMTS({
    url: 'http://localhost:8080/geoserver/gwc/service/wmts',
    layer: layerName,
    matrixSet: matrixSet,
    format: format,
    projection: projection,
    requestEncoding: 'KVP',
    tileGrid: tileGrid,
    crossOrigin: 'anonymous',
  })
}

// 更新地图信息
const updateMapInfo = () => {
  if (map) {
    const view = map.getView()
    const center = view.getCenter()
    const zoom = view.getZoom()
    
    if (center) {
      currentCoords.value = `${center[0].toFixed(4)}, ${center[1].toFixed(4)}`
    }
    currentZoom.value = zoom ? Math.round(zoom) : 0
    currentProjection.value = view.getProjection().getCode()
  }
}

onMounted(() => {
  console.log('WMTS 地图组件已挂载')
  
  // 创建 OSM 底图图层
  const osmLayer = new TileLayer({
    source: new OSM(),
    opacity: 0.6,
  })
  
  // 创建 WMTS 图层
  wmtsLayer = new TileLayer({
    source: createGeoServerWMTS(),
    opacity: 0.8,
  })
  
  // 监听 WMTS 加载事件
  wmtsLayer.getSource().on('tileloadstart', () => {
    status.value = 'WMTS 瓦片开始加载...'
  })
  
  wmtsLayer.getSource().on('tileloadend', () => {
    status.value = 'WMTS 瓦片加载完成'
  })
  
  wmtsLayer.getSource().on('tileloaderror', (event) => {
    status.value = 'WMTS 瓦片加载失败'
    console.error('WMTS 加载错误:', event)
  })

  // 创建地图
  map = new Map({
    target: mapContainer.value,
    layers: [osmLayer, wmtsLayer],
    view: new View({
      projection: 'EPSG:4326',
      center: [105, 35], // 中国中心
      zoom: 6,
      minZoom: 2,
      maxZoom: 18,
    }),
    controls: defaultControls().extend([
      // 添加比例尺
      new ScaleLine({
        units: 'metric',
        className: 'ol-scale-line',
        target: undefined,
      }),
      // 添加鼠标位置坐标显示
      new MousePosition({
        coordinateFormat: createStringXY(4),
        projection: 'EPSG:4326',
        className: 'ol-mouse-position',
        target: undefined,
        undefinedHTML: '&nbsp;',
      }),
    ]),
  })

  // 监听地图视图变化
  map.getView().on('change', updateMapInfo)
  
  // 初始化信息
  updateMapInfo()
  
  status.value = '地图初始化完成'
})
</script>

<style scoped>
.wmts-map {
  width: 100%;
  height: 600px;
  border: 2px solid #007bff;
  border-radius: 8px;
  background-color: #e9ecef;
  margin-bottom: 20px;
  position: relative;
}

.status {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #dee2e6;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.status p {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: 500;
  color: #495057;
}

.map-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  margin-top: 10px;
}

.map-info p {
  margin: 0;
  padding: 10px;
  background: white;
  border-radius: 4px;
  border-left: 4px solid #007bff;
  font-size: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

/* OpenLayers 控件样式优化 */
:deep(.ol-scale-line) {
  bottom: 8px;
  left: 8px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
  padding: 2px 8px;
  border: 1px solid rgba(0, 0, 0, 0.2);
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  font-size: 12px;
}

:deep(.ol-mouse-position) {
  top: 8px;
  right: 8px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
  padding: 8px 12px;
  border: 1px solid rgba(0, 0, 0, 0.2);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  color: #495057;
  min-width: 160px;
  text-align: center;
}

:deep(.ol-attribution) {
  bottom: 4px;
  right: 4px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 4px;
  font-size: 10px;
}

:deep(.ol-zoom) {
  top: 8px;
  left: 8px;
}

:deep(.ol-zoom .ol-zoom-in),
:deep(.ol-zoom .ol-zoom-out) {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.2);
  border-radius: 4px;
  margin: 2px;
}

:deep(.ol-zoom .ol-zoom-in:hover),
:deep(.ol-zoom .ol-zoom-out:hover) {
  background: #007bff;
  color: white;
}
</style>

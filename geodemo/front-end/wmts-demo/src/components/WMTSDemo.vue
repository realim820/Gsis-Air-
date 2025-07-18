<template>
  <div>
    <h2>GeoServer WMTS 测试</h2>
    <div ref="mapRef" class="map-container"></div>

    <div class="controls">
      <div class="control-group">
        <label>选择图层：</label>
        <select v-model="layerName" @change="onLayerChange">
          <option value="">-- 请选择图层 --</option>
          <option v-for="layer in availableLayers" :key="layer.name" :value="layer.name">
            {{ layer.name }} - {{ layer.title }}
          </option>
        </select>
        <button @click="getCapabilities">🔄 刷新图层列表</button>
      </div>
      
      <div class="control-group">
        <label>或手动输入：</label>
        <input v-model="layerName" placeholder="输入图层名，如 ne:countries" />
        <button @click="loadLayer">加载图层</button>
      </div>
      
      <div class="status-info">
        <p><strong>状态:</strong> {{ status }}</p>
        <p v-if="layerInfo"><strong>当前图层:</strong> {{ layerInfo }}</p>
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
import OSM from 'ol/source/OSM'
import { get as getProjection } from 'ol/proj'
import WMTSCapabilities from 'ol/format/WMTSCapabilities'
import { optionsFromCapabilities } from 'ol/source/WMTS'
import { ScaleLine, MousePosition, defaults as defaultControls } from 'ol/control'
import { createStringXY } from 'ol/coordinate'

const mapRef = ref(null)
const layerName = ref('') 
const status = ref('初始化中...')
const availableLayers = ref([])
const layerInfo = ref('')
let map = null
let wmtsLayer = null
let osmLayer = null

// 获取可用图层列表
const getCapabilities = async () => {
  status.value = '获取图层列表中...'
  try {
    const response = await fetch('http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=GetCapabilities')
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }
    
    const text = await response.text()
    const parser = new WMTSCapabilities()
    const capabilities = parser.read(text)
    
    // 解析图层信息
    const layers = []
    if (capabilities.Contents && capabilities.Contents.Layer) {
      capabilities.Contents.Layer.forEach(layer => {
        layers.push({
          name: layer.Identifier,
          title: layer.Title || layer.Identifier,
          formats: layer.Format || [],
          tileMatrixSets: layer.TileMatrixSetLink ? 
            layer.TileMatrixSetLink.map(link => link.TileMatrixSet) : []
        })
      })
    }
    
    availableLayers.value = layers
    status.value = `发现 ${layers.length} 个可用图层`
    
    // 如果没有选中图层且有可用图层，选择第一个
    if (!layerName.value && layers.length > 0) {
      layerName.value = layers[0].name
    }
    
  } catch (error) {
    status.value = `获取图层列表失败: ${error.message}`
    console.error('获取 capabilities 错误:', error)
  }
}

// 图层选择变化处理
const onLayerChange = () => {
  if (layerName.value) {
    loadLayer()
  }
}

const loadLayer = async () => {
  if (!layerName.value) {
    status.value = '请输入图层名'
    return
  }

  status.value = `加载图层 ${layerName.value} 中...`

  try {
    const response = await fetch('http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=GetCapabilities')
    const text = await response.text()

    const parser = new WMTSCapabilities()
    const capabilities = parser.read(text)

    const options = optionsFromCapabilities(capabilities, {
      layer: layerName.value,
      matrixSet: 'EPSG:4326', // 优先使用 EPSG:4326
    })

    if (!options) {
      // 如果 EPSG:4326 不可用，尝试 EPSG:3857
      const options3857 = optionsFromCapabilities(capabilities, {
        layer: layerName.value,
        matrixSet: 'EPSG:3857',
      })
      
      if (!options3857) {
        throw new Error('图层不支持 EPSG:4326 或 EPSG:3857 坐标系')
      }
      
      const source = new WMTS({
        ...options3857,
        crossOrigin: 'anonymous',
      })

      wmtsLayer.setSource(source)
      
      // 切换到 EPSG:3857 投影
      map.setView(new View({
        projection: 'EPSG:3857',
        center: [11700000, 4200000], // 中国中心 (Web Mercator)
        zoom: 5,
      }))
      
      layerInfo.value = `${layerName.value} (EPSG:3857)`
      
    } else {
      const source = new WMTS({
        ...options,
        crossOrigin: 'anonymous',
      })

      wmtsLayer.setSource(source)

      // 使用 EPSG:4326 投影
      map.setView(new View({
        projection: 'EPSG:4326',
        center: [105, 35], // 中国中心
        zoom: 5,
      }))
      
      layerInfo.value = `${layerName.value} (EPSG:4326)`
    }

    status.value = `图层 ${layerName.value} 加载成功`

  } catch (e) {
    status.value = `图层加载失败: ${e.message}`
    console.error(e)
  }
}

onMounted(() => {
  // 创建 OSM 底图
  osmLayer = new TileLayer({
    source: new OSM(),
    opacity: 0.6
  })
  
  // 创建 WMTS 图层
  wmtsLayer = new TileLayer({
    opacity: 0.8
  })

  map = new Map({
    target: mapRef.value,
    layers: [osmLayer, wmtsLayer],
    view: new View({
      projection: 'EPSG:4326',
      center: [105, 35],
      zoom: 5,
    }),
    controls: defaultControls().extend([
      // 比例尺
      new ScaleLine({
        units: 'metric',
        className: 'ol-scale-line',
      }),
      // 坐标显示
      new MousePosition({
        coordinateFormat: createStringXY(4),
        projection: 'EPSG:4326',
        className: 'ol-mouse-position',
        undefinedHTML: '&nbsp;',
      }),
    ]),
  })

  status.value = '地图初始化完成'
  
  // 自动获取图层列表
  getCapabilities()
})
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 600px;
  border: 2px solid #007bff;
  border-radius: 8px;
  margin-bottom: 20px;
  position: relative;
}

.controls {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #dee2e6;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.control-group {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.control-group label {
  font-weight: 500;
  color: #495057;
  min-width: 80px;
}

.control-group select {
  min-width: 300px;
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  background: white;
  font-size: 14px;
}

.control-group input {
  min-width: 300px;
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
}

.control-group button {
  padding: 8px 16px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.control-group button:hover {
  background: #0056b3;
}

.status-info {
  margin-top: 15px;
  padding: 15px;
  background: white;
  border-radius: 4px;
  border-left: 4px solid #007bff;
}

.status-info p {
  margin: 5px 0;
  color: #495057;
}

/* OpenLayers 控件样式 */
:deep(.ol-scale-line) {
  bottom: 8px;
  left: 8px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
  padding: 2px 8px;
  border: 1px solid rgba(0, 0, 0, 0.2);
  font-family: 'Segoe UI', sans-serif;
  font-size: 12px;
  color: #333;
}

:deep(.ol-mouse-position) {
  top: 8px;
  right: 8px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
  padding: 8px 12px;
  border: 1px solid rgba(0, 0, 0, 0.2);
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: #333;
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
  color: #333;
}

:deep(.ol-zoom .ol-zoom-in:hover),
:deep(.ol-zoom .ol-zoom-out:hover) {
  background: #007bff;
  color: white;
}

/* 确保地图文字可见 */
:deep(.ol-overlay-container) {
  color: #333 !important;
}

:deep(.ol-control) {
  color: #333 !important;
}
</style>

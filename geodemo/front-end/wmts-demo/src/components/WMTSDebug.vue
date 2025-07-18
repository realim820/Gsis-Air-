<template>
  <div class="debug-panel">
    <h4>WMTS 调试工具</h4>
    
    <div class="debug-section">
      <h5>瓦片网格分析</h5>
      <input v-model="debugLayer" placeholder="图层名称" />
      <button @click="analyzeTileGrid">分析瓦片网格</button>
      
      <div v-if="tileGridInfo" class="grid-info">
        <h6>瓦片网格信息：</h6>
        <pre>{{ JSON.stringify(tileGridInfo, null, 2) }}</pre>
      </div>
    </div>
    
    <div class="debug-section">
      <h5>手动瓦片请求测试</h5>
      <div class="tile-test">
        <label>图层: <input v-model="testTileLayer" /></label>
        <label>缩放级别: <input v-model.number="testZ" type="number" min="0" max="20" /></label>
        <label>列: <input v-model.number="testX" type="number" /></label>
        <label>行: <input v-model.number="testY" type="number" /></label>
        <button @click="testSingleTile">测试单个瓦片</button>
      </div>
      
      <div v-if="tileTestResult" class="tile-result">
        <p><strong>测试结果:</strong> {{ tileTestResult.status }}</p>
        <p><strong>URL:</strong> <a :href="tileTestResult.url" target="_blank">{{ tileTestResult.url }}</a></p>
        <img v-if="tileTestResult.success" :src="tileTestResult.url" style="max-width: 256px; border: 1px solid #ccc;" />
      </div>
    </div>
    
    <div class="debug-section">
      <h5>推荐配置</h5>
      <div v-if="recommendedConfig" class="config-recommendation">
        <h6>基于分析的推荐配置：</h6>
        <pre>{{ recommendedConfig }}</pre>
        <button @click="applyRecommendedConfig">应用推荐配置</button>
      </div>
    </div>
    
    <div class="debug-section">
      <h5>瓦片坐标计算器</h5>
      <div class="coordinate-calculator">
        <div class="calc-input">
          <label>经度: <input v-model.number="calcLon" type="number" step="0.01" /></label>
          <label>纬度: <input v-model.number="calcLat" type="number" step="0.01" /></label>
          <label>缩放级别: <input v-model.number="calcZoom" type="number" min="0" max="20" /></label>
          <button @click="calculateTileCoords">计算瓦片坐标</button>
        </div>
        
        <div v-if="calculatedTile" class="calc-result">
          <p><strong>瓦片坐标:</strong></p>
          <p>缩放级别: {{ calculatedTile.z }}</p>
          <p>列 (X): {{ calculatedTile.x }}</p>
          <p>行 (Y): {{ calculatedTile.y }}</p>
          <button @click="useCalculatedTile">使用此坐标测试瓦片</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['apply-config'])

const debugLayer = ref('chinademo1:buildings')
const tileGridInfo = ref(null)
const testTileLayer = ref('chinademo1:buildings')
const testZ = ref(10)
const testX = ref(25)
const testY = ref(15)
const tileTestResult = ref(null)
const recommendedConfig = ref(null)
const calcLon = ref(116.4) // 北京经度
const calcLat = ref(39.9)  // 北京纬度
const calcZoom = ref(10)
const calculatedTile = ref(null)

const analyzeTileGrid = async () => {
  try {
    // 尝试通过 WMTS GetCapabilities 获取图层信息（无需认证）
    status.value = '通过 WMTS GetCapabilities 分析图层...'
    
    const response = await fetch('http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=GetCapabilities')
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    
    const xmlText = await response.text()
    const parser = new DOMParser()
    const doc = parser.parseFromString(xmlText, 'text/xml')
    
    // 查找指定图层
    const layers = doc.getElementsByTagName('Layer')
    let targetLayer = null
    
    for (let i = 0; i < layers.length; i++) {
      const layer = layers[i]
      const identifier = layer.querySelector('ows\\:Identifier, Identifier')?.textContent
      if (identifier === debugLayer.value) {
        targetLayer = layer
        break
      }
    }
    
    if (!targetLayer) {
      throw new Error(`图层 ${debugLayer.value} 未找到`)
    }
    
    // 提取图层信息
    const identifier = targetLayer.querySelector('ows\\:Identifier, Identifier')?.textContent
    const title = targetLayer.querySelector('ows\\:Title, Title')?.textContent
    
    // 获取支持的格式
    const formatElements = targetLayer.querySelectorAll('Format')
    const formats = Array.from(formatElements).map(el => el.textContent)
    
    // 获取支持的矩阵集
    const tileMatrixSetLinks = targetLayer.querySelectorAll('TileMatrixSetLink TileMatrixSet')
    const tileMatrixSets = Array.from(tileMatrixSetLinks).map(el => el.textContent)
    
    // 获取边界信息
    const bbox = targetLayer.querySelector('ows\\:WGS84BoundingBox, WGS84BoundingBox')
    let bounds = null
    if (bbox) {
      const lowerCorner = bbox.querySelector('ows\\:LowerCorner, LowerCorner')?.textContent?.split(' ')
      const upperCorner = bbox.querySelector('ows\\:UpperCorner, UpperCorner')?.textContent?.split(' ')
      if (lowerCorner && upperCorner) {
        bounds = [
          parseFloat(lowerCorner[0]), parseFloat(lowerCorner[1]),
          parseFloat(upperCorner[0]), parseFloat(upperCorner[1])
        ]
      }
    }
    
    // 获取矩阵集详细信息
    const matrixSetInfo = {}
    for (const matrixSetName of tileMatrixSets) {
      const matrixSet = doc.querySelector(`TileMatrixSet > ows\\:Identifier[textContent="${matrixSetName}"], TileMatrixSet > Identifier[textContent="${matrixSetName}"]`)?.parentElement
      if (matrixSet) {
        const matrices = matrixSet.querySelectorAll('TileMatrix')
        const matrixDetails = Array.from(matrices).map(matrix => {
          const id = matrix.querySelector('ows\\:Identifier, Identifier')?.textContent
          const scaleDenominator = matrix.querySelector('ScaleDenominator')?.textContent
          const topLeftCorner = matrix.querySelector('TopLeftCorner')?.textContent
          const tileWidth = matrix.querySelector('TileWidth')?.textContent
          const tileHeight = matrix.querySelector('TileHeight')?.textContent
          const matrixWidth = matrix.querySelector('MatrixWidth')?.textContent
          const matrixHeight = matrix.querySelector('MatrixHeight')?.textContent
          
          return {
            id,
            scaleDenominator: parseFloat(scaleDenominator),
            topLeftCorner,
            tileWidth: parseInt(tileWidth),
            tileHeight: parseInt(tileHeight),
            matrixWidth: parseInt(matrixWidth),
            matrixHeight: parseInt(matrixHeight)
          }
        })
        matrixSetInfo[matrixSetName] = matrixDetails
      }
    }
    
    tileGridInfo.value = {
      layer: identifier,
      title,
      formats,
      tileMatrixSets,
      bounds,
      matrixSetDetails: matrixSetInfo
    }
    
    // 生成推荐配置
    generateRecommendedConfig(tileMatrixSets, formats, bounds)
    
    console.log('图层分析完成:', tileGridInfo.value)
    
  } catch (error) {
    console.error('分析瓦片网格失败:', error)
    tileGridInfo.value = {
      error: error.message,
      note: 'REST API 需要认证，已改用 WMTS GetCapabilities 方式分析'
    }
  }
}

const testSingleTile = async () => {
  const url = `http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=GetTile&SERVICE=WMTS&VERSION=1.0.0&LAYER=${testTileLayer.value}&STYLE=&TILEMATRIXSET=EPSG:4326&TILEMATRIX=EPSG:4326:${testZ.value}&TILEROW=${testY.value}&TILECOL=${testX.value}&FORMAT=image/png`
  
  try {
    const response = await fetch(url)
    
    tileTestResult.value = {
      url,
      status: response.ok ? '成功' : `失败 (${response.status})`,
      success: response.ok
    }
    
    if (!response.ok) {
      const errorText = await response.text()
      console.error('瓦片请求失败:', errorText)
      
      // 解析错误信息
      if (errorText.includes('TileOutOfRange')) {
        const match = errorText.match(/min:\s*(\d+)\s*max:\s*(\d+)/)
        if (match) {
          console.log(`有效范围: ${match[1]} - ${match[2]}`)
          tileTestResult.value.status += ` (有效范围: ${match[1]}-${match[2]})`
        }
      }
    }
  } catch (error) {
    tileTestResult.value = {
      url,
      status: `网络错误: ${error.message}`,
      success: false
    }
  }
}

const generateRecommendedConfig = (tileMatrixSets, mimeFormats, bounds = null) => {
  // 智能选择最佳配置
  const bestMatrixSet = tileMatrixSets.includes('EPSG:4326') ? 'EPSG:4326' : 
                       tileMatrixSets.includes('EPSG:3857') ? 'EPSG:3857' : 
                       tileMatrixSets[0]
  
  const bestFormat = mimeFormats.includes('image/png') ? 'image/png' : 
                    mimeFormats.includes('image/jpeg') ? 'image/jpeg' : 
                    mimeFormats[0]
  
  // 根据边界信息智能设置视图
  let center, zoom
  if (bounds) {
    // 计算边界中心点
    center = [(bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2]
    
    // 根据边界大小估算合适的缩放级别
    const width = bounds[2] - bounds[0]
    const height = bounds[3] - bounds[1]
    const maxExtent = Math.max(width, height)
    
    if (maxExtent > 50) zoom = 4      // 大区域/国家级
    else if (maxExtent > 10) zoom = 6  // 省级
    else if (maxExtent > 2) zoom = 8   // 市级
    else zoom = 10                     // 区县级
  } else {
    // 默认中国区域
    center = [105, 35]
    zoom = 6
  }
  
  const config = {
    layer: debugLayer.value,
    recommendedMatrixSet: bestMatrixSet,
    recommendedFormat: bestFormat,
    recommendedCenter: center,
    recommendedZoom: zoom,
    bounds: bounds,
    availableMatrixSets: tileMatrixSets,
    availableFormats: mimeFormats,
    notes: [
      `选择 ${bestMatrixSet} 作为坐标系（最常用）`,
      `选择 ${bestFormat} 作为图像格式`,
      `建议缩放级别: ${zoom}（基于数据范围）`,
      bounds ? `数据边界: [${bounds.map(n => n.toFixed(2)).join(', ')}]` : '未找到边界信息'
    ]
  }
  
  recommendedConfig.value = JSON.stringify(config, null, 2)
}

const applyRecommendedConfig = () => {
  if (recommendedConfig.value) {
    const config = JSON.parse(recommendedConfig.value)
    emit('apply-config', config)
  }
}

const calculateTileCoords = () => {
  // EPSG:4326 瓦片坐标计算
  // 参考: https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
  
  const zoom = calcZoom.value
  const lon = calcLon.value
  const lat = calcLat.value
  
  // 转换为瓦片坐标
  const x = Math.floor((lon + 180) / 360 * Math.pow(2, zoom))
  const y = Math.floor((1 - Math.log(Math.tan(lat * Math.PI / 180) + 1 / Math.cos(lat * Math.PI / 180)) / Math.PI) / 2 * Math.pow(2, zoom))
  
  calculatedTile.value = { x, y, z: zoom }
  
  console.log(`坐标 (${lon}, ${lat}) 在缩放级别 ${zoom} 对应瓦片 [${zoom}, ${x}, ${y}]`)
}

const useCalculatedTile = () => {
  if (calculatedTile.value) {
    testZ.value = calculatedTile.value.z
    testX.value = calculatedTile.value.x
    testY.value = calculatedTile.value.y
    testTileLayer.value = debugLayer.value
    
    // 自动测试这个瓦片
    testSingleTile()
  }
}
</script>

<style scoped>
.debug-panel {
  margin: 20px 0;
  padding: 15px;
  background-color: #fff5f5;
  border: 1px solid #fecaca;
  border-radius: 6px;
}

.debug-section {
  margin: 15px 0;
  padding: 10px;
  background-color: white;
  border-radius: 4px;
}

.coordinate-calculator {
  background-color: #e8f5e8;
  padding: 10px;
  border-radius: 4px;
  margin: 10px 0;
}

.calc-input {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

.calc-input label {
  display: flex;
  flex-direction: column;
  font-size: 12px;
}

.calc-input input {
  padding: 4px;
  border: 1px solid #ccc;
  border-radius: 3px;
  margin-top: 2px;
}

.calc-result {
  background-color: #d4edda;
  padding: 8px;
  border-radius: 3px;
  border: 1px solid #c3e6cb;
}

.tile-test {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin: 10px 0;
}

.tile-test label {
  display: flex;
  flex-direction: column;
  font-size: 12px;
}

.tile-test input {
  padding: 4px;
  border: 1px solid #ccc;
  border-radius: 3px;
  margin-top: 2px;
}

.grid-info, .tile-result, .config-recommendation {
  margin: 10px 0;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

pre {
  background-color: #f1f3f4;
  padding: 8px;
  border-radius: 3px;
  overflow-x: auto;
  font-size: 11px;
}

button {
  padding: 6px 12px;
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  margin: 5px;
}

button:hover {
  background-color: #c82333;
}

.coordinate-calculator {
  display: flex;
  flex-direction: column;
}

.calc-input {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin: 10px 0;
}

.calc-input label {
  display: flex;
  flex-direction: column;
  font-size: 12px;
}

.calc-input input {
  padding: 4px;
  border: 1px solid #ccc;
  border-radius: 3px;
  margin-top: 2px;
}

.calc-result {
  margin: 10px 0;
  padding: 10px;
  background-color: #d1e7dd;
  border-radius: 4px;
}
</style>

<template>
  <div ref="mapContainer" class="map"></div>
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

onMounted(() => {
  console.log('MapViewer 组件已挂载')
  console.log('地图容器元素:', mapContainer.value)
  
  const projection = getProjection('EPSG:4326')
  const extent = projection.getExtent()
  const size = getWidth(extent) / 256

  // ✅ 设置有效层级范围（GeoServer capabilities 中有的）
  const minZoom = 4
  const maxZoom = 21
  const resolutions = []
  const matrixIds = []

  for (let z = minZoom; z <= maxZoom; ++z) {
    resolutions.push(size / Math.pow(2, z))
    matrixIds.push(`EPSG:4326:${z}`)  // ✅ 必须带前缀
  }

  // 使用 capabilities 中确实存在的图层
  const wmtsSource = new WMTS({
    url: 'http://localhost:8080/geoserver/gwc/service/wmts',
    layer: 'ne:countries', // 使用 capabilities 中存在的图层
    matrixSet: 'EPSG:4326',
    format: 'image/png',
    projection: projection,
    requestEncoding: 'KVP',
    tileGrid: new TileGrid({
      origin: getTopLeft(extent),
      resolutions,
      matrixIds,
    }),
    crossOrigin: 'anonymous',
  })

  // 监听 WMTS 源的事件
  wmtsSource.on('tileloadstart', () => {
    console.log('WMTS 瓦片开始加载')
  })
  
  wmtsSource.on('tileloadend', () => {
    console.log('WMTS 瓦片加载完成')
  })
  
  wmtsSource.on('tileloaderror', (event) => {
    console.error('WMTS 瓦片加载错误:', event)
    console.log('尝试使用备用源...')
  })

  const map = new Map({
    target: mapContainer.value,
    layers: [
      // ✅ OSM 底图
      new TileLayer({
        source: new OSM(),
      }),
      // ✅ WMTS 图层
      new TileLayer({
        source: wmtsSource,
        opacity: 0.7, // 设置透明度以便看到底图
      }),
    ],
    view: new View({
      projection: 'EPSG:4326',
      center: [-100, 40], // 美国中心位置
      zoom: 4,
      minZoom: 2,
      maxZoom: 18,
    }),
    controls: defaultControls().extend([
      new ScaleLine(),
      new MousePosition({
        coordinateFormat: createStringXY(4),
        projection: 'EPSG:4326',
        className: 'mouse-position',
      }),
    ]),
  })

  // 添加调试信息
  console.log('地图对象已创建:', map)
  console.log('地图目标元素:', map.getTarget())
  
  // 监听地图加载完成事件
  map.on('postrender', () => {
    console.log('地图渲染完成')
  })
  
  // 确保地图能正确计算大小
  setTimeout(() => {
    map.updateSize()
    console.log('地图大小已更新')
  }, 100)
})
</script>

<style scoped>
.map {
  width: 100%;
  height: 100vh;
  background-color: #f0f0f0; /* 添加背景色方便调试 */
  border: 1px solid #ccc; /* 添加边框方便看到容器 */
}

/* 鼠标位置显示样式 */
:global(.mouse-position) {
  top: 8px;
  right: 8px;
  position: absolute;
  background: rgba(255, 255, 255, 0.8);
  padding: 2px 4px;
  font-family: monospace;
  font-size: 12px;
  border-radius: 3px;
}
</style>

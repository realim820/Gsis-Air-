<template>
  <div ref="mapContainer" class="simple-map"></div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import OSM from 'ol/source/OSM'

const mapContainer = ref(null)

onMounted(() => {
  console.log('简单地图组件已挂载')
  
  const map = new Map({
    target: mapContainer.value,
    layers: [
      new TileLayer({
        source: new OSM(),
      }),
    ],
    view: new View({
      center: [0, 0], // 世界中心
      zoom: 2,
    }),
  })

  console.log('简单地图已创建')
})
</script>

<style scoped>
.simple-map {
  width: 100%;
  height: 400px;
  border: 2px solid #007bff;
  background-color: #e9ecef;
}
</style>

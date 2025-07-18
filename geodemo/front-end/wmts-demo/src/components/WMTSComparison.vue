<template>
  <div class="wmts-comparison">
    <h2>🗺️ WMTS 服务对比: OpenLayers vs Leaflet</h2>
    
    <div class="comparison-tabs">
      <button 
        @click="activeTab = 'openlayers'" 
        :class="{ active: activeTab === 'openlayers' }"
        class="tab-btn openlayers-tab"
      >
        📊 OpenLayers
      </button>
      <button 
        @click="activeTab = 'leaflet'" 
        :class="{ active: activeTab === 'leaflet' }"
        class="tab-btn leaflet-tab"
      >
        📍 Leaflet
      </button>
      <button 
        @click="activeTab = 'comparison'" 
        :class="{ active: activeTab === 'comparison' }"
        class="tab-btn comparison-tab"
      >
        ⚖️ 对比分析
      </button>
    </div>
    
    <!-- OpenLayers 实现 -->
    <div v-show="activeTab === 'openlayers'" class="tab-content">
      <div class="implementation-header">
        <h3>OpenLayers WMTS 实现</h3>
        <div class="features">
          <span class="feature-tag">✨ 功能强大</span>
          <span class="feature-tag">🔧 高度可定制</span>
          <span class="feature-tag">📐 精确控制</span>
        </div>
      </div>
      <WMTSTest />
    </div>
    
    <!-- Leaflet 实现 -->
    <div v-show="activeTab === 'leaflet'" class="tab-content">
      <div class="implementation-header">
        <h3>Leaflet WMTS 实现</h3>
        <div class="features">
          <span class="feature-tag">🚀 轻量级</span>
          <span class="feature-tag">📱 移动友好</span>
          <span class="feature-tag">⚡ 快速上手</span>
        </div>
      </div>
      <LeafletWMTS />
    </div>
    
    <!-- 对比分析 -->
    <div v-show="activeTab === 'comparison'" class="tab-content">
      <div class="comparison-content">
        <h3>📊 详细对比分析</h3>
        
        <div class="comparison-grid">
          <!-- 性能对比 -->
          <div class="comparison-card">
            <h4>⚡ 性能表现</h4>
            <div class="metrics">
              <div class="metric">
                <span class="label">库大小:</span>
                <div class="comparison-row">
                  <span class="ol-value">OpenLayers: ~180KB</span>
                  <span class="leaflet-value">Leaflet: ~39KB</span>
                </div>
              </div>
              <div class="metric">
                <span class="label">初始化速度:</span>
                <div class="comparison-row">
                  <span class="ol-value">中等</span>
                  <span class="leaflet-value">快速</span>
                </div>
              </div>
              <div class="metric">
                <span class="label">内存占用:</span>
                <div class="comparison-row">
                  <span class="ol-value">较高</span>
                  <span class="leaflet-value">较低</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 功能对比 -->
          <div class="comparison-card">
            <h4>🔧 功能特性</h4>
            <div class="feature-comparison">
              <div class="feature-row">
                <span class="feature-name">WMTS 支持</span>
                <span class="ol-support">✅ 原生支持</span>
                <span class="leaflet-support">✅ 插件支持</span>
              </div>
              <div class="feature-row">
                <span class="feature-name">坐标系支持</span>
                <span class="ol-support">✅ 全面支持</span>
                <span class="leaflet-support">⚠️ 有限支持</span>
              </div>
              <div class="feature-row">
                <span class="feature-name">瓦片网格控制</span>
                <span class="ol-support">✅ 精确控制</span>
                <span class="leaflet-support">⚠️ 基础控制</span>
              </div>
              <div class="feature-row">
                <span class="feature-name">错误处理</span>
                <span class="ol-support">✅ 详细事件</span>
                <span class="leaflet-support">✅ 基础事件</span>
              </div>
            </div>
          </div>
          
          <!-- 易用性对比 -->
          <div class="comparison-card">
            <h4>👩‍💻 开发体验</h4>
            <div class="usability">
              <div class="usability-item">
                <h5>OpenLayers</h5>
                <ul>
                  <li>✅ 功能全面，适合复杂项目</li>
                  <li>✅ 文档详细，社区活跃</li>
                  <li>⚠️ 学习曲线较陡</li>
                  <li>⚠️ 配置复杂</li>
                </ul>
              </div>
              <div class="usability-item">
                <h5>Leaflet</h5>
                <ul>
                  <li>✅ 简单易用，快速上手</li>
                  <li>✅ 插件生态丰富</li>
                  <li>✅ 移动设备优化</li>
                  <li>⚠️ 高级功能需要插件</li>
                </ul>
              </div>
            </div>
          </div>
          
          <!-- 代码示例对比 -->
          <div class="comparison-card full-width">
            <h4>💾 代码复杂度对比</h4>
            <div class="code-comparison">
              <div class="code-example">
                <h5>OpenLayers WMTS 配置</h5>
                <pre><code>// 复杂但精确的配置
const wmtsSource = new WMTS({
  url: 'http://localhost:8080/geoserver/gwc/service/wmts',
  layer: 'chinademo1:buildings',
  matrixSet: 'EPSG:4326',
  format: 'image/png',
  projection: getProjection('EPSG:4326'),
  requestEncoding: 'KVP',
  tileGrid: new TileGrid({
    origin: getTopLeft(extent),
    resolutions: customResolutions,
    matrixIds: customMatrixIds,
  }),
  crossOrigin: 'anonymous',
})</code></pre>
              </div>
              
              <div class="code-example">
                <h5>Leaflet WMTS 配置</h5>
                <pre><code>// 简洁的 URL 模板方式
const wmtsUrl = 'http://localhost:8080/geoserver/gwc/service/wmts?' +
  'SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&' +
  'LAYER=chinademo1:buildings&STYLE=&' +
  'TILEMATRIXSET=EPSG:4326&FORMAT=image/png&' +
  'TILEMATRIX=EPSG:4326:{z}&TILEROW={y}&TILECOL={x}'

const wmtsLayer = L.tileLayer(wmtsUrl, {
  attribution: 'GeoServer WMTS',
  opacity: 0.8
})</code></pre>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 选择建议 -->
        <div class="recommendation">
          <h4>🎯 选择建议</h4>
          <div class="recommendation-grid">
            <div class="rec-card">
              <h5>选择 OpenLayers 当你需要:</h5>
              <ul>
                <li>🎯 复杂的地理空间分析</li>
                <li>🗺️ 多种坐标系支持</li>
                <li>🔧 精确的瓦片控制</li>
                <li>📊 高级的可视化功能</li>
                <li>🏢 企业级应用开发</li>
              </ul>
            </div>
            
            <div class="rec-card">
              <h5>选择 Leaflet 当你需要:</h5>
              <ul>
                <li>🚀 快速原型开发</li>
                <li>📱 移动端地图应用</li>
                <li>💡 简单的地图展示</li>
                <li>⚡ 轻量级解决方案</li>
                <li>👨‍💻 快速学习和部署</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import WMTSTest from './WMTSTest.vue'
import LeafletWMTS from './LeafletWMTS.vue'

const activeTab = ref('leaflet') // 默认显示 Leaflet
</script>

<style scoped>
.wmts-comparison {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.wmts-comparison h2 {
  text-align: center;
  color: #2c3e50;
  margin-bottom: 30px;
  font-size: 2rem;
}

.comparison-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
  background: #f8f9fa;
  border-radius: 10px;
  padding: 5px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.tab-btn {
  flex: 1;
  padding: 12px 24px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s;
  max-width: 200px;
}

.tab-btn:hover {
  background: rgba(255,255,255,0.5);
}

.tab-btn.active {
  color: white;
  box-shadow: 0 4px 15px rgba(0,0,0,0.2);
}

.openlayers-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.leaflet-tab.active {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.comparison-tab.active {
  background: linear-gradient(135deg, #ff6b6b 0%, #ffd93d 100%);
}

.tab-content {
  animation: fadeIn 0.5s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.implementation-header {
  margin-bottom: 20px;
  text-align: center;
}

.implementation-header h3 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.features {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.feature-tag {
  background: #e3f2fd;
  color: #1976d2;
  padding: 4px 12px;
  border-radius: 15px;
  font-size: 12px;
  font-weight: 500;
}

.comparison-content h3 {
  text-align: center;
  color: #2c3e50;
  margin-bottom: 30px;
}

.comparison-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.comparison-card {
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
  border: 1px solid #e0e0e0;
}

.comparison-card.full-width {
  grid-column: 1 / -1;
}

.comparison-card h4 {
  color: #2c3e50;
  margin-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 8px;
}

.metrics .metric {
  margin-bottom: 12px;
}

.metrics .label {
  display: block;
  font-weight: 500;
  color: #555;
  margin-bottom: 5px;
}

.comparison-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.ol-value {
  background: #e3f2fd;
  color: #1976d2;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  flex: 1;
  text-align: center;
}

.leaflet-value {
  background: #e8f5e8;
  color: #2e7d32;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  flex: 1;
  text-align: center;
}

.feature-comparison .feature-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
}

.feature-name {
  font-weight: 500;
  color: #555;
}

.ol-support, .leaflet-support {
  text-align: center;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.ol-support {
  background: #e3f2fd;
  color: #1976d2;
}

.leaflet-support {
  background: #e8f5e8;
  color: #2e7d32;
}

.usability {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.usability-item h5 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.usability-item ul {
  list-style: none;
  padding: 0;
}

.usability-item li {
  padding: 4px 0;
  font-size: 14px;
}

.code-comparison {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.code-example h5 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.code-example pre {
  background: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 5px;
  padding: 15px;
  overflow-x: auto;
  font-size: 12px;
  line-height: 1.4;
}

.code-example code {
  color: #d63384;
}

.recommendation {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  border-radius: 10px;
  padding: 25px;
  margin-top: 30px;
}

.recommendation h4 {
  text-align: center;
  color: #8b4513;
  margin-bottom: 20px;
}

.recommendation-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.rec-card {
  background: rgba(255,255,255,0.9);
  border-radius: 8px;
  padding: 20px;
}

.rec-card h5 {
  color: #8b4513;
  margin-bottom: 15px;
}

.rec-card ul {
  list-style: none;
  padding: 0;
}

.rec-card li {
  padding: 5px 0;
  font-size: 14px;
  color: #555;
}

@media (max-width: 768px) {
  .comparison-tabs {
    flex-direction: column;
    gap: 5px;
  }
  
  .comparison-row {
    flex-direction: column;
  }
  
  .feature-comparison .feature-row {
    grid-template-columns: 1fr;
    gap: 5px;
    text-align: center;
  }
  
  .usability {
    grid-template-columns: 1fr;
  }
  
  .code-comparison {
    grid-template-columns: 1fr;
  }
  
  .recommendation-grid {
    grid-template-columns: 1fr;
  }
}
</style>

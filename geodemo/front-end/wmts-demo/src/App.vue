<template>
  <div>
    <h1>🗺️ WMTS 地图演示中心</h1>
    
    <!-- 导航选项卡 -->
    <div class="main-tabs">
      <button 
        @click="activeView = 'comparison'" 
        :class="{ active: activeView === 'comparison' }"
        class="main-tab-btn"
      >
        ⚖️ 对比分析
      </button>
      <button 
        @click="activeView = 'openlayers'" 
        :class="{ active: activeView === 'openlayers' }"
        class="main-tab-btn"
      >
        📊 OpenLayers
      </button>
      <button 
        @click="activeView = 'leaflet'" 
        :class="{ active: activeView === 'leaflet' }"
        class="main-tab-btn"
      >
        📍 Leaflet
      </button>
      <button 
        @click="activeView = 'demo'" 
        :class="{ active: activeView === 'demo' }"
        class="main-tab-btn"
      >
        🎯 demo
      </button>
      <button 
        @click="activeView = 'simple'" 
        :class="{ active: activeView === 'simple' }"
        class="main-tab-btn"
      >
        🎯 简单示例
      </button>
    </div>
    
    <!-- 内容区域 -->
    <div class="content-area">
      <!-- 对比分析 -->
      <div v-show="activeView === 'comparison'">
        <WMTSComparison />
      </div>
      
      <!-- OpenLayers 完整示例 -->
      <div v-show="activeView === 'openlayers'">
        <div class="view-header">
          <h2>📊 OpenLayers WMTS 完整示例</h2>
          <p>功能强大的地理空间可视化库，支持复杂的 WMTS 配置和高级功能</p>
        </div>
        <WMTSTest />
      </div>
      
      <!-- Leaflet 示例 -->
      <div v-show="activeView === 'leaflet'">
        <div class="view-header">
          <h2>📍 Leaflet WMTS 示例</h2>
          <p>轻量级、移动友好的地图库，简单易用的 WMTS 集成</p>
        </div>
        <LeafletWMTS />
      </div>
      <!-- demo示例 -->
      <div v-show="activeView === 'demo'">
        <div class="view-header">
          <h2>🎯 demo 示例</h2>
          <p>最简单的地图实现，用于快速验证基础功能</p>
        </div>
        <WMTSDemo />
      </div>
      <!-- 简单示例 -->
      <div v-show="activeView === 'simple'">
        <div class="view-header">
          <h2>🎯 基础地图示例</h2>
          <p>最简单的地图实现，用于快速验证基础功能</p>
        </div>
        <SimpleMap />
        <div id="mouse-pos" style="padding: 8px; background: #eee; font-family: monospace; margin-top: 10px; border-radius: 4px;"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import WMTSComparison from './components/WMTSComparison.vue'
import WMTSTest from './components/WMTSTest.vue'
import LeafletWMTS from './components/LeafletWMTS.vue'
import SimpleMap from './components/SimpleMap.vue'
import WMTSDemo from './components/WMTSDemo.vue'

const activeView = ref('comparison') // 默认显示对比分析
</script>

<style>
body {
  margin: 0;
  padding: 20px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: 100vh;
}

#app {
  max-width: 1400px;
  margin: 0 auto;
}

h1 {
  text-align: center;
  color: #2c3e50;
  margin-bottom: 30px;
  font-size: 2.5rem;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.1);
}

.main-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
  background: white;
  border-radius: 15px;
  padding: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 30px;
}

.main-tab-btn {
  flex: 1;
  padding: 15px 20px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 10px;
  font-weight: 600;
  transition: all 0.3s ease;
  color: #666;
  font-size: 16px;
}

.main-tab-btn:hover {
  background: #f8f9fa;
  color: #2c3e50;
  transform: translateY(-2px);
}

.main-tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  transform: translateY(-2px);
}

.content-area {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.1);
  margin-bottom: 30px;
}

.view-header {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
}

.view-header h2 {
  color: #2c3e50;
  margin-bottom: 10px;
  font-size: 1.8rem;
}

.view-header p {
  color: #666;
  font-size: 1.1rem;
  max-width: 600px;
  margin: 0 auto;
  line-height: 1.6;
}

/* 响应式设计 */
@media (max-width: 768px) {
  body {
    padding: 10px;
  }
  
  h1 {
    font-size: 1.8rem;
  }
  
  .main-tabs {
    flex-direction: column;
    gap: 5px;
  }
  
  .main-tab-btn {
    font-size: 14px;
    padding: 12px 16px;
  }
  
  .content-area {
    padding: 20px;
  }
  
  .view-header h2 {
    font-size: 1.4rem;
  }
  
  .view-header p {
    font-size: 1rem;
  }
}

/* 全局动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.content-area > div {
  animation: fadeInUp 0.6s ease-out;
}
</style>

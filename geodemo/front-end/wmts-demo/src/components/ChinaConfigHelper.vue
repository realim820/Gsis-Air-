<template>
  <div class="china-config-helper">
    <h4>🇨🇳 中国数据配置助手</h4>
    
    <div class="quick-fixes">
      <h5>快速修复建议</h5>
      <div class="fix-buttons">
        <button @click="applyChinaOptimized" class="fix-btn primary">
          🔧 应用中国优化配置
        </button>
        <button @click="testBeijingArea" class="fix-btn">
          📍 测试北京区域
        </button>
        <button @click="testShanghaiArea" class="fix-btn">
          📍 测试上海区域
        </button>
        <button @click="checkTileAvailability" class="fix-btn warning">
          🔍 检查瓦片可用性
        </button>
      </div>
    </div>
    
    <div class="common-issues">
      <h5>常见问题诊断</h5>
      <div class="issue-list">
        <div class="issue-item">
          <span class="issue-icon">❌</span>
          <span class="issue-title">TileOutOfRange 错误</span>
          <p class="issue-desc">瓦片坐标超出数据范围</p>
          <button @click="fixTileRange" class="fix-small">修复</button>
        </div>
        
        <div class="issue-item">
          <span class="issue-icon">🔒</span>
          <span class="issue-title">401 未授权错误</span>
          <p class="issue-desc">REST API 需要用户名/密码</p>
          <button @click="showAuthInfo" class="fix-small">查看解决方案</button>
        </div>
        
        <div class="issue-item">
          <span class="issue-icon">🌍</span>
          <span class="issue-title">坐标系不匹配</span>
          <p class="issue-desc">使用错误的投影坐标系</p>
          <button @click="fixProjection" class="fix-small">修复</button>
        </div>
      </div>
    </div>
    
    <div class="recommended-settings">
      <h5>推荐设置</h5>
      <div class="settings-grid">
        <div class="setting-item">
          <label>图层名称:</label>
          <code>chinademo1:buildings</code>
        </div>
        <div class="setting-item">
          <label>坐标系:</label>
          <code>EPSG:4326</code>
        </div>
        <div class="setting-item">
          <label>格式:</label>
          <code>image/png</code>
        </div>
        <div class="setting-item">
          <label>缩放范围:</label>
          <code>8-14</code>
        </div>
        <div class="setting-item">
          <label>中心点:</label>
          <code>[116.4, 39.9]</code>
        </div>
        <div class="setting-item">
          <label>初始缩放:</label>
          <code>10</code>
        </div>
      </div>
    </div>
    
    <div v-if="diagnosisResult" class="diagnosis-result">
      <h5>诊断结果</h5>
      <div class="result-content">
        <pre>{{ diagnosisResult }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['apply-config', 'test-area'])

const diagnosisResult = ref(null)

const applyChinaOptimized = () => {
  const config = {
    layer: 'chinademo1:buildings',
    recommendedMatrixSet: 'EPSG:4326',
    recommendedFormat: 'image/png',
    recommendedCenter: [105, 35], // 中国中心
    recommendedZoom: 6,
    minZoom: 4,
    maxZoom: 14,
    type: 'china-optimized'
  }
  
  emit('apply-config', config)
  diagnosisResult.value = '已应用中国区域优化配置\n- 使用 EPSG:4326 坐标系\n- 缩放范围 4-14 级\n- 中心定位到中国'
}

const testBeijingArea = () => {
  const config = {
    layer: 'chinademo1:buildings',
    recommendedMatrixSet: 'EPSG:4326',
    recommendedFormat: 'image/png',
    recommendedCenter: [116.4, 39.9], // 北京
    recommendedZoom: 10,
    bounds: [115.4, 39.4, 117.4, 40.4], // 北京区域
    type: 'beijing-test'
  }
  
  emit('apply-config', config)
  diagnosisResult.value = '测试北京区域\n- 中心: 天安门广场\n- 缩放级别: 10\n- 适合建筑数据显示'
}

const testShanghaiArea = () => {
  const config = {
    layer: 'chinademo1:buildings',
    recommendedMatrixSet: 'EPSG:4326',
    recommendedFormat: 'image/png',
    recommendedCenter: [121.5, 31.2], // 上海
    recommendedZoom: 10,
    bounds: [120.8, 30.7, 122.2, 31.7], // 上海区域
    type: 'shanghai-test'
  }
  
  emit('apply-config', config)
  diagnosisResult.value = '测试上海区域\n- 中心: 人民广场\n- 缩放级别: 10\n- 适合建筑数据显示'
}

const checkTileAvailability = async () => {
  diagnosisResult.value = '正在检查瓦片可用性...'
  
  // 测试几个关键瓦片
  const testTiles = [
    { z: 8, x: 210, y: 100, desc: '中国北部' },
    { z: 9, x: 420, y: 200, desc: '华北地区' },
    { z: 10, x: 840, y: 400, desc: '北京区域' },
    { z: 11, x: 1680, y: 800, desc: '北京市区' }
  ]
  
  const results = []
  
  for (const tile of testTiles) {
    try {
      const url = `http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=GetTile&SERVICE=WMTS&VERSION=1.0.0&LAYER=chinademo1:buildings&STYLE=&TILEMATRIXSET=EPSG:4326&TILEMATRIX=EPSG:4326:${tile.z}&TILEROW=${tile.y}&TILECOL=${tile.x}&FORMAT=image/png`
      
      const response = await fetch(url)
      const status = response.ok ? '✅ 可用' : `❌ 失败 (${response.status})`
      
      results.push(`${tile.desc} [${tile.z}/${tile.x}/${tile.y}]: ${status}`)
      
      if (!response.ok) {
        const errorText = await response.text()
        if (errorText.includes('TileOutOfRange')) {
          const match = errorText.match(/min:\s*(\d+)\s*max:\s*(\d+)/)
          if (match) {
            results.push(`  有效列范围: ${match[1]}-${match[2]}`)
          }
        }
      }
    } catch (error) {
      results.push(`${tile.desc}: ❌ 网络错误`)
    }
  }
  
  diagnosisResult.value = '瓦片可用性检查结果:\n\n' + results.join('\n')
}

const fixTileRange = () => {
  diagnosisResult.value = `修复 TileOutOfRange 错误的方法:

1. 调整地图视图到数据覆盖区域
2. 使用正确的缩放级别范围 (8-14)
3. 确保中心点在中国境内
4. 检查瓦片矩阵集配置

建议操作:
- 点击"应用中国优化配置"
- 然后点击"测试北京区域"
`
}

const showAuthInfo = () => {
  diagnosisResult.value = `解决 401 未授权错误:

GeoServer REST API 默认需要管理员认证:
- 默认用户名: admin
- 默认密码: geoserver

解决方案:
1. 使用 WMTS GetCapabilities (无需认证)
2. 配置 GeoServer 允许匿名访问
3. 在请求中添加认证头

当前工具已自动使用方案1，无需额外配置。
`
}

const fixProjection = () => {
  const config = {
    layer: 'chinademo1:buildings',
    recommendedMatrixSet: 'EPSG:4326',
    recommendedFormat: 'image/png',
    recommendedCenter: [105, 35],
    recommendedZoom: 6,
    type: 'projection-fix'
  }
  
  emit('apply-config', config)
  diagnosisResult.value = '已修复投影设置\n- 强制使用 EPSG:4326\n- 适合中国地理数据\n- 兼容性最佳'
}
</script>

<style scoped>
.china-config-helper {
  background: linear-gradient(135deg, #ff6b6b 0%, #ffd93d 100%);
  padding: 15px;
  border-radius: 8px;
  margin: 20px 0;
  color: #333;
}

.china-config-helper h4 {
  margin: 0 0 15px 0;
  color: #d63031;
  font-weight: bold;
}

.quick-fixes, .common-issues, .recommended-settings {
  background: white;
  padding: 12px;
  border-radius: 6px;
  margin: 10px 0;
}

.fix-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
}

.fix-btn {
  padding: 8px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.fix-btn.primary {
  background: #00b894;
  color: white;
}

.fix-btn.warning {
  background: #fdcb6e;
  color: #333;
}

.fix-btn:not(.primary):not(.warning) {
  background: #74b9ff;
  color: white;
}

.fix-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.2);
}

.issue-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.issue-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 4px;
  border-left: 4px solid #e17055;
}

.issue-icon {
  font-size: 18px;
}

.issue-title {
  font-weight: 600;
  color: #2d3436;
}

.issue-desc {
  grid-column: 2;
  margin: 0;
  font-size: 12px;
  color: #636e72;
}

.fix-small {
  padding: 4px 8px;
  font-size: 11px;
  background: #0984e3;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  background: #f1f3f4;
  border-radius: 3px;
}

.setting-item label {
  font-size: 12px;
  color: #5f6368;
  font-weight: 500;
}

.setting-item code {
  background: #e8eaed;
  padding: 2px 6px;
  border-radius: 2px;
  font-size: 11px;
  font-family: 'Courier New', monospace;
}

.diagnosis-result {
  background: white;
  padding: 12px;
  border-radius: 6px;
  margin: 10px 0;
}

.result-content pre {
  background: #2d3436;
  color: #ddd;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
  margin: 0;
  white-space: pre-wrap;
}
</style>

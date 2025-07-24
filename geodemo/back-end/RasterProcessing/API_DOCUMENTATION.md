# 栅格处理服务API文档

## 概述
这个Spring Boot应用程序提供了基于GDAL和OpenCV的数据处理功能，包括：
1. 栅格数据属性读取
2. 基于DCT的暗水印嵌入和提取（支持TIFF遥感数据和JPEG/PNG普通图像）

## 环境要求
- Java 17+
- Spring Boot 3.5.3
- GDAL 3.x
- OpenCV 4.12.0

## 🚀 核心功能

### 自动文件类型识别
系统会自动识别输入文件类型并选择合适的处理算法：
- **栅格数据格式**: tif, tiff, geotiff, img, hdf, nc, grib, jp2
- **普通图像格式**: jpg, jpeg, png, bmp, gif

### 水印算法特点
- **栅格数据**: 在原始像素值上进行DCT变换，适合遥感数据
- **普通图像**: 在YUV颜色空间的Y通道进行DCT变换，保持视觉质量

## API端点

### 1. 通用水印处理API（推荐使用）

#### 1.1 🔥 通用水印嵌入
```
POST /api/watermark/embed?inputPath={输入路径}&outputPath={输出路径}&watermarkText={水印文本}
```
**功能**: 自动判断文件类型（TIFF遥感数据 或 JPEG/PNG普通图像）并选择合适的水印算法

**参数:**
- `inputPath`: 输入文件路径（支持 .tif/.tiff/.jpg/.jpeg/.png 等格式）
- `outputPath`: 输出文件路径
- `watermarkText`: 要嵌入的水印文本

**响应示例:**
```json
{
  "success": true,
  "message": "图像水印嵌入成功",
  "outputPath": "testdata/watermarked_image.png",
  "processingTime": 850
}
```

#### 1.2 🔥 通用水印提取
```
GET /api/watermark/extract?filePath={文件路径}&watermarkLength={水印长度}
```
**功能**: 自动判断文件类型并提取水印

**参数:**
- `filePath`: 包含水印的文件路径
- `watermarkLength`: 预期水印文本长度（字符数）

**响应示例:**
```json
{
  "success": true,
  "message": "水印提取成功",
  "watermarkText": "COPYRIGHT2025"
}
```

#### 1.3 支持的格式查询
```
GET /api/watermark/formats
```
**响应:**
```
支持的文件格式:
栅格数据: tif, tiff, geotiff, img, hdf, nc, grib, jp2
普通图像: jpg, jpeg, png, bmp, gif
```

#### 1.4 格式检查
```
GET /api/watermark/check-format?filePath={文件路径}
```
**响应示例:**
```json
{
  "supported": true,
  "message": "文件格式受支持"
}
```

### 2. 栅格数据读取API

#### 2.1 获取栅格信息
```
GET /api/raster/info?filePath={文件路径}
```
**响应示例:**
```json
{
  "fileName": "testdata/sample.tif",
  "width": 256,
  "height": 256,
  "bandCount": 3,
  "driver": "GTiff",
  "projection": "GEOGCS[\"GCS_WGS_1984\"...]",
  "geoTransform": [120.0, 0.001, 0.0, 40.0, 0.0, -0.001],
  "dataType": "Float32",
  "noDataValue": null,
  "metadata": {}
}
```

#### 2.2 获取像素值
```
GET /api/raster/pixels?filePath={文件路径}&bandIndex={波段}&x={X坐标}&y={Y坐标}&width={宽度}&height={高度}
```

#### 2.3 获取统计信息
```
GET /api/raster/statistics?filePath={文件路径}&bandIndex={波段}
```

### 3. 专用水印处理API（向后兼容）

#### 3.1 栅格专用水印嵌入
```
POST /api/watermark/embed-raster?inputPath={输入路径}&outputPath={输出路径}&watermarkText={水印文本}
```

#### 3.2 栅格专用水印提取
```
GET /api/watermark/extract-raster?filePath={文件路径}&watermarkLength={水印长度}
```

### 4. 测试端点

```
GET /test/gdal                    # 测试GDAL功能
GET /test/opencv                  # 测试OpenCV功能
GET /test/generate-sample         # 生成测试GeoTIFF文件
GET /test/raster-info            # 测试栅格信息读取
GET /test/watermark              # 测试TIFF水印功能
GET /test/image-watermark        # 测试图像水印功能
GET /test/all                    # 运行所有测试
```

## 使用示例

### 1. JPEG图像水印处理
```bash
# 嵌入水印
curl -X POST "http://localhost:8080/api/watermark/embed?inputPath=images/photo.jpg&outputPath=images/watermarked_photo.jpg&watermarkText=COPYRIGHT2025"

# 提取水印
curl "http://localhost:8080/api/watermark/extract?filePath=images/watermarked_photo.jpg&watermarkLength=12"
```

### 2. PNG图像水印处理
```bash
# 嵌入水印
curl -X POST "http://localhost:8080/api/watermark/embed?inputPath=images/logo.png&outputPath=images/watermarked_logo.png&watermarkText=BRAND123"

# 提取水印
curl "http://localhost:8080/api/watermark/extract?filePath=images/watermarked_logo.png&watermarkLength=8"
```

### 3. TIFF遥感数据水印处理
```bash
# 嵌入水印
curl -X POST "http://localhost:8080/api/watermark/embed?inputPath=raster/satellite.tif&outputPath=raster/watermarked_satellite.tif&watermarkText=SATELLITE_DATA_2025"

# 提取水印
curl "http://localhost:8080/api/watermark/extract?filePath=raster/watermarked_satellite.tif&watermarkLength=18"
```

### 4. 检查格式支持
```bash
curl "http://localhost:8080/api/watermark/check-format?filePath=test.jpg"
curl "http://localhost:8080/api/watermark/formats"
```

## 算法原理

### DCT暗水印技术
1. **分块处理**: 将数据分成8x8块
2. **DCT变换**: 对每个块进行离散余弦变换
3. **中频嵌入**: 在中频DCT系数中嵌入水印位
4. **逆变换**: 进行逆DCT得到含水印的数据

### 针对不同格式的优化
- **栅格数据**: 直接在像素值上操作，保持地理信息完整性
- **普通图像**: 在YUV颜色空间的亮度通道操作，保持视觉质量

## 性能和限制

### 容量限制
- 水印容量 = (图像宽度/8) × (图像高度/8) 位
- 例如：512×512图像可嵌入 4096 位（512字符）

### 性能参考
- 256×256 TIFF: ~200ms
- 512×512 JPEG: ~400ms
- 1024×1024 PNG: ~800ms

### 建议
1. **水印长度**: 建议不超过图像容量的50%
2. **图像尺寸**: 最小建议64×64像素
3. **格式选择**: PNG无损格式水印效果最佳

## 错误处理

常见错误及解决方案：
- `文件不存在`: 检查文件路径
- `不支持的文件格式`: 使用支持的格式列表
- `水印数据过长`: 减少水印文本长度或使用更大的图像
- `库加载失败`: 检查OpenCV和GDAL库安装

## 技术特点

✅ **自动类型识别**: 无需手动指定文件类型  
✅ **统一API接口**: 两个核心接口处理所有格式  
✅ **向后兼容**: 保留原有专用接口  
✅ **鲁棒水印**: 对轻微压缩和处理具有抗性  
✅ **视觉无损**: 水印对图像视觉效果影响极小

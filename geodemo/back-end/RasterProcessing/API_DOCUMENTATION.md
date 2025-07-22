# 栅格处理服务API文档

## 概述
这个Spring Boot应用程序提供了基于GDAL和OpenCV的栅格数据处理功能，包括：
1. 栅格数据属性读取
2. 基于DCT的暗水印嵌入和提取

## 环境要求
- Java 17+
- Spring Boot 3.5.3
- GDAL 3.x
- OpenCV 4.12.0

## API端点

### 1. 栅格数据读取API

#### 1.1 获取栅格信息
```
GET /api/raster/info?filePath={文件路径}
```
**参数:**
- `filePath`: 栅格文件的路径

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

#### 1.2 获取像素值
```
GET /api/raster/pixels?filePath={文件路径}&bandIndex={波段}&x={X坐标}&y={Y坐标}&width={宽度}&height={高度}
```
**参数:**
- `filePath`: 栅格文件路径
- `bandIndex`: 波段索引（从1开始，默认1）
- `x`: 起始X坐标（默认0）
- `y`: 起始Y坐标（默认0）
- `width`: 读取宽度（默认10）
- `height`: 读取高度（默认10）

#### 1.3 获取统计信息
```
GET /api/raster/statistics?filePath={文件路径}&bandIndex={波段}
```
**响应示例:**
```json
{
  "min": 0.0,
  "max": 255.0,
  "mean": 127.5,
  "stddev": 73.9
}
```

### 2. 水印处理API

#### 2.1 嵌入DCT暗水印
```
POST /api/watermark/embed?inputPath={输入路径}&outputPath={输出路径}&watermarkText={水印文本}
```
**参数:**
- `inputPath`: 输入栅格文件路径
- `outputPath`: 输出栅格文件路径
- `watermarkText`: 要嵌入的水印文本

**响应示例:**
```json
{
  "success": true,
  "message": "水印嵌入成功",
  "outputPath": "testdata/watermarked_sample.tif",
  "processingTime": 1250
}
```

#### 2.2 提取暗水印
```
GET /api/watermark/extract?filePath={文件路径}&watermarkLength={水印长度}
```
**参数:**
- `filePath`: 包含水印的栅格文件路径
- `watermarkLength`: 预期水印文本长度（字符数）

**响应示例:**
```json
{
  "success": true,
  "message": "水印提取成功",
  "watermarkText": "TEST123"
}
```

### 3. 测试端点

#### 3.1 库环境测试
```
GET /test/gdal       # 测试GDAL功能
GET /test/opencv     # 测试OpenCV功能
```

#### 3.2 功能测试
```
GET /test/generate-sample    # 生成测试GeoTIFF文件
GET /test/raster-info       # 测试栅格信息读取
GET /test/watermark         # 测试水印嵌入和提取
GET /test/all              # 运行所有测试
```

## 水印算法说明

### DCT暗水印原理
1. **分块处理**: 将栅格数据分成8x8像素块
2. **DCT变换**: 对每个块进行离散余弦变换
3. **水印嵌入**: 在中频DCT系数中嵌入水印位
4. **逆变换**: 进行逆DCT得到含水印的数据
5. **水印提取**: 通过比较特定DCT系数的大小关系提取水印

### 特点
- **不可见性**: 水印嵌入在频域，对栅格数据视觉影响极小
- **鲁棒性**: 对轻微的数据处理具有一定抗性
- **可逆性**: 可以从含水印数据中提取原始水印

## 使用示例

### 1. 读取栅格信息
```bash
curl "http://localhost:8080/api/raster/info?filePath=testdata/sample.tif"
```

### 2. 嵌入水印
```bash
curl -X POST "http://localhost:8080/api/watermark/embed?inputPath=testdata/sample.tif&outputPath=testdata/watermarked.tif&watermarkText=COPYRIGHT2025"
```

### 3. 提取水印
```bash
curl "http://localhost:8080/api/watermark/extract?filePath=testdata/watermarked.tif&watermarkLength=12"
```

## 注意事项

1. **文件路径**: 所有文件路径都是相对于项目根目录的相对路径
2. **水印长度**: 水印嵌入容量受栅格数据大小限制，建议不超过数据块数量
3. **数据类型**: 目前支持Float32类型的栅格数据
4. **性能**: 水印处理时间与栅格数据大小成正比

## 错误处理

API会返回详细的错误信息，常见错误包括：
- 文件不存在或无法访问
- 栅格数据格式不支持
- 水印数据过长
- 库加载失败

所有错误都会在响应中包含具体的错误描述。

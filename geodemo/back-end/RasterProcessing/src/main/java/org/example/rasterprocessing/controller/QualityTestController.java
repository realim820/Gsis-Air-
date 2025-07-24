package org.example.rasterprocessing.controller;

import org.example.rasterprocessing.model.WatermarkResult;
import org.example.rasterprocessing.service.ImageWatermarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 图像质量测试控制器
 */
@RestController
@RequestMapping("/api/quality")
public class QualityTestController {

    @Autowired
    private ImageWatermarkService imageWatermarkService;

    /**
     * 测试图像质量优化 - 详细调试版本
     */
    @PostMapping("/test")
    public String testImageQuality() {
        try {
            // 测试路径
            String inputPath = "f:/Desktop/geo/Gsis-Air-/geodemo/back-end/RasterProcessing/testdata/summerbg.png";
            String outputPath = "f:/Desktop/geo/Gsis-Air-/geodemo/back-end/RasterProcessing/testdata/quality_test_output.png";
            
            StringBuilder result = new StringBuilder();
            result.append("=== 图像质量优化与水印准确性测试 ===\n");
            
            // 测试不同长度的水印
            String[] testWatermarks = {
                "test",           // 英文简单
                "测试",           // 中文简单
                "hello世界",      // 中英混合
                "水印测试123"     // 复杂混合
            };
            
            for (int i = 0; i < testWatermarks.length; i++) {
                String watermark = testWatermarks[i];
                String testOutputPath = outputPath.replace(".png", "_test" + (i+1) + ".png");
                
                result.append("\n--- 测试 ").append(i+1).append(" ---\n");
                result.append("原始水印: [").append(watermark).append("] (长度: ").append(watermark.length()).append(")\n");
                
                try {
                    // 嵌入水印
                    long embedStart = System.currentTimeMillis();
                    WatermarkResult embedResult = imageWatermarkService.embedWatermarkInImage(
                        inputPath, testOutputPath, watermark);
                    long embedTime = System.currentTimeMillis() - embedStart;
                    
                    if (!embedResult.isSuccess()) {
                        result.append("❌ 嵌入失败: ").append(embedResult.getMessage()).append("\n");
                        continue;
                    }
                    
                    result.append("✅ 嵌入成功 (耗时: ").append(embedTime).append("ms)\n");
                    
                    // 提取水印
                    long extractStart = System.currentTimeMillis();
                    String extractedWatermark = imageWatermarkService.extractWatermarkFromImage(
                        testOutputPath, watermark.length());
                    long extractTime = System.currentTimeMillis() - extractStart;
                    
                    result.append("提取结果: [").append(extractedWatermark).append("] (耗时: ").append(extractTime).append("ms)\n");
                    
                    // 准确性分析
                    boolean isAccurate = watermark.equals(extractedWatermark);
                    result.append("准确性: ").append(isAccurate ? "✅ 完全匹配" : "❌ 不匹配").append("\n");
                    
                    if (!isAccurate) {
                        // 字符级别的对比
                        result.append("字符对比: ");
                        int minLen = Math.min(watermark.length(), extractedWatermark.length());
                        for (int j = 0; j < minLen; j++) {
                            char orig = watermark.charAt(j);
                            char extr = extractedWatermark.charAt(j);
                            if (orig == extr) {
                                result.append("✓");
                            } else {
                                result.append("✗(").append(orig).append("→").append(extr).append(")");
                            }
                        }
                        result.append("\n");
                    }
                    
                } catch (Exception e) {
                    result.append("❌ 测试异常: ").append(e.getMessage()).append("\n");
                }
            }
            
            result.append("\n=== 配置信息 ===\n");
            result.append("水印强度: 25.0 (提高准确性)\n");
            result.append("最大修改: 30.0\n");
            result.append("重复编码: 5次 (增强纠错)\n");
            result.append("检测阈值: 5% (高敏感度)\n");
            result.append("投票阈值: 60% (严格投票)\n");
            result.append("DCT位置: 4个中频位置\n");
            result.append("质量保护: 智能块评估\n");
            
            return result.toString();
            
        } catch (Exception e) {
            return "测试失败: " + e.getMessage();
        }
    }

    /**
     * 测试不同水印强度的效果
     */
    @PostMapping("/strength-test")
    public String testWatermarkStrength() {
        try {
            String inputPath = "f:/Desktop/geo/Gsis-Air-/geodemo/back-end/RasterProcessing/testdata/summerbg.png";
            String watermark = "强度测试";
            
            StringBuilder result = new StringBuilder();
            result.append("=== 水印强度测试 ===\n");
            result.append("当前配置: WATERMARK_STRENGTH = 15.0, MAX_MODIFICATION = 20.0\n");
            result.append("优化策略: 温和修改 + 平滑区域跳过 + UTF-8重复编码\n\n");
            
            // 进行多次测试
            for (int i = 1; i <= 3; i++) {
                String outputPath = String.format("f:/Desktop/geo/Gsis-Air-/geodemo/back-end/RasterProcessing/testdata/strength_test_%d.png", i);
                
                WatermarkResult embedResult = imageWatermarkService.embedWatermarkInImage(
                    inputPath, outputPath, watermark);
                
                if (embedResult.isSuccess()) {
                    String extracted = imageWatermarkService.extractWatermarkFromImage(
                        outputPath, watermark.length());
                    
                    result.append("测试 ").append(i).append(": ");
                    result.append(watermark.equals(extracted) ? "✓ 成功" : "✗ 失败");
                    result.append(" (提取: ").append(extracted).append(")\n");
                } else {
                    result.append("测试 ").append(i).append(": ✗ 嵌入失败\n");
                }
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "强度测试失败: " + e.getMessage();
        }
    }
}

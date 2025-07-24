package org.example.rasterprocessing.controller;

import org.example.rasterprocessing.model.WatermarkResult;
import org.example.rasterprocessing.service.ImageWatermarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 水印修复测试控制器
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private ImageWatermarkService imageWatermarkService;

    /**
     * 调试水印嵌入和提取过程
     */
    @PostMapping("/watermark-debug")
    public String debugWatermark(
            @RequestParam String inputPath,
            @RequestParam String outputPath,
            @RequestParam String watermarkText) {
        
        StringBuilder result = new StringBuilder();
        result.append("=== 水印调试测试 ===\n");
        result.append("输入文件: ").append(inputPath).append("\n");
        result.append("输出文件: ").append(outputPath).append("\n");
        result.append("水印文本: [").append(watermarkText).append("] (长度: ").append(watermarkText.length()).append(")\n\n");
        
        try {
            // 第1步：嵌入水印
            result.append("步骤1: 嵌入水印\n");
            WatermarkResult embedResult = imageWatermarkService.embedWatermarkInImage(
                inputPath, outputPath, watermarkText);
            
            if (!embedResult.isSuccess()) {
                result.append("❌ 嵌入失败: ").append(embedResult.getMessage()).append("\n");
                return result.toString();
            }
            
            result.append("✅ 嵌入成功 (耗时: ").append(embedResult.getProcessingTime()).append("ms)\n");
            result.append("输出: ").append(embedResult.getOutputPath()).append("\n\n");
            
            // 第2步：提取水印
            result.append("步骤2: 提取水印\n");
            String extractedWatermark = imageWatermarkService.extractWatermarkFromImage(
                outputPath, watermarkText.length());
            
            result.append("提取结果: [").append(extractedWatermark).append("]\n");
            result.append("提取长度: ").append(extractedWatermark.length()).append("\n");
            
            // 第3步：对比分析
            result.append("\n步骤3: 准确性分析\n");
            boolean isExactMatch = watermarkText.equals(extractedWatermark);
            result.append("完全匹配: ").append(isExactMatch ? "✅ 是" : "❌ 否").append("\n");
            
            if (!isExactMatch) {
                result.append("字符对比:\n");
                int maxLen = Math.max(watermarkText.length(), extractedWatermark.length());
                for (int i = 0; i < maxLen; i++) {
                    char origChar = i < watermarkText.length() ? watermarkText.charAt(i) : '?';
                    char extrChar = i < extractedWatermark.length() ? extractedWatermark.charAt(i) : '?';
                    
                    String status = (origChar == extrChar) ? "✓" : "✗";
                    result.append(String.format("  位置%d: [%c] -> [%c] %s\n", i, origChar, extrChar, status));
                }
                
                // 尝试UTF-8字节分析
                result.append("\nUTF-8字节分析:\n");
                try {
                    byte[] origBytes = watermarkText.getBytes("UTF-8");
                    byte[] extrBytes = extractedWatermark.getBytes("UTF-8");
                    
                    result.append("原始字节数: ").append(origBytes.length).append("\n");
                    result.append("提取字节数: ").append(extrBytes.length).append("\n");
                    
                    int minBytes = Math.min(origBytes.length, extrBytes.length);
                    int matchingBytes = 0;
                    for (int i = 0; i < minBytes; i++) {
                        if (origBytes[i] == extrBytes[i]) {
                            matchingBytes++;
                        }
                    }
                    result.append("匹配字节数: ").append(matchingBytes).append("/").append(minBytes).append("\n");
                    
                } catch (Exception e) {
                    result.append("UTF-8分析失败: ").append(e.getMessage()).append("\n");
                }
            }
            
            return result.toString();
            
        } catch (Exception e) {
            result.append("❌ 测试异常: ").append(e.getMessage()).append("\n");
            result.append("异常类型: ").append(e.getClass().getSimpleName()).append("\n");
            if (e.getCause() != null) {
                result.append("根本原因: ").append(e.getCause().getMessage()).append("\n");
            }
            return result.toString();
        }
    }

    /**
     * 快速测试不同类型的水印
     */
    @PostMapping("/quick-test")
    public String quickTest() {
        String inputPath = "testdata/test.png";
        String outputPath = "testdata/debug_output.png";
        
        StringBuilder result = new StringBuilder();
        result.append("=== 快速水印测试 ===\n");
        
        String[] testCases = {
            "test",       // 简单英文
            "你好",       // 简单中文
            "hello",      // 稍长英文
            "测试123"     // 中英数字混合
        };
        
        for (int i = 0; i < testCases.length; i++) {
            String watermark = testCases[i];
            String testOutput = outputPath.replace(".png", "_case" + (i+1) + ".png");
            
            result.append("\n--- 测试案例 ").append(i+1).append(" ---\n");
            result.append("水印: [").append(watermark).append("]\n");
            
            try {
                // 嵌入
                WatermarkResult embedResult = imageWatermarkService.embedWatermarkInImage(
                    inputPath, testOutput, watermark);
                
                if (!embedResult.isSuccess()) {
                    result.append("❌ 嵌入失败\n");
                    continue;
                }
                
                // 提取
                String extracted = imageWatermarkService.extractWatermarkFromImage(
                    testOutput, watermark.length());
                
                result.append("提取: [").append(extracted).append("]\n");
                result.append("结果: ").append(watermark.equals(extracted) ? "✅ 成功" : "❌ 失败").append("\n");
                
            } catch (Exception e) {
                result.append("❌ 异常: ").append(e.getMessage()).append("\n");
            }
        }
        
        return result.toString();
    }
}

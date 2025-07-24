package org.example.rasterprocessing.controller;

import org.example.rasterprocessing.service.SimpleWatermarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 简化的水印控制器 - 专注核心功能测试
 */
@RestController
@RequestMapping("/api/simple")
public class SimpleWatermarkController {

    @Autowired
    private SimpleWatermarkService simpleWatermarkService;

    /**
     * 简化的水印嵌入
     */
    @PostMapping("/embed")
    public Map<String, Object> embedWatermark(
            @RequestParam String inputPath,
            @RequestParam String outputPath,
            @RequestParam String watermarkText) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            
            simpleWatermarkService.embedWatermark(inputPath, outputPath, watermarkText);
            
            long endTime = System.currentTimeMillis();
            
            response.put("success", true);
            response.put("message", "简化水印嵌入成功");
            response.put("outputPath", outputPath);
            response.put("processingTime", endTime - startTime);
            response.put("watermarkLength", watermarkText.length());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "嵌入失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 简化的水印提取
     */
    @GetMapping("/extract")
    public Map<String, Object> extractWatermark(
            @RequestParam String filePath,
            @RequestParam int watermarkLength) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            
            String extractedText = simpleWatermarkService.extractWatermark(filePath, watermarkLength);
            
            long endTime = System.currentTimeMillis();
            
            response.put("success", true);
            response.put("message", "简化水印提取成功");
            response.put("watermarkText", extractedText);
            response.put("processingTime", endTime - startTime);
            response.put("extractedLength", extractedText.length());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "提取失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 快速测试接口
     */
    @GetMapping("/test")
    public Map<String, Object> quickTest() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String[] testTexts = {
                "Hello",
                "你好",
                "CopyRight",
                "版权所有",
                "Test123",
                "测试456"
            };
            
            Map<String, Object> results = new HashMap<>();
            
            for (String text : testTexts) {
                try {
                    // 嵌入
                    String inputPath = "testdata/test2.png";
                    String outputPath = "testdata/simple_test_" + text.hashCode() + ".png";
                    
                    simpleWatermarkService.embedWatermark(inputPath, outputPath, text);
                    
                    // 提取
                    String extracted = simpleWatermarkService.extractWatermark(outputPath, text.length());
                    
                    Map<String, Object> testResult = new HashMap<>();
                    testResult.put("original", text);
                    testResult.put("extracted", extracted);
                    testResult.put("match", text.equals(extracted));
                    testResult.put("utf8Match", 
                        text.getBytes("UTF-8").length == extracted.getBytes("UTF-8").length);
                    
                    results.put("test_" + text, testResult);
                    
                } catch (Exception e) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("original", text);
                    errorResult.put("error", e.getMessage());
                    results.put("test_" + text + "_ERROR", errorResult);
                }
            }
            
            response.put("success", true);
            response.put("message", "简化测试完成");
            response.put("results", results);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "测试失败: " + e.getMessage());
        }
        
        return response;
    }
}

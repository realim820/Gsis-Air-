package org.example.rasterprocessing.controller;

import org.example.rasterprocessing.service.SimpleWatermarkService;
import org.example.rasterprocessing.service.SimpleRasterWatermarkService;
import org.example.rasterprocessing.util.FileTypeDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一的水印控制器 - 自动识别文件类型并选择合适的处理方式
 */
@RestController
@RequestMapping("/api/watermark")
public class SimpleWatermarkController {

    @Autowired
    private SimpleWatermarkService imageWatermarkService;
    
    @Autowired
    private SimpleRasterWatermarkService rasterWatermarkService;

    /**
     * 统一的水印嵌入接口 - 自动识别文件类型
     */
    @PostMapping("/embed")
    public Map<String, Object> embedWatermark(
            @RequestParam String inputPath,
            @RequestParam String outputPath,
            @RequestParam String watermarkText) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查文件格式是否受支持
            if (!FileTypeDetector.isSupportedFormat(inputPath)) {
                response.put("success", false);
                response.put("message", "不支持的文件格式: " + inputPath);
                response.put("supportedFormats", FileTypeDetector.getSupportedFormats());
                return response;
            }
            
            long startTime = System.currentTimeMillis();
            String processingType;
            
            // 根据文件类型选择处理方式
            if (FileTypeDetector.isImageFormat(inputPath)) {
                // 处理普通图像格式
                imageWatermarkService.embedWatermark(inputPath, outputPath, watermarkText);
                processingType = "图像水印处理";
            } else if (FileTypeDetector.isRasterFormat(inputPath)) {
                // 处理栅格数据格式
                rasterWatermarkService.embedWatermark(inputPath, outputPath, watermarkText);
                processingType = "栅格水印处理";
            } else {
                response.put("success", false);
                response.put("message", "文件类型识别失败");
                return response;
            }
            
            long endTime = System.currentTimeMillis();
            
            response.put("success", true);
            response.put("message", processingType + "嵌入成功");
            response.put("outputPath", outputPath);
            response.put("processingTime", endTime - startTime);
            response.put("watermarkLength", watermarkText.length());
            response.put("fileType", FileTypeDetector.getFileTypeDescription(inputPath));
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "嵌入失败: " + e.getMessage());
            response.put("fileType", FileTypeDetector.getFileTypeDescription(inputPath));
        }
        
        return response;
    }

    /**
     * 统一的水印提取接口 - 自动识别文件类型
     */
    @GetMapping("/extract")
    public Map<String, Object> extractWatermark(
            @RequestParam String filePath,
            @RequestParam int watermarkLength) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查文件格式是否受支持
            if (!FileTypeDetector.isSupportedFormat(filePath)) {
                response.put("success", false);
                response.put("message", "不支持的文件格式: " + filePath);
                response.put("supportedFormats", FileTypeDetector.getSupportedFormats());
                return response;
            }
            
            long startTime = System.currentTimeMillis();
            String extractedText;
            String processingType;
            
            // 根据文件类型选择处理方式
            if (FileTypeDetector.isImageFormat(filePath)) {
                // 处理普通图像格式
                extractedText = imageWatermarkService.extractWatermark(filePath, watermarkLength);
                processingType = "图像水印提取";
            } else if (FileTypeDetector.isRasterFormat(filePath)) {
                // 处理栅格数据格式
                extractedText = rasterWatermarkService.extractWatermark(filePath, watermarkLength);
                processingType = "栅格水印提取";
            } else {
                response.put("success", false);
                response.put("message", "文件类型识别失败");
                return response;
            }
            
            long endTime = System.currentTimeMillis();
            
            response.put("success", true);
            response.put("message", processingType + "成功");
            response.put("watermarkText", extractedText);
            response.put("processingTime", endTime - startTime);
            response.put("extractedLength", extractedText.length());
            response.put("fileType", FileTypeDetector.getFileTypeDescription(filePath));
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "提取失败: " + e.getMessage());
            response.put("fileType", FileTypeDetector.getFileTypeDescription(filePath));
        }
        
        return response;
    }

    /**
     * 检查文件格式是否受支持
     */
    @GetMapping("/check-format")
    public Map<String, Object> checkFormat(@RequestParam String filePath) {
        Map<String, Object> response = new HashMap<>();
        
        boolean supported = FileTypeDetector.isSupportedFormat(filePath);
        response.put("supported", supported);
        response.put("fileType", FileTypeDetector.getFileTypeDescription(filePath));
        response.put("message", supported ? "文件格式受支持" : "文件格式不受支持");
        
        if (!supported) {
            response.put("supportedFormats", FileTypeDetector.getSupportedFormats());
        }
        
        return response;
    }

    /**
     * 获取支持的格式列表
     */
    @GetMapping("/formats")
    public String getSupportedFormats() {
        return FileTypeDetector.getSupportedFormats();
    }

    /**
     * 快速测试接口 - 测试多种文本和格式
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
                    // 测试PNG图像格式
                    String inputPath = "testdata/test2.png";
                    String outputPath = "testdata/test_" + text.hashCode() + ".png";
                    
                    long embedStart = System.currentTimeMillis();
                    imageWatermarkService.embedWatermark(inputPath, outputPath, text);
                    long embedTime = System.currentTimeMillis() - embedStart;
                    
                    long extractStart = System.currentTimeMillis();
                    String extracted = imageWatermarkService.extractWatermark(outputPath, text.length());
                    long extractTime = System.currentTimeMillis() - extractStart;
                    
                    Map<String, Object> testResult = new HashMap<>();
                    testResult.put("original", text);
                    testResult.put("extracted", extracted);
                    testResult.put("match", text.equals(extracted));
                    testResult.put("embedTime", embedTime);
                    testResult.put("extractTime", extractTime);
                    testResult.put("fileType", "PNG图像");
                    
                    results.put("test_" + text, testResult);
                    
                } catch (Exception e) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("original", text);
                    errorResult.put("error", e.getMessage());
                    results.put("test_" + text + "_ERROR", errorResult);
                }
            }
            
            response.put("success", true);
            response.put("message", "快速测试完成");
            response.put("results", results);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "测试失败: " + e.getMessage());
        }
        
        return response;
    }
}

package org.example.rasterprocessing.controller;

import org.example.rasterprocessing.model.WatermarkResult;
import org.example.rasterprocessing.service.UniversalWatermarkService;
import org.example.rasterprocessing.service.WatermarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 通用水印处理控制器 - 支持栅格数据和普通图像
 */
@RestController
@RequestMapping("/api/watermark")
public class WatermarkController {

    @Autowired
    private UniversalWatermarkService universalWatermarkService;

    @Autowired
    private WatermarkService rasterWatermarkService; // 保留原有的栅格专用服务

    /**
     * 通用水印嵌入接口 - 自动判断文件类型（TIFF遥感数据 或 JPEG/PNG普通图像）
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param watermarkText 水印文本
     * @return 处理结果
     */
    @PostMapping("/embed")
    public ResponseEntity<WatermarkResult> embedWatermark(
            @RequestParam String inputPath,
            @RequestParam String outputPath,
            @RequestParam String watermarkText) {
        try {
            WatermarkResult result = universalWatermarkService.embedWatermark(inputPath, outputPath, watermarkText);
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            WatermarkResult errorResult = new WatermarkResult(false, "水印嵌入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 通用水印提取接口 - 自动判断文件类型（TIFF遥感数据 或 JPEG/PNG普通图像）
     * @param filePath 包含水印的文件路径
     * @param watermarkLength 预期水印长度（字符数）
     * @return 提取的水印文本
     */
    @GetMapping("/extract")
    public ResponseEntity<?> extractWatermark(
            @RequestParam String filePath,
            @RequestParam int watermarkLength) {
        try {
            String watermarkText = universalWatermarkService.extractWatermark(filePath, watermarkLength);
            return ResponseEntity.ok().body(new WatermarkExtractionResult(true, "水印提取成功", watermarkText));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new WatermarkExtractionResult(false, "水印提取失败: " + e.getMessage(), null));
        }
    }

    /**
     * 获取支持的文件格式
     * @return 支持的格式列表
     */
    @GetMapping("/formats")
    public ResponseEntity<String> getSupportedFormats() {
        String formats = universalWatermarkService.getSupportedFormats();
        return ResponseEntity.ok(formats);
    }

    /**
     * 检查文件格式是否受支持
     * @param filePath 文件路径
     * @return 是否支持
     */
    @GetMapping("/check-format")
    public ResponseEntity<?> checkFormat(@RequestParam String filePath) {
        boolean supported = universalWatermarkService.isFormatSupported(filePath);
        return ResponseEntity.ok().body(new FormatCheckResult(supported, 
            supported ? "文件格式受支持" : "文件格式不受支持"));
    }

    // ========== 以下为原有的栅格专用接口，保持向后兼容 ==========

    /**
     * 在栅格数据中嵌入DCT暗水印（专用于栅格数据）
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param watermarkText 水印文本
     * @return 处理结果
     */
    @PostMapping("/embed-raster")
    public ResponseEntity<WatermarkResult> embedRasterWatermark(
            @RequestParam String inputPath,
            @RequestParam String outputPath,
            @RequestParam String watermarkText) {
        try {
            WatermarkResult result = rasterWatermarkService.embedWatermark(inputPath, outputPath, watermarkText);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            WatermarkResult errorResult = new WatermarkResult(false, "栅格水印嵌入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 从栅格数据中提取暗水印（专用于栅格数据）
     * @param filePath 包含水印的文件路径
     * @param watermarkLength 预期水印长度（字符数）
     * @return 提取的水印文本
     */
    @GetMapping("/extract-raster")
    public ResponseEntity<?> extractRasterWatermark(
            @RequestParam String filePath,
            @RequestParam int watermarkLength) {
        try {
            String watermarkText = rasterWatermarkService.extractWatermark(filePath, watermarkLength);
            return ResponseEntity.ok().body(new WatermarkExtractionResult(true, "栅格水印提取成功", watermarkText));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new WatermarkExtractionResult(false, "栅格水印提取失败: " + e.getMessage(), null));
        }
    }

    /**
     * 水印提取结果内部类
     */
    public static class WatermarkExtractionResult {
        private boolean success;
        private String message;
        private String watermarkText;

        public WatermarkExtractionResult(boolean success, String message, String watermarkText) {
            this.success = success;
            this.message = message;
            this.watermarkText = watermarkText;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getWatermarkText() { return watermarkText; }
    }

    /**
     * 格式检查结果内部类
     */
    public static class FormatCheckResult {
        private boolean supported;
        private String message;

        public FormatCheckResult(boolean supported, String message) {
            this.supported = supported;
            this.message = message;
        }

        // Getters
        public boolean isSupported() { return supported; }
        public String getMessage() { return message; }
    }
}

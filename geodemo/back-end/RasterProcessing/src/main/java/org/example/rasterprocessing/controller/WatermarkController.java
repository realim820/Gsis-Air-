package org.example.rasterprocessing.controller;

import org.example.rasterprocessing.model.WatermarkResult;
import org.example.rasterprocessing.service.WatermarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 水印处理控制器
 */
@RestController
@RequestMapping("/api/watermark")
public class WatermarkController {

    @Autowired
    private WatermarkService watermarkService;

    /**
     * 在栅格数据中嵌入DCT暗水印
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
            WatermarkResult result = watermarkService.embedWatermark(inputPath, outputPath, watermarkText);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            WatermarkResult errorResult = new WatermarkResult(false, "水印嵌入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 从栅格数据中提取暗水印
     * @param filePath 包含水印的文件路径
     * @param watermarkLength 预期水印长度（字符数）
     * @return 提取的水印文本
     */
    @GetMapping("/extract")
    public ResponseEntity<?> extractWatermark(
            @RequestParam String filePath,
            @RequestParam int watermarkLength) {
        try {
            String watermarkText = watermarkService.extractWatermark(filePath, watermarkLength);
            return ResponseEntity.ok().body(new WatermarkExtractionResult(true, "水印提取成功", watermarkText));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new WatermarkExtractionResult(false, "水印提取失败: " + e.getMessage(), null));
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
}

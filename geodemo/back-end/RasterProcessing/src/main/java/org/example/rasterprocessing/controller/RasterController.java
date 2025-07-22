package org.example.rasterprocessing.controller;

import org.example.rasterprocessing.model.RasterInfo;
import org.example.rasterprocessing.service.RasterReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 栅格数据读取控制器
 */
@RestController
@RequestMapping("/api/raster")
public class RasterController {

    @Autowired
    private RasterReaderService rasterReaderService;

    /**
     * 获取栅格数据的基本信息
     * @param filePath 文件路径
     * @return 栅格信息
     */
    @GetMapping("/info")
    public ResponseEntity<?> getRasterInfo(@RequestParam String filePath) {
        try {
            RasterInfo rasterInfo = rasterReaderService.readRasterInfo(filePath);
            return ResponseEntity.ok(rasterInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("错误: " + e.getMessage());
        }
    }

    /**
     * 获取栅格数据的像素值
     * @param filePath 文件路径
     * @param bandIndex 波段索引（从1开始）
     * @param x 起始X坐标
     * @param y 起始Y坐标
     * @param width 读取宽度
     * @param height 读取高度
     * @return 像素值数组
     */
    @GetMapping("/pixels")
    public ResponseEntity<?> getPixelValues(
            @RequestParam String filePath,
            @RequestParam(defaultValue = "1") int bandIndex,
            @RequestParam(defaultValue = "0") int x,
            @RequestParam(defaultValue = "0") int y,
            @RequestParam(defaultValue = "10") int width,
            @RequestParam(defaultValue = "10") int height) {
        try {
            double[] pixels = rasterReaderService.readPixelValues(filePath, bandIndex, x, y, width, height);
            return ResponseEntity.ok(pixels);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("错误: " + e.getMessage());
        }
    }

    /**
     * 获取栅格数据的统计信息
     * @param filePath 文件路径
     * @param bandIndex 波段索引（从1开始）
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getRasterStatistics(
            @RequestParam String filePath,
            @RequestParam(defaultValue = "1") int bandIndex) {
        try {
            Map<String, Double> statistics = rasterReaderService.getRasterStatistics(filePath, bandIndex);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("错误: " + e.getMessage());
        }
    }
}

package org.example.rasterprocessing.service;

import org.example.rasterprocessing.model.WatermarkResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 通用水印管理服务 - 自动判断文件类型并选择合适的处理方式
 */
@Service
public class UniversalWatermarkService {

    @Autowired
    private WatermarkService rasterWatermarkService; // 栅格数据水印服务

    @Autowired
    private ImageWatermarkService imageWatermarkService; // 普通图像水印服务

    // 支持的栅格数据格式
    private static final Set<String> RASTER_FORMATS = new HashSet<>(Arrays.asList(
            "tif", "tiff", "geotiff", "img", "hdf", "nc", "grib", "jp2"
    ));

    // 支持的普通图像格式
    private static final Set<String> IMAGE_FORMATS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "bmp", "gif"
    ));

    /**
     * 通用水印嵌入接口 - 自动判断文件类型
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param watermarkText 水印文本
     * @return 水印嵌入结果
     */
    public WatermarkResult embedWatermark(String inputPath, String outputPath, String watermarkText) {
        try {
            // 检查输入文件是否存在
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                return new WatermarkResult(false, "输入文件不存在: " + inputPath);
            }

            // 判断文件类型
            FileType fileType = determineFileType(inputPath);
            
            switch (fileType) {
                case RASTER:
                    System.out.println("检测到栅格数据格式，使用栅格水印算法...");
                    return rasterWatermarkService.embedWatermark(inputPath, outputPath, watermarkText);
                    
                case IMAGE:
                    System.out.println("检测到普通图像格式，使用图像水印算法...");
                    return imageWatermarkService.embedWatermarkInImage(inputPath, outputPath, watermarkText);
                    
                default:
                    return new WatermarkResult(false, "不支持的文件格式: " + getFileExtension(inputPath));
            }

        } catch (Exception e) {
            return new WatermarkResult(false, "水印嵌入过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 通用水印提取接口 - 自动判断文件类型
     * @param filePath 包含水印的文件路径
     * @param watermarkLength 预期水印长度（字符数）
     * @return 提取的水印文本
     */
    public String extractWatermark(String filePath, int watermarkLength) {
        try {
            // 检查文件是否存在
            File file = new File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("文件不存在: " + filePath);
            }

            // 判断文件类型
            FileType fileType = determineFileType(filePath);
            
            switch (fileType) {
                case RASTER:
                    System.out.println("检测到栅格数据格式，使用栅格水印提取算法...");
                    return rasterWatermarkService.extractWatermark(filePath, watermarkLength);
                    
                case IMAGE:
                    System.out.println("检测到普通图像格式，使用图像水印提取算法...");
                    return imageWatermarkService.extractWatermarkFromImage(filePath, watermarkLength);
                    
                default:
                    throw new RuntimeException("不支持的文件格式: " + getFileExtension(filePath));
            }

        } catch (Exception e) {
            throw new RuntimeException("水印提取过程中发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 判断文件类型
     * @param filePath 文件路径
     * @return 文件类型枚举
     */
    private FileType determineFileType(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        
        if (RASTER_FORMATS.contains(extension)) {
            return FileType.RASTER;
        } else if (IMAGE_FORMATS.contains(extension)) {
            return FileType.IMAGE;
        } else {
            return FileType.UNSUPPORTED;
        }
    }

    /**
     * 获取文件扩展名
     * @param filePath 文件路径
     * @return 文件扩展名（不包含点号）
     */
    private String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filePath.length() - 1) {
            return "";
        }
        
        return filePath.substring(lastDotIndex + 1);
    }

    /**
     * 获取支持的文件格式信息
     * @return 支持格式的描述
     */
    public String getSupportedFormats() {
        StringBuilder sb = new StringBuilder();
        sb.append("支持的文件格式:\n");
        sb.append("栅格数据: ").append(String.join(", ", RASTER_FORMATS)).append("\n");
        sb.append("普通图像: ").append(String.join(", ", IMAGE_FORMATS));
        return sb.toString();
    }

    /**
     * 检查文件格式是否受支持
     * @param filePath 文件路径
     * @return 是否支持
     */
    public boolean isFormatSupported(String filePath) {
        return determineFileType(filePath) != FileType.UNSUPPORTED;
    }

    /**
     * 文件类型枚举
     */
    private enum FileType {
        RASTER,      // 栅格数据
        IMAGE,       // 普通图像
        UNSUPPORTED  // 不支持的格式
    }
}

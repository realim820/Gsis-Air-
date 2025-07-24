package org.example.rasterprocessing.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 文件类型检测工具
 */
public class FileTypeDetector {
    
    // 栅格数据格式
    private static final List<String> RASTER_FORMATS = Arrays.asList(
        "tif", "tiff", "geotiff", "img", "hdf", "nc", "grib", "jp2"
    );
    
    // 普通图像格式  
    private static final List<String> IMAGE_FORMATS = Arrays.asList(
        "jpg", "jpeg", "png", "bmp", "gif"
    );
    
    /**
     * 检测是否为栅格数据格式
     */
    public static boolean isRasterFormat(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        return RASTER_FORMATS.contains(extension);
    }
    
    /**
     * 检测是否为普通图像格式
     */
    public static boolean isImageFormat(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        return IMAGE_FORMATS.contains(extension);
    }
    
    /**
     * 检测文件格式是否被支持
     */
    public static boolean isSupportedFormat(String filePath) {
        return isRasterFormat(filePath) || isImageFormat(filePath);
    }
    
    /**
     * 获取文件类型描述
     */
    public static String getFileTypeDescription(String filePath) {
        if (isRasterFormat(filePath)) {
            return "栅格数据";
        } else if (isImageFormat(filePath)) {
            return "普通图像";
        } else {
            return "不支持的格式";
        }
    }
    
    /**
     * 获取支持的格式列表
     */
    public static String getSupportedFormats() {
        return "支持的文件格式:\n" +
               "栅格数据: " + String.join(", ", RASTER_FORMATS) + "\n" +
               "普通图像: " + String.join(", ", IMAGE_FORMATS);
    }
    
    /**
     * 提取文件扩展名
     */
    private static String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        
        File file = new File(filePath);
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        
        return "";
    }
}

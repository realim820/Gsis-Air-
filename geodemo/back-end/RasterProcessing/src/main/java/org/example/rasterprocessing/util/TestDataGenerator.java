package org.example.rasterprocessing.util;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

/**
 * 测试数据生成工具
 */
public class TestDataGenerator {
    
    /**
     * 生成测试用的GeoTIFF文件
     */
    public static void generateTestGeoTiff(String outputPath, int width, int height) {
        try {
            // 确保GDAL已初始化
            gdal.AllRegister();
            
            // 创建GeoTIFF驱动
            Driver driver = gdal.GetDriverByName("GTiff");
            if (driver == null) {
                throw new RuntimeException("无法获取GeoTIFF驱动");
            }
            
            // 创建数据集
            Dataset dataset = driver.Create(outputPath, width, height, 3, gdalconst.GDT_Float32);
            if (dataset == null) {
                throw new RuntimeException("无法创建数据集");
            }
            
            // 设置地理变换参数 (左上角坐标、像素尺寸等)
            double[] geoTransform = {
                120.0,  // 左上角X坐标
                0.001,  // 像素宽度
                0.0,    // 旋转参数
                40.0,   // 左上角Y坐标
                0.0,    // 旋转参数
                -0.001  // 像素高度（负值表示北向上）
            };
            dataset.SetGeoTransform(geoTransform);
            
            // 设置投影（WGS84地理坐标系）
            dataset.SetProjection("GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]]");
            
            // 生成测试数据并写入各波段
            for (int bandIndex = 1; bandIndex <= 3; bandIndex++) {
                float[] data = new float[width * height];
                
                // 生成不同模式的测试数据
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int index = y * width + x;
                        switch (bandIndex) {
                            case 1: // 红色波段 - 渐变模式
                                data[index] = (float) ((x + y) % 256);
                                break;
                            case 2: // 绿色波段 - 棋盘模式
                                data[index] = (float) (((x / 8) + (y / 8)) % 2 * 255);
                                break;
                            case 3: // 蓝色波段 - 随机模式
                                data[index] = (float) (Math.random() * 255);
                                break;
                        }
                    }
                }
                
                // 写入波段数据
                dataset.GetRasterBand(bandIndex).WriteRaster(0, 0, width, height, width, height, gdalconst.GDT_Float32, data);
                dataset.GetRasterBand(bandIndex).FlushCache();
            }
            
            // 关闭数据集
            dataset.FlushCache();
            dataset.delete();
            
            System.out.println("测试GeoTIFF文件生成成功: " + outputPath);
            
        } catch (Exception e) {
            System.err.println("生成测试文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        // 生成测试文件
        generateTestGeoTiff("testdata/sample.tif", 256, 256);
    }
}

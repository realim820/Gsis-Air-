package org.example.rasterprocessing.service;

import org.example.rasterprocessing.model.RasterInfo;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 栅格数据读取服务
 */
@Service
public class RasterReaderService {

    /**
     * 读取栅格数据的详细信息
     * @param filePath 栅格文件路径
     * @return 栅格信息对象
     */
    public RasterInfo readRasterInfo(String filePath) {
        Dataset dataset = null;
        try {
            // 打开栅格数据集
            dataset = gdal.Open(filePath, gdalconst.GA_ReadOnly);
            if (dataset == null) {
                throw new RuntimeException("无法打开栅格文件: " + filePath);
            }

            RasterInfo rasterInfo = new RasterInfo();
            
            // 基本信息
            rasterInfo.setFileName(filePath);
            rasterInfo.setWidth(dataset.getRasterXSize());
            rasterInfo.setHeight(dataset.getRasterYSize());
            rasterInfo.setBandCount(dataset.getRasterCount());
            rasterInfo.setDriver(dataset.GetDriver().getShortName());
            
            // 投影信息
            String projection = dataset.GetProjection();
            if (projection != null && !projection.isEmpty()) {
                rasterInfo.setProjection(projection);
            }
            
            // 地理变换参数
            double[] geoTransform = new double[6];
            dataset.GetGeoTransform(geoTransform);
            rasterInfo.setGeoTransform(geoTransform);
            
            // 波段信息（使用第一个波段的信息作为代表）
            if (dataset.getRasterCount() > 0) {
                Band band = dataset.GetRasterBand(1);
                if (band != null) {
                    rasterInfo.setDataType(gdal.GetDataTypeName(band.getDataType()));
                    
                    // NoData值
                    Double[] noDataValue = new Double[1];
                    band.GetNoDataValue(noDataValue);
                    if (noDataValue[0] != null) {
                        rasterInfo.setNoDataValue(noDataValue[0]);
                    }
                }
            }
            
            // 元数据
            Map<String, String> metadata = new HashMap<>();
            java.util.Vector<String> metadataList = dataset.GetMetadata_List("");
            if (metadataList != null) {
                for (String item : metadataList) {
                    String[] parts = item.split("=", 2);
                    if (parts.length == 2) {
                        metadata.put(parts[0], parts[1]);
                    }
                }
            }
            rasterInfo.setMetadata(metadata);
            
            return rasterInfo;
            
        } catch (Exception e) {
            throw new RuntimeException("读取栅格数据失败: " + e.getMessage(), e);
        } finally {
            if (dataset != null) {
                dataset.delete();
            }
        }
    }

    /**
     * 读取栅格数据的像素值
     * @param filePath 文件路径
     * @param bandIndex 波段索引（从1开始）
     * @param x 起始X坐标
     * @param y 起始Y坐标
     * @param width 读取宽度
     * @param height 读取高度
     * @return 像素值数组
     */
    public double[] readPixelValues(String filePath, int bandIndex, int x, int y, int width, int height) {
        Dataset dataset = null;
        try {
            dataset = gdal.Open(filePath, gdalconst.GA_ReadOnly);
            if (dataset == null) {
                throw new RuntimeException("无法打开栅格文件: " + filePath);
            }

            Band band = dataset.GetRasterBand(bandIndex);
            if (band == null) {
                throw new RuntimeException("无法获取波段: " + bandIndex);
            }

            double[] buffer = new double[width * height];
            int result = band.ReadRaster(x, y, width, height, width, height, 
                                       gdalconst.GDT_Float64, buffer);
            
            if (result != gdalconst.CE_None) {
                throw new RuntimeException("读取像素数据失败");
            }

            return buffer;
            
        } catch (Exception e) {
            throw new RuntimeException("读取像素值失败: " + e.getMessage(), e);
        } finally {
            if (dataset != null) {
                dataset.delete();
            }
        }
    }

    /**
     * 获取栅格数据的统计信息
     * @param filePath 文件路径
     * @param bandIndex 波段索引
     * @return 统计信息映射
     */
    public Map<String, Double> getRasterStatistics(String filePath, int bandIndex) {
        Dataset dataset = null;
        try {
            dataset = gdal.Open(filePath, gdalconst.GA_ReadOnly);
            if (dataset == null) {
                throw new RuntimeException("无法打开栅格文件: " + filePath);
            }

            Band band = dataset.GetRasterBand(bandIndex);
            if (band == null) {
                throw new RuntimeException("无法获取波段: " + bandIndex);
            }

            // 计算统计信息
            double[] min = new double[1];
            double[] max = new double[1];
            double[] mean = new double[1];
            double[] stddev = new double[1];
            
            band.GetStatistics(0, 1, min, max, mean, stddev);

            Map<String, Double> statistics = new HashMap<>();
            statistics.put("min", min[0]);
            statistics.put("max", max[0]);
            statistics.put("mean", mean[0]);
            statistics.put("stddev", stddev[0]);

            return statistics;
            
        } catch (Exception e) {
            throw new RuntimeException("获取统计信息失败: " + e.getMessage(), e);
        } finally {
            if (dataset != null) {
                dataset.delete();
            }
        }
    }
}

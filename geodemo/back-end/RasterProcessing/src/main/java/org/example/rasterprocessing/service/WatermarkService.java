package org.example.rasterprocessing.service;

import org.example.rasterprocessing.model.WatermarkResult;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于DCT的栅格数据暗水印嵌入服务
 */
@Service
public class WatermarkService {

    // DCT块大小
    private static final int DCT_BLOCK_SIZE = 8;
    // 水印强度
    private static final double WATERMARK_STRENGTH = 0.1;

    /**
     * 在栅格数据中嵌入暗水印
     * @param inputPath 输入栅格文件路径
     * @param outputPath 输出栅格文件路径
     * @param watermarkData 水印数据（二进制串）
     * @return 水印嵌入结果
     */
    public WatermarkResult embedWatermark(String inputPath, String outputPath, String watermarkData) {
        long startTime = System.currentTimeMillis();
        Dataset inputDataset = null;
        Dataset outputDataset = null;

        try {
            // 打开输入数据集
            inputDataset = gdal.Open(inputPath, gdalconst.GA_ReadOnly);
            if (inputDataset == null) {
                return new WatermarkResult(false, "无法打开输入文件: " + inputPath);
            }

            // 获取数据集信息
            int width = inputDataset.getRasterXSize();
            int height = inputDataset.getRasterYSize();
            int bandCount = inputDataset.getRasterCount();

            // 创建输出数据集
            Driver driver = gdal.GetDriverByName("GTiff");
            outputDataset = driver.Create(outputPath, width, height, bandCount, gdalconst.GDT_Float32);
            if (outputDataset == null) {
                return new WatermarkResult(false, "无法创建输出文件: " + outputPath);
            }

            // 复制地理信息
            double[] geoTransform = new double[6];
            inputDataset.GetGeoTransform(geoTransform);
            outputDataset.SetGeoTransform(geoTransform);
            outputDataset.SetProjection(inputDataset.GetProjection());

            // 对每个波段进行水印嵌入
            for (int bandIndex = 1; bandIndex <= bandCount; bandIndex++) {
                Band inputBand = inputDataset.GetRasterBand(bandIndex);
                Band outputBand = outputDataset.GetRasterBand(bandIndex);

                // 读取波段数据
                float[] data = new float[width * height];
                inputBand.ReadRaster(0, 0, width, height, width, height, gdalconst.GDT_Float32, data);

                // 嵌入水印
                float[] watermarkedData = embedWatermarkInBand(data, width, height, watermarkData);

                // 写入输出波段
                outputBand.WriteRaster(0, 0, width, height, width, height, gdalconst.GDT_Float32, watermarkedData);
                outputBand.FlushCache();
            }

            long processingTime = System.currentTimeMillis() - startTime;
            return new WatermarkResult(true, "水印嵌入成功", outputPath, processingTime);

        } catch (Exception e) {
            return new WatermarkResult(false, "水印嵌入失败: " + e.getMessage());
        } finally {
            if (outputDataset != null) {
                outputDataset.FlushCache();
                outputDataset.delete();
            }
            if (inputDataset != null) {
                inputDataset.delete();
            }
        }
    }

    /**
     * 在单个波段中嵌入水印
     */
    private float[] embedWatermarkInBand(float[] data, int width, int height, String watermarkData) {
        // 将数据转换为Mat对象
        Mat imageMat = new Mat(height, width, CvType.CV_32F);
        imageMat.put(0, 0, data);

        // 计算可嵌入的块数量
        int blocksX = width / DCT_BLOCK_SIZE;
        int blocksY = height / DCT_BLOCK_SIZE;
        int totalBlocks = blocksX * blocksY;

        // 将水印数据转换为二进制位
        List<Integer> watermarkBits = convertToBinary(watermarkData);
        
        // 确保有足够的块来嵌入水印
        if (watermarkBits.size() > totalBlocks) {
            throw new RuntimeException("水印数据过长，超出了可嵌入的容量");
        }

        Mat watermarkedMat = imageMat.clone();

        // 对每个8x8块进行DCT变换并嵌入水印
        int bitIndex = 0;
        for (int blockY = 0; blockY < blocksY && bitIndex < watermarkBits.size(); blockY++) {
            for (int blockX = 0; blockX < blocksX && bitIndex < watermarkBits.size(); blockX++) {
                // 提取8x8块
                Rect blockRect = new Rect(blockX * DCT_BLOCK_SIZE, blockY * DCT_BLOCK_SIZE, 
                                        DCT_BLOCK_SIZE, DCT_BLOCK_SIZE);
                Mat block = new Mat(watermarkedMat, blockRect);

                // 进行DCT变换
                Mat dctBlock = new Mat();
                Core.dct(block, dctBlock);

                // 嵌入水印位
                int bit = watermarkBits.get(bitIndex++);
                embedBitInDCTBlock(dctBlock, bit);

                // 进行逆DCT变换
                Mat idctBlock = new Mat();
                Core.idct(dctBlock, idctBlock);

                // 将处理后的块复制回原图像
                idctBlock.copyTo(block);

                // 释放临时Mat对象
                dctBlock.release();
                idctBlock.release();
            }
        }

        // 将Mat转换回float数组
        float[] result = new float[width * height];
        watermarkedMat.get(0, 0, result);
        
        // 释放Mat对象
        imageMat.release();
        watermarkedMat.release();

        return result;
    }

    /**
     * 在DCT块中嵌入单个二进制位
     */
    private void embedBitInDCTBlock(Mat dctBlock, int bit) {
        // 选择中频系数位置进行嵌入（避免低频和高频）
        int[] positions = {
            3, 2,  // (3,2)
            2, 3,  // (2,3)
            4, 1,  // (4,1)
            1, 4   // (1,4)
        };

        // 获取选定位置的DCT系数
        double[] coeff1 = dctBlock.get(positions[0], positions[1]);
        double[] coeff2 = dctBlock.get(positions[2], positions[3]);

        if (coeff1 != null && coeff2 != null && coeff1.length > 0 && coeff2.length > 0) {
            double c1 = coeff1[0];
            double c2 = coeff2[0];

            // 根据水印位调整系数关系
            if (bit == 1) {
                // 确保 c1 > c2
                if (c1 <= c2) {
                    double temp = c1;
                    c1 = c2 + WATERMARK_STRENGTH;
                    c2 = temp - WATERMARK_STRENGTH;
                }
            } else {
                // 确保 c1 < c2
                if (c1 >= c2) {
                    double temp = c1;
                    c1 = c2 - WATERMARK_STRENGTH;
                    c2 = temp + WATERMARK_STRENGTH;
                }
            }

            // 更新DCT系数
            dctBlock.put(positions[0], positions[1], c1);
            dctBlock.put(positions[2], positions[3], c2);
        }
    }

    /**
     * 将字符串转换为二进制位列表
     */
    private List<Integer> convertToBinary(String text) {
        List<Integer> bits = new ArrayList<>();
        
        for (char c : text.toCharArray()) {
            int ascii = (int) c;
            // 将字符转换为8位二进制
            for (int i = 7; i >= 0; i--) {
                bits.add((ascii >> i) & 1);
            }
        }
        
        return bits;
    }

    /**
     * 从栅格数据中提取水印
     * @param filePath 包含水印的栅格文件路径
     * @param watermarkLength 预期的水印长度（字符数）
     * @return 提取的水印文本
     */
    public String extractWatermark(String filePath, int watermarkLength) {
        Dataset dataset = null;
        try {
            dataset = gdal.Open(filePath, gdalconst.GA_ReadOnly);
            if (dataset == null) {
                throw new RuntimeException("无法打开文件: " + filePath);
            }

            int width = dataset.getRasterXSize();
            int height = dataset.getRasterYSize();
            
            // 读取第一个波段的数据
            Band band = dataset.GetRasterBand(1);
            float[] data = new float[width * height];
            band.ReadRaster(0, 0, width, height, width, height, gdalconst.GDT_Float32, data);

            // 提取水印
            return extractWatermarkFromBand(data, width, height, watermarkLength);

        } catch (Exception e) {
            throw new RuntimeException("水印提取失败: " + e.getMessage(), e);
        } finally {
            if (dataset != null) {
                dataset.delete();
            }
        }
    }

    /**
     * 从波段数据中提取水印
     */
    private String extractWatermarkFromBand(float[] data, int width, int height, int watermarkLength) {
        Mat imageMat = new Mat(height, width, CvType.CV_32F);
        imageMat.put(0, 0, data);

        int blocksX = width / DCT_BLOCK_SIZE;
        int blocksY = height / DCT_BLOCK_SIZE;

        List<Integer> extractedBits = new ArrayList<>();
        int bitsNeeded = watermarkLength * 8; // 每个字符8位

        for (int blockY = 0; blockY < blocksY && extractedBits.size() < bitsNeeded; blockY++) {
            for (int blockX = 0; blockX < blocksX && extractedBits.size() < bitsNeeded; blockX++) {
                Rect blockRect = new Rect(blockX * DCT_BLOCK_SIZE, blockY * DCT_BLOCK_SIZE, 
                                        DCT_BLOCK_SIZE, DCT_BLOCK_SIZE);
                Mat block = new Mat(imageMat, blockRect);

                Mat dctBlock = new Mat();
                Core.dct(block, dctBlock);

                // 提取二进制位
                int bit = extractBitFromDCTBlock(dctBlock);
                extractedBits.add(bit);

                dctBlock.release();
            }
        }

        imageMat.release();

        // 将二进制位转换回字符串
        return convertBinaryToString(extractedBits);
    }

    /**
     * 从DCT块中提取二进制位
     */
    private int extractBitFromDCTBlock(Mat dctBlock) {
        int[] positions = {3, 2, 2, 3}; // 对应嵌入时使用的位置

        double[] coeff1 = dctBlock.get(positions[0], positions[1]);
        double[] coeff2 = dctBlock.get(positions[2], positions[3]);

        if (coeff1 != null && coeff2 != null && coeff1.length > 0 && coeff2.length > 0) {
            return coeff1[0] > coeff2[0] ? 1 : 0;
        }
        
        return 0; // 默认返回0
    }

    /**
     * 将二进制位列表转换为字符串
     */
    private String convertBinaryToString(List<Integer> bits) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < bits.size(); i += 8) {
            if (i + 7 < bits.size()) {
                int ascii = 0;
                for (int j = 0; j < 8; j++) {
                    ascii = (ascii << 1) | bits.get(i + j);
                }
                if (ascii > 0 && ascii < 128) { // 有效ASCII字符
                    result.append((char) ascii);
                }
            }
        }
        
        return result.toString();
    }
}

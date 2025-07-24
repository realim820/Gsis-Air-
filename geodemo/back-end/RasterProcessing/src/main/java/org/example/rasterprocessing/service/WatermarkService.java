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
    // 水印强度 - 进一步提高以支持UTF-8
    private static final double WATERMARK_STRENGTH = 0.35;
    // 最大修改阈值，平衡质量和准确性
    private static final double MAX_MODIFICATION = 0.40;
    // 超强纠错编码 - 专门针对UTF-8
    private static final int REPETITION_COUNT = 7; // 从5增加到7次重复

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

        // 将水印数据转换为二进制位（使用重复编码提高鲁棒性）
        List<Integer> watermarkBits = convertToBinaryWithRepetition(watermarkData);
        
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
     * 在DCT块中嵌入单个二进制位 - 高准确性版本
     */
    private void embedBitInDCTBlock(Mat dctBlock, int bit) {
        // 使用多个中频位置提高鲁棒性
        int[][] positions = {{2, 3}, {3, 2}, {1, 4}, {4, 1}};

        for (int[] pos : positions) {
            double[] coeff = dctBlock.get(pos[0], pos[1]);

            if (coeff != null && coeff.length > 0) {
                double originalValue = coeff[0];
                double newValue;

                // 更强的修改策略
                if (bit == 1) {
                    // 正向强化
                    newValue = originalValue + WATERMARK_STRENGTH;
                } else {
                    // 负向强化
                    newValue = originalValue - WATERMARK_STRENGTH;
                }

                // 确保修改足够明显
                double modification = Math.abs(newValue - originalValue);
                if (modification < WATERMARK_STRENGTH * 0.8) {
                    // 如果修改幅度不够，强制增强
                    if (bit == 1) {
                        newValue = originalValue + WATERMARK_STRENGTH;
                    } else {
                        newValue = originalValue - WATERMARK_STRENGTH;
                    }
                }

                // 限制最大修改幅度
                if (modification > MAX_MODIFICATION) {
                    if (newValue > originalValue) {
                        newValue = originalValue + MAX_MODIFICATION;
                    } else {
                        newValue = originalValue - MAX_MODIFICATION;
                    }
                }

                // 更新DCT系数
                dctBlock.put(pos[0], pos[1], newValue);
            }
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
        int bitsNeeded = getBitsNeededForText(watermarkLength); // 根据UTF-8需要的位数

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

        // 将二进制位转换回字符串（使用重复解码）
        return convertBinaryToStringWithRepetition(extractedBits);
    }

    /**
     * 从DCT块中提取二进制位 - 高准确性版本
     */
    private int extractBitFromDCTBlock(Mat dctBlock) {
        // 使用与嵌入相同的多个位置
        int[][] positions = {{2, 3}, {3, 2}, {1, 4}, {4, 1}};
        
        // 使用加权投票机制
        double totalScore = 0;
        int validPositions = 0;

        for (int[] pos : positions) {
            double[] coeff = dctBlock.get(pos[0], pos[1]);
            if (coeff != null && coeff.length > 0) {
                double value = coeff[0];
                // 累积系数值，正值支持1，负值支持0
                totalScore += value;
                validPositions++;
            }
        }

        if (validPositions == 0) {
            return 0; // 默认返回0
        }

        // 基于平均值判断，使用更宽松的阈值
        double averageScore = totalScore / validPositions;
        
        // 进一步降低阈值，提高敏感度
        double threshold = WATERMARK_STRENGTH * 0.02; // 2%的阈值，极高敏感度
        
        if (averageScore > threshold) {
            return 1;
        } else if (averageScore < -threshold) {
            return 0;
        } else {
            // 在阈值范围内，使用更细粒度的判断
            return averageScore > 0 ? 1 : 0;
        }
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

    /**
     * 将字符串转换为二进制位列表（UTF-8编码 + 重复编码）
     */
    private List<Integer> convertToBinaryWithRepetition(String text) {
        List<Integer> bits = new ArrayList<>();
        
        try {
            // 使用UTF-8编码
            byte[] bytes = text.getBytes("UTF-8");
            
            // 添加长度信息（用于解码时确定边界）
            int length = bytes.length;
            for (int i = 7; i >= 0; i--) {
                int bit = (length >> i) & 1;
                // 每个位重复REPETITION_COUNT次
                for (int rep = 0; rep < REPETITION_COUNT; rep++) {
                    bits.add(bit);
                }
            }
            
            // 转换每个字节为二进制
            for (byte b : bytes) {
                int unsignedByte = b & 0xFF; // 转换为无符号整数
                for (int i = 7; i >= 0; i--) {
                    int bit = (unsignedByte >> i) & 1;
                    // 每个位重复REPETITION_COUNT次
                    for (int rep = 0; rep < REPETITION_COUNT; rep++) {
                        bits.add(bit);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("UTF-8编码失败: " + e.getMessage(), e);
        }
        
        return bits;
    }

    /**
     * 将二进制位列表转换为字符串（UTF-8解码 + 重复解码）
     */
    private String convertBinaryToStringWithRepetition(List<Integer> bits) {
        try {
            if (bits.size() < 8 * REPETITION_COUNT) {
                return ""; // 数据不足
            }
            
            // 首先解码长度信息
            int lengthBits = 8 * REPETITION_COUNT;
            int length = 0;
            for (int i = 0; i < 8; i++) {
                // 使用多数投票恢复每个位
                int vote = 0;
                for (int rep = 0; rep < REPETITION_COUNT; rep++) {
                    int bitIndex = i * REPETITION_COUNT + rep;
                    if (bitIndex < bits.size()) {
                        vote += bits.get(bitIndex);
                    }
                }
                int recoveredBit = vote > REPETITION_COUNT / 2 ? 1 : 0;
                length = (length << 1) | recoveredBit;
            }
            
            // 确保长度合理
            if (length <= 0 || length > 1000) {
                length = Math.min((bits.size() - lengthBits) / (8 * REPETITION_COUNT), 100);
            }
            
            // 解码字节数据
            List<Byte> byteList = new ArrayList<>();
            int startIndex = lengthBits;
            
            for (int byteIndex = 0; byteIndex < length && startIndex + 8 * REPETITION_COUNT <= bits.size(); byteIndex++) {
                int byteValue = 0;
                for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                    // 使用多数投票恢复每个位
                    int vote = 0;
                    for (int rep = 0; rep < REPETITION_COUNT; rep++) {
                        int bitPos = startIndex + bitIndex * REPETITION_COUNT + rep;
                        if (bitPos < bits.size()) {
                            vote += bits.get(bitPos);
                        }
                    }
                    int recoveredBit = vote > REPETITION_COUNT / 2 ? 1 : 0;
                    byteValue = (byteValue << 1) | recoveredBit;
                }
                byteList.add((byte) byteValue);
                startIndex += 8 * REPETITION_COUNT;
            }
            
            // 转换为字节数组
            byte[] byteArray = new byte[byteList.size()];
            for (int i = 0; i < byteList.size(); i++) {
                byteArray[i] = byteList.get(i);
            }
            
            // 使用UTF-8解码
            return new String(byteArray, "UTF-8");
            
        } catch (Exception e) {
            System.err.println("UTF-8解码失败: " + e.getMessage());
            // 回退到简单解码
            return convertBinaryToString(bits);
        }
    }

    /**
     * 计算UTF-8文本需要的位数（包括重复编码）
     */
    private int getBitsNeededForText(int characterCount) {
        // 估算：每个中文字符平均3字节，英文1字节，取平均2.5字节
        int estimatedBytes = (int) Math.ceil(characterCount * 2.5);
        // 长度信息8位 + 数据位，都需要重复编码
        return (8 + estimatedBytes * 8) * REPETITION_COUNT;
    }
}

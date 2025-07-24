package org.example.rasterprocessing.service;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.opencv.core.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 简化的栅格水印服务 - 专门处理TIFF等栅格数据
 */
@Service
public class SimpleRasterWatermarkService {
    
    // 核心参数 - 针对栅格数据优化
    private static final int BLOCK_SIZE = 8;
    private static final double STRENGTH = 10.0;   // 大幅提高栅格数据的强度
    private static final int REPEAT_TIMES = 9;     // 9次重复（奇数便于投票）
    
    /**
     * 嵌入水印到栅格数据
     */
    public void embedWatermark(String inputPath, String outputPath, String watermark) {
        Dataset inputDataset = null;
        Dataset outputDataset = null;
        
        try {
            // 1. 打开输入栅格
            inputDataset = gdal.Open(inputPath, gdalconst.GA_ReadOnly);
            if (inputDataset == null) {
                throw new RuntimeException("无法打开栅格文件: " + inputPath);
            }
            
            // 2. 创建输出栅格（复制结构）
            org.gdal.gdal.Driver driver = inputDataset.GetDriver();
            outputDataset = driver.CreateCopy(outputPath, inputDataset);
            if (outputDataset == null) {
                throw new RuntimeException("无法创建输出文件: " + outputPath);
            }
            
            // 3. 获取第一个波段进行处理
            org.gdal.gdal.Band band = outputDataset.GetRasterBand(1);
            int width = band.getXSize();
            int height = band.getYSize();
            
            // 4. 读取数据
            float[] data = new float[width * height];
            band.ReadRaster(0, 0, width, height, data);
            
            // 5. 转换为OpenCV Mat进行DCT处理
            Mat image = new Mat(height, width, CvType.CV_32F);
            image.put(0, 0, data);
            
            // 6. 准备水印数据
            List<Integer> bits = prepareWatermarkBits(watermark);
            
            // 7. 嵌入水印
            embedBitsIntoMat(image, bits);
            
            // 8. 写回数据
            float[] modifiedData = new float[width * height];
            image.get(0, 0, modifiedData);
            band.WriteRaster(0, 0, width, height, modifiedData);
            
            // 9. 刷新并保存
            outputDataset.FlushCache();
            
        } catch (Exception e) {
            throw new RuntimeException("栅格水印嵌入失败: " + e.getMessage(), e);
        } finally {
            if (inputDataset != null) inputDataset.delete();
            if (outputDataset != null) outputDataset.delete();
        }
    }
    
    /**
     * 从栅格数据提取水印
     */
    public String extractWatermark(String filePath, int expectedLength) {
        Dataset dataset = null;
        
        try {
            // 1. 打开栅格文件
            dataset = gdal.Open(filePath, gdalconst.GA_ReadOnly);
            if (dataset == null) {
                throw new RuntimeException("无法打开栅格文件: " + filePath);
            }
            
            // 2. 读取第一个波段
            org.gdal.gdal.Band band = dataset.GetRasterBand(1);
            int width = band.getXSize();
            int height = band.getYSize();
            
            float[] data = new float[width * height];
            band.ReadRaster(0, 0, width, height, data);
            
            // 3. 转换为OpenCV Mat
            Mat image = new Mat(height, width, CvType.CV_32F);
            image.put(0, 0, data);
            
            // 4. 提取位序列
            List<Integer> extractedBits = extractBitsFromMat(image, expectedLength);
            
            // 5. 解码为字符串
            return decodeBits(extractedBits);
            
        } catch (Exception e) {
            throw new RuntimeException("栅格水印提取失败: " + e.getMessage(), e);
        } finally {
            if (dataset != null) dataset.delete();
        }
    }
    
    /**
     * 准备水印位数据
     */
    private List<Integer> prepareWatermarkBits(String watermark) {
        byte[] utf8Bytes = watermark.getBytes(StandardCharsets.UTF_8);
        List<Integer> bits = new ArrayList<>();
        
        // 添加长度标记
        int length = Math.min(utf8Bytes.length, 255);
        for (int i = 7; i >= 0; i--) {
            int bit = (length >> i) & 1;
            for (int r = 0; r < REPEAT_TIMES; r++) {
                bits.add(bit);
            }
        }
        
        // 添加数据位
        for (int i = 0; i < length; i++) {
            int b = utf8Bytes[i] & 0xFF;
            for (int j = 7; j >= 0; j--) {
                int bit = (b >> j) & 1;
                for (int r = 0; r < REPEAT_TIMES; r++) {
                    bits.add(bit);
                }
            }
        }
        
        return bits;
    }
    
    /**
     * 将位序列嵌入到Mat中
     */
    private void embedBitsIntoMat(Mat image, List<Integer> bits) {
        int rows = image.rows() / BLOCK_SIZE;
        int cols = image.cols() / BLOCK_SIZE;
        int bitIndex = 0;
        
        for (int r = 0; r < rows && bitIndex < bits.size(); r++) {
            for (int c = 0; c < cols && bitIndex < bits.size(); c++) {
                // 提取8x8块
                Rect roi = new Rect(c * BLOCK_SIZE, r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                Mat block = new Mat(image, roi);
                
                // DCT变换
                Mat dctBlock = new Mat();
                Core.dct(block, dctBlock);
                
                // 嵌入位
                int bit = bits.get(bitIndex);
                double[] coeff = dctBlock.get(2, 3);
                if (coeff != null && coeff.length > 0) {
                    double newValue = bit == 1 ? 
                        coeff[0] + STRENGTH : coeff[0] - STRENGTH;
                    dctBlock.put(2, 3, newValue);
                }
                
                // 逆DCT
                Mat idctBlock = new Mat();
                Core.idct(dctBlock, idctBlock);
                idctBlock.copyTo(block);
                
                // 清理
                dctBlock.release();
                idctBlock.release();
                
                bitIndex++;
            }
        }
    }
    
    /**
     * 从Mat中提取位序列
     */
    private List<Integer> extractBitsFromMat(Mat image, int expectedLength) {
        int rows = image.rows() / BLOCK_SIZE;
        int cols = image.cols() / BLOCK_SIZE;
        List<Integer> bits = new ArrayList<>();
        
        int maxBits = (8 + expectedLength * 8) * REPEAT_TIMES;
        
        for (int r = 0; r < rows && bits.size() < maxBits; r++) {
            for (int c = 0; c < cols && bits.size() < maxBits; c++) {
                // 提取8x8块
                Rect roi = new Rect(c * BLOCK_SIZE, r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                Mat block = new Mat(image, roi);
                
                // DCT变换
                Mat dctBlock = new Mat();
                Core.dct(block, dctBlock);
                
                // 提取位 - 使用更灵敏的阈值
                double[] coeff = dctBlock.get(2, 3);
                if (coeff != null && coeff.length > 0) {
                    // 使用基于强度的阈值
                    double threshold = STRENGTH * 0.1; // 10%的阈值
                    if (coeff[0] > threshold) {
                        bits.add(1);
                    } else if (coeff[0] < -threshold) {
                        bits.add(0);
                    } else {
                        // 模糊区域，根据符号判断
                        bits.add(coeff[0] >= 0 ? 1 : 0);
                    }
                }
                
                // 清理
                dctBlock.release();
            }
        }
        
        return bits;
    }
    
    /**
     * 解码位序列为UTF-8字符串 - 增强调试版本
     */
    private String decodeBits(List<Integer> bits) {
        try {
            System.out.println("栅格解码调试 - 总位数: " + bits.size());
            
            if (bits.size() < 8 * REPEAT_TIMES) {
                System.err.println("位数不足，需要至少: " + (8 * REPEAT_TIMES) + "，实际: " + bits.size());
                return "";
            }
            
            // 提取长度
            int length = 0;
            for (int i = 0; i < 8; i++) {
                int vote = 0;
                for (int r = 0; r < REPEAT_TIMES; r++) {
                    int index = i * REPEAT_TIMES + r;
                    if (index < bits.size()) {
                        vote += bits.get(index);
                    }
                }
                int bit = vote > REPEAT_TIMES / 2 ? 1 : 0;
                length = (length << 1) | bit;
            }
            
            System.out.println("解码得到的长度: " + length);
            
            if (length <= 0 || length > 100) {
                System.err.println("长度不合理: " + length);
                return "";
            }
            
            // 检查是否有足够的位
            int neededBits = 8 * REPEAT_TIMES + length * 8 * REPEAT_TIMES;
            if (bits.size() < neededBits) {
                System.err.println("位数不足，需要: " + neededBits + "，实际: " + bits.size());
                return "";
            }
            
            // 提取数据字节
            List<Byte> bytes = new ArrayList<>();
            int startIndex = 8 * REPEAT_TIMES;
            
            for (int byteIdx = 0; byteIdx < length; byteIdx++) {
                int byteValue = 0;
                
                for (int bitIdx = 0; bitIdx < 8; bitIdx++) {
                    int vote = 0;
                    
                    for (int r = 0; r < REPEAT_TIMES; r++) {
                        int index = startIndex + bitIdx * REPEAT_TIMES + r;
                        if (index < bits.size()) {
                            vote += bits.get(index);
                        }
                    }
                    
                    int bit = vote > REPEAT_TIMES / 2 ? 1 : 0;
                    byteValue = (byteValue << 1) | bit;
                }
                
                bytes.add((byte) byteValue);
                startIndex += 8 * REPEAT_TIMES;
            }
            
            // 转换为UTF-8字符串
            byte[] byteArray = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++) {
                byteArray[i] = bytes.get(i);
            }
            
            String result = new String(byteArray, StandardCharsets.UTF_8);
            System.out.println("栅格解码结果: '" + result + "'");
            
            return result;
            
        } catch (Exception e) {
            System.err.println("栅格解码失败: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
}

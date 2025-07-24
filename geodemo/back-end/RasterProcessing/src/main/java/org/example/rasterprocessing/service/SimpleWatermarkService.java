package org.example.rasterprocessing.service;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 简化的水印服务 - 专注于高精度UTF-8处理
 * 重新设计，去除冗余逻辑，专注核心功能
 */
@Service
public class SimpleWatermarkService {
    
    // 核心参数 - 简化配置
    private static final int BLOCK_SIZE = 8;
    private static final double STRENGTH = 50.0;  // 极强信号
    private static final int REPEAT_TIMES = 9;    // 9次重复（奇数便于投票）
    
    /**
     * 嵌入水印 - 简化版本
     */
    public void embedWatermark(String inputPath, String outputPath, String watermark) {
        try {
            // 1. 读取图像
            Mat image = Imgcodecs.imread(inputPath, Imgcodecs.IMREAD_COLOR);
            if (image.empty()) {
                throw new RuntimeException("无法读取图像: " + inputPath);
            }
            
            // 2. 转换为YUV并提取Y通道
            Mat yuv = new Mat();
            Imgproc.cvtColor(image, yuv, Imgproc.COLOR_BGR2YUV);
            List<Mat> channels = new ArrayList<>();
            Core.split(yuv, channels);
            Mat yChannel = channels.get(0);
            
            // 3. 准备水印数据
            byte[] utf8Bytes = watermark.getBytes(StandardCharsets.UTF_8);
            List<Integer> bits = new ArrayList<>();
            
            // 添加长度标记（8位，支持255字节）
            int length = Math.min(utf8Bytes.length, 255);
            for (int i = 7; i >= 0; i--) {
                int bit = (length >> i) & 1;
                for (int r = 0; r < REPEAT_TIMES; r++) {
                    bits.add(bit);
                }
            }
            
            // 添加UTF-8字节数据
            for (int i = 0; i < length; i++) {
                int b = utf8Bytes[i] & 0xFF;
                for (int j = 7; j >= 0; j--) {
                    int bit = (b >> j) & 1;
                    for (int r = 0; r < REPEAT_TIMES; r++) {
                        bits.add(bit);
                    }
                }
            }
            
            // 4. 嵌入水印
            embedBitsIntoChannel(yChannel, bits);
            
            // 5. 重新组合并保存
            channels.set(0, yChannel);
            Core.merge(channels, yuv);
            Mat result = new Mat();
            Imgproc.cvtColor(yuv, result, Imgproc.COLOR_YUV2BGR);
            
            Imgcodecs.imwrite(outputPath, result);
            
            // 清理资源
            image.release();
            yuv.release();
            yChannel.release();
            result.release();
            for (Mat ch : channels) ch.release();
            
        } catch (Exception e) {
            throw new RuntimeException("水印嵌入失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 提取水印 - 简化版本
     */
    public String extractWatermark(String imagePath, int expectedLength) {
        try {
            // 1. 读取图像并转换
            Mat image = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_COLOR);
            if (image.empty()) {
                throw new RuntimeException("无法读取图像: " + imagePath);
            }
            
            Mat yuv = new Mat();
            Imgproc.cvtColor(image, yuv, Imgproc.COLOR_BGR2YUV);
            List<Mat> channels = new ArrayList<>();
            Core.split(yuv, channels);
            Mat yChannel = channels.get(0);
            
            // 2. 提取位序列
            List<Integer> extractedBits = extractBitsFromChannel(yChannel, expectedLength);
            
            // 3. 解码UTF-8
            String result = decodeBits(extractedBits);
            
            // 清理资源
            image.release();
            yuv.release();
            yChannel.release();
            for (Mat ch : channels) ch.release();
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("水印提取失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将位序列嵌入到图像通道
     */
    private void embedBitsIntoChannel(Mat channel, List<Integer> bits) {
        int rows = channel.rows() / BLOCK_SIZE;
        int cols = channel.cols() / BLOCK_SIZE;
        int bitIndex = 0;
        
        for (int r = 0; r < rows && bitIndex < bits.size(); r++) {
            for (int c = 0; c < cols && bitIndex < bits.size(); c++) {
                // 提取8x8块
                Rect roi = new Rect(c * BLOCK_SIZE, r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                Mat block = new Mat(channel, roi);
                
                // 转换为浮点数进行DCT
                Mat floatBlock = new Mat();
                block.convertTo(floatBlock, CvType.CV_32F);
                
                // DCT变换
                Mat dctBlock = new Mat();
                Core.dct(floatBlock, dctBlock);
                
                // 嵌入位（使用简单但强力的方法）
                int bit = bits.get(bitIndex);
                double[] coeff = dctBlock.get(2, 3); // 中频位置
                if (coeff != null && coeff.length > 0) {
                    double newValue = bit == 1 ? 
                        coeff[0] + STRENGTH : coeff[0] - STRENGTH;
                    dctBlock.put(2, 3, newValue);
                }
                
                // 逆DCT
                Mat idctBlock = new Mat();
                Core.idct(dctBlock, idctBlock);
                
                // 转换回原类型并写回
                Mat resultBlock = new Mat();
                idctBlock.convertTo(resultBlock, block.type());
                resultBlock.copyTo(block);
                
                // 清理临时变量
                floatBlock.release();
                dctBlock.release();
                idctBlock.release();
                resultBlock.release();
                
                bitIndex++;
            }
        }
    }
    
    /**
     * 从图像通道提取位序列
     */
    private List<Integer> extractBitsFromChannel(Mat channel, int expectedLength) {
        int rows = channel.rows() / BLOCK_SIZE;
        int cols = channel.cols() / BLOCK_SIZE;
        List<Integer> bits = new ArrayList<>();
        
        // 估算需要的位数：长度(8位) + 数据位(expectedLength * 8位) ，每位重复REPEAT_TIMES次
        int maxBits = (8 + expectedLength * 8) * REPEAT_TIMES;
        
        for (int r = 0; r < rows && bits.size() < maxBits; r++) {
            for (int c = 0; c < cols && bits.size() < maxBits; c++) {
                // 提取8x8块
                Rect roi = new Rect(c * BLOCK_SIZE, r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                Mat block = new Mat(channel, roi);
                
                // DCT变换
                Mat floatBlock = new Mat();
                block.convertTo(floatBlock, CvType.CV_32F);
                Mat dctBlock = new Mat();
                Core.dct(floatBlock, dctBlock);
                
                // 提取位
                double[] coeff = dctBlock.get(2, 3);
                if (coeff != null && coeff.length > 0) {
                    bits.add(coeff[0] > 0 ? 1 : 0);
                }
                
                // 清理
                floatBlock.release();
                dctBlock.release();
            }
        }
        
        return bits;
    }
    
    /**
     * 解码位序列为UTF-8字符串
     */
    private String decodeBits(List<Integer> bits) {
        try {
            if (bits.size() < 8 * REPEAT_TIMES) {
                return "";
            }
            
            // 1. 提取长度信息（前8位，每位重复REPEAT_TIMES次）
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
            
            if (length <= 0 || length > 100) {
                return "";
            }
            
            // 2. 提取数据字节
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
            
            // 3. 转换为UTF-8字符串
            byte[] byteArray = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++) {
                byteArray[i] = bytes.get(i);
            }
            
            return new String(byteArray, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            System.err.println("解码失败: " + e.getMessage());
            return "";
        }
    }
}

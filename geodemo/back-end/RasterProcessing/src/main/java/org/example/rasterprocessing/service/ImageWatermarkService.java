package org.example.rasterprocessing.service;

import org.example.rasterprocessing.model.WatermarkResult;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 通用图像水印服务 - 处理JPEG、JPG、PNG格式
 */
@Service
public class ImageWatermarkService {

    // DCT块大小
    private static final int DCT_BLOCK_SIZE = 8;
    // 水印强度 - 进一步提高以支持UTF-8
    private static final double WATERMARK_STRENGTH = 35.0;
    // 最大修改阈值
    private static final double MAX_MODIFICATION = 40.0;
    // 超强纠错编码 - 专门针对UTF-8
    private static final int REPETITION_COUNT = 7; // 7次重复，提供极强的纠错能力

    /**
     * 在普通图像中嵌入DCT暗水印
     * @param inputPath 输入图像文件路径
     * @param outputPath 输出图像文件路径
     * @param watermarkData 水印数据（文本）
     * @return 水印嵌入结果
     */
    public WatermarkResult embedWatermarkInImage(String inputPath, String outputPath, String watermarkData) {
        long startTime = System.currentTimeMillis();

        try {
            // 读取图像
            Mat image = Imgcodecs.imread(inputPath, Imgcodecs.IMREAD_COLOR);
            if (image.empty()) {
                return new WatermarkResult(false, "无法读取图像文件: " + inputPath);
            }

            // 转换为YUV颜色空间（在Y通道嵌入水印）
            Mat yuvImage = new Mat();
            Imgproc.cvtColor(image, yuvImage, Imgproc.COLOR_BGR2YUV);

            // 分离通道
            List<Mat> channels = new ArrayList<>();
            Core.split(yuvImage, channels);

            // 在Y通道（亮度通道）嵌入水印
            Mat yChannel = channels.get(0);
            Mat watermarkedY = embedWatermarkInChannel(yChannel, watermarkData);

            // 替换Y通道
            channels.set(0, watermarkedY);

            // 合并通道
            Mat watermarkedYUV = new Mat();
            Core.merge(channels, watermarkedYUV);

            // 转换回BGR
            Mat watermarkedBGR = new Mat();
            Imgproc.cvtColor(watermarkedYUV, watermarkedBGR, Imgproc.COLOR_YUV2BGR);

            // 保存水印图像
            boolean saved = Imgcodecs.imwrite(outputPath, watermarkedBGR);
            if (!saved) {
                return new WatermarkResult(false, "无法保存水印图像: " + outputPath);
            }

            // 释放资源
            image.release();
            yuvImage.release();
            for (Mat channel : channels) {
                channel.release();
            }
            watermarkedY.release();
            watermarkedYUV.release();
            watermarkedBGR.release();

            long processingTime = System.currentTimeMillis() - startTime;
            return new WatermarkResult(true, "图像水印嵌入成功", outputPath, processingTime);

        } catch (Exception e) {
            return new WatermarkResult(false, "图像水印嵌入失败: " + e.getMessage());
        }
    }

    /**
     * 从普通图像中提取DCT暗水印
     * @param imagePath 包含水印的图像文件路径
     * @param watermarkLength 预期水印长度（字符数）
     * @return 提取的水印文本
     */
    public String extractWatermarkFromImage(String imagePath, int watermarkLength) {
        try {
            // 读取图像
            Mat image = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_COLOR);
            if (image.empty()) {
                throw new RuntimeException("无法读取图像文件: " + imagePath);
            }

            // 转换为YUV颜色空间
            Mat yuvImage = new Mat();
            Imgproc.cvtColor(image, yuvImage, Imgproc.COLOR_BGR2YUV);

            // 分离通道，获取Y通道
            List<Mat> channels = new ArrayList<>();
            Core.split(yuvImage, channels);
            Mat yChannel = channels.get(0);

            // 从Y通道提取水印
            String watermarkText = extractWatermarkFromChannel(yChannel, watermarkLength);

            // 释放资源
            image.release();
            yuvImage.release();
            for (Mat channel : channels) {
                channel.release();
            }

            return watermarkText;

        } catch (Exception e) {
            throw new RuntimeException("图像水印提取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 在单个通道中嵌入水印
     */
    private Mat embedWatermarkInChannel(Mat channel, String watermarkData) {
        // 确保图像尺寸是8的倍数
        int adjustedWidth = (channel.width() / DCT_BLOCK_SIZE) * DCT_BLOCK_SIZE;
        int adjustedHeight = (channel.height() / DCT_BLOCK_SIZE) * DCT_BLOCK_SIZE;

        Mat adjustedChannel = new Mat();
        if (adjustedWidth != channel.width() || adjustedHeight != channel.height()) {
            Size newSize = new Size(adjustedWidth, adjustedHeight);
            Imgproc.resize(channel, adjustedChannel, newSize);
        } else {
            adjustedChannel = channel.clone();
        }

        // 转换为浮点数
        Mat floatChannel = new Mat();
        adjustedChannel.convertTo(floatChannel, CvType.CV_32F);

        // 计算可嵌入的块数量
        int blocksX = adjustedWidth / DCT_BLOCK_SIZE;
        int blocksY = adjustedHeight / DCT_BLOCK_SIZE;
        int availableBlocks = blocksX * blocksY;

        // 将水印数据转换为二进制位（使用重复编码提高鲁棒性）
        List<Integer> watermarkBits = convertToBinaryWithRepetition(watermarkData);
        
        System.out.println("容量检查: 可用块数=" + availableBlocks + ", 需要位数=" + watermarkBits.size());
        
        // 确保有足够的块来嵌入水印
        if (watermarkBits.size() > availableBlocks) {
            throw new RuntimeException("水印数据过长，超出了图像容量。图像容量: " + availableBlocks + " 位，实际需要: " + watermarkBits.size() + " 位");
        }

        Mat watermarkedChannel = floatChannel.clone();
        int bitIndex = 0;
        int processedBlocks = 0;
        int skippedBlocks = 0;

        // 简化策略：优先使用复杂块，但确保能嵌入所有水印位
        for (int blockY = 0; blockY < blocksY && bitIndex < watermarkBits.size(); blockY++) {
            for (int blockX = 0; blockX < blocksX && bitIndex < watermarkBits.size(); blockX++) {
                processedBlocks++;
                
                // 提取8x8块
                Rect blockRect = new Rect(blockX * DCT_BLOCK_SIZE, blockY * DCT_BLOCK_SIZE,
                        DCT_BLOCK_SIZE, DCT_BLOCK_SIZE);
                Mat block = new Mat(watermarkedChannel, blockRect);

                // 计算剩余需要嵌入的位数和剩余可用块数
                int remainingBits = watermarkBits.size() - bitIndex;
                int remainingBlocks = (blocksY - blockY) * blocksX - blockX;
                
                // 如果是复杂块，直接使用；如果剩余块数不多，强制使用
                boolean shouldUse = isBlockSuitableForWatermark(block) || remainingBits >= remainingBlocks;
                
                if (!shouldUse) {
                    skippedBlocks++;
                    continue;
                }

                // 进行DCT变换和嵌入
                Mat dctBlock = new Mat();
                Core.dct(block, dctBlock);
                
                int bit = watermarkBits.get(bitIndex++);
                embedBitInDCTBlock(dctBlock, bit);

                // 进行逆DCT变换
                Mat idctBlock = new Mat();
                Core.idct(dctBlock, idctBlock);
                idctBlock.copyTo(block);

                // 释放资源
                dctBlock.release();
                idctBlock.release();
            }
        }

        System.out.println("嵌入完成: 处理块数=" + processedBlocks + ", 跳过=" + skippedBlocks + 
                          ", 嵌入位数=" + bitIndex + "/" + watermarkBits.size());

        // 转换回原始数据类型
        Mat result = new Mat();
        watermarkedChannel.convertTo(result, channel.type());

        // 如果调整了尺寸，需要恢复原始尺寸
        if (adjustedWidth != channel.width() || adjustedHeight != channel.height()) {
            Mat resizedResult = new Mat();
            Size originalSize = new Size(channel.width(), channel.height());
            Imgproc.resize(result, resizedResult, originalSize);
            result.release();
            result = resizedResult;
        }

        // 释放临时Mat对象
        adjustedChannel.release();
        floatChannel.release();
        watermarkedChannel.release();

        return result;
    }

    /**
     * 从单个通道中提取水印 - 改进版本，与嵌入策略保持一致
     */
    private String extractWatermarkFromChannel(Mat channel, int watermarkLength) {
        // 确保图像尺寸是8的倍数
        int adjustedWidth = (channel.width() / DCT_BLOCK_SIZE) * DCT_BLOCK_SIZE;
        int adjustedHeight = (channel.height() / DCT_BLOCK_SIZE) * DCT_BLOCK_SIZE;

        Mat adjustedChannel = new Mat();
        if (adjustedWidth != channel.width() || adjustedHeight != channel.height()) {
            Size newSize = new Size(adjustedWidth, adjustedHeight);
            Imgproc.resize(channel, adjustedChannel, newSize);
        } else {
            adjustedChannel = channel.clone();
        }

        // 转换为浮点数
        Mat floatChannel = new Mat();
        adjustedChannel.convertTo(floatChannel, CvType.CV_32F);

        int blocksX = adjustedWidth / DCT_BLOCK_SIZE;
        int blocksY = adjustedHeight / DCT_BLOCK_SIZE;

        List<Integer> extractedBits = new ArrayList<>();
        int bitsNeeded = getBitsNeededForText(watermarkLength);
        int totalBlocks = 0;
        int skippedBlocks = 0;
        int usedBlocks = 0;

        // 按照与嵌入相同的顺序遍历块
        for (int blockY = 0; blockY < blocksY && extractedBits.size() < bitsNeeded; blockY++) {
            for (int blockX = 0; blockX < blocksX && extractedBits.size() < bitsNeeded; blockX++) {
                totalBlocks++;
                
                Rect blockRect = new Rect(blockX * DCT_BLOCK_SIZE, blockY * DCT_BLOCK_SIZE,
                        DCT_BLOCK_SIZE, DCT_BLOCK_SIZE);
                Mat block = new Mat(floatChannel, blockRect);

                // 只从适合水印的块中提取，与嵌入时保持一致
                if (!isBlockSuitableForWatermark(block)) {
                    skippedBlocks++;
                    continue; // 跳过这个块，与嵌入时行为一致
                }

                Mat dctBlock = new Mat();
                Core.dct(block, dctBlock);

                // 提取二进制位
                int bit = extractBitFromDCTBlock(dctBlock);
                extractedBits.add(bit);
                usedBlocks++;

                dctBlock.release();
            }
        }

        // 输出统计信息用于调试
        System.out.println("水印提取统计: 总块数=" + totalBlocks + ", 跳过=" + skippedBlocks + 
                          ", 使用=" + usedBlocks + ", 需要=" + bitsNeeded + ", 提取到=" + extractedBits.size());

        // 释放资源
        adjustedChannel.release();
        floatChannel.release();

        // 将二进制位转换回字符串（使用重复解码）
        return convertBinaryToStringWithRepetition(extractedBits);
    }

    /**
     * 在DCT块中嵌入单个二进制位 - 高准确性版本
     */
    private void embedBitInDCTBlock(Mat dctBlock, int bit) {
        // 使用多个中频位置提高鲁棒性（增加到6个位置）
        int[][] positions = {{2, 3}, {3, 2}, {1, 4}, {4, 1}, {2, 4}, {4, 2}};

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

                // 确保修改足够明显，但不超过最大限制
                double modification = Math.abs(newValue - originalValue);
                if (modification < WATERMARK_STRENGTH * 0.8) {
                    // 如果修改幅度不够，强制增强
                    if (bit == 1) {
                        newValue = originalValue + WATERMARK_STRENGTH;
                    } else {
                        newValue = originalValue - WATERMARK_STRENGTH;
                    }
                }

                // 限制最大修改幅度，保护视觉质量
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
     * 从DCT块中提取二进制位 - 高准确性版本
     */
    private int extractBitFromDCTBlock(Mat dctBlock) {
        // 使用与嵌入相同的多个位置（增加到6个位置）
        int[][] positions = {{2, 3}, {3, 2}, {1, 4}, {4, 1}, {2, 4}, {4, 2}};
        
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
        
        // 进一步降低阈值，提高敏感度（从5%降到2%）
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
     * 检查8x8块是否适合嵌入水印
     * 更精确的质量评估
     */
    private boolean isBlockSuitableForWatermark(Mat block) {
        // 计算块的方差和梯度
        Scalar mean = Core.mean(block);
        Mat temp = new Mat();
        Core.subtract(block, mean, temp);
        Core.multiply(temp, temp, temp);
        Scalar variance = Core.mean(temp);
        temp.release();

        double varianceValue = variance.val[0];
        double meanValue = mean.val[0];
        
        // 多重检查标准
        // 1. 方差检查：避免过于平滑的区域
        boolean hasGoodVariance = varianceValue > 15.0; // 降低阈值但仍过滤纯色
        
        // 2. 避免极值区域（过亮或过暗）
        boolean notExtremeValue = meanValue > 20 && meanValue < 235;
        
        // 3. 避免纯色块
        boolean notPureColor = varianceValue > 1.0;
        
        // 优先级：有良好方差的非极值区域 > 非纯色区域
        return (hasGoodVariance && notExtremeValue) || notPureColor;
    }

    /**
     * 评估块的水印嵌入质量得分
     */
    private double evaluateBlockQuality(Mat block) {
        Scalar mean = Core.mean(block);
        Mat temp = new Mat();
        Core.subtract(block, mean, temp);
        Core.multiply(temp, temp, temp);
        Scalar variance = Core.mean(temp);
        temp.release();

        double varianceValue = variance.val[0];
        double meanValue = mean.val[0];
        
        // 计算质量得分
        double score = 0;
        
        // 方差得分（更高的方差 = 更好的嵌入环境）
        score += Math.min(varianceValue / 100.0, 1.0) * 50;
        
        // 中间亮度得分（避免极端亮度）
        double brightnessScore = 1.0 - Math.abs(127.5 - meanValue) / 127.5;
        score += brightnessScore * 30;
        
        // 纹理复杂度得分
        if (varianceValue > 50) score += 20;
        
        return score;
    }    /**
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
     * 将二进制位列表转换为字符串（UTF-8解码 + 重复解码） - 增强容错版本
     */
    private String convertBinaryToStringWithRepetition(List<Integer> bits) {
        try {
            if (bits.size() < 8 * REPETITION_COUNT) {
                System.err.println("提取的位数不足，尝试简单解码");
                return convertBinaryToString(bits);
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
            
            // 长度合理性检查和调整（更宽松的范围）
            if (length <= 0 || length > 200) {
                // 如果长度信息损坏，根据剩余位数估算
                int remainingBits = bits.size() - lengthBits;
                length = Math.min(remainingBits / (8 * REPETITION_COUNT), 50); // 最多50字节
                System.err.println("长度信息可能损坏，估算长度: " + length);
            }
            
            // 解码字节数据
            List<Byte> byteList = new ArrayList<>();
            int startIndex = lengthBits;
            int successfulBytes = 0;
            
            for (int byteIndex = 0; byteIndex < length && startIndex + 8 * REPETITION_COUNT <= bits.size(); byteIndex++) {
                int byteValue = 0;
                boolean byteValid = true;
                
                for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                    // 使用严格的多数投票机制
                    int vote = 0;
                    int totalVotes = 0;
                    
                    for (int rep = 0; rep < REPETITION_COUNT; rep++) {
                        int bitPos = startIndex + bitIndex * REPETITION_COUNT + rep;
                        if (bitPos < bits.size()) {
                            vote += bits.get(bitPos);
                            totalVotes++;
                        }
                    }
                    
                    if (totalVotes == 0) {
                        byteValid = false;
                        break;
                    }
                    
                    // 降低投票阈值至50%，但增加额外的UTF-8字节验证
                    int threshold = Math.max(1, totalVotes / 2);
                    int recoveredBit = vote >= threshold ? 1 : 0;
                    
                    // 额外检查：如果投票过于接近，标记为可疑
                    if (Math.abs(vote - (totalVotes - vote)) <= 1 && totalVotes >= 3) {
                        // 投票太接近，可能是噪声，采用保守策略
                        recoveredBit = vote > totalVotes / 2 ? 1 : 0;
                    }
                    
                    byteValue = (byteValue << 1) | recoveredBit;
                }
                
                if (byteValid) {
                    byteList.add((byte) byteValue);
                    successfulBytes++;
                }
                startIndex += 8 * REPETITION_COUNT;
            }
            
            if (successfulBytes == 0) {
                System.err.println("未能成功解码任何字节，回退到简单解码");
                return convertBinaryToString(bits);
            }
            
            // 转换为字节数组
            byte[] byteArray = new byte[byteList.size()];
            for (int i = 0; i < byteList.size(); i++) {
                byteArray[i] = byteList.get(i);
            }
            
            // 使用UTF-8解码
            String result = new String(byteArray, "UTF-8");
            
            // 检查结果是否合理（更宽松的验证）
            if (result.trim().isEmpty()) {
                System.err.println("UTF-8解码结果为空，回退到简单解码");
                return convertBinaryToString(bits);
            }
            
            // 检查是否包含过多控制字符（但允许正常的UTF-8字符）
            long controlChars = result.chars().filter(c -> c < 32 && c != 9 && c != 10 && c != 13).count();
            if (controlChars > result.length() / 2) {
                System.err.println("UTF-8解码结果包含过多控制字符，回退到简单解码");
                return convertBinaryToString(bits);
            }
            
            return result;
            
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

    /**
     * 原始的ASCII编码方法（保持兼容性）
     */
    private List<Integer> convertToBinary(String text) {
        List<Integer> bits = new ArrayList<>();

        try {
            byte[] bytes = text.getBytes("UTF-8"); // 使用UTF-8编码转换字符为字节
            for (byte b : bytes) {
                for (int i = 7; i >= 0; i--) {
                    bits.add((b >> i) & 1);
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return bits;
    }


    /**
     * 将二进制位列表转换为字符串
     */
    private String convertBinaryToString(List<Integer> bits) {
        StringBuilder result = new StringBuilder();
        byte[] byteArray = new byte[bits.size() / 8];

        for (int i = 0; i < bits.size(); i += 8) {
            byte b = 0;
            for (int j = 0; j < 8; j++) {
                b = (byte) (b << 1 | bits.get(i + j));
            }
            byteArray[i / 8] = b;
        }

        try {
            result.append(new String(byteArray, "UTF-8")); // 转换为UTF-8字符串
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    /**
     * 计算UTF-8文本需要的位数（包括重复编码）
     */

}

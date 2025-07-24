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
    // 水印强度
    private static final double WATERMARK_STRENGTH = 30.0;

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
        int totalBlocks = blocksX * blocksY;

        // 将水印数据转换为二进制位
        List<Integer> watermarkBits = convertToBinary(watermarkData);

        // 确保有足够的块来嵌入水印
        if (watermarkBits.size() > totalBlocks) {
            throw new RuntimeException("水印数据过长，超出了可嵌入的容量。最大容量: " + totalBlocks + " 位，实际需要: " + watermarkBits.size() + " 位");
        }

        Mat watermarkedChannel = floatChannel.clone();

        // 对每个8x8块进行DCT变换并嵌入水印
        int bitIndex = 0;
        for (int blockY = 0; blockY < blocksY && bitIndex < watermarkBits.size(); blockY++) {
            for (int blockX = 0; blockX < blocksX && bitIndex < watermarkBits.size(); blockX++) {
                // 提取8x8块
                Rect blockRect = new Rect(blockX * DCT_BLOCK_SIZE, blockY * DCT_BLOCK_SIZE,
                        DCT_BLOCK_SIZE, DCT_BLOCK_SIZE);
                Mat block = new Mat(watermarkedChannel, blockRect);

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
     * 从单个通道中提取水印
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
        int bitsNeeded = watermarkLength * 8; // 每个字符8位

        for (int blockY = 0; blockY < blocksY && extractedBits.size() < bitsNeeded; blockY++) {
            for (int blockX = 0; blockX < blocksX && extractedBits.size() < bitsNeeded; blockX++) {
                Rect blockRect = new Rect(blockX * DCT_BLOCK_SIZE, blockY * DCT_BLOCK_SIZE,
                        DCT_BLOCK_SIZE, DCT_BLOCK_SIZE);
                Mat block = new Mat(floatChannel, blockRect);

                Mat dctBlock = new Mat();
                Core.dct(block, dctBlock);

                // 提取二进制位
                int bit = extractBitFromDCTBlock(dctBlock);
                extractedBits.add(bit);

                dctBlock.release();
            }
        }

        // 释放资源
        adjustedChannel.release();
        floatChannel.release();

        // 将二进制位转换回字符串
        return convertBinaryToString(extractedBits);
    }

    /**
     * 在DCT块中嵌入单个二进制位
     */
    private void embedBitInDCTBlock(Mat dctBlock, int bit) {
        // 选择中频系数位置进行嵌入
        int[] pos1 = {2, 3}; // 位置1
        int[] pos2 = {3, 2}; // 位置2

        // 获取选定位置的DCT系数
        double[] coeff1 = dctBlock.get(pos1[0], pos1[1]);
        double[] coeff2 = dctBlock.get(pos2[0], pos2[1]);

        if (coeff1 != null && coeff2 != null && coeff1.length > 0 && coeff2.length > 0) {
            double c1 = coeff1[0];
            double c2 = coeff2[0];

            // 根据水印位调整系数关系
            if (bit == 1) {
                // 确保 c1 > c2
                if (c1 <= c2) {
                    double avg = (c1 + c2) / 2;
                    c1 = avg + WATERMARK_STRENGTH;
                    c2 = avg - WATERMARK_STRENGTH;
                }
            } else {
                // 确保 c1 < c2
                if (c1 >= c2) {
                    double avg = (c1 + c2) / 2;
                    c1 = avg - WATERMARK_STRENGTH;
                    c2 = avg + WATERMARK_STRENGTH;
                }
            }

            // 更新DCT系数
            dctBlock.put(pos1[0], pos1[1], c1);
            dctBlock.put(pos2[0], pos2[1], c2);
        }
    }

    /**
     * 从DCT块中提取二进制位
     */
    private int extractBitFromDCTBlock(Mat dctBlock) {
        int[] pos1 = {2, 3};
        int[] pos2 = {3, 2};

        double[] coeff1 = dctBlock.get(pos1[0], pos1[1]);
        double[] coeff2 = dctBlock.get(pos2[0], pos2[1]);

        if (coeff1 != null && coeff2 != null && coeff1.length > 0 && coeff2.length > 0) {
            return coeff1[0] > coeff2[0] ? 1 : 0;
        }

        return 0; // 默认返回0
    }

    /**
     * 将字符串转换为二进制位列表
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

}

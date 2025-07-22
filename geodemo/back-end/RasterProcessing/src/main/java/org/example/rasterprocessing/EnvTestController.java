package org.example.rasterprocessing;

import org.example.rasterprocessing.model.RasterInfo;
import org.example.rasterprocessing.model.WatermarkResult;
import org.example.rasterprocessing.service.RasterReaderService;
import org.example.rasterprocessing.service.WatermarkService;
import org.example.rasterprocessing.util.TestDataGenerator;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.File;

@RestController
public class EnvTestController {

    @Autowired
    private RasterReaderService rasterReaderService;
    
    @Autowired
    private WatermarkService watermarkService;

    static {
        try {
            // 直接使用 resources 目录下的 OpenCV DLL
            String projectRoot = System.getProperty("user.dir");
            String dllPath = projectRoot + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "opencv_java4120.dll";
            
            System.out.println("Loading OpenCV DLL from resources: " + dllPath);
            
            // 检查文件是否存在
            File dllFile = new File(dllPath);
            if (!dllFile.exists()) {
                System.err.println("DLL file does not exist: " + dllPath);
                throw new RuntimeException("OpenCV DLL not found at: " + dllPath);
            }
            
            // 直接加载 resources 目录下的 DLL
            System.load(dllPath);
            System.out.println("OpenCV DLL loaded successfully from resources!");
            
            // 初始化 OpenCV Core
            System.out.println("OpenCV version: " + Core.VERSION);
            
        } catch (Exception e) {
            System.err.println("Failed to load OpenCV from resources: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to load OpenCV", e);
        }
        
        try {
            gdal.AllRegister();
            System.out.println("GDAL initialized successfully!");
        } catch (Exception e) {
            System.err.println("Failed to initialize GDAL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @GetMapping("/test/gdal")
    public String testGdal() {
        String filepath = "testdata/sample.tif";

        Dataset dataset = gdal.Open(filepath);
        if (dataset == null) {
            return "GDAL 打开失败，请检查路径或库加载是否成功";
        }

        int width = dataset.getRasterXSize();
        int height = dataset.getRasterYSize();
        String driver = dataset.GetDriver().getShortName();

        dataset.delete();
        return String.format("GDAL 成功打开文件：%s，宽度：%d，高度：%d，驱动：%s",
                filepath, width, height, driver);
    }

    @GetMapping("/test/opencv")
    public String testOpenCV() {
        String imgPath = "testdata/test.png";

        Mat img = Imgcodecs.imread(imgPath);
        if (img.empty()) {
            return "OpenCV 加载图像失败，请检查路径或 DLL 加载是否成功";
        }

        int width = img.width();
        int height = img.height();
        img.release();
        
        return String.format("OpenCV 成功加载图像，宽度：%d，高度：%d", width, height);
    }

    @GetMapping("/test/generate-sample")
    public String generateSampleData() {
        try {
            TestDataGenerator.generateTestGeoTiff("testdata/sample.tif", 256, 256);
            return "测试GeoTIFF文件生成成功！";
        } catch (Exception e) {
            return "生成测试文件失败: " + e.getMessage();
        }
    }

    @GetMapping("/test/raster-info")
    public String testRasterInfo() {
        try {
            String filepath = "testdata/sample.tif";
            
            // 确保测试文件存在
            File testFile = new File(filepath);
            if (!testFile.exists()) {
                TestDataGenerator.generateTestGeoTiff(filepath, 256, 256);
            }
            
            RasterInfo info = rasterReaderService.readRasterInfo(filepath);
            return String.format("栅格信息读取成功 - 文件: %s, 尺寸: %dx%d, 波段数: %d, 驱动: %s", 
                    info.getFileName(), info.getWidth(), info.getHeight(), 
                    info.getBandCount(), info.getDriver());
        } catch (Exception e) {
            return "栅格信息读取失败: " + e.getMessage();
        }
    }

    @GetMapping("/test/watermark")
    public String testWatermark() {
        try {
            String inputPath = "testdata/sample.tif";
            String outputPath = "testdata/watermarked_sample.tif";
            String watermarkText = "TEST123";
            
            // 确保输入文件存在
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                TestDataGenerator.generateTestGeoTiff(inputPath, 256, 256);
            }
            
            // 嵌入水印
            WatermarkResult embedResult = watermarkService.embedWatermark(inputPath, outputPath, watermarkText);
            if (!embedResult.isSuccess()) {
                return "水印嵌入失败: " + embedResult.getMessage();
            }
            
            // 提取水印
            String extractedWatermark = watermarkService.extractWatermark(outputPath, watermarkText.length());
            
            return String.format("水印测试成功！嵌入: '%s', 提取: '%s', 处理时间: %dms", 
                    watermarkText, extractedWatermark, embedResult.getProcessingTime());
                    
        } catch (Exception e) {
            return "水印测试失败: " + e.getMessage();
        }
    }

    @GetMapping("/test/all")
    public String testAll() {
        StringBuilder result = new StringBuilder();
        
        try {
            result.append("=== 完整功能测试 ===\n");
            
            // 测试GDAL
            result.append("1. GDAL测试: ").append(testGdal()).append("\n");
            
            // 测试OpenCV
            result.append("2. OpenCV测试: ").append(testOpenCV()).append("\n");
            
            // 测试栅格信息读取
            result.append("3. 栅格信息读取测试: ").append(testRasterInfo()).append("\n");
            
            // 测试水印功能
            result.append("4. DCT水印测试: ").append(testWatermark()).append("\n");
            
            result.append("=== 所有测试完成 ===");
            
        } catch (Exception e) {
            result.append("测试过程中发生错误: ").append(e.getMessage());
        }
        
        return result.toString().replace("\n", "<br>");
    }
}

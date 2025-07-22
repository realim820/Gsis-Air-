package org.example.rasterprocessing;

import org.gdal.gdal.gdal;
import org.opencv.core.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class RasterProcessingApplication {

	static {
		try {
			// 初始化 OpenCV 和 GDAL
			String projectRoot = System.getProperty("user.dir");
			String dllPath = projectRoot + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "opencv_java4120.dll";
			
			System.out.println("Loading OpenCV DLL from: " + dllPath);
			System.load(dllPath);
			System.out.println("OpenCV loaded successfully! Version: " + Core.VERSION);
			
			gdal.AllRegister();
			System.out.println("GDAL initialized successfully!");
			
		} catch (Exception e) {
			System.err.println("Failed to initialize libraries: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Library initialization failed", e);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(RasterProcessingApplication.class, args);
	}
}

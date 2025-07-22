package org.example.rasterprocessing.model;

/**
 * 水印嵌入结果模型
 */
public class WatermarkResult {
    private boolean success;
    private String message;
    private String outputPath;
    private long processingTime;

    public WatermarkResult() {}

    public WatermarkResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public WatermarkResult(boolean success, String message, String outputPath, long processingTime) {
        this.success = success;
        this.message = message;
        this.outputPath = outputPath;
        this.processingTime = processingTime;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }
}

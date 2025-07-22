package org.example.rasterprocessing.model;

import java.util.Map;

/**
 * 栅格数据信息模型
 */
public class RasterInfo {
    private String fileName;
    private int width;
    private int height;
    private int bandCount;
    private String driver;
    private String projection;
    private double[] geoTransform;
    private String dataType;
    private Double noDataValue;
    private Map<String, String> metadata;

    public RasterInfo() {}

    public RasterInfo(String fileName, int width, int height, int bandCount, String driver) {
        this.fileName = fileName;
        this.width = width;
        this.height = height;
        this.bandCount = bandCount;
        this.driver = driver;
    }

    // Getters and Setters
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getBandCount() {
        return bandCount;
    }

    public void setBandCount(int bandCount) {
        this.bandCount = bandCount;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getProjection() {
        return projection;
    }

    public void setProjection(String projection) {
        this.projection = projection;
    }

    public double[] getGeoTransform() {
        return geoTransform;
    }

    public void setGeoTransform(double[] geoTransform) {
        this.geoTransform = geoTransform;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Double getNoDataValue() {
        return noDataValue;
    }

    public void setNoDataValue(Double noDataValue) {
        this.noDataValue = noDataValue;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}

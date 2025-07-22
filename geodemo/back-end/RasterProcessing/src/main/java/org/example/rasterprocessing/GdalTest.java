package org.example.rasterprocessing;

import org.gdal.gdal.gdal;
import static org.gdal.gdalconst.gdalconstConstants.GDT_Int32;

public class GdalTest {
    public static void main(String[] args) {
        try {
            gdal.AllRegister();
            System.out.println("GDAL Version: " + gdal.VersionInfo("--version"));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

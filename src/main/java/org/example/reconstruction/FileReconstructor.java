package org.example.reconstruction;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class FileReconstructor {
    private static final String OUTPUT_FILE = "reconstructed.txt";

    public static void reconstructFile(List<String> chunkPaths) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(OUTPUT_FILE)) {
            for (String chunkPath : chunkPaths) {
                decompressAndWrite(chunkPath, fos);
            }
        }
    }

    private static void decompressAndWrite(String inputPath, FileOutputStream fos) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputPath);
             GZIPInputStream gis = new GZIPInputStream(fis)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }
}
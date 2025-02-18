package org.example.chunking;

import org.springframework.stereotype.Component;
import org.example.util.RabinFingerprint;
import java.io.*;
import java.util.*;

@Component
public class RabinChunker {
    private static final int MIN_CHUNK_SIZE = 2048;
    private static final int AVG_CHUNK_SIZE = 8192;
    private static final int MAX_CHUNK_SIZE = 16384;

    public List<byte[]> chunkData(byte[] data) {
        List<byte[]> chunks = new ArrayList<>();
        ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();
        RabinFingerprint fingerprint = new RabinFingerprint();

        for (byte b : data) {
            chunkBuffer.write(b);
            fingerprint.update(b);

            if (chunkBuffer.size() >= MIN_CHUNK_SIZE && fingerprint.isBoundary()) {
                chunks.add(chunkBuffer.toByteArray());
                chunkBuffer.reset();
                fingerprint.reset();
            }
        }

        if (chunkBuffer.size() > 0) {
            chunks.add(chunkBuffer.toByteArray());
        }
        return chunks;
    }

    public List<byte[]> chunkData(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Fichier introuvable : " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            return chunkData(fis.readAllBytes());
        }
    }
}
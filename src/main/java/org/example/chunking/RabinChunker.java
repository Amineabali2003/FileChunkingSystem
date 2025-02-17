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

    public List<byte[]> chunkFile(String filePath) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();
            RabinFingerprint fingerprint = new RabinFingerprint();
            int byteRead;

            while ((byteRead = fis.read()) != -1) {
                chunkBuffer.write(byteRead);
                fingerprint.update(byteRead);

                if (chunkBuffer.size() >= MIN_CHUNK_SIZE && fingerprint.isBoundary()) {
                    chunks.add(chunkBuffer.toByteArray());
                    chunkBuffer.reset();
                    fingerprint.reset();
                }
            }

            if (chunkBuffer.size() > 0) {
                chunks.add(chunkBuffer.toByteArray());
            }
        }
        return chunks;
    }
}
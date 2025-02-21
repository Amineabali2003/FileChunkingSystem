package org.example.chunking;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FastCDCChunker implements ChunkerInterface {
    public List<byte[]> chunkData(byte[] data, boolean isText) {
        List<byte[]> chunks = new ArrayList<>();

        if (isText) {
            String text = new String(data, StandardCharsets.UTF_8);
            String[] words = text.split("\\s+");
            for (String word : words) {
                chunks.add(word.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            chunks.add(data);
        }

        return chunks;
    }
}

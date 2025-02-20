package org.example.chunking;

import io.github.zabuzard.fastcdc4j.external.chunking.Chunk;
import io.github.zabuzard.fastcdc4j.external.chunking.Chunker;
import io.github.zabuzard.fastcdc4j.external.chunking.ChunkerBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class FastCDCChunker implements ChunkerInterface {

    public final Chunker chunker;

    public FastCDCChunker(Chunker chunker) {
        this.chunker = chunker;
    }

    public FastCDCChunker() {
        this(new ChunkerBuilder().build());
    }

    @Override
    public List<byte[]> chunkData(byte[] data) throws IOException {
        List<byte[]> chunks = new ArrayList<>();

        for (Chunk c : chunker.chunk(data)) {
            chunks.add(c.getData());
        }
        return chunks;
    }
}

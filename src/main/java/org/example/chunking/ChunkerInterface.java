package org.example.chunking;

import java.io.IOException;
import java.util.List;

public interface ChunkerInterface {
    List<byte[]> chunkData(byte[] data) throws IOException;
}

package org.example.compression;

import java.io.IOException;

public interface CompressionServiceInterface {
    byte[] compress(byte[] data) throws IOException;

    byte[] decompress(byte[] compressedData);
}

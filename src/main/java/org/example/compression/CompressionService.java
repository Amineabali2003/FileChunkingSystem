package org.example.compression;

import com.github.luben.zstd.Zstd;
import org.springframework.stereotype.Service;

@Service
public class CompressionService {
    public byte[] compress(byte[] data) {
        return data.length < 100 ? data : Zstd.compress(data, 3);
    }

    public byte[] decompress(byte[] compressedData) {
        long decompressedSize = Zstd.decompressedSize(compressedData);
        return Zstd.decompress(compressedData, (int) decompressedSize);
    }
}
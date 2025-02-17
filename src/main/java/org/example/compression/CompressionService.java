package org.example.compression;

import net.jpountz.lz4.*;
import org.springframework.stereotype.Service;
import java.io.*;

@Service
public class CompressionService {
    public byte[] compress(byte[] data) throws IOException {
        LZ4Factory factory = LZ4Factory.fastestInstance();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LZ4Compressor compressor = factory.fastCompressor();
        LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(baos, 2048, compressor);
        lz4Out.write(data);
        lz4Out.close();
        return baos.toByteArray();
    }
}
package org.example.service;

import org.example.chunking.ChunkerInterface;
import org.example.chunking.FastCDCChunker;
import org.example.compression.CompressionServiceInterface;
import org.example.deduplication.DuplicateDetectorInterface;
import org.example.model.Chunk;
import org.example.repository.ChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileProcessorTest {

  @Mock
  private ChunkerInterface chunker;

  @Mock
  private FastCDCChunker textChunker;

  @Mock
  private DuplicateDetectorInterface deduplicator;

  @Mock
  private CompressionServiceInterface compressor;

  @Mock
  private ChunkRepository chunkRepository;

  @InjectMocks
  private FileProcessor fileProcessor;

  @TempDir
  File tempDir;

  private File testFile;
  private File textFile;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    fileProcessor = new FileProcessor(chunker, deduplicator, compressor, chunkRepository, textChunker);

    testFile = new File(tempDir, "testfile.bin");
    try (FileOutputStream fos = new FileOutputStream(testFile)) {
      fos.write(new byte[]{1, 2, 3, 4, 5});
    }

    textFile = new File(tempDir, "testfile.txt");
    try (FileOutputStream fos = new FileOutputStream(textFile)) {
      fos.write("Mardi Mercredi Lundi Jeudi Avril".getBytes(StandardCharsets.UTF_8));
    }
  }

  @Test
  void testProcessFile_SuccessfulProcessing() throws Exception {
    when(chunkRepository.existsByFilePath(testFile.getAbsolutePath())).thenReturn(false);
    when(chunker.chunkData(any())).thenReturn(List.of("chunk1".getBytes(), "chunk2".getBytes()));
    when(deduplicator.computeXXHash(any())).thenReturn(123L);
    when(compressor.compress(any())).thenReturn("compressed".getBytes());

    ArgumentCaptor<Chunk> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
    when(chunkRepository.save(chunkCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

    List<Chunk> chunks = fileProcessor.processFile(testFile.getAbsolutePath());

    assertNotNull(chunks);
    assertEquals(2, chunks.size());
    verify(chunkRepository, times(2)).save(any(Chunk.class));
  }

  @Test
  void testProcessTextFile_SuccessfulProcessing() throws Exception {
    when(chunkRepository.existsByFilePath(textFile.getAbsolutePath())).thenReturn(false);
    when(textChunker.chunkTextData(anyString())).thenReturn(
            List.of("Mardi".getBytes(), "Mercredi".getBytes(), "Lundi".getBytes(), "Jeudi".getBytes(), "Avril".getBytes())
    );
    when(deduplicator.computeXXHash(any())).thenReturn(123L);
    when(compressor.compress(any())).thenReturn("compressed".getBytes());

    List<Chunk> chunks = fileProcessor.processFile(textFile.getAbsolutePath());

    assertNotNull(chunks);
    assertEquals(5, chunks.size());
    verify(chunkRepository, times(5)).save(any(Chunk.class));
  }

  @Test
  void testProcessFile_FileNotFound() {
    Exception exception = assertThrows(Exception.class, () -> fileProcessor.processFile("invalid/path.txt"));
    assertEquals("Fichier introuvable : invalid/path.txt", exception.getMessage());
  }

  @Test
  void testProcessFile_AlreadyProcessedFile() throws Exception {
    when(chunkRepository.existsByFilePath(testFile.getAbsolutePath())).thenReturn(true);
    when(chunkRepository.findByFilePath(testFile.getAbsolutePath())).thenReturn(Collections.emptyList());

    List<Chunk> chunks = fileProcessor.processFile(testFile.getAbsolutePath());

    assertNotNull(chunks);
    assertTrue(chunks.isEmpty());
    verify(chunkRepository, never()).save(any());
  }
}
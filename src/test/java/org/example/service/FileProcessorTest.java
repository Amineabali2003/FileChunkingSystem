package org.example.service;

import org.example.chunking.ChunkerInterface;
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

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    fileProcessor = new FileProcessor(chunker, deduplicator, compressor, chunkRepository);

    // Créer un fichier temporaire pour le test
    testFile = new File(tempDir, "testfile.txt");
    try (FileOutputStream fos = new FileOutputStream(testFile)) {
      fos.write("test data for chunking".getBytes(StandardCharsets.UTF_8));
    }
  }

  @Test
  void testProcessFile_SuccessfulProcessing() throws Exception {

    when(chunkRepository.existsByFilePath(testFile.getAbsolutePath())).thenReturn(false);
    when(chunker.chunkData(any())).thenReturn(List.of("chunk1".getBytes(),
        "chunk2".getBytes()));
    when(deduplicator.isDuplicate(any())).thenReturn(false);
    when(compressor.compress(any())).thenReturn("compressed".getBytes());

    ArgumentCaptor<Chunk> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
    when(chunkRepository.save(chunkCaptor.capture())).thenAnswer(invocation -> {
      Chunk chunk = invocation.getArgument(0);
      return chunk;
    });

    List<Chunk> chunks = fileProcessor.processFile(testFile.getAbsolutePath());

    assertNotNull(chunks);
    assertEquals(2, chunks.size());

    verify(chunkRepository, times(2)).save(any(Chunk.class));
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

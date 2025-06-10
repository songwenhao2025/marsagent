package com.marsreg.document.service;

import com.marsreg.document.model.MarsregDocument;
import com.marsreg.document.repository.MarsregDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarsregDocumentServiceTest {

    @Mock
    private MarsregDocumentRepository documentRepository;

    @InjectMocks
    private MarsregDocumentService documentService;

    private MarsregDocument testDocument;

    @BeforeEach
    void setUp() {
        testDocument = new MarsregDocument();
        testDocument.setId("test-id");
        testDocument.setTitle("Test Document");
        testDocument.setContent("Test Content");
        testDocument.setType("test");
        testDocument.setMetadata("{\"key\":\"value\"}");
    }

    @Test
    void createDocument_ShouldReturnCreatedDocument() {
        when(documentRepository.save(any(MarsregDocument.class))).thenReturn(testDocument);

        MarsregDocument result = documentService.createDocument(testDocument);

        assertNotNull(result);
        assertEquals(testDocument.getId(), result.getId());
        assertEquals(testDocument.getTitle(), result.getTitle());
        verify(documentRepository).save(any(MarsregDocument.class));
    }

    @Test
    void getDocument_ShouldReturnDocument_WhenExists() {
        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));

        MarsregDocument result = documentService.getDocument(testDocument.getId());

        assertNotNull(result);
        assertEquals(testDocument.getId(), result.getId());
        verify(documentRepository).findById(testDocument.getId());
    }

    @Test
    void getDocument_ShouldReturnNull_WhenNotExists() {
        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.empty());

        MarsregDocument result = documentService.getDocument(testDocument.getId());

        assertNull(result);
        verify(documentRepository).findById(testDocument.getId());
    }

    @Test
    void updateDocument_ShouldReturnUpdatedDocument_WhenExists() {
        when(documentRepository.existsById(testDocument.getId())).thenReturn(true);
        when(documentRepository.save(any(MarsregDocument.class))).thenReturn(testDocument);

        MarsregDocument result = documentService.updateDocument(testDocument.getId(), testDocument);

        assertNotNull(result);
        assertEquals(testDocument.getId(), result.getId());
        verify(documentRepository).existsById(testDocument.getId());
        verify(documentRepository).save(any(MarsregDocument.class));
    }

    @Test
    void updateDocument_ShouldReturnNull_WhenNotExists() {
        when(documentRepository.existsById(testDocument.getId())).thenReturn(false);

        MarsregDocument result = documentService.updateDocument(testDocument.getId(), testDocument);

        assertNull(result);
        verify(documentRepository).existsById(testDocument.getId());
        verify(documentRepository, never()).save(any(MarsregDocument.class));
    }

    @Test
    void deleteDocument_ShouldReturnTrue_WhenExists() {
        when(documentRepository.existsById(testDocument.getId())).thenReturn(true);
        doNothing().when(documentRepository).deleteById(testDocument.getId());

        boolean result = documentService.deleteDocument(testDocument.getId());

        assertTrue(result);
        verify(documentRepository).existsById(testDocument.getId());
        verify(documentRepository).deleteById(testDocument.getId());
    }

    @Test
    void deleteDocument_ShouldReturnFalse_WhenNotExists() {
        when(documentRepository.existsById(testDocument.getId())).thenReturn(false);

        boolean result = documentService.deleteDocument(testDocument.getId());

        assertFalse(result);
        verify(documentRepository).existsById(testDocument.getId());
        verify(documentRepository, never()).deleteById(testDocument.getId());
    }
} 
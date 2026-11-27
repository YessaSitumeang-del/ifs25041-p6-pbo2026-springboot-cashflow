package org.delcom.ifs25041springbootcashflow.adapter.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageTest {

    @TempDir
    Path tempDir;

    private FileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new FileStorage(tempDir.toString());
    }

    private String store(String folder, String name) throws IOException {
        return storage.store(folder, name, new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }

    @Test
    void storesImageUnderGeneratedNameKeepingOnlyTheExtension() throws Exception {
        String path = store("proofs", "../../struk belanja.PNG");

        assertTrue(path.startsWith("proofs/"));
        assertTrue(path.endsWith(".png"));
        assertFalse(path.contains("struk"));
        assertTrue(Files.exists(storage.root().resolve(path)));
        assertEquals(3, Files.size(storage.root().resolve(path)));
    }

    @Test
    void acceptsAllAllowedExtensions() throws Exception {
        for (String name : new String[]{"a.png", "a.jpg", "a.jpeg", "a.webp"}) {
            assertTrue(Files.exists(storage.root().resolve(store("proofs", name))));
        }
    }

    @Test
    void rejectsNonImageExtensionsAndMissingNames() {
        assertThrows(IOException.class, () -> store("proofs", "malware.html"));
        assertThrows(IOException.class, () -> store("proofs", "vector.svg"));
        assertThrows(IOException.class, () -> store("proofs", "tanpaekstensi"));
        assertThrows(IOException.class, () -> store("proofs", null));
        assertFalse(Files.exists(storage.root().resolve("proofs")));
    }

    @Test
    void deleteRemovesAStoredFile() throws Exception {
        String path = store("proofs", "a.png");

        assertTrue(storage.delete(path));
        assertFalse(Files.exists(storage.root().resolve(path)));
        assertFalse(storage.delete(path));
    }

    @Test
    void deleteIgnoresBlankPathsAndEscapesFromRoot() throws Exception {
        Path outside = Files.createTempFile(tempDir.getParent(), "outside", ".png");
        try {
            assertFalse(storage.delete(null));
            assertFalse(storage.delete("  "));
            assertFalse(storage.delete("../" + outside.getFileName()));
            assertFalse(storage.delete("."));
            assertTrue(Files.exists(outside));
            assertTrue(Files.isDirectory(storage.root()));
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void deleteReturnsFalseWhenTargetCannotBeRemoved() throws Exception {
        store("proofs", "a.png");
        // folder tidak kosong -> deleteIfExists melempar IOException -> false
        assertFalse(storage.delete("proofs"));
    }
}

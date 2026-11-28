package org.delcom.ifs25041springbootcashflow.adapter.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Menyimpan file upload (foto profil / bukti transaksi) ke folder lokal.
 * Hanya menerima gambar (png/jpg/jpeg/webp). Nama file selalu di-generate (UUID),
 * sehingga nama dari klien tidak pernah dipakai sebagai path.
 */
public class FileStorage {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp");

    private final Path root;

    public FileStorage(String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    /**
     * Menyimpan konten ke {@code folder} dan mengembalikan path relatif terhadap root.
     *
     * @throws IOException jika format file tidak didukung atau penulisan gagal
     */
    public String store(String folder, String originalFilename, InputStream content) throws IOException {
        String extension = allowedExtensionOf(originalFilename);

        Path directory = root.resolve(folder);
        Files.createDirectories(directory);

        String filename = UUID.randomUUID() + extension;
        Path target = directory.resolve(filename);
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return folder + "/" + filename;
    }

    /**
     * Menghapus file berdasarkan path relatif. Path di luar root diabaikan (anti path traversal).
     *
     * @return true jika file benar-benar terhapus
     */
    public boolean delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        Path target = root.resolve(relativePath).normalize();
        if (target.equals(root) || !target.startsWith(root)) {
            return false;
        }
        try {
            return Files.deleteIfExists(target);
        } catch (IOException ex) {
            return false;
        }
    }

    private static String allowedExtensionOf(String originalFilename) throws IOException {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IOException("Format file tidak didukung (gunakan PNG, JPG, JPEG, atau WEBP)");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IOException("Format file tidak didukung (gunakan PNG, JPG, JPEG, atau WEBP)");
        }
        return ext;
    }
}

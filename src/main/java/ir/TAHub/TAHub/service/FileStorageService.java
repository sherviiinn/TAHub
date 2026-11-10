package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.StoredFile;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Keeps uploaded files on the server's disk, in tahub.storage.dir (default "data/files", which is in .gitignore).
 * The folder is NOT a public web folder: a file is only sent by a controller after it checks who is asking.
 * Every file gets a random name from the server, so a user can never choose or guess a path.
 * Today only PDFs are accepted (questions now; answers, notes and card photos come later).
 */
@Service
public class FileStorageService {

    /** The biggest PDF we keep (decided: 15 MB). The multipart limit in application.properties is higher on purpose. */
    public static final long MAX_PDF_BYTES = 15L * 1024 * 1024;

    private static final byte[] PDF_START = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final int ORIGINAL_NAME_MAX = 255;

    private final StoredFileRepository storedFileRepository;
    private final Path root;

    public FileStorageService(StoredFileRepository storedFileRepository,
                              @Value("${tahub.storage.dir:data/files}") String storageDir) {
        this.storedFileRepository = storedFileRepository;
        this.root = Path.of(storageDir).toAbsolutePath().normalize();
    }

    /** True when the user chose a file in the form (an empty file input sends an empty part). */
    public static boolean isChosen(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    /**
     * Checks and keeps a PDF. Throws FileRejectedException (with a message key) when it is empty,
     * too big, or not really a PDF (the first bytes are checked, not the name or the browser's type).
     */
    public StoredFile savePdf(MultipartFile file, User uploader) {
        if (!isChosen(file)) {
            throw new FileRejectedException("files.error.required");
        }
        if (file.getSize() > MAX_PDF_BYTES) {
            throw new FileRejectedException("files.error.tooBig");
        }
        if (!startsLikePdf(file)) {
            throw new FileRejectedException("files.error.notPdf");
        }

        String storedName = UUID.randomUUID() + ".pdf";
        Path target = root.resolve(storedName);
        try {
            Files.createDirectories(root);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the uploaded file", e);
        }

        StoredFile stored = new StoredFile();
        stored.setStoredName(storedName);
        stored.setOriginalName(cleanOriginalName(file.getOriginalFilename()));
        stored.setContentType("application/pdf");
        stored.setSizeBytes(file.getSize());
        stored.setUploadedBy(uploader);
        stored.setUploadedAt(Instant.now());
        return storedFileRepository.save(stored);
    }

    /** Where the file is on disk (only for sending it back). */
    public Path pathOf(StoredFile file) {
        Path path = root.resolve(file.getStoredName()).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalStateException("Stored file outside the storage folder");
        }
        return path;
    }

    public boolean exists(StoredFile file) {
        return Files.isRegularFile(pathOf(file));
    }

    /** Removes the file from the disk and from the database (for a file nobody uses any more). */
    public void delete(StoredFile file) {
        try {
            Files.deleteIfExists(pathOf(file));
        } catch (IOException e) {
            // The row is removed anyway; a file left on disk does no harm.
        }
        storedFileRepository.delete(file);
    }

    /**
     * A safe file name for a download: no slashes or other characters Windows does not allow,
     * no line breaks, not too long, and always ending in ".pdf".
     */
    public static String downloadName(String wanted) {
        String name = wanted == null ? "" : wanted;
        name = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").replaceAll("\\s+", " ").trim();
        if (name.isEmpty()) {
            name = "file";
        }
        if (name.length() > 150) {
            name = name.substring(0, 150).trim();
        }
        return name + ".pdf";
    }

    private boolean startsLikePdf(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] start = in.readNBytes(PDF_START.length);
            return Arrays.equals(start, PDF_START);
        } catch (IOException e) {
            return false;
        }
    }

    private String cleanOriginalName(String original) {
        if (original == null || original.isBlank()) {
            return "file.pdf";
        }
        // Some browsers send the whole path ("C:\Users\...\hw1.pdf"); keep only the last part.
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) {
            name = "file.pdf";
        }
        return name.length() > ORIGINAL_NAME_MAX ? name.substring(name.length() - ORIGINAL_NAME_MAX) : name;
    }
}

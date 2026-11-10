package ir.TAHub.TAHub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A file kept on the server's disk (FileStorageService), outside the public web folders.
 * The file on disk has a random name made by the server; the name the user uploaded is kept here only to show it.
 * Used by assignments (the question PDF) now, and later by submissions, notes and student card photos.
 */
@Entity
@Table(name = "stored_files")
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The name on disk, for example "3f2a...c1.pdf". Never shown and never taken from the user. */
    @Column(nullable = false, unique = true, length = 80)
    private String storedName;

    /** The name of the file on the uploader's computer (only for showing). */
    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private Instant uploadedAt;

    public StoredFile() {
    }

    public Long getId() { return id; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}

package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}

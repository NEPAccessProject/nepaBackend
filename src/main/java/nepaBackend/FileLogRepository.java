package nepaBackend;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import nepaBackend.model.EISDoc;
import nepaBackend.model.FileLog;

public interface FileLogRepository extends JpaRepository<FileLog, Long> {
	List<FileLog> findAll();

	boolean existsByFilename(String string);

	boolean existsByFilenameAndImported(String string, boolean b);

	List<FileLog> findAllByDocumentId(Long id);

	Optional<FileLog> findByDocumentIdAndFilenameAndImportedIn(Long id, String fullPath, boolean b);
}
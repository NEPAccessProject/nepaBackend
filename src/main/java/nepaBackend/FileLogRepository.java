package nepaBackend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nepaBackend.model.FileLog;

public interface FileLogRepository extends JpaRepository<FileLog, Long> {
	List<FileLog> findAll();

	boolean existsByFilename(String string);

	boolean existsByFilenameAndImported(String string, boolean b);
}
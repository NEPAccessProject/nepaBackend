package nepaBackend;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nepaBackend.model.EISDoc;
import nepaBackend.model.NEPAFile;

public interface NEPAFileRepository extends JpaRepository<NEPAFile, Long> {

	// Declare automatically generated methods here

//	boolean existsByEisdocAndFilename(EISDoc eisdoc, String filename);

	List<NEPAFile> findAllByEisdoc(EISDoc eis);

	boolean existsByFilenameAndEisdocIn(String filenameWithoutPath, EISDoc eisdoc);

	boolean existsByFilenameAndRelativePathIn(String filenameWithoutPath, String pathOnly);

	Optional<NEPAFile> findByEisdoc(EISDoc eisDoc);

	Optional<NEPAFile> findByEisdocAndFilenameIn(EISDoc eisDoc, String filename);

	Optional<NEPAFile> findByDocumentTypeAndEisdocAndFolderAndFilenameAndRelativePathIn(String documentType, EISDoc doc, String folder,
			String filename, String relativePath);

	boolean existsByEisdocAndFilenameIn(EISDoc eisdoc, String extractedFilename);


//	boolean existsByEisdoc(EISDoc eis);

	
	/** likely one-time-use deduplication helper */
	@Query(value = "select * from nepafile where LOCATE(\"/Document_collection_MASTER/\",relative_path);",
			nativeQuery = true)
	List<NEPAFile> getGarbage();
	
}

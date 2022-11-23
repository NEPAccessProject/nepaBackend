package nepaBackend;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	List<NEPAFile> findAllByFolderAndEisdocIn(String folder, EISDoc eisdoc);
	
	/** Find all NEPAFiles that have more than one source for their files. 
	 *  In some situations this can indicate duplicated files and therefore also duplicate indexed tests,
	 *  even if the files are different sizes, names and quantities, because potentially multiple agencies
	 *  have their own files for the same EIS - but it's not standardized, thus the likely differences */
	@Query(value = "SELECT n1.* FROM nepafile n1 JOIN nepafile n2 ON n1.document_id = n2.document_id " + 
			"WHERE n1.folder != n2.folder;", nativeQuery = true)
	List<NEPAFile> getMultiFolder();


	@Query(value = "SELECT COUNT(DISTINCT (n1.folder)) FROM nepafile n1 WHERE n1.document_id = :id", 
			nativeQuery = true)
	Long countDistinctFoldersByDocumentId(@Param("id") Long id);

	@Query(value = "SELECT DISTINCT (n1.folder) FROM nepafile n1 WHERE n1.document_id = :id", 
			nativeQuery = true)
	List<String> getDistinctFoldersByDocumentId(@Param("id") Long id);


//	boolean existsByEisdoc(EISDoc eis);

	
	/** likely one-time-use deduplication helper */
	@Query(value = "select * from nepafile where LOCATE(\"/Document_collection_MASTER/\",relative_path);",
			nativeQuery = true)
	List<NEPAFile> getGarbage();


	
}

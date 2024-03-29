package nepaBackend;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;

public interface TextRepository extends JpaRepository<DocumentText, Long>, CustomizedTextRepository {

	// Declare automatically generated methods here

	boolean existsByEisdocAndFilename(EISDoc eisdoc, String filename);

	List<DocumentText> findAllByEisdoc(EISDoc eis);
	
	Optional<DocumentText> findById(long id);

	boolean existsByEisdoc(EISDoc eis);

	boolean existsByEisdocAndFilenameIn(EISDoc foundDoc, String filename);

	Optional<DocumentText> findByEisdocAndFilenameIn(EISDoc eisDoc, String filename);

	/** this'll allow filenames returned (and therefore downloads) even if the indexing didn't work. */
	@Query(value = "SELECT filename FROM nepafile WHERE " +
			"(nepafile.document_id = :document_id) " +
			"LIMIT 100",
			nativeQuery = true)
	List<String> findFilenameByDocumentId(@Param("document_id") long document_id);
	/** old version: only returns filenames if files were successfully converted to text and indexed. */
	@Query(value = "SELECT filename FROM document_text WHERE " +
			"(document_text.document_id = :document_id) " +
			"LIMIT 100",
			nativeQuery = true)
	List<String> findFilenameByDocumentIdOld(@Param("document_id") long document_id);

	@Query(value = "SELECT id FROM document_text WHERE " +
			"(length(document_text.plaintext) = :len) " +
			"LIMIT 100",
			nativeQuery = true)
	List<BigInteger> findIdsByPlaintextLength(@Param("len") int len);

	@Query(value = "SELECT length(plaintext) FROM document_text WHERE " +
			"(document_text.id = :id) " +
			"LIMIT 1",
			nativeQuery = true)
	int findPlaintextLengthById(@Param("id") long id);

	int getTotalHits(String field);

	
	
	// probably one-time-use
	List<DocumentText> findAllByEisdocAndFilenameIn(EISDoc eisdoc, String filename);
}

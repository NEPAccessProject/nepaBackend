package nepaBackend;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import nepaBackend.controller.MetadataWithContext;
import nepaBackend.enums.SearchType;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.pojo.SearchInputs;

public interface TextRepository extends JpaRepository<DocumentText, Long>, CustomizedTextRepository {

	// Declare automatically generated methods here

	boolean existsByEisdocAndFilename(EISDoc eisdoc, String filename);

	List<DocumentText> findAllByEisdoc(EISDoc eis);
	
	Optional<DocumentText> findById(long id);

	boolean existsByEisdoc(EISDoc eis);

	boolean existsByEisdocAndFilenameIn(EISDoc foundDoc, String filename);

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

	Optional<DocumentText> findByEisdocAndFilenameIn(EISDoc eisDoc, String filename);
}

package nepaBackend;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;

public interface TextRepository extends JpaRepository<DocumentText, Long>, CustomizedTextRepository {

	// Declare automatically generated methods here

	boolean existsByEisdocAndFilename(EISDoc eisdoc, String filename);

	List<DocumentText> findAllByEisdoc(EISDoc eis);

	boolean existsByEisdoc(EISDoc eis);

	boolean existsByEisdocAndFilenameIn(EISDoc foundDoc, String filename);
	
}

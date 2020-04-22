package nepaBackend;

import org.springframework.data.repository.CrudRepository;

import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;

public interface TextRepository extends CrudRepository<DocumentText, Long>, CustomizedTextRepository {

	// Declare automatically generated methods here

	boolean existsByEisdocAndFilename(EISDoc eisdoc, String filename);
	
}

package nepaBackend;

import org.springframework.data.repository.CrudRepository;

import nepaBackend.model.DocumentText;

public interface TextRepository extends CrudRepository<DocumentText, Long>, CustomizedTextRepository {
	// Declare automatically generated methods here
}

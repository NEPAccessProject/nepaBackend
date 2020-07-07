package nepaBackend;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import nepaBackend.model.EISDoc;
import nepaBackend.model.NEPAFile;

public interface NEPAFileRepository extends JpaRepository<NEPAFile, Long> {

	// Declare automatically generated methods here

//	boolean existsByEisdocAndFilename(EISDoc eisdoc, String filename);

	List<NEPAFile> findAllByEisdoc(EISDoc eis);

//	boolean existsByEisdoc(EISDoc eis);
	
}

package nepaBackend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nepaBackend.model.EISDoc;
import nepaBackend.model.GeojsonLookup;

@Repository
public interface GeojsonLookupRepository extends JpaRepository<GeojsonLookup, Long> {

	List<GeojsonLookup> findAllByEisdoc(EISDoc doc);
	List<GeojsonLookup> findAllByEisdocIn(List<EISDoc> docList);

	boolean existsByEisdoc(EISDoc eisDoc);


}

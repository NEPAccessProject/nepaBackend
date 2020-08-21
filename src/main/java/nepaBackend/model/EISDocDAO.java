package nepaBackend.model;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface EISDocDAO extends CrudRepository<EISDoc,Long>,JpaSpecificationExecutor<EISDoc> {
	
}
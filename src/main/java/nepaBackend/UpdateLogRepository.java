package nepaBackend;
import org.springframework.data.jpa.repository.JpaRepository;

import nepaBackend.model.UpdateLog;

// TODO: Get by date range etc.
public interface UpdateLogRepository extends JpaRepository<UpdateLog, Long> {
	
}
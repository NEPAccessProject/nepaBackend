package nepaBackend;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nepaBackend.model.UpdateLog;

// TODO: Get by date range
// TODO: Get most recent by document ID
// TODO: Get all by user ID
public interface UpdateLogRepository extends JpaRepository<UpdateLog, Long> {

	List<UpdateLog> findAllByDocumentId(Long id);

	@Query(value = 
			"SELECT * FROM test.update_log where document_id = :id order by saved_time desc limit 1;",
			nativeQuery = true)
	Optional<UpdateLog> getMostRecentByDocumentId(@Param("id") Long id);

	@Query(value = 
			"SELECT * FROM test.update_log where document_id = :id and user_id = :userid and saved_time >= :date_time order by saved_time asc limit 1;",
			nativeQuery = true)
	Optional<UpdateLog> getByDocumentIdAfterDateTimeByUser(
			@Param("id") Long id, 
			@Param("date_time") String datetime, 
			@Param("userid") Long user);
	
}
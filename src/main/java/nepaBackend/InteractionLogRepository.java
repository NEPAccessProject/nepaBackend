package nepaBackend;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nepaBackend.model.InteractionLog;
import nepaBackend.model.UpdateLog;

public interface InteractionLogRepository extends JpaRepository<InteractionLog, Long> {

	List<InteractionLog> findAllByUserId(Long id);

	@Query(value = 
			"SELECT * FROM test.interaction_log where document_id = :id order by saved_time desc limit 1;",
			nativeQuery = true)
	Optional<UpdateLog> getMostRecentByDocumentId(@Param("id") Long id);

	@Query(value = 
			"SELECT * FROM test.interaction_log where log_time >= :start and log_time <= :end",
			nativeQuery = true)
	List<InteractionLog> getFromDateRange(
			@Param("start") String start, 
			@Param("end") String end);

	@Query(value = 
			"SELECT * FROM test.interaction_log where user_id = :userid and log_time >= :date_start and log_time <= :date_end",
			nativeQuery = true)
	List<InteractionLog> getFromDateRangeAndUser(
			@Param("date_start") String date_start, 
			@Param("date_end") String date_end,
			@Param("userid") Long userid);

	
}
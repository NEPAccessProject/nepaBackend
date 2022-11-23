package nepaBackend;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nepaBackend.model.UserStatusLog;

public interface UserStatusLogRepository extends JpaRepository<UserStatusLog, Long> {

	List<UserStatusLog> findAllByUserId(Long id);

	@Query(value = 
			"SELECT * FROM user_status_log where document_id = :id order by saved_time desc limit 1;",
			nativeQuery = true)
	Optional<UserStatusLog> getMostRecentByDocumentId(@Param("id") Long id);

	@Query(value = 
			"SELECT * FROM user_status_log where log_time >= :start and log_time <= :end",
			nativeQuery = true)
	List<UserStatusLog> getFromDateRange(
			@Param("start") String start, 
			@Param("end") String end);

	@Query(value = 
			"SELECT * FROM user_status_log where user_id = :userid and log_time >= :date_start and log_time <= :date_end",
			nativeQuery = true)
	List<UserStatusLog> getFromDateRangeAndUser(
			@Param("date_start") String date_start, 
			@Param("date_end") String date_end,
			@Param("userid") Long userid);

	
}
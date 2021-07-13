package nepaBackend;
import java.math.BigInteger;
import java.util.ArrayList;
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

	@Query(value = 
			"SELECT * FROM test.update_log where document_id = :id and saved_time >= :date_time order by saved_time asc limit 1;",
			nativeQuery = true)
	Optional<UpdateLog> getByDocumentIdAfterDateTime(
			@Param("id") Long id, 
			@Param("date_time") String datetime);
	

	@Query(value = 
			"SELECT * FROM test.update_log where document_id = :id and user_id = :userid and id >= :id_start order by id asc limit 1;",
			nativeQuery = true)
	Optional<UpdateLog> getByDocumentIdAfterIdByUser(
			@Param("id") Long id, 
			@Param("id_start") String idStart,
			@Param("userid") Long user);
	
	@Query(value = 
			"SELECT * FROM test.update_log where document_id = :id and id >= :id_start order by id asc limit 1;",
			nativeQuery = true)
	Optional<UpdateLog> getByDocumentIdAfterId(
			@Param("id") Long id, 
			@Param("id_start") String idStart);
	
	

	@Query(value = 
			"SELECT DISTINCT document_id FROM test.update_log where saved_time >= :start and saved_time <= :end",
			nativeQuery = true)
	List<BigInteger> getDistinctDocumentsFromDateRange(
			@Param("start") String start, 
			@Param("end") String end);

	@Query(value = 
			"SELECT DISTINCT document_id FROM test.update_log where user_id = :userid and saved_time >= :date_start and saved_time <= :date_end",
			nativeQuery = true)
	List<BigInteger> getDistinctDocumentsFromDateRangeAndUser(
			@Param("date_start") String start, 
			@Param("date_end") String end,
			@Param("userid") Long userid);

	@Query(value = 
			"SELECT DISTINCT document_id FROM test.update_log where id >= :id_start and id <= :id_end",
			nativeQuery = true)
	List<BigInteger> getDistinctDocumentsFromIdRange(
			@Param("id_start") String idStart, 
			@Param("id_end") String idEnd);

	@Query(value = 
			"SELECT DISTINCT document_id FROM test.update_log where user_id = :userid and id >= :id_start and id <= :id_end",
			nativeQuery = true)
	List<BigInteger> getDistinctDocumentsFromIdRangeAndUser(String idStart, String idEnd, Long userid);
	
}
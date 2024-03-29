package nepaBackend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nepaBackend.model.SearchLog;

// TODO: Get by date range etc.
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
	
	List<SearchLog> findAllByUserId(String userId);
	
	/** @return List of top 50 searches containing: Long (count); String (title) 
	 * since May '21 (closed beta starting time) */
	@Query(
			value = "SELECT terms,count(terms) FROM search_log "
					+ "WHERE terms IS NOT NULL "
					+ "AND search_time > '2021-05-01' "
					+ "GROUP BY terms "
					+ "ORDER BY count(terms) DESC "
					+ "LIMIT 50", 
			nativeQuery=true
	) 
	List<Object> countDistinctTerms();
	
	/** @return List of top 50 searches containing: Long (count); String (title) */
	@Query(
			value = "SELECT terms,count(terms) FROM search_log "
					+ "WHERE terms IS NOT NULL "
					+ "GROUP BY terms "
					+ "ORDER BY count(terms) DESC "
					+ "LIMIT 50", 
			nativeQuery=true
	) 
	List<Object> countDistinctLegacyTerms();
	
	@Query(
			value = "SELECT a.username,terms,search_time,search_mode FROM search_log JOIN application_user a ON a.id = user_id WHERE terms IS NOT NULL "
					+ "AND user_id != 30 AND search_time > '2021-05-01' ORDER BY search_time DESC;",
			nativeQuery=true)
	List<Object> findAllWithUsername();

	@Query(
			value = "SELECT * FROM search_log WHERE user_id IS NOT NULL;",
			nativeQuery=true)
	List<SearchLog> findAllWithUser();
	
}
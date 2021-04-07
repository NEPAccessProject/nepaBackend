package nepaBackend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nepaBackend.model.SearchLog;

// TODO: Get by date range etc.
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
	List<SearchLog> findAllByUserId(String userId);
	
	/** @return List of top 50 searches containing: Long (count); String (title) */
	@Query(
			value = "SELECT title,count(title) FROM test.search_log "
					+ "WHERE title IS NOT NULL "
					+ "GROUP BY title "
					+ "ORDER BY count(title) DESC "
					+ "LIMIT 50", 
			nativeQuery=true
	) 
	List<Object> countDistinctTitles();
}
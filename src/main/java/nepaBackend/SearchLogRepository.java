package nepaBackend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nepaBackend.model.SearchLog;

// TODO: Get by date range etc.
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
//	List<SearchLog> findAll(); // haven't researched if this is a thing
	List<SearchLog> findAllByUserId(String userId);
}
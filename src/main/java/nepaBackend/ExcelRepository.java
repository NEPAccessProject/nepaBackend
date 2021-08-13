package nepaBackend;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nepaBackend.model.Excel;

public interface ExcelRepository extends JpaRepository<Excel, Long> {

	@Query(value = 
			"SELECT * FROM test.excel ORDER BY saved_time DESC LIMIT 1;",
			nativeQuery = true)
	Optional<Excel> findMostRecent();
}
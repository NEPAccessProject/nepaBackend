package nepaBackend;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nepaBackend.model.EISMatch;

@Repository
public interface EISMatchRepository extends JpaRepository<EISMatch, Long>{
	/** Return list of matches where id appears in either document1 or document2
	 * and match_percent is greater or equal than found value
	 * @param id
	 * @param match_percent
	 * @return
	 */
	@Query(value = "SELECT match_id, document1, document2, match_percent FROM eismatch WHERE " +
			"(eismatch.match_percent >= :match_percent) " +
			"AND " +
			"((eismatch.document1 = :id) " +
			"OR (eismatch.document2 = :id)) " +
			"ORDER BY eismatch.match_percent DESC " +
			"LIMIT 1000",
			nativeQuery = true)
	List<EISMatch> queryBy(@Param("id") int id, 
							@Param("match_percent") BigDecimal match_percent);
}

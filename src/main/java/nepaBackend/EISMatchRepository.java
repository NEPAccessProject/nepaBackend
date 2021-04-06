package nepaBackend;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
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
			"LIMIT 15000",
			nativeQuery = true)
	List<EISMatch> queryBy(@Param("id") int id, 
							@Param("match_percent") BigDecimal match_percent);
	
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
			"LIMIT 15000",
			nativeQuery = true)
	List<EISMatch> queryBy(@Param("id") Long id, 
							@Param("match_percent") BigDecimal match_percent);

	
	
	/** Return list of matches where match_percent is greater or equal to .5
	 * Includes IDs, titles, filenames, match %
	 */
	@Query(value = "SELECT document1, "
			+ "eisdoc1.title AS title1, "
			+ "eisdoc1.filename AS filename1, "
			+ "eisdoc1.agency AS agency1, "
			+ "eisdoc1.document_type AS type1, "
			+ "eisdoc1.register_date AS date1, "
			+ "document2, "
			+ "eisdoc2.title AS title2, "
			+ "eisdoc2.filename AS filename2, "
			+ "eisdoc2.agency AS agency2, "
			+ "eisdoc2.document_type AS type2, "
			+ "eisdoc2.register_date AS date2, "
			+ "match_percent "
			+ "FROM test.eismatch "
			+ "INNER JOIN eisdoc eisdoc1 ON (eismatch.document1 = eisdoc1.id) "
			+ "INNER JOIN eisdoc eisdoc2 ON (eismatch.document2 = eisdoc2.id) "
			+ "WHERE match_percent > 0.5 "
			+ "ORDER BY document1 ASC, document2 ASC;",
			nativeQuery = true)
	List<Object> getMetaPairs();

	/** Return list of matches where match_percent is greater or equal to .5
	 * Includes IDs, titles, filenames, match %
	 */
	@Query(value = "SELECT document1, "
			+ "eisdoc1.title AS title1, "
			+ "eisdoc1.filename AS filename1, "
			+ "eisdoc1.agency AS agency1, "
			+ "eisdoc1.document_type AS type1, "
			+ "eisdoc1.register_date AS date1, "
			+ "document2, "
			+ "eisdoc2.title AS title2, "
			+ "eisdoc2.filename AS filename2, "
			+ "eisdoc2.agency AS agency2, "
			+ "eisdoc2.document_type AS type2, "
			+ "eisdoc2.register_date AS date2, "
			+ "match_percent "
			+ "FROM test.eismatch "
			+ "INNER JOIN eisdoc eisdoc1 ON (eismatch.document1 = eisdoc1.id) "
			+ "INNER JOIN eisdoc eisdoc2 ON (eismatch.document2 = eisdoc2.id) "
			+ "WHERE match_percent > 0.5 "
			+ "AND (eisdoc1.size > 77 || eisdoc2.size > 77)"
			+ "ORDER BY document1 ASC, document2 ASC;",
			nativeQuery = true)
	List<Object> getMetaPairsAtLeastOneFile();

	/** Return list of matches where match_percent is greater or equal to .5
	 * Includes IDs, titles, filenames, match %
	 */
	@Query(value = "SELECT document1, "
			+ "eisdoc1.title AS title1, "
			+ "eisdoc1.filename AS filename1, "
			+ "eisdoc1.agency AS agency1, "
			+ "eisdoc1.document_type AS type1, "
			+ "eisdoc1.register_date AS date1, "
			+ "document2, "
			+ "eisdoc2.title AS title2, "
			+ "eisdoc2.filename AS filename2, "
			+ "eisdoc2.agency AS agency2, "
			+ "eisdoc2.document_type AS type2, "
			+ "eisdoc2.register_date AS date2, "
			+ "match_percent "
			+ "FROM test.eismatch "
			+ "INNER JOIN eisdoc eisdoc1 ON (eismatch.document1 = eisdoc1.id) "
			+ "INNER JOIN eisdoc eisdoc2 ON (eismatch.document2 = eisdoc2.id) "
			+ "WHERE match_percent > 0.5 "
			+ "AND (eisdoc1.size > 77 && eisdoc2.size > 77)"
			+ "ORDER BY document1 ASC, document2 ASC;",
			nativeQuery = true)
	List<Object> getMetaPairsTwoFiles();
}

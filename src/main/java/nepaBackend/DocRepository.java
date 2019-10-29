package nepaBackend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nepaBackend.model.EISDoc;
import nepaBackend.model.EISDocMatchJoin;
import nepaBackend.model.EISMatch;

@Repository
public interface DocRepository extends JpaRepository<EISDoc, Long> {

	// TODO: Can change to <Object> and then JOIN with eismatch to get everything,
	// but column names are lost.  Alternative is to try again to set up
	// relationships between the model entities
	/**
	 * Returns distinct list of EISDocs whose IDs appear in document1/document2 
	 * column, excluding the EISDoc whose ID is provided as the first parameter.
	 * @param id
	 * @param matches
	 * @return
	 */
	@Query(value = "SELECT DISTINCT * FROM eisdoc e"
			+ " WHERE"
			+ " (e.id IN :idList1"
			+ " OR e.id IN :idList2)"
			+ " AND (e.id != :id)",
			nativeQuery = true)
	List<EISDoc> queryBy(@Param("id") int id,
			@Param("idList1") List<Integer> idList1, 
			@Param("idList2") List<Integer> idList2);
}

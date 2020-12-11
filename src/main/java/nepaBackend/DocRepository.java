package nepaBackend;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nepaBackend.model.ApplicationUser;
import nepaBackend.model.EISDoc;
import nepaBackend.model.EISDocMatchJoin;
import nepaBackend.model.EISMatch;

@Repository
public interface DocRepository extends JpaRepository<EISDoc, Long> {

	// Note: Can change to <Object> and then JOIN with eismatch to get everything 
	// in one list of objects, but column names are lost.  
	// Alternative is to try again to set up relationships between the model entities
	/**
	 * Returns distinct list of EISDocs whose IDs appear in document1/document2 
	 * column, excluding the EISDoc whose ID is provided as the first parameter.
	 * @param id
	 * @param matches
	 * @return
	 */
	@Query(value = "SELECT DISTINCT * FROM eisdoc e"
			+ " WHERE"
			+ " (e.id != :id)"
			+ " AND "
			+ " (e.id IN :idList1"
			+ " OR e.id IN :idList2)",
			nativeQuery = true)
	List<EISDoc> queryBy(@Param("id") int id,
			@Param("idList1") List<Integer> idList1, 
			@Param("idList2") List<Integer> idList2);

//	@Query(value = "SELECT DISTINCT title FROM eisdoc"
//			+ " ORDER BY title"
//			+ " LIMIT 1000000",
//			nativeQuery = true)
//	List<String> queryAllTitles();

	// TODO: Do a natural language mode search to get top X (5-10?) suggestions
	// and then plug them into the search box as selectable suggestions
	@Query(value = "SELECT DISTINCT title FROM eisdoc"
			+ " WHERE MATCH(title) AGAINST(? IN NATURAL LANGUAGE MODE)"
			+ " LIMIT 5",
			nativeQuery = true)
	List<String> queryByTitle(@Param("titleInput") String titleInput);
	
	Optional<EISDoc> findById(long id);

	@Query(value = "SELECT * FROM eisdoc"
			+ " WHERE LENGTH(filename) > 0",
			nativeQuery = true)
	List<EISDoc> findByFilenameNotEmpty();

	List<EISDoc> findAllByTitle(String title);

	Optional<EISDoc> findTopByFilename(String filename);

	Optional<EISDoc> findTopByTitleAndDocumentTypeIn(String title, String documentType);

	Optional<EISDoc> findTopByTitleAndDocumentTypeAndRegisterDateIn(String title, String type, LocalDate registerDate);

	List<EISDoc> findAllByFolder(String folder); // TODO: Enforce uniqueness of foldername if non-empty?

	Optional<EISDoc> findTopByFolder(String folderName);

	long countByFolder(String folderName);

	Optional<EISDoc> findTopByFolderAndDocumentTypeIn(String folder, String documentType);

	/**
	 * Returns distinct list of EISDocs whose IDs appear in document1/document2 
	 * column, excluding the EISDoc whose ID is provided as the first parameter.
	 * @param id
	 * @param matches
	 * @return
	 */
	@Query(value = "SELECT DISTINCT * FROM eisdoc e"
			+ " WHERE"
			+ " (e.id != :id)"
			+ " AND "
			+ " (e.id IN :idList1"
			+ " OR e.id IN :idList2)",
			nativeQuery = true)
	List<EISDoc> queryBy(@Param("id") Long id,
			@Param("idList1") List<Integer> idList1, 
			@Param("idList2") List<Integer> idList2);

	/** Return count of each type */
	@Query(value = "SELECT document_type, COUNT(*) "
			+ "FROM test.eisdoc "
			+ "GROUP BY document_type",
			nativeQuery = true)
	public List<Object> getTypeCount();
	
	/** Return count of filenames by document type */
	@Query(value = "SELECT document_type, COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE LENGTH(filename)>0 "
			+ "GROUP BY document_type;",
			nativeQuery = true)
	public List<Object> getDownloadableCountByType();
	
//	@Query(value = "SELECT document_type, YEAR(register_date), COUNT(*) "
//			+ "FROM test.eisdoc "
//			+ "GROUP BY YEAR(register_date), document_type "
//			+ "ORDER BY document_type, YEAR(register_date);",
//			nativeQuery = true)
//	public List<Object> getCountByTypeAndYear();

	/** Return counts of drafts and finals by year */
	@Query(value = "SELECT document_type, YEAR(register_date), COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Final' OR document_type='Draft' "
			+ "GROUP BY YEAR(register_date), document_type "
			+ "ORDER BY document_type, YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getDraftFinalCountByYear();


	/** Return downloadable counts of drafts and finals by year */
	@Query(value = "SELECT document_type, YEAR(register_date), COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE (document_type='Final' OR document_type='Draft') "
			+ "AND LENGTH(filename)>0 "
			+ "GROUP BY YEAR(register_date), document_type "
			+ "ORDER BY document_type, YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getDownloadableDraftFinalCountByYear();

	/** Return counts of drafts and finals by state */
	@Query(value = "SELECT document_type, state, COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Final' OR document_type='Draft' "
			+ "GROUP BY state, document_type "
			+ "ORDER BY document_type, state;",
			nativeQuery = true)
	public List<Object> getDraftFinalCountByState();

	/** Return count of drafts and finals by agency */
	@Query(value = "SELECT document_type, agency, COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Final' OR document_type='Draft' "
			+ "GROUP BY agency, document_type "
			+ "ORDER BY document_type, agency;",
			nativeQuery = true)
	public List<Object> getDraftFinalCountByAgency();

	@Query(value = "SELECT DISTINCT document_type FROM test.eisdoc ORDER BY document_type;",
			nativeQuery = true)
	public List<String> getDocumentTypes();

	@Query(value = "SELECT DISTINCT agency FROM test.eisdoc ORDER BY agency;",
			nativeQuery = true)
	public List<String> getAgencies();
	
	@Query(value = "SELECT DISTINCT state FROM test.eisdoc ORDER BY state;",
			nativeQuery = true)
	public List<String> getStates();

	@Query(value = "SELECT DISTINCT YEAR(register_date) "
			+ "FROM test.eisdoc "
			+ "ORDER BY register_date "
			+ "DESC;",
			nativeQuery = true)
	public List<String> getYears();

	@Query(value = "SELECT COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Final' "
			+ "AND LENGTH(filename)>0;",
			nativeQuery = true)
	long getFinalCountDownloadable();

	@Query(value = "SELECT COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Final';",
			nativeQuery = true)
	long getFinalCount();

	@Query(value = "SELECT COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Draft' "
			+ "AND LENGTH(filename)>0;",
			nativeQuery = true)
	long getDraftCountDownloadable();
	
	@Query(value = "SELECT COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Draft';",
			nativeQuery = true)
	long getDraftCount();
	
}

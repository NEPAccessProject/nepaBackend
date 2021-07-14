package nepaBackend;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nepaBackend.model.EISDoc;

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

	@Query(value = "SELECT * FROM eisdoc"
			+ " WHERE filename IS NOT NULL"
			+ " AND LENGTH(filename) > 0",
			nativeQuery = true)
	List<EISDoc> findAllWithFilenames();

	List<EISDoc> findAllByTitle(String title);

	Optional<EISDoc> findTopByFilename(String filename);

	Optional<EISDoc> findTopByTitleAndDocumentTypeIn(String title, String documentType);

	Optional<EISDoc> findTopByTitleAndDocumentTypeAndRegisterDateIn(String title, String type, LocalDate registerDate);

	List<EISDoc> findAllByFolder(String folder);

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
//	@Query(value = "SELECT document_type, COUNT(*) "
//			+ "FROM test.eisdoc "
//			+ "WHERE LENGTH(filename)>0 "
//			+ "GROUP BY document_type;",
//			nativeQuery = true)
//	public List<Object> getDownloadableCountByType();

	/** Return count of valid size by document type */
	@Query(value = "SELECT document_type, COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE size>200 "
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


	/** Return metadata counts by year */
	@Query(value = "SELECT YEAR(register_date), COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE (document_type='Final' OR document_type='Draft') "
			+ "GROUP BY YEAR(register_date) "
			+ "ORDER BY YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getMetadataCountByYear();
	/** Return downloadable counts by year according to filename existing */
//	@Query(value = "SELECT YEAR(register_date), COUNT(*) "
//			+ "FROM test.eisdoc "
//			+ "WHERE (document_type='Final' OR document_type='Draft') "
//			+ "AND LENGTH(filename)>0 "
//			+ "GROUP BY YEAR(register_date) "
//			+ "ORDER BY YEAR(register_date) "
//			+ "DESC;",
//			nativeQuery = true)
//	public List<Object> getDownloadableCountByYear();
	/** Return downloadable counts by year according to file size */
	@Query(value = "SELECT YEAR(register_date), COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE (document_type='Final' OR document_type='Draft') "
			+ "AND size>200 "
			+ "GROUP BY YEAR(register_date) "
			+ "ORDER BY YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getDownloadableCountByYear();

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
			+ "ORDER BY YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getYears();

	@Query(value = "SELECT COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Final' "
			+ "AND size>200;",
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
			+ "AND size>200;",
			nativeQuery = true)
	long getDraftCountDownloadable();
	
	@Query(value = "SELECT COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Draft';",
			nativeQuery = true)
	long getDraftCount();
	
	List<EISDoc> findAllByAgency(String agency);

	@Query(value = "SELECT * "
			+ "FROM test.eisdoc "
			+ "WHERE size<=200 "
			+ "OR size is null;",
			nativeQuery = true)
	List<EISDoc> findMissingSize();

	/** For finding documents that expect files on disk (filename or folder listed) 
	 * but have no size recorded, implying they're missing the actual files (nothing to download/index) */
	@Query(value = "SELECT id,filename,folder,document_type "
			+ "FROM test.eisdoc "
			+ "where (size is null OR size <=200) and (length(filename)>0 OR length(folder)>0)",
			nativeQuery = true)
	List<Object[]> findMissingFiles();

	List<EISDoc> findAllByTitleAndDocumentTypeIn(String title, String documentType);

	@Query(value = "SELECT distinct year(e.register_date) from " + 
			"(select distinct size,register_date from eisdoc " + 
			"WHERE size > 200 AND register_date is not null " + 
			"order by register_date asc limit 1 ) e",
			nativeQuery = true)
	Object getEarliestYear();
	@Query(value = "SELECT distinct year(e.register_date) from " + 
			"(select distinct size,register_date from eisdoc " + 
			"WHERE size > 200 AND register_date is not null " + 
			"order by register_date desc limit 1 ) e",
			nativeQuery = true)
	Object getLatestYear();

//	@Query(value = "SELECT EXISTS(SELECT * FROM eisdoc WHERE ")
	boolean existsByFolder(String folderName);

	boolean existsByFolderAndDocumentTypeIn(String folder, String documentType);

	boolean existsByFilename(String filename);

	Optional<EISDoc> findByProcessId(long processId);
	
	@Query(value = "SELECT * "
			+ "FROM test.eisdoc "
			+ "WHERE process_id IS NULL",
			nativeQuery = true)
	List<EISDoc> findMissingProcesses();

	@Query(value = "SELECT * "
			+ "FROM test.eisdoc "
			+ "WHERE process_id IS NULL "
			+ "AND folder is not null and length(folder) > 0",
			nativeQuery = true)
	List<EISDoc> findAllWithFolderMissingProcess();

	@Query(value = "SELECT MAX(process_id) "
			+ "FROM test.eisdoc",
			nativeQuery = true)
	long findMaxProcessId();

	@Query(value = 
			"		select t.* \r\n" + 
			"		from eisdoc t join \r\n" + 
			"		(select document_type, title, register_date, count(*) as NumDuplicates \r\n" + 
			"		  from eisdoc \r\n" + 
			"		  group by document_type, title, register_date\r\n" + 
			"		  having NumDuplicates > 1\r\n" + 
			"		) tsum \r\n" + 
			"		on t.document_type = tsum.document_type and t.title = tsum.title and t.register_date = tsum.register_date\r\n" + 
			"		ORDER BY title", nativeQuery = true)
	List<EISDoc> findAllDuplicates();

	@Query(value = 
			"		select t.* " + 
			"		from eisdoc t join " + 
			"		(select document_type, title, register_date, count(*) as NumDuplicates " + 
			"		  from eisdoc " + 
			"		  group by document_type, title, register_date " + 
			"		  having NumDuplicates > 1 " + 
			"		) tsum " + 
			"		on t.document_type = tsum.document_type and t.title = tsum.title " +
			"       and ABS(DATEDIFF(t.register_date,tsum.register_date)) <= 31" + 
			"		ORDER BY title", nativeQuery = true)
	List<EISDoc> findAllDuplicatesCloseDates();

	@Query(value = 
			"		select t.* " + 
			"		from eisdoc t join " + 
			"		(select document_type, title, count(*) as NumDuplicates " + 
			"		  from eisdoc " + 
			"		  group by document_type, title " + 
			"		  having NumDuplicates > 1 " + 
			"		) tsum " + 
			"		on t.document_type = tsum.document_type and t.title = tsum.title " +
			"		ORDER BY title", nativeQuery = true)
	List<EISDoc> findAllSameTitleType();

	List<EISDoc> findAllByProcessId(Long processId);

	@Query(value = 	
			"		select t.* \r\n" + 
			"		from eisdoc t join \r\n" + 
			"		(select document_type, process_id, count(*) as NumDuplicates \r\n" + 
			"		  from eisdoc \r\n" + 
			"		  group by document_type, process_id \r\n" + 
			"		  having NumDuplicates > 1\r\n" + 
			"		) tsum \r\n" + 
			"		on t.document_type = tsum.document_type and t.process_id = tsum.process_id" + 
			"		ORDER BY process_id", nativeQuery = true)
	List<EISDoc> findAllDuplicatesProcess();

	/** For finding documents that expect files on disk (filename or folder listed) 
	 * but have no size recorded, implying they're missing the actual files (nothing to download/index) */
	@Query(value = "SELECT * "
			+ "FROM test.eisdoc "
			+ "where (size is null OR size <=200) and (length(filename)>0 OR length(folder)>0)",
			nativeQuery = true)
	List<EISDoc> sizeUnder200();

	@Query(value = "SELECT A.agency, MAX(tableACount) as total, MAX(tableBCount) as files\r\n" + 
			"FROM (SELECT agency, COUNT(1) tableACount, 0 AS tableBCount\r\n" + 
			"      FROM eisdoc GROUP BY agency\r\n" + 
			"      UNION \r\n" + 
			"      SELECT agency, 0 AS tableACount, COUNT(1) tableBCount\r\n" + 
			"      FROM eisdoc \r\n" + 
			"      WHERE size > 200\r\n" + 
			"      GROUP BY agency\r\n" + 
			"     ) AS A\r\n" + 
			"GROUP BY A.agency\r\n" + 
			"ORDER BY A.agency",nativeQuery = true)
	List<Object[]> reportAgencyCombined();
	

	@Query(value = "SELECT A.agency, MAX(tableACount) as total, MAX(tableBCount) as files\r\n" + 
			"FROM (SELECT agency, COUNT(1) tableACount, 0 AS tableBCount\r\n" + 
			"      FROM eisdoc WHERE YEAR(register_date) >= 2000 GROUP BY agency\r\n" + 
			"      UNION \r\n" + 
			"      SELECT agency, 0 AS tableACount, COUNT(1) tableBCount\r\n" + 
			"      FROM eisdoc \r\n" + 
			"      WHERE size > 200 AND YEAR(register_date) >= 2000\r\n" + 
			"      GROUP BY agency\r\n" + 
			"     ) AS A\r\n" + 
			"GROUP BY A.agency\r\n" + 
			"ORDER BY A.agency",nativeQuery = true)
	List<Object[]> reportAgencyCombinedAfter2000();
	
	
	
	@Query(value ="select agency,count(*) from eisdoc group by agency;",
	nativeQuery = true)
	List<Object[]> reportTotalMetadataByAgency();
	@Query(value ="select agency,count(*) from eisdoc where size > 200 group by agency;",
	nativeQuery = true)
	List<Object[]> reportHasFilesByAgency();
	@Query(value ="select agency,count(*) from eisdoc where YEAR(register_date) >= 2000 group by agency;",
	nativeQuery = true)
	List<Object[]> reportTotalMetadataByAgencyAfter2000();
	@Query(value ="select agency,count(*) from eisdoc where size > 200 and YEAR(register_date) >= 2000 group by agency;",
	nativeQuery = true)
	List<Object[]> reportHasFilesByAgencyAfter2000();

}

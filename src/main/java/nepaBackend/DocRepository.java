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

	/** TODO: Do this in Lucene instead */
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
	Optional<EISDoc> findTopByCommentsFilename(String commentsFilename);

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
			+ "OR document_type='Final and ROD' "
			+ "GROUP BY YEAR(register_date), document_type "
			+ "ORDER BY document_type, YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getDraftFinalCountByYear();

	/** Return EIS metadata counts by year */
	@Query(value = "SELECT YEAR(register_date), COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE (document_type='Final'"
			+ " OR document_type='Final and ROD'"
			+ " OR document_type='Draft'"
		    + " OR document_type = 'Second Draft'"
		    + " OR document_type = 'Second Final'"
		    + " OR document_type = 'Revised Draft'"
		    + " OR document_type = 'Revised Final'"
		    + " OR document_type = 'Draft Supplement'"
		    + " OR document_type = 'Final Supplement'"
		    + " OR document_type = 'Second Draft Supplemental'"
		    + " OR document_type = 'Second Final Supplemental'"
		    + " OR document_type = 'Third Draft Supplemental'"
		    + " OR document_type = 'Third Final Supplemental') "
			+ "GROUP BY YEAR(register_date) "
			+ "ORDER BY YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getMetadataCountByYear();
	
	@Query(value = "SELECT COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE (document_type='Final'"
			+ " OR document_type='Final and ROD'"
			+ " OR document_type='Draft'"
		    + " OR document_type = 'Second Draft'"
		    + " OR document_type = 'Second Final'"
		    + " OR document_type = 'Revised Draft'"
		    + " OR document_type = 'Revised Final'"
		    + " OR document_type = 'Draft Supplement'"
		    + " OR document_type = 'Final Supplement'"
		    + " OR document_type = 'Second Draft Supplemental'"
		    + " OR document_type = 'Second Final Supplemental'"
		    + " OR document_type = 'Third Draft Supplemental'"
		    + " OR document_type = 'Third Final Supplemental') "
			+ "AND size>200;",
			nativeQuery = true)
	public Long getDownloadableEISCount();
	
	/** Return downloadable EIS counts by year according to file size */
	@Query(value = "SELECT YEAR(register_date), COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE (document_type='Final'"
			+ " OR document_type='Final and ROD'"
			+ " OR document_type='Draft'"
		    + " OR document_type = 'Second Draft'"
		    + " OR document_type = 'Second Final'"
		    + " OR document_type = 'Revised Draft'"
		    + " OR document_type = 'Revised Final'"
		    + " OR document_type = 'Draft Supplement'"
		    + " OR document_type = 'Final Supplement'"
		    + " OR document_type = 'Second Draft Supplemental'"
		    + " OR document_type = 'Second Final Supplemental'"
		    + " OR document_type = 'Third Draft Supplemental'"
		    + " OR document_type = 'Third Final Supplemental') "
			+ "AND size>200 "
			+ "GROUP BY YEAR(register_date) "
			+ "ORDER BY YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getDownloadableCountByYear();
	/** Return downloadable ROD counts by year according to file size */
	@Query(value = "SELECT YEAR(register_date), COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE (document_type='ROD') "
			+ "OR (document_type='Final and ROD') "
			+ "AND size>200 "
			+ "GROUP BY YEAR(register_date) "
			+ "ORDER BY YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getRODDownloadableCountByYear();
	/** Return downloadable ROD counts by year according to file size */
	@Query(value = "SELECT YEAR(register_date), COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE (document_type='ROD' "
			+ "OR document_type='Final and ROD') "
			+ "GROUP BY YEAR(register_date) "
			+ "ORDER BY YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getRODCountByYear();

	/** Return supplement metadata counts by year */
	@Query(value = "SELECT YEAR(register_date), COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE (document_type='Final Revised'"
			+ " OR document_type='Draft Revised'"
		    + " OR document_type = 'Second Draft'"
		    + " OR document_type = 'Second Final'"
		    + " OR document_type = 'Revised Draft'"
		    + " OR document_type = 'Revised Final'"
		    + " OR document_type = 'Draft Supplement'"
		    + " OR document_type = 'Final Supplement'"
		    + " OR document_type = 'Second Draft Supplemental'"
		    + " OR document_type = 'Second Final Supplemental'"
		    + " OR document_type = 'Third Draft Supplemental'"
		    + " OR document_type = 'Third Final Supplemental') "
			+ "GROUP BY YEAR(register_date) "
			+ "ORDER BY YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getSupplementCountByYear();
	/** Return downloadable supplement counts by year */
	@Query(value = "SELECT YEAR(register_date), COUNT(*)"
			+ " FROM test.eisdoc"
			+ " WHERE (document_type='Final Revised'"
			+ " OR document_type='Draft Revised'"
		    + " OR document_type = 'Second Draft'"
		    + " OR document_type = 'Second Final'"
		    + " OR document_type = 'Revised Draft'"
		    + " OR document_type = 'Revised Final'"
		    + " OR document_type = 'Draft Supplement'"
		    + " OR document_type = 'Final Supplement'"
		    + " OR document_type = 'Second Draft Supplemental'"
		    + " OR document_type = 'Second Final Supplemental'"
		    + " OR document_type = 'Third Draft Supplemental'"
		    + " OR document_type = 'Third Final Supplemental')"
			+ " AND size>200"
			+ " GROUP BY YEAR(register_date)"
			+ " ORDER BY YEAR(register_date)"
			+ " DESC;",
			nativeQuery = true)
	public List<Object> getDownloadableSupplementCountByYear();
	/** Return draft metadata counts by year */
	@Query(value = "SELECT YEAR(register_date), COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE (document_type='Draft') "
			+ "GROUP BY YEAR(register_date) "
			+ "ORDER BY YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getDraftCountByYear();
	/** Return downloadable draft counts by year */
	@Query(value = "SELECT YEAR(register_date), COUNT(*)"
			+ " FROM test.eisdoc"
			+ " WHERE (document_type='Draft')"
			+ " AND size>200"
			+ " GROUP BY YEAR(register_date)"
			+ " ORDER BY YEAR(register_date)"
			+ " DESC;",
			nativeQuery = true)
	public List<Object> getDownloadableDraftCountByYear();
	/** Return final metadata counts by year */
	@Query(value = "SELECT YEAR(register_date), COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE (document_type='Final' OR document_type='Final and ROD') "
			+ "GROUP BY YEAR(register_date) "
			+ "ORDER BY YEAR(register_date) "
			+ "DESC;",
			nativeQuery = true)
	public List<Object> getFinalCountByYear();
	/** Return downloadable final counts by year */
	@Query(value = "SELECT YEAR(register_date), COUNT(*)"
			+ " FROM test.eisdoc"
			+ " WHERE (document_type='Final' OR document_type='Final and ROD') "
			+ " AND size>200"
			+ " GROUP BY YEAR(register_date)"
			+ " ORDER BY YEAR(register_date)"
			+ " DESC;",
			nativeQuery = true)
	public List<Object> getDownloadableFinalCountByYear();
	
	
	/** Return downloadable "other" counts by year aka non-final/draft/rod/ea */
	@Query(value = "SELECT YEAR(register_date), COUNT(*)"
			+ " FROM test.eisdoc"
			+ " WHERE (document_type!='Final'"
			+ " AND document_type!='Final and ROD'"
			+ " AND document_type!='Draft'"
			+ " AND document_type!='ROD'"
			+ " AND document_type!='EA')"
			+ " AND size>200"
			+ " GROUP BY YEAR(register_date)"
			+ " ORDER BY YEAR(register_date)"
			+ " DESC;",
			nativeQuery = true)
	public List<Object> getDownloadableOtherCountByYear();
	/** Return all "other" counts by year */
	@Query(value = "SELECT YEAR(register_date), COUNT(*)"
			+ " FROM test.eisdoc"
			+ " WHERE (document_type!='Final'"
			+ " AND document_type!='Final and ROD'"
			+ " AND document_type!='Draft'"
			+ " AND document_type!='ROD'"
			+ " AND document_type!='EA')"
			+ " GROUP BY YEAR(register_date)"
			+ " ORDER BY YEAR(register_date)"
			+ " DESC;",
			nativeQuery = true)
	public List<Object> getOtherCountByYear();


	/** Return counts of drafts and finals by state */
	@Query(value = "SELECT document_type, state, COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Final' OR document_type='Draft' "
			+ "OR document_type='Final and ROD' "
			+ "GROUP BY state, document_type "
			+ "ORDER BY document_type, state;",
			nativeQuery = true)
	public List<Object> getDraftFinalCountByState();

	/** Return count of drafts and finals by agency */
	@Query(value = "SELECT document_type, agency, COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Final' OR document_type='Draft' "
			+ "OR document_type='Final and ROD' "
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
			+ "OR document_type='Final and ROD' "
			+ "AND size>200;",
			nativeQuery = true)
	long getFinalCountDownloadable();

	@Query(value = "SELECT COUNT(*) "
			+ "FROM test.eisdoc "
			+ "WHERE document_type='Final'"
			+ "OR document_type='Final and ROD';",
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

	@Query(value = "SELECT COUNT(*) FROM test.eisdoc WHERE size > 178;",
			nativeQuery = true)
	Long getDownloadableCount();
	
	List<EISDoc> findAllByAgency(String agency);

	@Query(value = "SELECT * "
			+ "FROM test.eisdoc "
			+ "WHERE size<=200 "
			+ "OR size is null;",
			nativeQuery = true)
	List<EISDoc> findMissingSize();
	
	@Query(value = "SELECT DISTINCT filename "
			+ "FROM test.eisdoc "
			+ "WHERE size<=200 "
			+ "OR size is null AND LENGTH(filename)>0 ;",
			nativeQuery = true)
	List<String> findMissingFilenames();

	/** For finding documents that expect files on disk (filename or folder listed) 
	 * but have no size recorded, implying they're missing the actual files (nothing to download/index) */
	@Query(value = "SELECT folder,id,document_type,filename "
			+ "FROM test.eisdoc "
			+ "where (size is null OR size <=200) and (length(filename)>0 OR length(folder)>0) "
			+ "ORDER BY folder",
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
	
	boolean existsByCommentsFilename(String commentsFilename);

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
			"		select t.* " + 
			"		from eisdoc t join " + 
			"		(select document_type, REGEXP_REPLACE(title, '[^0-9a-zA-Z]', '') as title, register_date, count(*) as NumDuplicates " + 
			"		  from eisdoc " + 
			"		  group by document_type, REGEXP_REPLACE(title, '[^0-9a-zA-Z]', ''), register_date " + 
			"		  having NumDuplicates > 1 " + 
			"		) tsum " + 
			"		on t.document_type = tsum.document_type and (REGEXP_REPLACE(t.title, '[^0-9a-zA-Z]', '')) = tsum.title and t.register_date = tsum.register_date " + 
			"		ORDER BY title", nativeQuery = true)
	List<EISDoc> findAllDuplicates();

	@Query(value = 
			"		select t.* " + 
			"		from eisdoc t join " + 
			"		(select document_type, REGEXP_REPLACE(title, '[^0-9a-zA-Z]', '') as title, register_date, count(*) as NumDuplicates " + 
			"		  from eisdoc " + 
			"		  group by document_type, REGEXP_REPLACE(title, '[^0-9a-zA-Z]', ''), register_date " + 
			"		  having NumDuplicates > 1 " + 
			"		) tsum " + 
			"		on t.document_type = tsum.document_type and (REGEXP_REPLACE(t.title, '[^0-9a-zA-Z]', '')) = tsum.title " +
			"       and ABS(DATEDIFF(t.register_date,tsum.register_date)) <= 31" + 
			"		ORDER BY title", nativeQuery = true)
	List<EISDoc> findAllDuplicatesCloseDates();

	@Query(value = 
			"		select t.* " + 
			"		from eisdoc t join " + 
			"		(select document_type, REGEXP_REPLACE(title, '[^0-9a-zA-Z]', '') as title, count(*) as NumDuplicates " + 
			"		  from eisdoc " + 
			"		  group by document_type, REGEXP_REPLACE(title, '[^0-9a-zA-Z]', '') " + 
			"		  having NumDuplicates > 1 " + 
			"		) tsum " + 
			"		on t.document_type = tsum.document_type and (REGEXP_REPLACE(t.title, '[^0-9a-zA-Z]', '')) = tsum.title " +
			"		ORDER BY title", nativeQuery = true)
	List<EISDoc> findAllSameTitleType();
	// if we have duplicates with the same files and the same exact byte size it can show very clear
	// duplicates to handle
	@Query(value = 
			"		select t.* " + 
			"		from eisdoc t join " + 
			"		(select size, count(*) as NumDuplicates " + 
			"		  from eisdoc " + 
			"         where size > 200 " +
			"		  group by size " + 
			"		  having NumDuplicates > 1 " + 
			"		) tsum " + 
			"		on t.size = tsum.size " +
			"		ORDER BY title", nativeQuery = true)
	List<EISDoc> findAllSameSize();

	List<EISDoc> findAllByProcessId(Long processId);

	@Query(value = 	
			"		select t.* " + 
			"		from eisdoc t join " + 
			"		(select document_type, process_id, count(*) as NumDuplicates " + 
			"		  from eisdoc " + 
			"		  group by document_type, process_id " + 
			"		  having NumDuplicates > 1 " + 
			"		) tsum " + 
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

	@Query(value = "SELECT A.agency, MAX(tableACount) as total, MAX(tableBCount) as files " + 
			"FROM (SELECT agency, 0 AS tableACount, 0 AS tableBCount " + 
			"	  FROM eisdoc " + 
			"      GROUP BY agency " + 
			"      UNION " + 
			"      SELECT agency, COUNT(1) tableACount, 0 AS tableBCount " + 
			"      FROM eisdoc GROUP BY agency " + 
			"      UNION " + 
			"      SELECT agency, 0 AS tableACount, COUNT(1) tableBCount " + 
			"      FROM eisdoc " + 
			"      WHERE size > 200 " + 
			"      GROUP BY agency " + 
			"     ) AS A " + 
			"GROUP BY A.agency " + 
			"ORDER BY A.agency",nativeQuery = true)
	List<Object[]> reportAgencyCombined();
	

	@Query(value = "SELECT A.agency, MAX(tableACount) as total, MAX(tableBCount) as files " + 
			"FROM (SELECT agency, 0 AS tableACount, 0 AS tableBCount " + 
			"	  FROM eisdoc " + 
			"      GROUP BY agency " + 
			"      UNION " + 
			"	   SELECT agency, COUNT(1) tableACount, 0 AS tableBCount " + 
			"      FROM eisdoc " + 
			"      WHERE YEAR(register_date) >= 2000 " + 
			"      GROUP BY agency " + 
			"      UNION " + 
			"      SELECT agency, 0 AS tableACount, COUNT(1) tableBCount " + 
			"      FROM eisdoc " + 
			"      WHERE size > 200 AND YEAR(register_date) >= 2000 " + 
			"      GROUP BY agency " + 
			"     ) AS A " + 
			"GROUP BY A.agency " + 
			"ORDER BY A.agency",nativeQuery = true)
	List<Object[]> reportAgencyCombinedAfter2000();

	@Query(value = "SELECT A.agency, MAX(tableACount) as total, MAX(tableBCount) as files, MAX(tableCCount) as processCount, MAX(tableDCount) as processAndFileCount " + 
	"FROM ("+
	"	  SELECT agency, COUNT(1) tableACount, 0 AS tableBCount, 0 AS tableCCount, 0 AS tableDCount " + 
	"      FROM eisdoc " + 
	"      GROUP BY agency " + 
	"      UNION " + 
	"      SELECT agency, 0 AS tableACount, COUNT(1) tableBCount, 0 AS tableCCount, 0 AS tableDCount " + 
	"      FROM eisdoc " + 
	"      WHERE size > 200 " + 
	"      GROUP BY agency " + 
	"      UNION " + 
	"      SELECT agency, 0 AS tableACount, 0 AS tableBCount, COUNT(DISTINCT process_id) tableCCount, 0 AS tableDCount " + 
	"      FROM eisdoc " + 
	"      WHERE process_id IS NOT NULL AND process_id >= 0 " + 
	"      GROUP BY agency " + 
	"      UNION " + 
	"      SELECT agency, 0 AS tableACount, 0 AS tableBCount, 0 AS tableCCount, COUNT(1) tableDCount " + 
	"      FROM eisdoc " + 
	"      WHERE process_id IS NOT NULL AND process_id >= 0 AND size > 200 " + 
	"      GROUP BY agency " + 
	"     ) AS A " + 
	"GROUP BY A.agency " + 
	"ORDER BY A.agency",nativeQuery = true)
	List<Object[]> reportAgencyProcess();
	
	@Query(value = "SELECT A.agency, MAX(tableACount) as total, MAX(tableBCount) as files, MAX(tableCCount) as processCount, MAX(tableDCount) as processAndFileCount " + 
			"FROM (SELECT agency, 0 AS tableACount, 0 AS tableBCount, 0 AS tableCCount, 0 AS tableDCount " + 
			"	  FROM eisdoc " + 
			"      GROUP BY agency " + 
			"      UNION " + 
			"	  SELECT agency, COUNT(1) tableACount, 0 AS tableBCount, 0 AS tableCCount, 0 AS tableDCount " + 
			"      FROM eisdoc " + 
			"      WHERE YEAR(register_date) >= 2000 " + 
			"      GROUP BY agency " + 
			"      UNION " + 
			"      SELECT agency, 0 AS tableACount, COUNT(1) tableBCount, 0 AS tableCCount, 0 AS tableDCount " + 
			"      FROM eisdoc " + 
			"      WHERE size > 200 AND YEAR(register_date) >= 2000 " + 
			"      GROUP BY agency " + 
			"      UNION " + 
			"      SELECT agency, 0 AS tableACount, 0 AS tableBCount, COUNT(DISTINCT process_id) tableCCount, 0 AS tableDCount " + 
			"      FROM eisdoc " + 
			"      WHERE process_id IS NOT NULL AND process_id >= 0 AND YEAR(register_date) >= 2000 " + 
			"      GROUP BY agency " + 
			"      UNION " + 
			"      SELECT agency, 0 AS tableACount, 0 AS tableBCount, 0 AS tableCCount, COUNT(1) tableDCount " + 
			"      FROM eisdoc " + 
			"      WHERE process_id IS NOT NULL AND process_id >= 0 AND YEAR(register_date) >= 2000 AND size > 200 " + 
			"      GROUP BY agency " + 
			"     ) AS A " + 
			"GROUP BY A.agency " + 
			"ORDER BY A.agency",nativeQuery = true)
	List<Object[]> reportAgencyProcessAfter2000();
	
	
	
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

	@Query(value ="select * from eisdoc where first_rod_date is not null and document_type LIKE 'final';",
	nativeQuery = true)
	List<EISDoc> findAllFinalsWithFirstRodDates();

	/** compares titles on alphanumeric and spaces only.  We probably want this most of the time.
	/* New titles should also have their space normalized 
	/* (remove tabs, newlines, reduce double spaces+ to one space and remove trailing/leading space) */
	@Query(value ="select * from eisdoc where REGEXP_REPLACE(title, '[^0-9a-zA-Z]', '') " + 
			"LIKE REGEXP_REPLACE(:title, '[^0-9a-zA-Z]', '') " + 
			"AND document_type = :type " +
			"AND register_date = :date " + 
			"LIMIT 1;",
	nativeQuery = true)
	Optional<EISDoc> findByTitleTypeDateCompareAlphanumericOnly(
			@Param("title") String title, 
			@Param("type") String type, 
			@Param("date") LocalDate date);
	
	@Query(value = 	
			"		select t.* " + 
			"		from eisdoc t join " + 
			"		(select document_type, folder, count(*) as NumDuplicates " + 
			"		  from eisdoc " + 
			"		  where folder is not null and LENGTH(folder)>0 " +
			"		  group by document_type, folder " + 
			"		  having NumDuplicates > 1 " + 
			"		) tsum " + 
			"		on t.document_type = tsum.document_type and t.folder = tsum.folder" + 
			"		ORDER BY folder", nativeQuery = true)
	List<EISDoc> findNonUniqueTypeFolderPairs();

	@Query(value = "SELECT " + 
			"    e.* " + 
			"FROM " + 
			"    eisdoc e " + 
			"    LEFT OUTER JOIN nepafile n ON " + 
			"        e.id = n.document_id " + 
			"	 LEFT OUTER JOIN document_text d ON " + 
			"		 e.id = d.document_id " + 
			"WHERE " + 
			"	e.size > 0 " + 
			"   AND n.document_id is null " + 
			"	AND d.document_id is null", nativeQuery = true)
	List<EISDoc> findNotIndexed();

}

package nepaBackend.controller;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.DocRepository;
import nepaBackend.Globals;
import nepaBackend.SearchLogRepository;
import nepaBackend.TextRepository;
import nepaBackend.enums.SearchType;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.model.SearchLog;
import nepaBackend.pojo.HighlightedResult;
import nepaBackend.pojo.SearchInputs;
import nepaBackend.pojo.Unhighlighted2DTO;
import nepaBackend.pojo.UnhighlightedDTO;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/text")
public class FulltextController {
	
	@Autowired
	private TextRepository textRepository;
	private DocRepository docRepository;
	private ApplicationUserRepository applicationUserRepository;
	private SearchLogRepository searchLogRepository;
	
	public FulltextController(TextRepository textRepository, 
								DocRepository docRepository,
								SearchLogRepository searchLogRepository,
								ApplicationUserRepository applicationUserRepository) {
		this.textRepository = textRepository;
		this.docRepository = docRepository;
		this.applicationUserRepository = applicationUserRepository;
		this.searchLogRepository = searchLogRepository;
	}
	
	private boolean testing = Globals.TESTING;
	

//	@CrossOrigin
//	@GetMapping(path = "/search_A", 
//	consumes = "application/json", 
//	produces = "application/json", 
//	headers = "Accept=application/json")
//	public @ResponseBody List<EISDoc> searchA(@RequestBody SearchInputs searchInputs, Pageable pageable) {
//        Page page = EISDocDAO.findAll(new Specification<EISDoc>() {
//            @Override
//            public Predicate toPredicate(Root<Employee> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
//                List<Predicate> predicates = new ArrayList<>();
//                if(searchInputs.title!=null) {
//                    predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("title"), searchInputs.title)));
//                }
//                return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
//            }
//        }, pageable);
//
//        page.getTotalElements();        // get total elements
//        page.getTotalPages();           // get total pages
//        return page.getContent();       // get List of Employee
//    }
	

	/** TODO: Log search terms */
	/** Get DocumentText matches across entire database for fulltext search term(s) */
	@CrossOrigin
	@PostMapping(path = "/full")
	public List<EISDoc> fullSearch(@RequestBody String terms)
	{
		try {
			return textRepository.search(terms, 100000, 0);
		} catch(Exception e) { // ParseException, etc.
			e.printStackTrace();
			return null;
		}
	}
	
	@CrossOrigin
	@GetMapping(path = "/search_6")
	public List<List<?>> search6(@RequestBody String terms)
	{
		try {
			return textRepository.searchHibernate6(terms);
		} catch(Exception e) { // ParseException, etc.
			e.printStackTrace();
			return null;
		}
	}

	// Note: Probably unnecessary
	/** Get highlights across entire database for fulltext search term(s) */
//	public List<String> contextSearch(@RequestParam("terms") String terms)
//	{
//		try { 
//			return textRepository.searchContext(terms, 100, 0);
//		} catch(org.hibernate.search.exception.EmptyQueryException e) {
//			return null;
//		} catch(Exception e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

	/** TODO: Log search terms */
	/** Get EISDoc with ellipses-separated highlights and context across entire database for fulltext search term(s). 
	 * Note: Common words aren't indexed and will give no results.  */
	@CrossOrigin
	@PostMapping(path = "/fulltext_meta")
	public List<MetadataWithContext> fulltext_meta(@RequestBody String terms)
	{
//		System.out.println(terms);
		if(terms != null) {
			// Whitespace can prevent Lucene from finding results
			terms = Globals.normalizeSpace(terms.strip());
			
			try { // Note: Limit matters a lot when getting highlights.  Lack of SSD, RAM, CPU probably important, in that order
				List<MetadataWithContext> highlightsMeta = new ArrayList<MetadataWithContext>(
						(textRepository.metaContext(terms, 100, 0, SearchType.ALL)));
				return highlightsMeta;
			} catch(Exception e) {
				e.printStackTrace();
				return new ArrayList<MetadataWithContext>();
			}
		} else {
			return null;
		}
	}
	
	// Metadata search using Lucene (and JDBC) returns ArrayList of EISDoc
//	@CrossOrigin
//	@PostMapping(path = "/search")
//	public ResponseEntity<List<EISDoc>> search(@RequestBody SearchInputs searchInputs)
//	{
//		saveSearchLog(searchInputs);
//		
//		System.out.println("LIMIT: " + searchInputs.limit);
//
//		try { 
//			List<EISDoc> metaList = new ArrayList<EISDoc>(
//					(textRepository.metadataSearch(searchInputs, searchInputs.limit, 0, SearchType.ALL)));
//			return new ResponseEntity<List<EISDoc>>(metaList, HttpStatus.OK);
//		} catch(org.hibernate.search.exception.EmptyQueryException e) {
//			return new ResponseEntity<List<EISDoc>>(HttpStatus.BAD_REQUEST);
//		} catch(Exception e) {
//			e.printStackTrace();
//			return new ResponseEntity<List<EISDoc>>(HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}

	// Metadata search using Lucene (and JDBC) returns ArrayList of MetadataWithContext
	@CrossOrigin
	@PostMapping(path = "/search")
	public ResponseEntity<List<MetadataWithContext>> search(@RequestBody SearchInputs searchInputs,
			@RequestHeader Map<String, String> headers)
	{
		String token = headers.get("authorization");
		Long userId = idFromToken(token);
		saveSearchLog(searchInputs, "title", userId);

		try { 
			List<EISDoc> metaList = new ArrayList<EISDoc>(
					(textRepository.metadataSearch(searchInputs, searchInputs.limit, 0, SearchType.ALL)));
			List<MetadataWithContext> convertedList = new ArrayList<MetadataWithContext>();
			for(EISDoc doc : metaList) {
				convertedList.add(new MetadataWithContext(doc, "", ""));
			}
			return new ResponseEntity<List<MetadataWithContext>>(convertedList, HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<MetadataWithContext>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Metadata with context search using Lucene (and JDBC) returns ArrayList of MetadataWithContext, prioritizes title matches
	@CrossOrigin
	@PostMapping(path = "/search_title_priority")
	public ResponseEntity<List<MetadataWithContext>> searchPriorityTitle(@RequestBody SearchInputs searchInputs)
	{
		saveSearchLog(searchInputs, "all",(long) 30);

		try { 
			List<MetadataWithContext> highlightsMeta = new ArrayList<MetadataWithContext>(
					(textRepository.CombinedSearchTitlePriority(searchInputs, SearchType.ALL)));
			return new ResponseEntity<List<MetadataWithContext>>(highlightsMeta, HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<MetadataWithContext>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	// Metadata with context search using Lucene (and JDBC) returns ArrayList of MetadataWithContext
	@CrossOrigin
	@PostMapping(path = "/search_lucene_priority")
	public ResponseEntity<List<MetadataWithContext>> searchPriorityLucene(@RequestBody SearchInputs searchInputs)
	{
		saveSearchLog(searchInputs, "all",(long) 30);

		try { 
			List<MetadataWithContext> highlightsMeta = new ArrayList<MetadataWithContext>(
					(textRepository.CombinedSearchLucenePriority(searchInputs, SearchType.ALL)));
			return new ResponseEntity<List<MetadataWithContext>>(highlightsMeta, HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<MetadataWithContext>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	

	// Metadata without context search using Lucene (and JDBC) returns ArrayList of MetadataWithContext
	@CrossOrigin
	@PostMapping(path = "/search_no_context")
	public ResponseEntity<List<MetadataWithContext3>> searchNoContext(@RequestBody SearchInputs searchInputs,
			@RequestHeader Map<String, String> headers)
	{
		String token = headers.get("authorization");
		Long userId = idFromToken(token);
		saveSearchLog(searchInputs, "all", userId);

		try { 
			List<MetadataWithContext3> metaAndFilenames = 
					textRepository.CombinedSearchNoContextHibernate6(searchInputs, SearchType.ALL);
			return new ResponseEntity<List<MetadataWithContext3>>(metaAndFilenames, HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<MetadataWithContext3>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	

	// Returns highlights for given list of IDs and filenames
	@CrossOrigin
	@PostMapping(path = "/get_highlights")
	public ResponseEntity<List<List<String>>> getHighlights(@RequestBody UnhighlightedDTO unhighlighted)
	{
		try {
			// Could turn IDs into list of eisdocs, hand those off instead?
			List<List<String>> highlights = new ArrayList<List<String>>(
//					(textRepository.getHighlights(unhighlighted)));
					(textRepository.getUnifiedHighlights(unhighlighted)));
			return new ResponseEntity<List<List<String>>>(highlights, HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<List<String>>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Returns highlights for given list of IDs and filenames (manually parsed)
//	@CrossOrigin
//	@PostMapping(path = "/get_highlightsFVH_manual")
//	public ResponseEntity<List<List<String>>> getHighlightsFVH(@RequestBody String unhighlighted)
//	{
//		try {
//			List<Unhighlighted2> uns = new ArrayList<Unhighlighted2>();
//			JSONParser jsp = new JSONParser();
//			JSONObject jso = (JSONObject) jsp.parse(unhighlighted);
////			System.out.println("Terms " + jso.get("terms"));
//
//			JSONArray jsa = (JSONArray) jso.get("unhighlighted");
////			System.out.println("Array of objects " + jsa);
////			System.out.println("First object " + (JSONObject) jsa.get(0));
//			Iterator<JSONObject> iterator = jsa.iterator();
//			while(iterator.hasNext()) {
//				JSONObject internalJso = iterator.next();
//				JSONArray lids = (JSONArray) internalJso.get("luceneIds");
//				Iterator<Long> lidsIterator = lids.iterator();
//				List<Long> lidsInts = new ArrayList<Long>(lids.size());
//				while(lidsIterator.hasNext()) {
//					lidsInts.add(lidsIterator.next());
//				}
//				String filenames = (String) internalJso.get("filename");
//				Unhighlighted2 un2 = new Unhighlighted2(lidsInts,filenames);
//				uns.add(un2);
//			}
//			
//			Unhighlighted2DTO unhighlighted2DTO = new Unhighlighted2DTO();
//			unhighlighted2DTO.setTerms((String) jso.get("terms"));
//			unhighlighted2DTO.setUnhighlighted(uns);
////			System.out.println("Length " + unhighlighted2DTO.getUnhighlighted().size());
////			System.out.println("0 " + unhighlighted2DTO.getUnhighlighted().get(0));
////			System.out.println("Filenames " + unhighlighted2DTO.getUnhighlighted().get(0).getFilename());
////			System.out.println("Lucene ID list 0 " + unhighlighted2DTO.getUnhighlighted().get(0).getIds());
//			List<List<String>> highlights = new ArrayList<List<String>>(
//					(textRepository.getHighlightsFVH( unhighlighted2DTO )));
//			return new ResponseEntity<List<List<String>>>(highlights, HttpStatus.OK);
//		} catch (ParseException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//			return new ResponseEntity<List<List<String>>>(HttpStatus.BAD_REQUEST);
//		} catch(Exception e) {
//			e.printStackTrace();
//			return new ResponseEntity<List<List<String>>>(HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}
	

	// Returns highlights for given list of IDs and filenames
	@CrossOrigin
	@PostMapping(path = "/get_highlightsFVH")
	public ResponseEntity<List<List<String>>> getHighlightsFVH(@RequestBody Unhighlighted2DTO unhighlighted)
	{
		try {
			if(unhighlighted.isMarkup()) {
				List<List<String>> highlights = new ArrayList<List<String>>(
						(textRepository.getHighlightsFVH( unhighlighted )));
				return new ResponseEntity<List<List<String>>>(highlights, HttpStatus.OK);
			} else {
				List<List<String>> highlights = new ArrayList<List<String>>(
						(textRepository.getHighlightsFVHNoMarkup( unhighlighted )));
				return new ResponseEntity<List<List<String>>>(highlights, HttpStatus.OK);
			}
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<List<String>>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

//	@CrossOrigin
//	@PostMapping(path = "/get_all")
//	ResponseEntity<List<MetadataWithContext3>> allInOne(@RequestBody SearchInputs searchInputs) {
//		try {
//			return new ResponseEntity<List<MetadataWithContext3>>(
//					textRepository.allInOne(searchInputs),
//					HttpStatus.OK);
//		} catch(Exception e) {
//			e.printStackTrace();
//			List<MetadataWithContext3> returnList = new ArrayList<MetadataWithContext3>();
//			MetadataWithContext3 datum = new MetadataWithContext3(null, null, null, e.getMessage(), 0);
//			returnList.add(datum);
//			return new ResponseEntity<List<MetadataWithContext3>>(returnList,HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}
	
	@CrossOrigin
	@GetMapping(path = "/new_test")
	ResponseEntity<List<HighlightedResult>> newTest(@RequestBody String query) {
		try {
//			LuceneHighlighter.searchIndexExample("test");
//			LuceneHighlighter.searchAndHighLightKeywords("test");
			return new ResponseEntity<List<HighlightedResult>>(textRepository.searchAndHighlight(query),HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Mostly for testing, returns raw results from a .getResultList() call */
	@CrossOrigin
	@PostMapping(path = "/get_raw")
	public ResponseEntity<List<Object[]>> getRaw(@RequestBody SearchInputs searchInputs)
	{
		try {
			// Could turn IDs into list of eisdocs, hand those off instead?
			List<Object[]> results = new ArrayList<Object[]>(
					(textRepository.getRaw(searchInputs.title)));
			return new ResponseEntity<List<Object[]>>(results, HttpStatus.OK);
//		} catch(org.hibernate.search.exception.EmptyQueryException e) {
//			return new ResponseEntity<List<Object[]>>(HttpStatus.BAD_REQUEST);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<Object[]>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	

	@CrossOrigin
	@PostMapping(path = "/get_scored")
	public ResponseEntity<List<MetadataWithContext2>> getScored(@RequestBody SearchInputs searchInputs)
	{
		try {
			// Could turn IDs into list of eisdocs, hand those off instead?
			List<MetadataWithContext2> results = 
					textRepository.getScored(searchInputs.title);
			return new ResponseEntity<List<MetadataWithContext2>>(results, HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<MetadataWithContext2>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	// Testing getting data offset# results at a time
	@CrossOrigin
	@PostMapping(path = "/search_test")
	public ResponseEntity<List<MetadataWithContext>> searchTest(@RequestBody SearchInputs searchInputs)
	{
		try { 
			List<MetadataWithContext> highlightsMeta = new ArrayList<MetadataWithContext>(
					(textRepository.CombinedSearchLucenePriority(searchInputs, SearchType.ALL)));
			return new ResponseEntity<List<MetadataWithContext>>(highlightsMeta, HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<MetadataWithContext>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	/** Refresh Lucene index so that searching works 
	 * (adds MySQL document_text table to Lucene with denormalization) 
	 * Now also adds eisdoc because it's set to @Indexed and the title field is set to @Field 
	 * Shouldn't need to be run again unless adding entirely new fields or tables to Lucene */
	@CrossOrigin
	@RequestMapping(path = "/sync", method = RequestMethod.GET)
	public boolean sync(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(!isAdmin(token)) 
		{
			return false;
		} else {
			return textRepository.sync();
		}
	}
	

	/** Rewrite all existing titles for normalized space (deduplication should compare with normalized input)
	 * Legacy titles all had a \r at the end, and we don't want that.  Also turns double spaces into single */
	@CrossOrigin
	@RequestMapping(path = "/normalize_titles", method = RequestMethod.GET)
	public List<String> normalizeTitleSpace(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		List<String> results = new ArrayList<String>();
		if(!isAdmin(token)) 
		{
			return null;
		} else {
			// Count how many actually change
			try {
				List<EISDoc> docs = docRepository.findAll();
				for(EISDoc doc : docs) {
					String title = doc.getTitle();
					if(title.contentEquals(Globals.normalizeSpace(title))) {
						// do nothing
					} else {
						doc.setTitle(Globals.normalizeSpace(title));
						docRepository.save(doc);
						// add pre-altered title to results following ID
						results.add(doc.getId() + ": " + title);
					}
				}
				return results;
			} catch(Exception e) {
				e.printStackTrace();
				return results;
			}
		}
	}
	
	/** Get a list of DocumentTexts for a given EIS ID (DocumentText.document_id) */
	@CrossOrigin
	@RequestMapping(path = "/get_by_id", method = RequestMethod.GET)
	public List<DocumentText> getById(@RequestParam String id, @RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		Long lid = Long.parseLong(id);
		
		if(!isAdmin(token)) 
		{
			return new ArrayList<DocumentText>();
		} else {
			try {
				Optional<EISDoc> eis = docRepository.findById(lid);
				return textRepository.findAllByEisdoc(eis.get());
			} catch(Exception e) {
				e.printStackTrace();
				return new ArrayList<DocumentText>();
			}
		}
	}
	
	/** Get a list of DocumentTexts for a given EIS title (EISDoc.title) (title is not unique)*/
	@CrossOrigin
	@RequestMapping(path = "/get_by_title", method = RequestMethod.GET)
	public List<DocumentText> getByTitle(@RequestParam String title, @RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		
		if(!isAdmin(token)) 
		{
			return new ArrayList<DocumentText>();
		} else {
			try {
				List<EISDoc> eisList = docRepository.findAllByTitle(title);
				List<DocumentText> results = new ArrayList<DocumentText>();
				for(EISDoc eis : eisList) {
					results.addAll(textRepository.findAllByEisdoc(eis));
				}
				return results;
			} catch(Exception e) {
				e.printStackTrace();
				return new ArrayList<DocumentText>();
			}
		}
	}
	

	/** Get a list of DocumentTexts by the first EISDoc to match on filename (filename is not unique)*/
	@CrossOrigin
	@RequestMapping(path = "/get_by_filename", method = RequestMethod.GET)
	public List<DocumentText> getByFilename(@RequestParam String filename, @RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		
		if(!isAdmin(token)) 
		{
			return new ArrayList<DocumentText>();
		} else {
			try {
				Optional<EISDoc> eis = docRepository.findTopByFilename(filename);
				return textRepository.findAllByEisdoc(eis.get());
			} catch(Exception e) {
				e.printStackTrace();
				return new ArrayList<DocumentText>();
			}
		}
	}

	/** Given id, returns length of plaintext for document_text if found.  If not found, returns -1
	 */
	@CrossOrigin
	@RequestMapping(path = "/get_length", method = RequestMethod.GET)
	public int getLengthOfDocumentText(@RequestParam long id) {
		try {
			return textRepository.findPlaintextLengthById(id);
		} catch(Exception e) {
			return -1;
		}
	}

	/** Get a list of document_text IDs who have the same length(plaintext) as for the given document_text ID. 
	 * It's one method for finding potential duplicates which may or may not have the same filename or be associated
	 * with the same EISDoc record */
	@CrossOrigin
	@RequestMapping(path = "/get_length_ids", method = RequestMethod.GET)
	public List<BigInteger> getIdsByLengthFromId(@RequestParam long id) {
		if(testing) {
			System.out.println(id);
			System.out.println(getLengthOfDocumentText(id));
		}
		// empty list if invalid ID (won't find any plaintext lengths of -1)
		return textRepository.findIdsByPlaintextLength(getLengthOfDocumentText(id));
	}

	
	
	
	/** Decode trusted token and then ask database if user is admin */
	private boolean isAdmin(String token) {
		boolean result = false;
		// get ID
		if(token != null) {
			/** By necessity token is verified as valid via filter by this point as long as it's going through the 
			 * public API.  Alternatively you can store admin credentials in the token and hand that to the filter,
			 * but then if admin access is revoked, that token still has admin access until it expires.
			 * Therefore this is a slightly more secure flow. */
			String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
					.getId();

			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id))
					.get();
			
			if(user.getRole().contentEquals("ADMIN")) {
				result = true;
			}
		}
		return result;
	}
	
	private Long idFromToken(String token) {
		if(token != null) {
			/** By necessity token is verified as valid via filter by this point as long as it's going through the 
			 * public API.  Alternatively you can store admin credentials in the token and hand that to the filter,
			 * but then if admin access is revoked, that token still has admin access until it expires.
			 * Therefore this is a slightly more secure flow. */
			String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
					.getId();
			return Long.parseLong(id);
		} else {
			return null;
		}
	}

	private void saveSearchLog(SearchInputs searchInputs, String searchMode, Long userId) {

		// current admin user ID is 30; this could change depending on if/how db is migrated
		if(userId != null && userId == 30) { 
			// don't save
		} else {
			try {
				SearchLog searchLog = new SearchLog();
				searchLog.setTerms(searchInputs.title);
				searchLog.setSearchMode(searchMode);
				
				searchLog.setUserId(userId); // TODO: Opt-in/out option?
				
	
				searchLogRepository.save(searchLog);
				
			} catch (Exception e) {
	//			if (log.isDebugEnabled()) {
	//				log.debug(e);
	//			}
			}
		}
		
	}
	
	
}
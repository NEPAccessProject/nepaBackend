package nepaBackend.controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import nepaBackend.pojo.SearchInputs;
import nepaBackend.pojo.UnhighlightedDTO;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/text")
public class FulltextController {
	
	@Autowired
	private TextRepository textRepository;
	@Autowired
	private DocRepository docRepository;
	@Autowired
	private ApplicationUserRepository applicationUserRepository;
	@Autowired
	private SearchLogRepository searchLogRepository;
	
	public FulltextController() {
	}
	
	private boolean testing = Globals.TESTING;
	private static final Logger logger = LoggerFactory.getLogger(FulltextController.class);
	
	@CrossOrigin
	@GetMapping(path = "/test_terms")
	public ResponseEntity<String> testTerms(@RequestParam String terms) {
		return new ResponseEntity<String>(textRepository.testTerms(terms), HttpStatus.OK);
	}
	

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
		} catch(ParseException pe) {
			searchInputs.title = MultiFieldQueryParser.escape(searchInputs.title);
//			return this.search(searchInputs, headers);
//			
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
		} catch(ParseException pe) {
			searchInputs.title = MultiFieldQueryParser.escape(searchInputs.title);
			try {
				List<MetadataWithContext3> metaAndFilenames = 
						textRepository.CombinedSearchNoContextHibernate6(searchInputs, SearchType.ALL);
				return new ResponseEntity<List<MetadataWithContext3>>(metaAndFilenames, HttpStatus.OK);
			} catch(Exception e) {
				e.printStackTrace();
				return new ResponseEntity<List<MetadataWithContext3>>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<MetadataWithContext3>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Returns highlights for given list of IDs and filenames
	@CrossOrigin
	@PostMapping(path = "/get_highlightsFVH")
	public ResponseEntity<List<List<String>>> getHighlightsFVH(@RequestBody UnhighlightedDTO unhighlighted)
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
		} catch(ParseException pe) {
			unhighlighted.setTerms( MultiFieldQueryParser.escape(unhighlighted.getTerms()) );

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
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<List<String>>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
//	@CrossOrigin
//	@GetMapping(path = "/new_test")
//	ResponseEntity<List<HighlightedResult>> newTest(@RequestBody String query) {
//		try {
////			LuceneHighlighter.searchIndexExample("test");
////			LuceneHighlighter.searchAndHighLightKeywords("test");
//			return new ResponseEntity<List<HighlightedResult>>(textRepository.searchAndHighlight(query),HttpStatus.OK);
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

//	/** Mostly for testing, returns raw results from a .getResultList() call */
//	@CrossOrigin
//	@PostMapping(path = "/get_raw")
//	public ResponseEntity<List<Object[]>> getRaw(@RequestBody SearchInputs searchInputs)
//	{
//		try {
//			// Could turn IDs into list of eisdocs, hand those off instead?
//			List<Object[]> results = new ArrayList<Object[]>(
//					(textRepository.getRaw(searchInputs.title)));
//			return new ResponseEntity<List<Object[]>>(results, HttpStatus.OK);
////		} catch(org.hibernate.search.exception.EmptyQueryException e) {
////			return new ResponseEntity<List<Object[]>>(HttpStatus.BAD_REQUEST);
//		} catch(Exception e) {
//			e.printStackTrace();
//			return new ResponseEntity<List<Object[]>>(HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}


//	// Testing getting data offset# results at a time
//	@CrossOrigin
//	@PostMapping(path = "/search_test")
//	public ResponseEntity<List<MetadataWithContext>> searchTest(@RequestBody SearchInputs searchInputs)
//	{
//		try { 
//			List<MetadataWithContext> highlightsMeta = new ArrayList<MetadataWithContext>(
//					(textRepository.CombinedSearchLucenePriority(searchInputs, SearchType.ALL)));
//			return new ResponseEntity<List<MetadataWithContext>>(highlightsMeta, HttpStatus.OK);
//		} catch(Exception e) {
//			e.printStackTrace();
//			return new ResponseEntity<List<MetadataWithContext>>(HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}
	
	
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

	private int getTotalHits(String field) {
		try {
			return textRepository.getTotalHits(field);
		} catch(Exception e) {
			return -1;
		}
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
			/** By necessity token is verified as valid via filter by this point as long as it's
			 *  going through the public API. */
			String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
					.getId();
			
			return Long.parseLong(id);
		} else {
			return null;
		}
	}

	/** Excludes specific IDs, then excludes if appropriate (admin/curator/...?), otherwise saves log */
	private void saveSearchLog(SearchInputs searchInputs, String searchMode, Long userId) {
		boolean shouldSave = true;
		
		if(userId == null) {
			// save anonymous search
		}
		else if( userId == 104 || userId == 80 ) { 
			// exclude specific ID
			shouldSave = false;
		} else {
			Optional<ApplicationUser> maybeUser = applicationUserRepository.findById(userId);
			
			if(maybeUser.isPresent()) {
				// TODO: Opt-in/out option?
				String role = maybeUser.get().getRole();
				// exclude all admin or curator
				if(role.contentEquals("CURATOR") || role.contentEquals("ADMIN")) {
					// exclude
					shouldSave = false;
				}
			}
			
		} 
		
		if(shouldSave) {
			saveNewSearchLog(searchInputs.title,searchMode,userId);
		}
		
	}
	
	/** Helper method performs actual field assignment, saving, and Logging on error */
	private void saveNewSearchLog(String terms, String searchMode, Long userId) {
		try {
			SearchLog searchLog = new SearchLog();
			searchLog.setTerms(terms);
			searchLog.setSearchMode(searchMode);
			
			searchLog.setUserId(userId); 

			searchLogRepository.save(searchLog);
			
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
		}
	}
	
	
}
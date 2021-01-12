package nepaBackend.controller;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;

import nepaBackend.TextRepository;
import nepaBackend.enums.SearchType;
import nepaBackend.ApplicationUserRepository;
import nepaBackend.DocRepository;
import nepaBackend.Globals;
import nepaBackend.SearchLogRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.model.EISDocDAO;
import nepaBackend.model.SearchLog;
import nepaBackend.pojo.SearchInputs;
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
		} catch(org.hibernate.search.exception.EmptyQueryException e) {
			return null;
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
			terms = org.apache.commons.lang3.StringUtils.normalizeSpace(terms.strip());
			
			try { // Note: Limit matters a lot when getting highlights.  Lack of SSD, RAM, CPU probably important, in that order
				List<MetadataWithContext> highlightsMeta = new ArrayList<MetadataWithContext>(
						(textRepository.metaContext(terms, 100, 0, SearchType.ALL)));
				return highlightsMeta;
			} catch(org.hibernate.search.exception.EmptyQueryException e) {
				return new ArrayList<MetadataWithContext>();
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
	public ResponseEntity<List<MetadataWithContext>> search(@RequestBody SearchInputs searchInputs)
	{
		saveSearchLog(searchInputs);

		try { 
			List<EISDoc> metaList = new ArrayList<EISDoc>(
					(textRepository.metadataSearch(searchInputs, searchInputs.limit, 0, SearchType.ALL)));
			List<MetadataWithContext> convertedList = new ArrayList<MetadataWithContext>();
			for(EISDoc doc : metaList) {
				convertedList.add(new MetadataWithContext(doc, "", ""));
			}
			return new ResponseEntity<List<MetadataWithContext>>(convertedList, HttpStatus.OK);
		} catch(org.hibernate.search.exception.EmptyQueryException e) {
			return new ResponseEntity<List<MetadataWithContext>>(HttpStatus.BAD_REQUEST);
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
		saveSearchLog(searchInputs);

		try { 
			List<MetadataWithContext> highlightsMeta = new ArrayList<MetadataWithContext>(
					(textRepository.CombinedSearchTitlePriority(searchInputs, SearchType.ALL)));
			return new ResponseEntity<List<MetadataWithContext>>(highlightsMeta, HttpStatus.OK);
		} catch(org.hibernate.search.exception.EmptyQueryException e) {
			return new ResponseEntity<List<MetadataWithContext>>(HttpStatus.BAD_REQUEST);
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
		saveSearchLog(searchInputs);

		try { 
			List<MetadataWithContext> highlightsMeta = new ArrayList<MetadataWithContext>(
					(textRepository.CombinedSearchLucenePriority(searchInputs, SearchType.ALL)));
			return new ResponseEntity<List<MetadataWithContext>>(highlightsMeta, HttpStatus.OK);
		} catch(org.hibernate.search.exception.EmptyQueryException e) {
			return new ResponseEntity<List<MetadataWithContext>>(HttpStatus.BAD_REQUEST);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<MetadataWithContext>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	

	// Metadata without context search using Lucene (and JDBC) returns ArrayList of MetadataWithContext
	@CrossOrigin
	@PostMapping(path = "/search_no_context")
	public ResponseEntity<List<MetadataWithContext2>> searchNoContext(@RequestBody SearchInputs searchInputs)
	{
		saveSearchLog(searchInputs);

		try { 
			List<MetadataWithContext2> metaAndFilenames = new ArrayList<MetadataWithContext2>(
					(textRepository.CombinedSearchNoContext(searchInputs, SearchType.ALL)));
			return new ResponseEntity<List<MetadataWithContext2>>(metaAndFilenames, HttpStatus.OK);
		} catch(org.hibernate.search.exception.EmptyQueryException e) {
			return new ResponseEntity<List<MetadataWithContext2>>(HttpStatus.BAD_REQUEST);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<MetadataWithContext2>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Returns highlights for given list of IDs and filenames
	@CrossOrigin
	@PostMapping(path = "/get_highlights")
	public ResponseEntity<List<List<String>>> getHighlights(@RequestBody UnhighlightedDTO unhighlighted)
	{
		System.out.println("Anything?");
		try {
			// Could turn IDs into list of eisdocs, hand those off instead?
			List<List<String>> highlights = new ArrayList<List<String>>(
					(textRepository.getHighlights(unhighlighted)));
			return new ResponseEntity<List<List<String>>>(highlights, HttpStatus.OK);
		} catch(org.hibernate.search.exception.EmptyQueryException e) {
			return new ResponseEntity<List<List<String>>>(HttpStatus.BAD_REQUEST);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<List<String>>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}



	// Testing getting data offset# results at a time
	@CrossOrigin
	@PostMapping(path = "/search_test")
	public ResponseEntity<List<MetadataWithContext>> searchTest(@RequestBody SearchInputs searchInputs)
	{
		saveSearchLog(searchInputs);

		try { 
			List<MetadataWithContext> highlightsMeta = new ArrayList<MetadataWithContext>(
					(textRepository.CombinedSearchLucenePriority(searchInputs, SearchType.ALL)));
			return new ResponseEntity<List<MetadataWithContext>>(highlightsMeta, HttpStatus.OK);
		} catch(org.hibernate.search.exception.EmptyQueryException e) {
			return new ResponseEntity<List<MetadataWithContext>>(HttpStatus.BAD_REQUEST);
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

	private void saveSearchLog(SearchInputs searchInputs) {
			try {
				SearchLog searchLog = new SearchLog();
				
				// TODO: For the future, load Dates of format yyyy-MM=dd into db.
				// This will change STR_TO_DATE(register_date, '%m/%d/%Y') >= ?
				// to just register_date >= ?
				// Right now the db has Strings of MM/dd/yyyy
				
				// Populate lists
				if(Globals.saneInput(searchInputs.startPublish)) {
					searchLog.setStartPublish(searchInputs.startPublish);
				}
				
				if(Globals.saneInput(searchInputs.endPublish)) {
					searchLog.setEndPublish(searchInputs.endPublish);
				}
	
				if(Globals.saneInput(searchInputs.startComment)) {
					searchLog.setStartComment(searchInputs.startComment);
				}
				
				if(Globals.saneInput(searchInputs.endComment)) {
					searchLog.setEndComment(searchInputs.endComment);
				}
	
				searchLog.setDocumentTypes("All"); // handles all or blank (equivalent to all)
				if(Globals.saneInput(searchInputs.typeAll)) { 
					// do nothing
				} else {
					ArrayList<String> typesList = new ArrayList<>();
					if(Globals.saneInput(searchInputs.typeFinal)) {
						typesList.add("Final");
					}
	
					if(Globals.saneInput(searchInputs.typeDraft)) {
						typesList.add("Draft");
					}
					
					if(Globals.saneInput(searchInputs.typeOther)) {
						List<String> typesListOther = Arrays.asList("Draft Supplement",
								"Final Supplement",
								"Second Draft Supplemental",
								"Second Draft",
								"Adoption",
								"LF",
								"Revised Final",
								"LD",
								"Third Draft Supplemental",
								"Second Final",
								"Second Final Supplemental",
								"DC",
								"FC",
								"RF",
								"RD",
								"Third Final Supplemental",
								"DD",
								"Revised Draft",
								"NF",
								"F2",
								"D2",
								"F3",
								"DE",
								"FD",
								"DF",
								"FE",
								"A3",
								"A1");
						typesList.addAll(typesListOther);
					}
					if(typesList.size() > 0) {
						searchLog.setDocumentTypes( String.join(",", typesList) );
					}
	
				}
	
				// TODO: Temporary logic, filenames should each have their own field in the database later 
				// and they may also be a different format
				// (this will eliminate the need for the _% LIKE logic also)
				// _ matches exactly one character and % matches zero to many, so _% matches at least one arbitrary character
				if(Globals.saneInput(searchInputs.needsComments)) {
					searchLog.setNeedsComments(searchInputs.needsComments);
				}
	
				if(Globals.saneInput(searchInputs.needsDocument)) { 
					searchLog.setNeedsDocument(searchInputs.needsDocument);
				}
				
				if(Globals.saneInput(searchInputs.state)) {
					searchLog.setState(String.join(",", searchInputs.state));
				}
	
				if(Globals.saneInput(searchInputs.agency)) {
					searchLog.setAgency(String.join(",", searchInputs.agency));
				}
				
				if(Globals.saneInput(searchInputs.title)) {
					searchLog.setTitle(searchInputs.title);
				}
				
				if(Globals.saneInput(searchInputs.searchMode)) {
					searchLog.setSearchMode(searchInputs.searchMode);
				}
				
				searchLog.setHowMany(1000); 
				if(Globals.saneInput(searchInputs.limit)) {
					if(searchInputs.limit > 100000) {
						searchLog.setHowMany(100000); // TODO: Review 100k as upper limit
					} else {
						searchLog.setHowMany(searchInputs.limit);
					}
				}
				
				searchLog.setUserId(null); // TODO: Non-anonymous user IDs
				searchLog.setSavedTime(LocalDateTime.now());
	
				searchLogRepository.save(searchLog);
				
			} catch (Exception e) {
	//			if (log.isDebugEnabled()) {
	//				log.debug(e);
	//			}
	//			System.out.println(e);
			}
		
		}
	
	
}
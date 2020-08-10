package nepaBackend.controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/text")
public class FulltextController {

	@Autowired
	JdbcTemplate jdbcTemplate;
	
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
		} catch(Exception e) {
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
	
	
	/** Refresh Lucene index so that searching works (adds MySQL document_text table to Lucene via denormalization) */
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
	
	
}
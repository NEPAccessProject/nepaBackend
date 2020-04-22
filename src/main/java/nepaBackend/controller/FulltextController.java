package nepaBackend.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;

import nepaBackend.TextRepository;
import nepaBackend.ApplicationUserRepository;
import nepaBackend.SearchLogRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.DocumentText;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/text")
public class FulltextController {

	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@Autowired
	private TextRepository textRepository;
	private ApplicationUserRepository applicationUserRepository;
	private SearchLogRepository searchLogRepository;
	
	public FulltextController(TextRepository textRepository, 
								SearchLogRepository searchLogRepository,
								ApplicationUserRepository applicationUserRepository) {
		this.textRepository = textRepository;
		this.applicationUserRepository = applicationUserRepository;
		this.searchLogRepository = searchLogRepository;
	}
	

	/** TODO: Finalize, log search terms? Possibly don't need this functionality; remove? */
	/** Get DocumentText matches across entire database for fulltext search term(s) */
	@CrossOrigin
	@PostMapping(path = "/full")
	public List<DocumentText> fullSearch(@RequestParam("terms") String terms)
	{
		try {
			return textRepository.search(terms, 1000, 0);
		} catch(org.hibernate.search.exception.EmptyQueryException e) {
			return null;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Get highlights across entire database for fulltext search term(s) */
	@CrossOrigin
	@PostMapping(path = "/context")
	public List<String> contextSearch(@RequestParam("terms") String terms)
	{
		try {
			return textRepository.searchContext(terms, 1000, 0);
		} catch(org.hibernate.search.exception.EmptyQueryException e) {
			return null;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** TODO: test live, log search terms? */
	/** Get EISDoc with ellipses-separated highlights and context across entire database for fulltext search term(s)*/
	@CrossOrigin
	@PostMapping(path = "/fulltext_meta")
	public List<MetadataWithContext> fulltext_meta(@RequestParam("terms") String terms)
	{
		try {
			List<MetadataWithContext> highlightsMeta = new ArrayList<MetadataWithContext>(
					(textRepository.metaContext(terms, 1000, 0)));
			return highlightsMeta;
		} catch(org.hibernate.search.exception.EmptyQueryException e) {
			return null;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	/** Refresh Lucene index so that searching works (adds MySQL document_text table to Lucene via denormalization) */
	@CrossOrigin
	@PostMapping(path = "/sync")
	public boolean sync(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(!isAdmin(token)) 
		{
			return false;
		} else {
			return textRepository.sync();
		}
	}
	

	
	private boolean isAdmin(String token) {
		boolean result = false;
		// get ID
		if(token != null) {
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
package nepaBackend.controller;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.TextRepository;
import nepaBackend.SearchLogRepository;
import nepaBackend.model.DocumentText;

@RestController
@RequestMapping("/test")
public class FulltextController {

	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@Autowired
	private TextRepository textRepository;
	private SearchLogRepository searchLogRepository;
	
	public FulltextController(TextRepository textRepository, 
								SearchLogRepository searchLogRepository) {
		this.textRepository = textRepository;
		this.searchLogRepository = searchLogRepository;
	}

	/** TODO: Finalize, log search terms? */
	@CrossOrigin
	@PostMapping(path = "/full")
	public List<DocumentText> fulltextSearch(@RequestParam("terms") String terms)
	{
		try {
			return textRepository.search(terms, 1000, 0);
		} catch(org.hibernate.search.exception.EmptyQueryException e) {
			return null;
		}
	}
	
	
	
	
	
	// TODO: Temporary dir for testing, will differ for live
//	private static final String INDEX_DIRECTORY = "C:/lucene";
	private static final String INDEX_DIRECTORY = "./data/lucene";
	private File f = new File(INDEX_DIRECTORY);
	
	// TODO: Restrict access
	/** Refresh Lucene index so that searching works (adds MySQL document_text table to Lucene via denormalization) */
	@CrossOrigin
	@PostMapping(path = "/sync")
	public boolean sync( ) {
		return textRepository.sync();
	}

	
	
	
}
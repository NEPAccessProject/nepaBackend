package nepaBackend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.DateValidator;
import nepaBackend.DateValidatorUsingLocalDate;
import nepaBackend.DocService;
import nepaBackend.EISMatchService;
import nepaBackend.SearchLogRepository;
import nepaBackend.model.EISDoc;
import nepaBackend.model.EISMatch;
import nepaBackend.model.SearchLog;
import nepaBackend.pojo.MatchParams;
import nepaBackend.pojo.SearchInputs;

@RestController
@RequestMapping("/test")
public class EISController {
	private SearchLogRepository searchLogRepository;
	
	public EISController(SearchLogRepository searchLogRepository) {
		this.searchLogRepository = searchLogRepository;
	}

	@Autowired
	JdbcTemplate jdbcTemplate;
	@Autowired
	EISMatchService matchService;
	@Autowired
	DocService docService;
	

//	@CrossOrigin(origins = "http://localhost:8081")
//	@GetMapping(path="/search")
//	public @ResponseBody ResponseEntity<String> search(@RequestParam String title) {
//
//	//URI location = ...;
//	//HttpHeaders responseHeaders = new HttpHeaders();
////	responseHeaders.setLocation(location);
////	responseHeaders.set("MyResponseHeader", "MyValue");
//	return new ResponseEntity<String>("Hello World", HttpStatus.OK);
//		
//	}

	@CrossOrigin
	@PostMapping(path = "/search", 
	consumes = "application/json", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<EISDoc>> search(@RequestBody SearchInputs searchInputs) {

		saveSearchLog(searchInputs);
		
		try {
			// Init parameter list
			ArrayList<String> inputList = new ArrayList<String>();
			ArrayList<String> whereList = new ArrayList<String>();
			
			// Select tables, columns
			String sQuery = "SELECT * FROM eisdoc";
			
			// TODO: join lists/logic
			
			// TODO: For the future, load Dates of format yyyy-MM=dd into db.
			// This will change STR_TO_DATE(register_date, '%m/%d/%Y') >= ?
			// to just register_date >= ?
			// Right now the db has Strings of MM/dd/yyyy
			
			// Populate lists
			if(saneInput(searchInputs.startPublish)) {
				inputList.add(searchInputs.startPublish);
				whereList.add(" ((register_date) >= ?)");
			}
			
			if(saneInput(searchInputs.endPublish)) {
				inputList.add(searchInputs.endPublish);
				whereList.add(" ((register_date) <= ?)");
			}

			if(saneInput(searchInputs.startComment)) {
				inputList.add(searchInputs.startComment);
				whereList.add(" ((comment_date) >= ?)");
			}
			
			if(saneInput(searchInputs.endComment)) {
				inputList.add(searchInputs.endComment);
				whereList.add(" ((comment_date) <= ?)");
			}
			
			if(saneInput(searchInputs.typeAll)) { 
				// do nothing
			} else {
				ArrayList<String> typesList = new ArrayList<>();
				StringBuilder query = new StringBuilder(" document_type IN (");
				if(saneInput(searchInputs.typeFinal)) {
					typesList.add("Final");
				}

				if(saneInput(searchInputs.typeDraft)) {
					typesList.add("Draft");
				}
				
				if(saneInput(searchInputs.typeOther)) {
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
				String[] docTypes = typesList.toArray(new String[0]);
				for (int i = 0; i < docTypes.length; i++) {
					if (i > 0) {
						query.append(",");
					}
					query.append("?");
				}
				query.append(")");

				for (int i = 0; i < docTypes.length; i++) {
					inputList.add(docTypes[i]);
				}
				
				if(docTypes.length>0) {
					whereList.add(query.toString());
				}

			}

			// TODO: Temporary logic, filenames should each have their own field in the database later 
			// and they may also be a different format
			// (this will eliminate the need for the _% LIKE logic also)
			// _ matches exactly one character and % matches zero to many, so _% matches at least one arbitrary character
			if(saneInput(searchInputs.needsComments)) {
//				whereList.add(" (documents LIKE 'CommentLetters-_%' OR documents LIKE 'EisDocuments-_%;CommentLetters-_%')");
				whereList.add(" (comments_filename<>'')");
			}

			if(saneInput(searchInputs.needsDocument)) { // Don't need an input for this right now
//				whereList.add(" (documents LIKE 'EisDocuments-_%' OR documents LIKE 'EisDocuments-_%;CommentLetters-_%')");
				whereList.add(" (filename<>'')");
			}
			
			if(saneInput(searchInputs.state)) {
				StringBuilder query = new StringBuilder(" state IN (");
				for (int i = 0; i < searchInputs.state.length; i++) {
					if (i > 0) {
						query.append(",");
					}
					query.append("?");
				}
				query.append(")");

				for (int i = 0; i < searchInputs.state.length; i++) {
					inputList.add(searchInputs.state[i]);
				}
				whereList.add(query.toString());
			}

			if(saneInput(searchInputs.agency)) {
				StringBuilder query = new StringBuilder(" agency IN (");
				for (int i = 0; i < searchInputs.agency.length; i++) {
					if (i > 0) {
						query.append(",");
					}
					query.append("?");
				}
				query.append(")");

				for (int i = 0; i < searchInputs.agency.length; i++) {
					inputList.add(searchInputs.agency[i]);
				}
				whereList.add(query.toString());
			}
			
			boolean no_title = false;
			if(saneInput(searchInputs.title)) { // Good to put this last
				inputList.add(searchInputs.title);
				if(searchInputs.searchMode.equals("boolean")) {
					whereList.add(" MATCH(title) AGAINST(? IN BOOLEAN MODE)");
				} else {
					whereList.add(" MATCH(title) AGAINST(? IN NATURAL LANGUAGE MODE)");
				}
			} else {
				no_title = true;
			}
			
			boolean addAnd = false;
			for (String i : whereList) {
				if(addAnd) { // Not first conditional, append AND
					sQuery += " AND";
				} else { // First conditional, append WHERE
					sQuery += " WHERE";
				}
				sQuery += i; // Append conditional
				
				addAnd = true; // Raise AND flag for future iterations
			}

			// Finalize query
			int limit = 1000; 
			if(no_title) {
				sQuery += " ORDER BY title";
			}
			if(saneInput(searchInputs.limit)) {
				if(searchInputs.limit > 100000) {
					limit = 100000; // TODO: Review 100k as upper limit
				} else {
					limit = searchInputs.limit;
				}
			}
			sQuery += " LIMIT " + String.valueOf(limit);
			
			// Run query
			List<EISDoc> records = jdbcTemplate.query
			(
				sQuery, 
				inputList.toArray(new Object[] {}),
				(rs, rowNum) -> new EISDoc(
					rs.getLong("id"), 
					rs.getString("title"), 
					rs.getString("document_type"),
					rs.getObject("comment_date", LocalDate.class), 
					rs.getObject("register_date", LocalDate.class), 
					rs.getString("agency"),
					rs.getString("state"), 
					rs.getString("filename"),
					rs.getString("comments_filename"),
					rs.getString("folder")
				)
			);
			
			
			// TODO: If title is blank, or if using boolean mode, order by title.
			// Otherwise let natural language mode pick the top results
			
			// debugging
			Boolean testing = false;
			if(testing) {
				DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
				DateValidator validator = new DateValidatorUsingLocalDate(dateFormatter);
				System.out.println(validator.isValid(searchInputs.endPublish));
				System.out.println(sQuery); 
				System.out.println(searchInputs.endPublish);
				System.out.println(searchInputs.title);
			}
			

			return new ResponseEntity<List<EISDoc>>(records, HttpStatus.OK);
		} catch (Exception e) {
//	if (log.isDebugEnabled()) {
//		log.debug(e);
//	}
			e.printStackTrace();
			return new ResponseEntity<List<EISDoc>>(HttpStatus.NO_CONTENT);
		}
	}
	
	private void saveSearchLog(SearchInputs searchInputs) {
		try {
			SearchLog searchLog = new SearchLog();
			
			// TODO: For the future, load Dates of format yyyy-MM=dd into db.
			// This will change STR_TO_DATE(register_date, '%m/%d/%Y') >= ?
			// to just register_date >= ?
			// Right now the db has Strings of MM/dd/yyyy
			
			// Populate lists
			if(saneInput(searchInputs.startPublish)) {
				searchLog.setStartPublish(searchInputs.startPublish);
			}
			
			if(saneInput(searchInputs.endPublish)) {
				searchLog.setEndPublish(searchInputs.endPublish);
			}

			if(saneInput(searchInputs.startComment)) {
				searchLog.setStartComment(searchInputs.startComment);
			}
			
			if(saneInput(searchInputs.endComment)) {
				searchLog.setEndComment(searchInputs.endComment);
			}

			searchLog.setDocumentTypes("All"); // handles all or blank (equivalent to all)
			if(saneInput(searchInputs.typeAll)) { 
				// do nothing
			} else {
				ArrayList<String> typesList = new ArrayList<>();
				if(saneInput(searchInputs.typeFinal)) {
					typesList.add("Final");
				}

				if(saneInput(searchInputs.typeDraft)) {
					typesList.add("Draft");
				}
				
				if(saneInput(searchInputs.typeOther)) {
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
			if(saneInput(searchInputs.needsComments)) {
				searchLog.setNeedsComments(searchInputs.needsComments);
			}

			if(saneInput(searchInputs.needsDocument)) { 
				searchLog.setNeedsDocument(searchInputs.needsDocument);
			}
			
			if(saneInput(searchInputs.state)) {
				searchLog.setState(String.join(",", searchInputs.state));
			}

			if(saneInput(searchInputs.agency)) {
				searchLog.setAgency(String.join(",", searchInputs.agency));
			}
			
			if(saneInput(searchInputs.title)) {
				searchLog.setTitle(searchInputs.title);
			}
			
			if(saneInput(searchInputs.searchMode)) {
				searchLog.setSearchMode(searchInputs.searchMode);
			}
			
			searchLog.setHowMany(1000); 
			if(saneInput(searchInputs.limit)) {
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

	@CrossOrigin
	@PostMapping(path = "/match", 
	consumes = "application/json", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<EISMatchData> match(@RequestBody MatchParams matchParams) {
		try {
//			System.out.println("ID " + matchParams.id);
//			System.out.println("Match % " + matchParams.matchPercent);
			
			// Sanity check match percent, force bounds 1-100
			BigDecimal match_percent = validateInput(matchParams.matchPercent);
			
			// Sanity check ID
			if(matchParams.id < 0) {
				// No negative IDs possible
				return new ResponseEntity<EISMatchData>(HttpStatus.NO_CONTENT);
			}
			
			List<EISMatch> matches = matchService.getAllBy(matchParams.id, match_percent);

			List<Integer> idList1 = matches.stream().map(EISMatch::getDocument1).collect(Collectors.toList());
			List<Integer> idList2 = matches.stream().map(EISMatch::getDocument2).collect(Collectors.toList());
			
//			idList1.forEach(System.out::println);
//			idList2.forEach(System.out::println);
//			System.out.println(matches.get(0).getDocument1());
//			System.out.println(matches.get(0).getDocument2());
			if(idList1.isEmpty() || idList2.isEmpty()) { // No match
				return new ResponseEntity<EISMatchData>(HttpStatus.OK);
			}
			List<EISDoc> docs = docService.getAllDistinctBy(matchParams.id, idList1, idList2);
			
			EISMatchData matchData = new EISMatchData(matches, docs);

			return new ResponseEntity<EISMatchData>(matchData, HttpStatus.OK);
		} catch (IndexOutOfBoundsException e ) { // Result set empty (length 0)
			return new ResponseEntity<EISMatchData>(HttpStatus.OK);
		} catch (Exception e) {
//			System.out.println(e);
			return new ResponseEntity<EISMatchData>(HttpStatus.NO_CONTENT);
		}
	}
	
	/** Get a list of matches (only data from Match table: ID pair, percentage) */
	@CrossOrigin
	@PostMapping(path = "/matchTest", 
	consumes = "application/json", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody List<EISMatch> matchTest(@RequestParam("match_id") int match_id,
			@RequestParam("match_percent") BigDecimal match_percent) {

		return matchService.getAllBy(match_id, match_percent);
		// TODO: Validate the two params required?
		// Don't want to allow match_percent < 1, etc.
	}
	
	@CrossOrigin
	@PostMapping(path = "/check") // to simply verify user has access to /test/**
	public void check() {
	}
	
	/** Get all titles (too heavy to put into a select on frontend) */
//	@CrossOrigin
//	@PostMapping(path = "/titles")
//	public List<String> titles()
//	{
//		return docService.getAllTitles();
//	}
	
	/** Get close titles */
	@CrossOrigin
	@PostMapping(path = "/titles")
	public List<String> titles(@RequestParam("title") String title)
	{
		return docService.getByTitle(title);
	}
	
	@CrossOrigin
	@RequestMapping(path = "/get_by_id", method = RequestMethod.GET)
	public Optional<EISDoc> getById(@RequestParam String id, @RequestHeader Map<String, String> headers) {
		Long lid = Long.parseLong(id);
		try {
			return docService.findById(lid);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// TODO: Validation for everything, like Dates
	
	private BigDecimal validateInput(BigDecimal decInput) {
		BigDecimal match_percent;
		if(decInput.compareTo(new BigDecimal("0.01")) < 0) {
			match_percent = new BigDecimal("0.01");
		} else if(decInput.compareTo(new BigDecimal("1.00")) > 0) {
			match_percent = new BigDecimal("1.00");
		} else {
			match_percent = decInput;
		}
		return match_percent;
	}

	// TODO: Smarter sanity check
	private boolean saneInput(String sInput) {
		if(sInput == null) {
			return false;
		}
		return (sInput.trim().length() > 0);
	}
	
	private boolean saneInput(String[] sInput) {
		if(sInput == null || sInput.length == 0) {
			return false;
		}
		return true;
	}
	
	private boolean saneInput(boolean bInput) {
		return bInput;
	}
	// TODO: Validation for everything, like Dates

	private boolean saneInput(int iInput) {
		if(iInput > 0 && iInput <= Integer.MAX_VALUE) {
			return true;
		}
		return false;
	}
}
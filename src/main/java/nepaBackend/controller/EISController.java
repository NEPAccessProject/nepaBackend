package nepaBackend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.DateValidator;
import nepaBackend.DateValidatorUsingLocalDate;
import nepaBackend.DocService;
import nepaBackend.EISMatchService;
import nepaBackend.Globals;
import nepaBackend.SearchLogRepository;
import nepaBackend.UpdateLogRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.EISDoc;
import nepaBackend.model.EISMatch;
import nepaBackend.model.SearchLog;
import nepaBackend.model.UpdateLog;
import nepaBackend.pojo.MatchParams;
import nepaBackend.pojo.SearchInputs;
import nepaBackend.pojo.UploadInputs;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/test")
public class EISController {
	private SearchLogRepository searchLogRepository;
	private ApplicationUserRepository applicationUserRepository;
	private UpdateLogRepository updateLogRepository;
	
	private static DateTimeFormatter[] parseFormatters = Stream.of("yyyy-MM-dd", "MM-dd-yyyy", 
			"yyyy/MM/dd", "MM/dd/yyyy", 
			"M/dd/yyyy", "yyyy/M/dd", "M-dd-yyyy", "yyyy-M-dd",
			"MM/d/yyyy", "yyyy/MM/d", "MM-d-yyyy", "yyyy-MM-d",
			"M/d/yyyy", "yyyy/M/d", "M-d-yyyy", "yyyy-M-d",
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
			.map(DateTimeFormatter::ofPattern)
			.toArray(DateTimeFormatter[]::new);
	
	public EISController(SearchLogRepository searchLogRepository, ApplicationUserRepository applicationUserRepository,
			UpdateLogRepository updateLogRepository) {
		this.searchLogRepository = searchLogRepository;
		this.applicationUserRepository = applicationUserRepository;
		this.updateLogRepository = updateLogRepository;
	}

	@Autowired
	JdbcTemplate jdbcTemplate;
	@Autowired
	EISMatchService matchService;
	@Autowired
	DocService docService;
	
//	@CrossOrigin
//	@PostMapping(path = "/search", 
//	consumes = "application/json", 
//	produces = "application/json", 
//	headers = "Accept=application/json")
//	public @ResponseBody ResponseEntity<List<EISDoc>> search(@RequestBody SearchInputs searchInputs) {
//
//		saveSearchLog(searchInputs);
//		
//		try {
//			// Init parameter list
//			ArrayList<String> inputList = new ArrayList<String>();
//			ArrayList<String> whereList = new ArrayList<String>();
//			
//			// Select tables, columns
//			String sQuery = "SELECT * FROM eisdoc";
//			
//			// TODO: join lists/logic
//			
//			// TODO: For the future, load Dates of format yyyy-MM=dd into db.
//			// This will change STR_TO_DATE(register_date, '%m/%d/%Y') >= ?
//			// to just register_date >= ?
//			// Right now the db has Strings of MM/dd/yyyy
//			
//			// Populate lists
//			if(saneInput(searchInputs.startPublish)) {
//				inputList.add(searchInputs.startPublish);
//				whereList.add(" ((register_date) >= ?)");
//			}
//			
//			if(saneInput(searchInputs.endPublish)) {
//				inputList.add(searchInputs.endPublish);
//				whereList.add(" ((register_date) <= ?)");
//			}
//
//			if(saneInput(searchInputs.startComment)) {
//				inputList.add(searchInputs.startComment);
//				whereList.add(" ((comment_date) >= ?)");
//			}
//			
//			if(saneInput(searchInputs.endComment)) {
//				inputList.add(searchInputs.endComment);
//				whereList.add(" ((comment_date) <= ?)");
//			}
//			
//			if(saneInput(searchInputs.typeAll)) { 
//				// do nothing
//			} else {
//				ArrayList<String> typesList = new ArrayList<>();
//				StringBuilder query = new StringBuilder(" document_type IN (");
//				if(saneInput(searchInputs.typeFinal)) {
//					typesList.add("Final");
//				}
//
//				if(saneInput(searchInputs.typeDraft)) {
//					typesList.add("Draft");
//				}
//				
//				if(saneInput(searchInputs.typeOther)) {
//					List<String> typesListOther = Arrays.asList("Draft Supplement",
//							"Final Supplement",
//							"Second Draft Supplemental",
//							"Second Draft",
//							"Adoption",
//							"LF",
//							"Revised Final",
//							"LD",
//							"Third Draft Supplemental",
//							"Second Final",
//							"Second Final Supplemental",
//							"DC",
//							"FC",
//							"RF",
//							"RD",
//							"Third Final Supplemental",
//							"DD",
//							"Revised Draft",
//							"NF",
//							"F2",
//							"D2",
//							"F3",
//							"DE",
//							"FD",
//							"DF",
//							"FE",
//							"A3",
//							"A1");
//					typesList.addAll(typesListOther);
//				}
//				String[] docTypes = typesList.toArray(new String[0]);
//				for (int i = 0; i < docTypes.length; i++) {
//					if (i > 0) {
//						query.append(",");
//					}
//					query.append("?");
//				}
//				query.append(")");
//
//				for (int i = 0; i < docTypes.length; i++) {
//					inputList.add(docTypes[i]);
//				}
//				
//				if(docTypes.length>0) {
//					whereList.add(query.toString());
//				}
//
//			}
//
//			// TODO: Temporary logic, filenames should each have their own field in the database later 
//			// and they may also be a different format
//			// (this will eliminate the need for the _% LIKE logic also)
//			// _ matches exactly one character and % matches zero to many, so _% matches at least one arbitrary character
//			if(saneInput(searchInputs.needsComments)) {
////				whereList.add(" (documents LIKE 'CommentLetters-_%' OR documents LIKE 'EisDocuments-_%;CommentLetters-_%')");
//				whereList.add(" (comments_filename<>'')");
//			}
//
//			if(saneInput(searchInputs.needsDocument)) { // Don't need an input for this right now
////				whereList.add(" (documents LIKE 'EisDocuments-_%' OR documents LIKE 'EisDocuments-_%;CommentLetters-_%')");
//				whereList.add(" (filename<>'')");
//			}
//			
//			if(saneInput(searchInputs.state)) {
//				StringBuilder query = new StringBuilder(" state IN (");
//				for (int i = 0; i < searchInputs.state.length; i++) {
//					if (i > 0) {
//						query.append(",");
//					}
//					query.append("?");
//				}
//				query.append(")");
//
//				for (int i = 0; i < searchInputs.state.length; i++) {
//					inputList.add(searchInputs.state[i]);
//				}
//				whereList.add(query.toString());
//			}
//
//			if(saneInput(searchInputs.agency)) {
//				StringBuilder query = new StringBuilder(" agency IN (");
//				for (int i = 0; i < searchInputs.agency.length; i++) {
//					if (i > 0) {
//						query.append(",");
//					}
//					query.append("?");
//				}
//				query.append(")");
//
//				for (int i = 0; i < searchInputs.agency.length; i++) {
//					inputList.add(searchInputs.agency[i]);
//				}
//				whereList.add(query.toString());
//			}
//			
//			boolean no_title = false;
//			if(saneInput(searchInputs.title)) { // Good to put this last
//				inputList.add(searchInputs.title);
////				if(searchInputs.searchMode.equals("boolean")) {
//					whereList.add(" MATCH(title) AGAINST(? IN BOOLEAN MODE)");
////				} else {
////					whereList.add(" MATCH(title) AGAINST(? IN NATURAL LANGUAGE MODE)");
////				}
//			} else {
//				no_title = true;
//			}
//			
//			boolean addAnd = false;
//			for (String i : whereList) {
//				if(addAnd) { // Not first conditional, append AND
//					sQuery += " AND";
//				} else { // First conditional, append WHERE
//					sQuery += " WHERE";
//				}
//				sQuery += i; // Append conditional
//				
//				addAnd = true; // Raise AND flag for future iterations
//			}
//			
//			// If natural language mode with title, accept default order (sorted by internal score). Otherwise, order by title
//			if(no_title || searchInputs.searchMode.equals("boolean")) {
//				sQuery += " ORDER BY title";
//			}
//			
//			// Finalize query
//			int limit = 100000;
//			if(saneInput(searchInputs.limit)) {
//				if(searchInputs.limit <= 100000) {
//					limit = searchInputs.limit;
//				}
//			}
//			sQuery += " LIMIT " + String.valueOf(limit);
//			
//			// Run query
//			List<EISDoc> records = jdbcTemplate.query
//			(
//				sQuery, 
//				inputList.toArray(new Object[] {}),
//				(rs, rowNum) -> new EISDoc(
//					rs.getLong("id"), 
//					rs.getString("title"), 
//					rs.getString("document_type"),
//					rs.getObject("comment_date", LocalDate.class), 
//					rs.getObject("register_date", LocalDate.class), 
//					rs.getString("agency"),
//					rs.getString("department"),
//					rs.getString("cooperating_agency"),
//					rs.getString("summary_text"),
//					rs.getString("state"), 
//					rs.getString("filename"),
//					rs.getString("comments_filename"),
//					rs.getString("folder"),
//					rs.getLong("size"),
//					rs.getString("web_link"),
//					rs.getString("notes"),
//					rs.getObject("noi_date", LocalDate.class), 
//					rs.getObject("draft_noa", LocalDate.class), 
//					rs.getObject("final_noa", LocalDate.class), 
//					rs.getObject("first_rod_date", LocalDate.class)
//				)
//			);
//			
//			
//			// TODO: If title is blank, or if using boolean mode, order by title.
//			// Otherwise let natural language mode pick the top results
//			
//			// debugging
//			if(Globals.TESTING && searchInputs.endPublish != null) {
//				DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
//				DateValidator validator = new DateValidatorUsingLocalDate(dateFormatter);
//				System.out.println(validator.isValid(searchInputs.endPublish));
//				System.out.println(sQuery); 
//				System.out.println(searchInputs.endPublish);
//				System.out.println(searchInputs.title);
//			}
//			
//
//			return new ResponseEntity<List<EISDoc>>(records, HttpStatus.OK);
//		} catch (Exception e) {
////	if (log.isDebugEnabled()) {
////		log.debug(e);
////	}
//			e.printStackTrace();
//			return new ResponseEntity<List<EISDoc>>(HttpStatus.NO_CONTENT);
//		}
//	}
	
//	private void saveSearchLog(SearchInputs searchInputs) {
//		try {
//			SearchLog searchLog = new SearchLog();
//			
//			// TODO: For the future, load Dates of format yyyy-MM=dd into db.
//			// This will change STR_TO_DATE(register_date, '%m/%d/%Y') >= ?
//			// to just register_date >= ?
//			// Right now the db has Strings of MM/dd/yyyy
//			
//			// Populate lists
//			if(saneInput(searchInputs.startPublish)) {
//				searchLog.setStartPublish(searchInputs.startPublish);
//			}
//			
//			if(saneInput(searchInputs.endPublish)) {
//				searchLog.setEndPublish(searchInputs.endPublish);
//			}
//
//			if(saneInput(searchInputs.startComment)) {
//				searchLog.setStartComment(searchInputs.startComment);
//			}
//			
//			if(saneInput(searchInputs.endComment)) {
//				searchLog.setEndComment(searchInputs.endComment);
//			}
//
//			searchLog.setDocumentTypes("All"); // handles all or blank (equivalent to all)
//			if(saneInput(searchInputs.typeAll)) { 
//				// do nothing
//			} else {
//				ArrayList<String> typesList = new ArrayList<>();
//				if(saneInput(searchInputs.typeFinal)) {
//					typesList.add("Final");
//				}
//
//				if(saneInput(searchInputs.typeDraft)) {
//					typesList.add("Draft");
//				}
//				
//				if(saneInput(searchInputs.typeOther)) {
//					List<String> typesListOther = Arrays.asList("Draft Supplement",
//							"Final Supplement",
//							"Second Draft Supplemental",
//							"Second Draft",
//							"Adoption",
//							"LF",
//							"Revised Final",
//							"LD",
//							"Third Draft Supplemental",
//							"Second Final",
//							"Second Final Supplemental",
//							"DC",
//							"FC",
//							"RF",
//							"RD",
//							"Third Final Supplemental",
//							"DD",
//							"Revised Draft",
//							"NF",
//							"F2",
//							"D2",
//							"F3",
//							"DE",
//							"FD",
//							"DF",
//							"FE",
//							"A3",
//							"A1");
//					typesList.addAll(typesListOther);
//				}
//				if(typesList.size() > 0) {
//					searchLog.setDocumentTypes( String.join(",", typesList) );
//				}
//
//			}
//
//			// TODO: Temporary logic, filenames should each have their own field in the database later 
//			// and they may also be a different format
//			// (this will eliminate the need for the _% LIKE logic also)
//			// _ matches exactly one character and % matches zero to many, so _% matches at least one arbitrary character
//			if(saneInput(searchInputs.needsComments)) {
//				searchLog.setNeedsComments(searchInputs.needsComments);
//			}
//
//			if(saneInput(searchInputs.needsDocument)) { 
//				searchLog.setNeedsDocument(searchInputs.needsDocument);
//			}
//			
//			if(saneInput(searchInputs.state)) {
//				searchLog.setState(String.join(",", searchInputs.state));
//			}
//
//			if(saneInput(searchInputs.agency)) {
//				searchLog.setAgency(String.join(",", searchInputs.agency));
//			}
//			
//			if(saneInput(searchInputs.title)) {
//				searchLog.setTitle(searchInputs.title);
//			}
//			
//			if(saneInput(searchInputs.searchMode)) {
//				searchLog.setSearchMode(searchInputs.searchMode);
//			}
//			
//			searchLog.setHowMany(1000); 
//			if(saneInput(searchInputs.limit)) {
//				if(searchInputs.limit > 100000) {
//					searchLog.setHowMany(100000); // TODO: Review 100k as upper limit
//				} else {
//					searchLog.setHowMany(searchInputs.limit);
//				}
//			}
//			
//			searchLog.setUserId(null); // TODO: Non-anonymous user IDs if opted-in
//			searchLog.setSavedTime(LocalDateTime.now());
//
//			searchLogRepository.save(searchLog);
//			
//		} catch (Exception e) {
////			if (log.isDebugEnabled()) {
////				log.debug(e);
////			}
////			System.out.println(e);
//		}
//	
//	}

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
	
	@CrossOrigin
	@GetMapping(path = "/match_all_pairs")
	public @ResponseBody ResponseEntity<Object> getAllPairs(@RequestHeader Map<String, String> headers) {
		// TODO: Admin only
		return new ResponseEntity<Object>(matchService.getAllPairs(), HttpStatus.OK);
	}
	@CrossOrigin
	@GetMapping(path = "/match_all_pairs_one")
	public @ResponseBody ResponseEntity<Object> getAllPairsOne(@RequestHeader Map<String, String> headers) {
		// TODO: Admin only
		return new ResponseEntity<Object>(matchService.getAllPairsAtLeastOneFile(), HttpStatus.OK);
	}
	@CrossOrigin
	@GetMapping(path = "/match_all_pairs_two")
	public @ResponseBody ResponseEntity<Object> getAllPairsTwo(@RequestHeader Map<String, String> headers) {
		// TODO: Admin only
		return new ResponseEntity<Object>(matchService.getAllPairsTwoFiles(), HttpStatus.OK);
	}
	@CrossOrigin
	@GetMapping(path = "/search_logs")
	public @ResponseBody ResponseEntity<List<Object>> getAllSearchLogs(@RequestHeader Map<String, String> headers) {
		// TODO: Admin only
		return new ResponseEntity<List<Object>>(searchLogRepository.countDistinctTerms(), HttpStatus.OK);
	}
	
	/** 
	 * Use additional heuristics for better matches.  
	 * Logic:
	 * For a given metadata ID (or each ID in a list of up to all IDs):
	 * 1. Get the highest % matching pair including that ID (or all pairs above 50% threshold)
	 * 2. Verify same state;
	 * 3. Verify same lead agency;
	 * 4. Verify different document type
	 * (option: could enforce draft + final pairs only if we don't care about supplemental etc.) 
	 * 5. Try to check that dates make sense: i.e. final date should be AFTER draft date;
	 * 
	 * 6. At the end, can pick the highest matching type for each type.  For example,
	 * we could stop showing two drafts if we have one match to a final at 51% and one at 95%.
	 * (There's probably a more efficient way to do this, like only getting the highest types
	 * in the original query.)
	 * 
	 * All of this can be done in either the frontend, here or in the SQL query. */
	@CrossOrigin
	@PostMapping(path = "/match_advanced", 
	consumes = "application/json", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<EISMatchData> matchAdvanced(@RequestBody MatchParams matchParams) {
		
		System.out.println(matchParams.id);
		System.out.println(matchParams.matchPercent);
		
		try {
			// Sanity check ID
			if(matchParams.id < 0) {
				// No negative IDs possible
				return new ResponseEntity<EISMatchData>(HttpStatus.NO_CONTENT);
			}

			List<EISMatch> matches = matchService.getAllBy(matchParams.id, matchParams.matchPercent);

			// Note: Could map eisdocs in the ORM so we don't have to do it this way
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
			if(Globals.TESTING) {System.out.println("Initial list length " + docs.size());}
			EISDoc original = docService.findById((long) matchParams.id).get();
			
			if(original == null) {
				return new ResponseEntity<EISMatchData>(HttpStatus.NOT_FOUND);
			}
			
//			for(int i = 0; i < docs.size(); i++) {
//				System.out.println("Doc: " + docs.get(i).getId());
//				System.out.print("Match 1: " + matches.get(i).getDocument1());
//				System.out.println(" Match 2: " + matches.get(i).getDocument2());
//			}

			// Absolutely must sort matches by document2.
			
			// When removing from list, need to also remove from match list.
			
			// Other heuristics (could be optional?)
			for(int i = docs.size() - 1; i >= 0; i--) { // Decrementing because list shrinks
				// State (remove if different)
				if(!original.getState().contentEquals(docs.get(i).getState())) {
					if(Globals.TESTING) {System.out.println("Unmatching because state doesn't match: " + docs.get(i).getState());}
					docs.remove(i);
				// Agency (remove if different)
				} else if (!original.getAgency().contentEquals(docs.get(i).getAgency())) {
					if(Globals.TESTING) {System.out.println("Unmatching because agency doesn't match: " + docs.get(i).getAgency());}
					docs.remove(i);
				// Type (remove if same)
				} else if (original.getDocumentType().contentEquals(docs.get(i).getDocumentType())) {
					if(Globals.TESTING) {System.out.println("Unmatching because type identical: " + docs.get(i).getDocumentType());}
					docs.remove(i);
				// Date (we've verified they're different types by now,
				// so if one of them is final and one is draft but the draft is later
				// then remove it)
				} else if (
						(original.getDocumentType().contentEquals("Final")
						&& docs.get(i).getDocumentType().contentEquals("Draft")
						&& original.getRegisterDate().compareTo(docs.get(i).getRegisterDate()) < 0)
						|| 
						(original.getDocumentType().contentEquals("Draft")
						&& docs.get(i).getDocumentType().contentEquals("Final")
						&& original.getRegisterDate().compareTo(docs.get(i).getRegisterDate()) > 0)
					) 
				{
					if(Globals.TESTING) {System.out.println("Unmatching because of date comparison: " + docs.get(i).getRegisterDate());}
					docs.remove(i);
				}
			}
			
			/** Comb through list, remove duplicate types of lower match percent */
			// for zero through size() - 1 docs...
//			for(int i = 0; i < docs.size() - 1; i++) {
				// for 1 through size() docs...
//				for(int j = 1; j < docs.size(); j++) {
					// If not a self-comparison...
//					if(j != i) {
						// If identical document type...
//						if(docs.get(i).getDocumentType().contentEquals(docs.get(j).getDocumentType())) {
							// if match i < j (returns -1, i is less of a match)
							// Can't expect matches and docs to line up...  so we actually have to look through
							// BOTH ID lists from the matching pairs.  Twice.  One for i, one for j.
							// May need to redesign things in the database and also maybe then use SQL for this.
//							if(!flags[i] && matches.get(i).getMatch_percent().compareTo(matches.get(j).getMatch_percent()) < 0) {
//								// remove at index i
//								if(Globals.TESTING) {System.out.println("Removing lower match " + i + " of " + matches.get(i).getMatch_percent() + " vs " + j + " " + matches.get(j).getMatch_percent());}
//							} else if(!flags[j]){
//								// otherwise, remove j (j is less than or equal to i's match)
//								if(Globals.TESTING) {System.out.println("Removing lower match " + j + " of " + matches.get(j).getMatch_percent() + " vs " + i + " " + matches.get(i).getMatch_percent());}
//							}
//						}
//					} else {
//						if(Globals.TESTING) {System.out.println("Skipping " + i + " " + j);}
//					}
//				}
//			}
			
			EISMatchData matchData = new EISMatchData(matches, docs);
			
//			System.out.println(matchData.getDocs().size());
//			System.out.println(matchData.getMatches().size());

			return new ResponseEntity<EISMatchData>(matchData, HttpStatus.OK);
		} catch (IndexOutOfBoundsException e ) { // Result set empty (length 0)
			System.out.println(e);
			return new ResponseEntity<EISMatchData>(HttpStatus.OK);
		} catch (Exception e) {
			System.out.println(e);
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
	
	/** Updates given UploadInputs "doc" string with ID.
	 * 200: Done
	 * 204: Got document from ID but couldn't update it, somehow 
	 * 401: Not curator/admin 
	 * 404: No document for ID 
	 * 500: Something broke before we even got to the ID */
	@CrossOrigin
	@RequestMapping(path = "/update_doc", method = RequestMethod.POST)
	public ResponseEntity<Void> updateDoc(@RequestPart(name="doc") String doc, @RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(userIsAuthorized(token)) {
			try {
				// translate
				ObjectMapper mapper = new ObjectMapper();
				UploadInputs dto = mapper.readValue(doc, UploadInputs.class);
				LocalDate parsedDate = parseDate(dto.federal_register_date);
				
				dto.federal_register_date = parsedDate.toString();
				
				// update
				ResponseEntity<Void> status = updateFromDTO(dto, token);
				
				return status;
			} catch(Exception e) {
				e.printStackTrace();
				return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	private String fixAgency(String before, String after) {
		List<EISDoc> docs = docService.findAllByAgency(before);
		String returnValue = "";
		for(EISDoc doc: docs) {
			returnValue += doc.getAgency() + ": ";
			
			// update
			doc.setAgency(after); 
			EISDoc status = docService.saveEISDoc(doc);
			
			returnValue += status.getAgency() + "\n";
		}
		return returnValue;
	}
	
	/** Fix errors by the federal government, return before/afters */
	@CrossOrigin
	@RequestMapping(path = "/fix_abbrev", method = RequestMethod.POST)
	public ResponseEntity<String> fixAbbrev(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		String returnValue = "";
		if(userIsAuthorized(token)) {
			try {
				returnValue += fixAgency("ARD","Department of Agriculture"); // Adoption by probably Rural Development, should list as USDA
				returnValue += fixAgency("AFS","Forest Service");
				returnValue += fixAgency("BOEMRE","Bureau of Ocean Energy Management");
				returnValue += fixAgency("BOR","Bureau of Reclamation");
				returnValue += fixAgency("CGD","U.S. Coast Guard");
				returnValue += fixAgency("COE","U.S. Army Corps of Engineers");
				returnValue += fixAgency("FHW","Federal Highway Administration");
				returnValue += fixAgency("FWS","Fish and Wildlife Service");
				returnValue += fixAgency("FirstNet","First Responder Network Authority");
				returnValue += fixAgency("NNS","National Nuclear Security Administration");
				returnValue += fixAgency("NGB","National Guard Bureau");
				returnValue += fixAgency("NIG","National Indian Gaming Commission");
				returnValue += fixAgency("Office of Surface Mining","Office of Surface Mining Reclamation and Enforcement");
				returnValue += fixAgency("OSMRE","Office of Surface Mining Reclamation and Enforcement");
				returnValue += fixAgency("UCG","U.S. Coast Guard");
				returnValue += fixAgency("USAF","United States Air Force");
				returnValue += fixAgency("URC","Utah Reclamation Mitigation and Conservation Commission");
				returnValue += fixAgency("WAP","Western Area Power Administration");
			} catch(Exception e) {
				e.printStackTrace();
				return new ResponseEntity<String>(returnValue + "\n" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			return new ResponseEntity<String>(returnValue, HttpStatus.UNAUTHORIZED);
		}
		return new ResponseEntity<String>(returnValue, HttpStatus.OK);
	}
	
	
	private boolean userIsAuthorized(String token) {
		boolean result = false;
		// get ID
		if(token != null) {
	        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
	                .getId();

			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
			if(user.getRole().equalsIgnoreCase("CURATOR") || user.getRole().equalsIgnoreCase("ADMIN")) {
				result = true;
			}
		}
		return result;

	}
	
	
	/** Turns UploadInputs into valid, current EISDoc and updates it, returns 200 (OK) or 500 (error), 
	 * 404 is no current EISDoc for ID, 400 if title, type or date are missing,
	 * 204 if the .save itself was somehow rejected (database deemed it invalid or no connection?) */
	private ResponseEntity<Void> updateFromDTO(UploadInputs itr, String token) {
		
		Optional<EISDoc> maybeRecord = docService.findById(Long.parseLong(itr.id));
		if(maybeRecord.isPresent()) {
			EISDoc recordToUpdate = maybeRecord.get();

			// translate
			recordToUpdate.setAgency(Globals.normalizeSpace(itr.agency));
			recordToUpdate.setDocumentType(Globals.normalizeSpace(itr.document));
			recordToUpdate.setFilename(itr.filename);
			recordToUpdate.setRegisterDate(LocalDate.parse(itr.federal_register_date));
			
			if(itr.comments_filename == null || itr.comments_filename.isBlank()) {
				// skip
			} else {
				recordToUpdate.setCommentsFilename(itr.comments_filename);
			}
			if(itr.epa_comment_letter_date == null || itr.epa_comment_letter_date.isBlank()) {
				// skip
			} else {
				LocalDate parsedDate = parseDate(itr.epa_comment_letter_date);
				
				itr.epa_comment_letter_date = parsedDate.toString();
				recordToUpdate.setCommentDate(LocalDate.parse(itr.epa_comment_letter_date));
			}
			
			recordToUpdate.setState(Globals.normalizeSpace(itr.state));
			recordToUpdate.setTitle(Globals.normalizeSpace(itr.title));
			recordToUpdate.setFolder(itr.eis_identifier.trim());
			recordToUpdate.setLink(itr.link.trim());
			recordToUpdate.setNotes(itr.notes.trim());
			
			if(recordToUpdate.getTitle().isBlank() || recordToUpdate.getDocumentType().isBlank() 
					|| recordToUpdate.getRegisterDate() == null) {
				return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
			}
			
			EISDoc updatedRecord = docService.saveEISDoc(recordToUpdate); // save to db
			
			if(updatedRecord != null) {
				
				// Log
				UpdateLog updateLog = new UpdateLog();
				updateLog.setDocumentId(updatedRecord.getId());
				updateLog.setAgency(updatedRecord.getAgency());
				updateLog.setTitle(updatedRecord.getTitle());
				updateLog.setDocument(updatedRecord.getDocumentType());
				updateLog.setFilename(updatedRecord.getFilename());
				updateLog.setState(updatedRecord.getState());
				updateLog.setFolder(updatedRecord.getFolder());
				updateLog.setLink(updatedRecord.getLink());
				updateLog.setNotes(updatedRecord.getNotes());

		        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, ""))).getId();
//				ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
				updateLog.setUserId(Long.parseLong(id));
				updateLogRepository.save(updateLog);
				
				return new ResponseEntity<Void>(HttpStatus.OK);
			} else {
				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
			}
		} else {
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}
	}
	
	
	/**
	 * Attempts to return valid parsed LocalDate from String argument, based on formats specified in  
	 * DateTimeFormatter[] parseFormatters
	 * @param date
	 * @throws IllegalArgumentException
	 */
	private LocalDate parseDate(String date) {
		for (DateTimeFormatter formatter : parseFormatters) {
			try {
				return LocalDate.parse(date, formatter);
			} catch (DateTimeParseException dtpe) {
				// ignore, try next
			}
		}
		throw new IllegalArgumentException("Couldn't parse date (preferred format is yyyy-MM-dd): " + date);
	}
	
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

//	private boolean saneInput(String sInput) {
//		if(sInput == null) {
//			return false;
//		}
//		return (sInput.trim().length() > 0);
//	}
//	
//	private boolean saneInput(String[] sInput) {
//		if(sInput == null || sInput.length == 0) {
//			return false;
//		}
//		return true;
//	}
//	
//	private boolean saneInput(boolean bInput) {
//		return bInput;
//	}
//	// TODO: Validation for everything, like Dates
//
//	private boolean saneInput(int iInput) {
//		if(iInput > 0 && iInput <= Integer.MAX_VALUE) {
//			return true;
//		}
//		return false;
//	}
}
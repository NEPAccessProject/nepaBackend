package nepaBackend.controller;

import java.util.ArrayList;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.DocService;
import nepaBackend.EISMatchService;
import nepaBackend.model.EISDoc;
import nepaBackend.model.EISMatch;
import nepaBackend.pojo.MatchParams;
import nepaBackend.pojo.SearchInputs;

@RestController
@RequestMapping("/test")
public class EISController {

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
//	    //URI location = ...;
//	    //HttpHeaders responseHeaders = new HttpHeaders();
////	    responseHeaders.setLocation(location);
////	    responseHeaders.set("MyResponseHeader", "MyValue");
//	    return new ResponseEntity<String>("Hello World", HttpStatus.OK);
//	    	
//	    }

	@CrossOrigin
	@PostMapping(path = "/search", 
	consumes = "application/json", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<EISDoc>> search(@RequestBody SearchInputs searchInputs) {
		
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
				String whereStatement = "";
				boolean orFlag = false;
				if(saneInput(searchInputs.typeFinal)) {
					whereStatement += " (document_type = 'Final')";
					orFlag = true;
				}

				if(saneInput(searchInputs.typeDraft)) {
					if(orFlag) {
						whereStatement += " OR";
					}
					whereStatement += " (document_type = 'Draft')";
				}
				whereList.add(whereStatement);
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
			
			if(saneInput(searchInputs.title)) { // Good to put this last
				inputList.add(searchInputs.title);
				whereList.add(" MATCH(title) AGAINST(? IN NATURAL LANGUAGE MODE)");
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
			sQuery += " LIMIT 1000";
			
			// Run query
			List<EISDoc> records = jdbcTemplate.query
			(
				sQuery, 
				inputList.toArray(new Object[] {}),
				(rs, rowNum) -> new EISDoc(
					rs.getLong("id"), 
					rs.getString("title"), 
					rs.getString("document_type"),
					rs.getString("comment_date"), 
					rs.getString("register_date"), 
					rs.getString("agency"),
					rs.getString("state"), 
					rs.getString("filename"),
					rs.getString("comments_filename")
				)
			);
			
			
			// debugging
			System.out.println(sQuery); 
//			System.out.println(searchInputs.title);

			return new ResponseEntity<List<EISDoc>>(records, HttpStatus.OK);
		} catch (Exception e) {
//	        if (log.isDebugEnabled()) {
//	            log.debug(e);
//	        }
			System.out.println(e);
			return new ResponseEntity<List<EISDoc>>(HttpStatus.NO_CONTENT);
		}
	}
	
	// TODO: Hit with postman, hook up, test
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
			int match_percent;
			if(matchParams.matchPercent < 1) {
				match_percent = 1;
			} else if(matchParams.matchPercent > 100) {
				match_percent = 100;
			} else {
				match_percent = matchParams.matchPercent;
			}
			
			// Sanity check
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
			@RequestParam("match_percent") int match_percent) {

		return matchService.getAllBy(match_id, match_percent);
		// TODO: Validate the two params required?  
		// Don't want to allow match_percent < 1, etc.
	}
	
	@CrossOrigin
	@PostMapping(path = "/check") // to simply verify user has access to /test/**
	public void check() {
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

	private boolean saneInput(double iInput) {
		if(iInput > 0 && iInput <= 100) {
			return true;
		}
		return false;
	}
}
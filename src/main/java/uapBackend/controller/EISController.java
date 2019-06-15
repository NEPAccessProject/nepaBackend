package uapBackend.controller;

import java.util.ArrayList;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.PreparedStatement;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
//import org.springframework.jdbc.core.PreparedStatementSetter;
//import org.springframework.jdbc.core.ResultSetExtractor;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.dao.DataAccessException;

import uapBackend.SearchInputs;
import uapBackend.model.EISDoc;

@RestController
@RequestMapping("/test")
public class EISController {

	@Autowired
	JdbcTemplate jdbcTemplate;

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
			String sQuery = "SELECT * FROM `eis-meta`";
			
			// TODO: join lists/logic
			// TODO: Use a library for table building, drop LIMIT 100
			
			// TODO: For the future, load Dates of format yyyy-MM=dd into db.
			// This will change STR_TO_DATE(register_date, '%m/%d/%Y') >= ?
			// to just register_date >= ?
			// Right now the db has Strings of MM/dd/yyyy
			
			// Populate lists
			if(saneInput(searchInputs.startPublish)) {
				inputList.add(searchInputs.startPublish);
				whereList.add(" (STR_TO_DATE(register_date, '%m/%d/%Y') >= ?)");
			}
			
			if(saneInput(searchInputs.endPublish)) {
				inputList.add(searchInputs.endPublish);
				whereList.add(" (STR_TO_DATE(register_date, '%m/%d/%Y') <= ?)");
			}

			if(saneInput(searchInputs.startComment)) {
				inputList.add(searchInputs.startComment);
				whereList.add(" (STR_TO_DATE(comment_date, '%m/%d/%Y') >= ?)");
			}
			
			if(saneInput(searchInputs.endComment)) {
				inputList.add(searchInputs.endComment);
				whereList.add(" (STR_TO_DATE(comment_date, '%m/%d/%Y') <= ?)");
			}

			if(saneInput(searchInputs.needsComments)) { // Don't need an input for this right now
				// TODO: Temporary logic, filenames should each have their own field in the database later 
				// and they may also be a different format
				whereList.add(" (documents LIKE 'CommentLetters-_____%' OR documents LIKE 'EisDocuments-_____%;CommentLetters-_____%')");
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
				
				addAnd = true; // Raise AND flag
			}
			
			// Finalize query
//			sQuery += " LIMIT 1000";
			
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
					rs.getString("documents")
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

}
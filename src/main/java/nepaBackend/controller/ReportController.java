package nepaBackend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.DocRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.EISDoc;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/reports")
public class ReportController {
	
//	private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
	
	@Autowired
	private ApplicationUserRepository applicationUserRepository;
	@Autowired
	private DocRepository docRepository;
	
	public ReportController() {
	}

	@GetMapping(path = "/report_agency")
	public @ResponseBody ResponseEntity<List<Object[]>> reportAgencyCombined(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(isAdmin(token)) {
			return new ResponseEntity<List<Object[]>>(docRepository.reportAgencyCombined(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<Object[]>>(HttpStatus.UNAUTHORIZED);
		}
	}
	// 2000 incl.
	@GetMapping(path = "/report_agency_2000")
	public @ResponseBody ResponseEntity<List<Object[]>> reportAgencyCombinedAfter2000(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(isAdmin(token)) {
			return new ResponseEntity<List<Object[]>>(docRepository.reportAgencyCombinedAfter2000(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<Object[]>>(HttpStatus.UNAUTHORIZED);
		}
	}
	@GetMapping(path = "/report_agency_process")
	public @ResponseBody ResponseEntity<List<Object[]>> reportAgencyProcess(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(isAdmin(token)) {
			return new ResponseEntity<List<Object[]>>(docRepository.reportAgencyProcess(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<Object[]>>(HttpStatus.UNAUTHORIZED);
		}
	}
	// 2000 incl.
	@GetMapping(path = "/report_agency_process_2000")
	public @ResponseBody ResponseEntity<List<Object[]>> reportAgencyProcessAfter2000(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(isAdmin(token)) {
			return new ResponseEntity<List<Object[]>>(docRepository.reportAgencyProcessAfter2000(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<Object[]>>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	// duplicates

	@GetMapping(path = "/duplicates_size")
	public @ResponseBody ResponseEntity<List<EISDoc>> findAllDuplicatesBySize(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(isAdmin(token)) {
			return new ResponseEntity<List<EISDoc>>(docRepository.findAllSameSize(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EISDoc>>(HttpStatus.UNAUTHORIZED);
		}
	}

	/** Return ApplicationUser given JWT String */
	private ApplicationUser getUser(String token) {
		if(token != null) {
			// get ID
			try {
				String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
					.getId();
//				if(testing) {System.out.println("ID: " + id);}

				ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
//				if(testing) {System.out.println("User ID: " + user.getId());}
				return user;
			} catch (Exception e) {
				return null;
			}
		} else {
			return null;
		}
	}
	
	/** Return whether trusted JWT is from Admin role */
	private boolean isAdmin(String token) {
		boolean result = false;
		ApplicationUser user = getUser(token);
		// get user
		if(user != null) {
			if(user.getRole().contentEquals("ADMIN")) {
				result = true;
			}
		}
		return result;
	}
}
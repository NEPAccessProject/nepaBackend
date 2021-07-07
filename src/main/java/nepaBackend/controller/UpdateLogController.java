package nepaBackend.controller;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.DocService;
import nepaBackend.UpdateLogRepository;
import nepaBackend.UpdateLogService;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.EISDoc;
import nepaBackend.model.UpdateLog;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/update_log")
public class UpdateLogController {
	
	private static final Logger logger = LoggerFactory.getLogger(UpdateLogController.class);
	
	@Autowired
	private ApplicationUserRepository applicationUserRepository;
	@Autowired
	private UpdateLogRepository updateLogRepository;
	@Autowired
	private UpdateLogService updateLogService;
	@Autowired
	DocService docService;
	
	private static DateTimeFormatter[] parseFormatters = Stream.of("yyyy-MM-dd", "MM-dd-yyyy", 
			"yyyy/MM/dd", "MM/dd/yyyy", 
			"M/dd/yyyy", "yyyy/M/dd", "M-dd-yyyy", "yyyy-M-dd",
			"MM/d/yyyy", "yyyy/MM/d", "MM-d-yyyy", "yyyy-MM-d",
			"M/d/yyyy", "yyyy/M/d", "M-d-yyyy", "yyyy-M-d",
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
			.map(DateTimeFormatter::ofPattern)
			.toArray(DateTimeFormatter[]::new);
	
	public UpdateLogController() {
	}


	/** restore EISDoc by update log ID, using update log contents therein */
	@CrossOrigin
	@RequestMapping(path = "/restore", method = RequestMethod.POST)
	public ResponseEntity<Void> restoreDoc(@RequestParam("id") String updateLogID, 
											@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(isAdmin(token)) {
			try {
		        String userID = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, ""))).getId();
				Optional<UpdateLog> logToRestoreFrom = updateLogRepository.findById(Long.parseLong(updateLogID));
				if(logToRestoreFrom.isPresent()) {
					Optional<EISDoc> docToRestore = docService.findById(logToRestoreFrom.get().getDocumentId());
					
					if(docToRestore.isPresent()) {
						EISDoc restored = updateLogService.restore(docToRestore.get(), logToRestoreFrom.get(), userID);

						if(restored != null) {
							return new ResponseEntity<Void>(HttpStatus.OK);
						} else { // restore function hit an error
							return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
						}
					} else {
						return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
					}
					
				} else {
					return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
				}
			} catch(Exception e) {
				return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		} else {
			return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
		}
	}

	/** restore meta record by ID, using the most recent update */
	@CrossOrigin
	@RequestMapping(path = "/restore_doc_last", method = RequestMethod.POST)
	public ResponseEntity<Void> restoreDocFromMostRecentUpdate(@RequestParam("id") String docID, 
											@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(isAdmin(token)) {
			try {
		        String userID = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, ""))).getId();
				Optional<EISDoc> docToRestore = docService.findById(Long.parseLong(docID));
				if(docToRestore.isPresent()) {
					EISDoc restored = this.restoreFromLastUpdate(docToRestore.get(), userID);
					
					if(restored != null) {
						return new ResponseEntity<Void>(HttpStatus.OK);
					} else { // restore function hit an error
						return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
					}
				} else {
					return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
				}
			} catch(Exception e) {
				return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		} else {
			return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
		}
	}

	/** 
	 *  Restores all changes after a given date by a given userID, for a given docID, 
	 *  to the earliest version of the record since that date. */
	@CrossOrigin
	@RequestMapping(path = "/restore_doc_date_user", method = RequestMethod.POST)
	public ResponseEntity<Void> restoreDocFromGivenDateAndUser(
											@RequestParam("id") String docID, 
											@RequestParam("datetime") String datetime, 
											@RequestParam("userid") String user, 
											@RequestHeader Map<String, String> headers) {
		System.out.println(docID);
		System.out.println(datetime);
		System.out.println(user);
		
		String token = headers.get("authorization");
		if(isAdmin(token)) {
			try {
				
				// ID to use to save update logs for this update/restoration
		        String restoringUserID = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, ""))).getId();
		        
		        // Try to get the document we want to restore first
				Optional<EISDoc> docToRestore = docService.findById(Long.parseLong(docID));
				
				if(docToRestore.isPresent()) {
					EISDoc restored = this.restoreFromDateByID(docToRestore.get(), datetime, user, restoringUserID);
					
					if(restored != null) {
						return new ResponseEntity<Void>(HttpStatus.OK);
					} else { // restore function hit an error
						return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
					}
				} else {
					return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
				}
			} catch(Exception e) {
				return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		} else {
			return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	private EISDoc restoreFromDateByID(EISDoc recordToRestore, String datetime, String user, String restoringUserID) {

		Optional<UpdateLog> log = updateLogRepository.getByDocumentIdAfterDateTimeByUser(recordToRestore.getId(),datetime,Long.parseLong(user));
		if(log.isPresent()) {
			EISDoc restored = restoreFromUpdate(recordToRestore,log.get(),restoringUserID);
			
			return restored;
		} else {
			return null;
		}

	}
	
	/** Attempts to restore from most recent update.  
	 *  Returns restored doc, or returns null on failure.
	 *  Current doc's fields before update are also logged in a new UpdateLog */
	private EISDoc restoreFromLastUpdate(EISDoc recordToRestore, String userID) {
		try {
			Optional<UpdateLog> log = updateLogRepository.getMostRecentByDocumentId(recordToRestore.getId());
			if(log.isPresent()) {
				return restoreFromUpdate(recordToRestore,log.get(),userID);
			} else {
				return null;
			}
		} catch(Exception e) {
			logger.error("Restore failed::" + e.getLocalizedMessage());
			return null;
		}
	}

	/** Attempts to restore given EISDoc from given UpdateLog.  
	 *  Returns restored doc, or returns null on failure.
	 *  Current doc's fields before update are also logged in a new UpdateLog */
	private EISDoc restoreFromUpdate(EISDoc recordToRestore, UpdateLog logToUse, String userID) {
		try {
			if(logToUse != null) {
				// Log the record we're about to restore before doing so.
				UpdateLog thisUpdate = updateLogService.newUpdateLogFromEIS(recordToRestore, userID);
				
				recordToRestore = updateLogService.fillEISFromUpdateLog(recordToRestore, logToUse);
				
				EISDoc restored = docService.saveEISDoc(recordToRestore);
				
				if(restored != null) {
					updateLogRepository.save(thisUpdate);
				}
				
				restored = docService.saveEISDoc(restored);
				
				return restored;
				
			} else {
				return null;
			}
		} catch(Exception e) {
			logger.error("Restore failed::" + e.getLocalizedMessage());
			return null;
		}
	}
	
	
	
	
	/** Return true if user is curator or admin */
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
}
package nepaBackend.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.ApplicationUserService;
import nepaBackend.DocService;
import nepaBackend.InteractionLogRepository;
import nepaBackend.SearchLogRepository;
import nepaBackend.enums.ActionSource;
import nepaBackend.enums.ActionType;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.EISDoc;
import nepaBackend.model.InteractionLog;
import nepaBackend.model.SearchLog;
import nepaBackend.pojo.InteractionSearchLog;

@RestController
@RequestMapping("/interaction")
public class InteractionController {

	private static final Logger logger = LoggerFactory.getLogger(InteractionController.class);

	@Autowired
	ApplicationUserService applicationUserService;
	@Autowired
	DocService docService;
	@Autowired
	InteractionLogRepository interactionRepo;
	@Autowired
	SearchLogRepository searchLogRepo;
	
	@CrossOrigin
	@RequestMapping(path = "/set", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<Boolean> interactionSet(@RequestPart(name="source") String source, 
				@RequestPart(name="type") String type, 
				@RequestPart(name="docId") String docId, 
				@RequestHeader Map<String, String> headers) {
		try {
			String token = headers.get("authorization");
			ApplicationUser user = applicationUserService.getUserFromToken(token);
			
			if(user != null) {
				InteractionLog log = new InteractionLog(
						user, 
						Enum.valueOf(ActionSource.class, source), 
						Enum.valueOf(ActionType.class, type),
						docService.findById(Long.parseLong(docId)).get());
				interactionRepo.save(log);
				
				return new ResponseEntity<Boolean>(true,HttpStatus.OK);
			} else {
				// TODO?: Log anonymous interaction
				return new ResponseEntity<Boolean>(false,HttpStatus.OK);
			}
		} catch(Exception e) {
			logger.error("Couldn't save log",e);
			return new ResponseEntity<Boolean>(false,HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@CrossOrigin
	@RequestMapping(path = "/get_all", method = RequestMethod.GET)
	private ResponseEntity<List<InteractionLog>> interactionGet(
				@RequestHeader Map<String, String> headers) {
		try {
			String token = headers.get("authorization");
			ApplicationUser user = applicationUserService.getUserFromToken(token);
			
			if(applicationUserService.isAdmin(user)) {
				List<InteractionLog> logs = interactionRepo.findAll();
				
				return new ResponseEntity<List<InteractionLog>>(logs,HttpStatus.OK);
			} else {
				return new ResponseEntity<List<InteractionLog>>(HttpStatus.UNAUTHORIZED);
			}
		} catch(Exception e) {
			logger.error("Couldn't get logs",e);
			return new ResponseEntity<List<InteractionLog>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Get search logs also */
	@CrossOrigin
	@RequestMapping(path = "/get_all_combined", method = RequestMethod.GET)
	private ResponseEntity<List<InteractionSearchLog>> interactionGetCombined(
				@RequestHeader Map<String, String> headers) {
		try {
			String token = headers.get("authorization");
			ApplicationUser user = applicationUserService.getUserFromToken(token);
			
			if(applicationUserService.approverOrHigher(user)) {
				List<InteractionLog> interactionLogs = interactionRepo.findAll();
				List<SearchLog> searchLogs = searchLogRepo.findAllWithUser();
				
				List<InteractionSearchLog> combinedLogs = 
						new ArrayList<InteractionSearchLog>(interactionLogs.size() + searchLogs.size());
				
				for(InteractionLog log : interactionLogs) {
					ApplicationUser logUser = log.getUser();
					if(logUser == null) {
						combinedLogs.add(new InteractionSearchLog(
								"(anonymous)",
								log.getDoc(),
								log.getActionType().toString(),
								log.getLogTime()
						));
					} else if(logUser.getRole().contentEquals("USER")) {
						combinedLogs.add(new InteractionSearchLog(
								log.getUser().getUsername(),
								log.getDoc(),
								log.getActionType().toString(),
								log.getLogTime()
						));
					}
					
					logUser = null; // help garbage collection
				}
				for(SearchLog log : searchLogs) {
					Optional<ApplicationUser> logUser = applicationUserService.findById(log.getUserId());
					if(!logUser.isPresent()) {
						combinedLogs.add(new InteractionSearchLog(
								"(anonymous or old data)",
								null,
								"SEARCH: " + log.getTerms(),
								log.getSearchTime()
						));
					}
					else if(logUser.isPresent() && logUser.get().getRole().contentEquals("USER")) {
						combinedLogs.add(new InteractionSearchLog(
								logUser.get().getUsername(),
								null,
								"SEARCH: " + log.getTerms(),
								log.getSearchTime()
						));
					}
					
					logUser = null;
				}
				
				// Sort all by timestamp (oldest first)
				Comparator<InteractionSearchLog> comp = (c1, c2) -> { 
			        return c1.getLogTime().compareTo(c2.getLogTime()); 
				};
				Collections.sort(combinedLogs, comp);

				interactionLogs = null;
				searchLogs = null;
				
				return new ResponseEntity<List<InteractionSearchLog>>(combinedLogs,HttpStatus.OK);
			} else {
				return new ResponseEntity<List<InteractionSearchLog>>(HttpStatus.UNAUTHORIZED);
			}
		} catch(Exception e) {
			logger.error("Couldn't get logs",e);
			return new ResponseEntity<List<InteractionSearchLog>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}

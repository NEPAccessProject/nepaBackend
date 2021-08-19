package nepaBackend.controller;

import java.util.List;
import java.util.Map;

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
import nepaBackend.enums.ActionSource;
import nepaBackend.enums.ActionType;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.InteractionLog;

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
	@RequestMapping(path = "/get_all", method = RequestMethod.GET, consumes = "multipart/form-data")
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
}

package nepaBackend.controller;

import java.io.IOException;
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

import com.auth0.jwt.JWT;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.SurveyRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.Survey;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/survey")
public class SurveyController {

	private static final Logger logger = LoggerFactory.getLogger(SurveyController.class);
	
	@Autowired
	SurveyRepository surveyRepo;
	@Autowired
	ApplicationUserRepository applicationUserRepository;
	
	@CrossOrigin
	@RequestMapping(path = "/save", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<Boolean> save(@RequestPart(name="surveyResult") String surveyResult, 
				@RequestHeader Map<String, String> headers) {
		try {
			String token = headers.get("authorization");
			ApplicationUser user = getUser(token);
			
			if(user != null) {
				Survey survey = new Survey(user, surveyResult);
				surveyRepo.save(survey);
				
				return new ResponseEntity<Boolean>(true,HttpStatus.OK);
			} else {
				return new ResponseEntity<Boolean>(false,HttpStatus.OK);
			}
		} catch(Exception e) {
			logger.debug("Couldn't save survey",e);
			return new ResponseEntity<Boolean>(false,HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/** Return ApplicationUser given trusted JWT String */
	private ApplicationUser getUser(String token) {
		if(token != null) {
			// get ID
			try {
				String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
					.getId();
				ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
				return user;
			} catch (Exception e) {
				return null;
			}
		} else {
			return null;
		}
	}
}

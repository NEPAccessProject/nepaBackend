package nepaBackend.controller;

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
import nepaBackend.SurveyRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.Survey;

@RestController
@RequestMapping("/survey")
public class SurveyController {

	private static final Logger logger = LoggerFactory.getLogger(SurveyController.class);
	
	@Autowired
	SurveyRepository surveyRepo;
	@Autowired
	ApplicationUserService applicationUserService;
	
	@CrossOrigin
	@RequestMapping(path = "/save", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<Boolean> save(@RequestPart(name="surveyResult") String surveyResult, 
				@RequestPart(name="searchTerms", required = false) String searchTerms, 
				@RequestHeader Map<String, String> headers) {
		try {
			String token = headers.get("authorization");
			ApplicationUser user = applicationUserService.getUserFromToken(token);
			
			if(user != null) {
				Survey survey = new Survey(user, surveyResult, searchTerms);
				surveyRepo.save(survey);
				
				return new ResponseEntity<Boolean>(true,HttpStatus.OK);
			} else {
				return new ResponseEntity<Boolean>(false,HttpStatus.OK);
			}
		} catch(Exception e) {
			e.printStackTrace();
			logger.debug("Couldn't save survey",e);
			return new ResponseEntity<Boolean>(false,HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}

package nepaBackend.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.DocRepository;

@RestController
@RequestMapping("/stats")
public class StatsController {

	private DocRepository docRepository;
	
	public StatsController(DocRepository docRepository) {
		this.docRepository = docRepository;
	}

	@CrossOrigin
	@GetMapping(path = "/type_count", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getTypeCount() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getTypeCount(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
//			e.printStackTrace();
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	

	@CrossOrigin
	@GetMapping(path = "/downloadable_count_type", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getDownloadableCountByType() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getDownloadableCountByType(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
//			e.printStackTrace();
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@CrossOrigin
	@GetMapping(path = "/draft_final_count_year", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getDraftFinalCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getDraftFinalCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
//			e.printStackTrace();
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@CrossOrigin
	@GetMapping(path = "/draft_final_count_state", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getDraftFinalCountByState() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getDraftFinalCountByState(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
//			e.printStackTrace();
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@CrossOrigin
	@GetMapping(path = "/draft_final_count_agency", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getDraftFinalCountByAgency() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getDraftFinalCountByAgency(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
//			e.printStackTrace();
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@CrossOrigin
	@GetMapping(path = "/agencies", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<String>> getAgencies() {
		try {
			return new ResponseEntity<List<String>>(docRepository.getAgencies(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
			//	e.printStackTrace();
			return new ResponseEntity<List<String>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@CrossOrigin
	@GetMapping(path = "/states", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<String>> getStates() {
		try {
			return new ResponseEntity<List<String>>(docRepository.getStates(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
			//	e.printStackTrace();
			return new ResponseEntity<List<String>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@CrossOrigin
	@GetMapping(path = "/years", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<String>> getYears() {
		try {
			return new ResponseEntity<List<String>>(docRepository.getYears(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
			//	e.printStackTrace();
			return new ResponseEntity<List<String>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
}
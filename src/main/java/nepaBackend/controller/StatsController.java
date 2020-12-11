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
	@GetMapping(path = "/draft_final_count_year_downloadable", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getDownloadableDraftFinalCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getDownloadableDraftFinalCountByYear(), HttpStatus.OK);
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
	
	/** dynamically pull and include a number for “draft records” (i.e., say “x”) and for the 
	 * corresponding searchable / downloadable draft PDF (say x1) files. Add those numbers
		- E.g., within the sentence “This includes draft and final documents”,  make it 
		“This includes x draft and y final documents”.
		- Then, if possible add another sentence like, “Of these, x1 draft and y1 final EISs 
		are in a format that supports full-text searching and downloading.” */

	@CrossOrigin
	@GetMapping(path = "/final_count", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<Long> finalCount() {
		try {
			return new ResponseEntity<Long>(docRepository.getFinalCount(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
			//	e.printStackTrace();
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@CrossOrigin
	@GetMapping(path = "/final_count_downloadable", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<Long> finalCountDownloadable() {
		try {
			return new ResponseEntity<Long>(docRepository.getFinalCountDownloadable(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
			//	e.printStackTrace();
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@CrossOrigin
	@GetMapping(path = "/draft_count", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<Long> draftCount() {
		try {
			return new ResponseEntity<Long>(docRepository.getDraftCount(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
			//	e.printStackTrace();
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@CrossOrigin
	@GetMapping(path = "/draft_count_downloadable", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<Long> draftCountDownloadable() {
		try {
			return new ResponseEntity<Long>(docRepository.getDraftCountDownloadable(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
			//	e.printStackTrace();
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
}
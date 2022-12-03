package nepaBackend.controller;

import java.util.ArrayList;
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

import nepaBackend.ApplicationUserService;
import nepaBackend.DocRepository;
import nepaBackend.SearchLogRepository;
import nepaBackend.TextRepository;

@RestController
@RequestMapping("/stats")
public class StatsController {

	@Autowired
	private DocRepository docRepository;
	@Autowired
	private TextRepository textRepository;
	@Autowired
	private SearchLogRepository searchLogRepository;
	@Autowired
	private ApplicationUserService applicationUserService;
	
	public StatsController() {
	}

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
	

	@GetMapping(path = "/text_count", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<Long> getTextCount() {
		try {
			return new ResponseEntity<Long>(textRepository.count(), HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
//			e.printStackTrace();
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	

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
	
	
	/** Stats for Paul */

	
	@GetMapping(path = "/count_year_downloadable", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getDownloadableCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getDownloadableCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/count_year_downloadable_rod", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getRODDownloadableCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getRODDownloadableCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/count_year", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getMetadataCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getMetadataCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/count_year_rod", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getRODCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getRODCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/count_year_draft", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getDraftCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getDraftCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/count_year_downloadable_draft", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getDownloadableDraftCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getDownloadableDraftCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/count_year_final", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getFinalCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getFinalCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/count_year_downloadable_final", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getDownloadableFinalCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getDownloadableFinalCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/count_year_supplement", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getSupplementCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getSupplementCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/count_year_downloadable_supplement", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getDownloadableSupplementCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getDownloadableSupplementCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/count_year_other", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getOtherCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getOtherCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping(path = "/count_year_downloadable_other", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Object>> getDownloadableOtherCountByYear() {
		try {
			return new ResponseEntity<List<Object>>(docRepository.getDownloadableOtherCountByYear(), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	
	/**  */

	
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

	@GetMapping(path = "/years", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<List<Integer>> getYears() {
		try {
			List<Object> results = docRepository.getYears();
			List<Integer> formattedResults = new ArrayList<Integer>();
			for(Object result : results) {
				if(result != null) {
					formattedResults.add(Integer.parseInt(String.valueOf(result)));
				}
			}
			return new ResponseEntity<List<Integer>>(formattedResults, HttpStatus.OK);
		} catch (Exception e) {
			//	if (log.isDebugEnabled()) {
			//		log.debug(e);
			//	}
//			e.printStackTrace();
			return new ResponseEntity<List<Integer>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping(path = "/earliest_year", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<Integer> getEarliestYear() {
		try {
			Object result = docRepository.getEarliestYear();
			int formattedResult = Integer.parseInt(String.valueOf(result));
			return new ResponseEntity<Integer>(formattedResult, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<Integer>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/latest_year", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<Integer> getLatestYear() {
		try {
			Object result = docRepository.getLatestYear();
			int formattedResult = Integer.parseInt(String.valueOf(result));
			return new ResponseEntity<Integer>(formattedResult, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<Integer>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/eis_count", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<Long> getDownloadableEISCount() {
		try {
			Long result = docRepository.getDownloadableEISCount();
			return new ResponseEntity<Long>(result, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping(path = "/total_count", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<Long> getDownloadableCount() {
		try {
			Long result = docRepository.getDownloadableCount();
			return new ResponseEntity<Long>(result, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/** dynamically pull and include a number for “draft records” (i.e., say “x”) and for the 
	 * corresponding searchable / downloadable draft PDF (say x1) files. Add those numbers
		- E.g., within the sentence “This includes draft and final documents”,  make it 
		“This includes x draft and y final documents”.
		- Then, if possible add another sentence like, “Of these, x1 draft and y1 final EISs 
		are in a format that supports full-text searching and downloading.” */

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
	
	@GetMapping(path = "/find_all_searches")
	public @ResponseBody ResponseEntity<List<Object>> findAllWithUsername(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.approverOrHigher(token)) {
			return new ResponseEntity<List<Object>>(searchLogRepository.findAllWithUsername(), HttpStatus.OK);			
		} else {
			return new ResponseEntity<List<Object>>(HttpStatus.UNAUTHORIZED);
		}
	}
	
}
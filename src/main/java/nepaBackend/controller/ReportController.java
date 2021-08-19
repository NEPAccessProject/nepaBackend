package nepaBackend.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.ApplicationUserService;
import nepaBackend.DeleteRequestRepository;
import nepaBackend.DocRepository;
import nepaBackend.ExcelRepository;
import nepaBackend.model.DeleteRequest;
import nepaBackend.model.EISDoc;
import nepaBackend.model.Excel;

@RestController
@RequestMapping("/reports")
public class ReportController {
	
//	private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
	
	@Autowired
	private ApplicationUserService applicationUserService;
	@Autowired
	private DocRepository docRepository;
	@Autowired
	private DeleteRequestRepository drr;
	@Autowired
	private ExcelRepository excelRepo;
	
	public ReportController() {
	}

	@GetMapping(path = "/report_agency")
	public @ResponseBody ResponseEntity<List<Object[]>> reportAgencyCombined(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.isAdmin(token)) {
			return new ResponseEntity<List<Object[]>>(docRepository.reportAgencyCombined(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<Object[]>>(HttpStatus.UNAUTHORIZED);
		}
	}
	// 2000 incl.
	@GetMapping(path = "/report_agency_2000")
	public @ResponseBody ResponseEntity<List<Object[]>> reportAgencyCombinedAfter2000(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.isAdmin(token)) {
			return new ResponseEntity<List<Object[]>>(docRepository.reportAgencyCombinedAfter2000(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<Object[]>>(HttpStatus.UNAUTHORIZED);
		}
	}
	@GetMapping(path = "/report_agency_process")
	public @ResponseBody ResponseEntity<List<Object[]>> reportAgencyProcess(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.isAdmin(token)) {
			return new ResponseEntity<List<Object[]>>(docRepository.reportAgencyProcess(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<Object[]>>(HttpStatus.UNAUTHORIZED);
		}
	}
	// 2000 incl.
	@GetMapping(path = "/report_agency_process_2000")
	public @ResponseBody ResponseEntity<List<Object[]>> reportAgencyProcessAfter2000(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.isAdmin(token)) {
			return new ResponseEntity<List<Object[]>>(docRepository.reportAgencyProcessAfter2000(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<Object[]>>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	// duplicates

	@GetMapping(path = "/duplicates_size")
	public @ResponseBody ResponseEntity<List<EISDoc>> findAllDuplicatesBySize(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.isAdmin(token)) {
			return new ResponseEntity<List<EISDoc>>(docRepository.findAllSameSize(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EISDoc>>(HttpStatus.UNAUTHORIZED);
		}
	}
	

	@GetMapping(path = "/excel_get")
	public @ResponseBody ResponseEntity<Excel> excelGet(@RequestHeader Map<String, String> headers) {
		return new ResponseEntity<Excel>(excelRepo.findMostRecent().get(), HttpStatus.OK);
	}
	@PostMapping(path = "/excel_post")
	public @ResponseBody ResponseEntity<Void> excelPost(@RequestHeader Map<String, String> headers,
			@RequestBody String json) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			excelRepo.save(new Excel(json));
			return new ResponseEntity<Void>(HttpStatus.OK);
		} else {
			return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	
	// TODO: Get proposed filenames to be deleted and the record ID each belongs to
	// (requires some custom sql, joins)
	@GetMapping(path = "/get_delete_requests")
	public @ResponseBody ResponseEntity<List<DeleteRequest>> findAllDeleteRequests(@RequestHeader Map<String,String> headers) {

		List<DeleteRequest> requests = drr.findAll();
		for(DeleteRequest req: requests) {
			if(!req.getFulfilled()) {
				if(req.getIdType().contentEquals("document_text")) {
					// get filename from table and document ID
				} else if(req.getIdType().contentEquals("nepafile")) { 
					// get filename from table and document ID
				}
			}
		}
		return new ResponseEntity<List<DeleteRequest>>(requests, HttpStatus.OK);
	}
}
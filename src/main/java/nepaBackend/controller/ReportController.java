package nepaBackend.controller;

import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.ApplicationUserService;
import nepaBackend.DeleteRequestRepository;
import nepaBackend.DocRepository;
import nepaBackend.ExcelRepository;
import nepaBackend.model.DeleteRequest;
import nepaBackend.model.EISDoc;
import nepaBackend.model.Excel;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/reports")
public class ReportController {
	
	private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private JavaMailSender sender;
    
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

	/** If report is missing, Spring returns a 400 error automatically */
	@RequestMapping(path = "/report_data_issue", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<Void> reportDataIssue(
				@RequestPart(name="report") String reportText, 
				@RequestPart(name="processId", required = false) String processId,
				@RequestHeader Map<String, String> headers) {
		
		
		String token = headers.get("authorization");
		String userEmail = "";
		try {
			userEmail = applicationUserService.getUserFromToken(token).getEmail();
		} catch(Exception e) {
			userEmail = "";
		}
		
		try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);

            helper.setTo(new String[] {
            		"ashleystava@arizona.edu",
            		SecurityConstants.EMAIL_HANDLE
    		});
//            message.setFrom(new InternetAddress("NEPAccess <Eller-NepAccess@email.arizona.edu>"));
            message.setFrom(new InternetAddress("NEPAccess <NEPAccess@NEPAccess.org>"));
            helper.setSubject("NEPAccess Data Issue Report");
            helper.setText("Reported by: " + userEmail
            		+ "\nFor process ID "+processId+": https://www.nepaccess.org/process-details?id=" + processId
            		+ "\n\nReport follows: \n\n" + reportText);
             
            sender.send(message);
		} catch(Exception e) {
			e.printStackTrace();
			logger.error("Couldn't send email report from " + userEmail + ": " + reportText + " :: "+e.getLocalizedMessage());
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<Void>(HttpStatus.OK);
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
	// Since files are added by folder name and document type, we want that to be a unique pair.
	// We could add a constraint to the database but that's a lot of overhead
	@GetMapping(path = "/duplicates_type_folder")
	public @ResponseBody ResponseEntity<List<EISDoc>> findNonUniqueTypeFolderPair(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.isAdmin(token)) {
			return new ResponseEntity<List<EISDoc>>(docRepository.findNonUniqueTypeFolderPairs(), HttpStatus.OK);
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
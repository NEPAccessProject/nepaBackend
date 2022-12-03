package nepaBackend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;

import nepaBackend.ApplicationUserService;
import nepaBackend.DocService;
import nepaBackend.EISMatchService;
import nepaBackend.Globals;
import nepaBackend.NEPAFileRepository;
import nepaBackend.ProcessRepository;
import nepaBackend.SearchLogRepository;
import nepaBackend.UpdateLogService;
import nepaBackend.model.EISDoc;
import nepaBackend.model.EISMatch;
import nepaBackend.model.NEPAFile;
import nepaBackend.model.SearchLog;
import nepaBackend.model.UpdateLog;
import nepaBackend.pojo.DocWithFilenames;
import nepaBackend.pojo.EISMatchData;
import nepaBackend.pojo.MatchParams;
import nepaBackend.pojo.UploadInputs;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/test")
public class EISController {
	
	private static final Logger logger = LoggerFactory.getLogger(EISController.class);
	
	@Autowired
	private SearchLogRepository searchLogRepository;
	@Autowired
	private ApplicationUserService applicationUserService;
	@Autowired
	private UpdateLogService updateLogService;
	@Autowired
	private ProcessRepository processRepository;
	@Autowired
	private NEPAFileRepository nepaFileRepo;
	
	private static DateTimeFormatter[] parseFormatters = Stream.of("yyyy-MM-dd", "MM-dd-yyyy", 
			"yyyy/MM/dd", "MM/dd/yyyy", 
			"M/dd/yyyy", "yyyy/M/dd", "M-dd-yyyy", "yyyy-M-dd",
			"MM/d/yyyy", "yyyy/MM/d", "MM-d-yyyy", "yyyy-MM-d",
			"M/d/yyyy", "yyyy/M/d", "M-d-yyyy", "yyyy-M-d",
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
			.map(DateTimeFormatter::ofPattern)
			.toArray(DateTimeFormatter[]::new);
	
	public EISController() {
	}

	@Autowired
	JdbcTemplate jdbcTemplate;
	@Autowired
	EISMatchService matchService;
	@Autowired
	DocService docService;
	
	
    @GetMapping("/findAllDocs")
    public @ResponseBody ResponseEntity<List<EISDoc>> findAllDocs(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
    	if(applicationUserService.curatorOrHigher(token)) {
    		return new ResponseEntity<List<EISDoc>>(docService.findAll(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EISDoc>>(new ArrayList<EISDoc>(), HttpStatus.UNAUTHORIZED);
		}
    }
	
    @GetMapping("/findAllSearchLogs")
    public @ResponseBody ResponseEntity<List<SearchLog>> findAllSearchLogs(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
    	if(applicationUserService.curatorOrHigher(token)) {
    		return new ResponseEntity<List<SearchLog>>(searchLogRepository.findAll(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<SearchLog>>(new ArrayList<SearchLog>(), HttpStatus.UNAUTHORIZED);
		}
    }
    
    @GetMapping("/findMissingProcesses")
    public @ResponseBody ResponseEntity<List<EISDoc>> findMissingProcesses(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
    	if(applicationUserService.curatorOrHigher(token)) {
    		return new ResponseEntity<List<EISDoc>>(docService.findMissingProcesses(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EISDoc>>(new ArrayList<EISDoc>(), HttpStatus.UNAUTHORIZED);
		}
    }
    
    /** To help importer decide what to upload in some cases */
    @GetMapping("/findMissingFilenames")
    public @ResponseBody ResponseEntity<List<String>> findMissingFilenames(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
    	if(applicationUserService.curatorOrHigher(token)) {
    		return new ResponseEntity<List<String>>(docService.findMissingFilenames(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
		}
    }

	@PostMapping(path = "/match", 
	consumes = "application/json", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<EISMatchData> match(@RequestBody MatchParams matchParams) {
		try {
			
			// Sanity check match percent, force bounds 1-100
			BigDecimal match_percent = validateInput(matchParams.matchPercent);
			
			// Sanity check ID
			if(matchParams.id < 0) {
				// No negative IDs possible
				return new ResponseEntity<EISMatchData>(HttpStatus.NO_CONTENT);
			}
			
			List<EISMatch> matches = matchService.getAllBy(matchParams.id, match_percent);

			List<Integer> idList1 = matches.stream().map(EISMatch::getDocument1).collect(Collectors.toList());
			List<Integer> idList2 = matches.stream().map(EISMatch::getDocument2).collect(Collectors.toList());
			
//			idList1.forEach(System.out::println);
//			idList2.forEach(System.out::println);
//			System.out.println(matches.get(0).getDocument1());
//			System.out.println(matches.get(0).getDocument2());
			if(idList1.isEmpty() || idList2.isEmpty()) { // No match
				return new ResponseEntity<EISMatchData>(HttpStatus.OK);
			}
			List<EISDoc> docs = docService.getAllDistinctBy(matchParams.id, idList1, idList2);
			
			EISMatchData matchData = new EISMatchData(matches, docs);

			return new ResponseEntity<EISMatchData>(matchData, HttpStatus.OK);
		} catch (IndexOutOfBoundsException e ) { // Result set empty (length 0)
			return new ResponseEntity<EISMatchData>(HttpStatus.OK);
		} catch (Exception e) {
//			System.out.println(e);
			return new ResponseEntity<EISMatchData>(HttpStatus.NO_CONTENT);
		}
	}
	

	@GetMapping(path = "/duplicates_process")
	public @ResponseBody ResponseEntity<List<EISDoc>> findAllDuplicatesProcess(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			return new ResponseEntity<List<EISDoc>>(docService.findAllDuplicatesProcess(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EISDoc>>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	@GetMapping(path = "/duplicates")
	public @ResponseBody ResponseEntity<List<EISDoc>> findAllDuplicates(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			return new ResponseEntity<List<EISDoc>>(docService.findAllDuplicates(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EISDoc>>(HttpStatus.UNAUTHORIZED);
		}
	}
	@GetMapping(path = "/duplicates_close")
	public @ResponseBody ResponseEntity<List<EISDoc>> findAllDuplicatesCloseDates(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			return new ResponseEntity<List<EISDoc>>(docService.findAllDuplicatesCloseDates(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EISDoc>>(HttpStatus.UNAUTHORIZED);
		}
	}
	@GetMapping(path = "/duplicates_no_date")
	public @ResponseBody ResponseEntity<List<EISDoc>> findAllDuplicatesNoDates(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			return new ResponseEntity<List<EISDoc>>(docService.findAllSameTitleType(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EISDoc>>(HttpStatus.UNAUTHORIZED);
		}
	}

	@GetMapping(path = "/size_under_200")
	public @ResponseBody ResponseEntity<List<EISDoc>> sizeUnder200(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			return new ResponseEntity<List<EISDoc>>(docService.sizeUnder200(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EISDoc>>(HttpStatus.UNAUTHORIZED);
		}
	}

	@GetMapping(path = "/not_indexed")
	public @ResponseBody ResponseEntity<List<EISDoc>> notIndexed(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			return new ResponseEntity<List<EISDoc>>(docService.findNotIndexed(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EISDoc>>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	@GetMapping(path = "/not_extracted")
	public @ResponseBody ResponseEntity<List<EISDoc>> notExtracted(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			return new ResponseEntity<List<EISDoc>>(docService.findNotExtracted(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EISDoc>>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	@GetMapping(path = "/match_all_pairs")
	public @ResponseBody ResponseEntity<Object> getAllPairs(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			return new ResponseEntity<Object>(matchService.getAllPairs(), HttpStatus.OK);
		} else {
			return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
		}
	}
	@GetMapping(path = "/match_all_pairs_one")
	public @ResponseBody ResponseEntity<Object> getAllPairsOne(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			return new ResponseEntity<Object>(matchService.getAllPairsAtLeastOneFile(), HttpStatus.OK);
		} else {
			return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
		}
	}
	@GetMapping(path = "/match_all_pairs_two")
	public @ResponseBody ResponseEntity<Object> getAllPairsTwo(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			return new ResponseEntity<Object>(matchService.getAllPairsTwoFiles(), HttpStatus.OK);
		} else {
			return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
		}
	}
	@GetMapping(path = "/search_logs")
	public @ResponseBody ResponseEntity<List<Object>> getAllSearchLogs(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.approverOrHigher(token)) {
			return new ResponseEntity<List<Object>>(searchLogRepository.countDistinctTerms(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<Object>>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	/** 
	 * Use additional heuristics for better matches.  
	 * Logic:
	 * For a given metadata ID (or each ID in a list of up to all IDs):
	 * 1. Get the highest % matching pair including that ID (or all pairs above 50% threshold)
	 * 2. Verify same state;
	 * 3. Verify same lead agency;
	 * 4. Verify different document type
	 * (option: could enforce draft + final pairs only if we don't care about supplemental etc.) 
	 * 5. Try to check that dates make sense: i.e. final date should be AFTER draft date;
	 * 
	 * 6. At the end, can pick the highest matching type for each type.  For example,
	 * we could stop showing two drafts if we have one match to a final at 51% and one at 95%.
	 * (There's probably a more efficient way to do this, like only getting the highest types
	 * in the original query.)
	 * 
	 * All of this can be done in either the frontend, here or in the SQL query. */
	@PostMapping(path = "/match_advanced", 
	consumes = "application/json", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody ResponseEntity<EISMatchData> matchAdvanced(@RequestBody MatchParams matchParams) {
		
		try {
			// Sanity check ID
			if(matchParams.id < 0) {
				// No negative IDs possible
				return new ResponseEntity<EISMatchData>(HttpStatus.NO_CONTENT);
			}

			List<EISMatch> matches = matchService.getAllBy(matchParams.id, matchParams.matchPercent);

			// Note: Could map eisdocs in the ORM so we don't have to do it this way
			List<Integer> idList1 = matches.stream().map(EISMatch::getDocument1).collect(Collectors.toList());
			List<Integer> idList2 = matches.stream().map(EISMatch::getDocument2).collect(Collectors.toList());

			if(idList1.isEmpty() || idList2.isEmpty()) { // No match
				return new ResponseEntity<EISMatchData>(HttpStatus.OK);
			}
			List<EISDoc> docs = docService.getAllDistinctBy(matchParams.id, idList1, idList2);
			if(Globals.TESTING) {System.out.println("Initial list length " + docs.size());}
			EISDoc original = docService.findById((long) matchParams.id).get();
			
			if(original == null) {
				return new ResponseEntity<EISMatchData>(HttpStatus.NOT_FOUND);
			}
			
//			for(int i = 0; i < docs.size(); i++) {
//				System.out.println("Doc: " + docs.get(i).getId());
//				System.out.print("Match 1: " + matches.get(i).getDocument1());
//				System.out.println(" Match 2: " + matches.get(i).getDocument2());
//			}

			// Absolutely must sort matches by document2.
			
			// When removing from list, need to also remove from match list.
			
			// Other heuristics (could be optional?)
			for(int i = docs.size() - 1; i >= 0; i--) { // Decrementing because list shrinks
				// State (remove if different)
				if(!original.getState().contentEquals(docs.get(i).getState())) {
					if(Globals.TESTING) {System.out.println("Unmatching because state doesn't match: " + docs.get(i).getState());}
					docs.remove(i);
				// Agency (remove if different)
				} else if (!original.getAgency().contentEquals(docs.get(i).getAgency())) {
					if(Globals.TESTING) {System.out.println("Unmatching because agency doesn't match: " + docs.get(i).getAgency());}
					docs.remove(i);
				// Type (remove if same)
				} else if (original.getDocumentType().contentEquals(docs.get(i).getDocumentType())) {
					if(Globals.TESTING) {System.out.println("Unmatching because type identical: " + docs.get(i).getDocumentType());}
					docs.remove(i);
				// Date (we've verified they're different types by now,
				// so if one of them is final and one is draft but the draft is later
				// then remove it)
				} else if (
						(original.getDocumentType().contentEquals("Final")
						&& docs.get(i).getDocumentType().contentEquals("Draft")
						&& original.getRegisterDate().compareTo(docs.get(i).getRegisterDate()) < 0)
						|| 
						(original.getDocumentType().contentEquals("Draft")
						&& docs.get(i).getDocumentType().contentEquals("Final")
						&& original.getRegisterDate().compareTo(docs.get(i).getRegisterDate()) > 0)
					) 
				{
					if(Globals.TESTING) {System.out.println("Unmatching because of date comparison: " + docs.get(i).getRegisterDate());}
					docs.remove(i);
				}
			}
			
			/** Comb through list, remove duplicate types of lower match percent */
			// for zero through size() - 1 docs...
//			for(int i = 0; i < docs.size() - 1; i++) {
				// for 1 through size() docs...
//				for(int j = 1; j < docs.size(); j++) {
					// If not a self-comparison...
//					if(j != i) {
						// If identical document type...
//						if(docs.get(i).getDocumentType().contentEquals(docs.get(j).getDocumentType())) {
							// if match i < j (returns -1, i is less of a match)
							// Can't expect matches and docs to line up...  so we actually have to look through
							// BOTH ID lists from the matching pairs.  Twice.  One for i, one for j.
							// May need to redesign things in the database and also maybe then use SQL for this.
//							if(!flags[i] && matches.get(i).getMatch_percent().compareTo(matches.get(j).getMatch_percent()) < 0) {
//								// remove at index i
//								if(Globals.TESTING) {System.out.println("Removing lower match " + i + " of " + matches.get(i).getMatch_percent() + " vs " + j + " " + matches.get(j).getMatch_percent());}
//							} else if(!flags[j]){
//								// otherwise, remove j (j is less than or equal to i's match)
//								if(Globals.TESTING) {System.out.println("Removing lower match " + j + " of " + matches.get(j).getMatch_percent() + " vs " + i + " " + matches.get(i).getMatch_percent());}
//							}
//						}
//					} else {
//						if(Globals.TESTING) {System.out.println("Skipping " + i + " " + j);}
//					}
//				}
//			}
			
			EISMatchData matchData = new EISMatchData(matches, docs);

			return new ResponseEntity<EISMatchData>(matchData, HttpStatus.OK);
		} catch (IndexOutOfBoundsException e ) { // Result set empty (length 0)
			System.out.println(e);
			return new ResponseEntity<EISMatchData>(HttpStatus.OK);
		} catch (Exception e) {
			System.out.println(e);
			return new ResponseEntity<EISMatchData>(HttpStatus.NO_CONTENT);
		}
	}
	
	/** Get a list of matches (only data from Match table: ID pair, percentage) */
	@PostMapping(path = "/matchTest", 
	consumes = "application/json", 
	produces = "application/json", 
	headers = "Accept=application/json")
	public @ResponseBody List<EISMatch> matchTest(@RequestParam("match_id") int match_id,
			@RequestParam("match_percent") BigDecimal match_percent) {

		return matchService.getAllBy(match_id, match_percent);
		// TODO: Validate the two params required?
		// Don't want to allow match_percent < 1, etc.
	}
	
	@PostMapping(path = "/check") // to simply verify user has access to /test/**
	public void check() {
	}
	
	/** Get close titles */
	@PostMapping(path = "/titles")
	public List<String> titles(@RequestParam("title") String title)
	{
		return docService.getByTitle(title);
	}
	
	@RequestMapping(path = "/get_by_id", method = RequestMethod.GET)
	public Optional<EISDoc> getById(@RequestParam String id, @RequestHeader Map<String, String> headers) {
		try {
			Long lid = Long.parseLong(id);
			return docService.findById(lid);
		} catch(Exception e) {
//			e.printStackTrace();
			return null;
		}
	}
	
	/** Updates given UploadInputs "doc" string with ID.
	 * 200: Done
	 * 204: Got document from ID but couldn't update it, somehow 
	 * 401: Not curator/admin 
	 * 404: No document for ID 
	 * 500: Something broke before we even got to the ID */
	@RequestMapping(path = "/update_doc", method = RequestMethod.POST)
	public ResponseEntity<Void> updateDoc(@RequestPart(name="doc") String doc, 
			@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.curatorOrHigher(token)) {
			try {
				// translate
				ObjectMapper mapper = new ObjectMapper();
				UploadInputs dto = mapper.readValue(doc, UploadInputs.class);
				LocalDate parsedDate = parseDate(dto.federal_register_date);
				
				dto.federal_register_date = parsedDate.toString();
				
				// update
				ResponseEntity<Void> status = updateFromDTO(dto, token);
				
				return status;
			} catch(Exception e) {
				e.printStackTrace();
				return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
		}
	}


	@RequestMapping(path = "/get_process_full", method = RequestMethod.GET)
	public ResponseEntity<List<DocWithFilenames>> getProcessFull(@RequestParam(name="processId") Long processId,
			@RequestHeader Map<String, String> headers) {
		List<EISDoc> docs = docService.findAllByProcessId(processId);
		List<DocWithFilenames> results = new ArrayList<DocWithFilenames>();
		
		for(EISDoc doc: docs) {
			List<NEPAFile> files = nepaFileRepo.findAllByEisdoc(doc);
			List<String> filenames = new ArrayList<String>();
			
			for(NEPAFile file: files) {
				filenames.add(file.getFilename());
			}

			// Order filenames here
//			try { // just in case the ordering fails?
//				NameRanker nr = new NameRanker();
//				nr.rank(filenames);
//			} catch(Exception e) {
//				logger.error("Couldn't NameRanker.rank(filenames): " + String.join(",", filenames));
//			}
			
			results.add(new DocWithFilenames(doc,filenames));
		}
		
		return new ResponseEntity<List<DocWithFilenames>>(results, HttpStatus.OK);
	}
	
	@RequestMapping(path = "/get_process", method = RequestMethod.GET)
	public ResponseEntity<List<EISDoc>> getProcess(@RequestParam(name="processId") Long processId,
			@RequestHeader Map<String, String> headers) {
		return new ResponseEntity<List<EISDoc>>( docService.findAllByProcessId(processId), HttpStatus.OK);
		// no
//		return new ResponseEntity<NEPAProcess>( processRepository.findByProcessId(processId).get(),HttpStatus.OK );
	}
	
	/** Helper method for fixAbbrev performs the actual update (.save) task on all relevant agencies */
	private String fixAgency(String before, String after) {
		List<EISDoc> docs = docService.findAllByAgency(before);
		String returnValue = "";
		for(EISDoc doc: docs) {
			returnValue += doc.getAgency() + ": ";
			
			// update
			doc.setAgency(after); 
			EISDoc status = docService.saveEISDoc(doc);
			
			returnValue += status.getAgency() + "\n";
		}
		return returnValue;
	}
	
	/** Fix errors by the federal government, return before/afters */
	@RequestMapping(path = "/fix_abbrev", method = RequestMethod.POST)
	public ResponseEntity<String> fixAbbrev(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		String returnValue = "";
		
		if(applicationUserService.curatorOrHigher(token)) {
			try {
				returnValue += fixAgency("ARD","Department of Agriculture"); // Adoption by probably Rural Development, should list as USDA
				returnValue += fixAgency("AFS","Forest Service");
				returnValue += fixAgency("BOEMRE","Bureau of Ocean Energy Management");
				returnValue += fixAgency("BOR","Bureau of Reclamation");
				returnValue += fixAgency("CGD","U.S. Coast Guard");
				returnValue += fixAgency("COE","U.S. Army Corps of Engineers");
				returnValue += fixAgency("FHW","Federal Highway Administration");
				returnValue += fixAgency("FWS","Fish and Wildlife Service");
				returnValue += fixAgency("FirstNet","First Responder Network Authority");
				returnValue += fixAgency("NNS","National Nuclear Security Administration");
				returnValue += fixAgency("NGB","National Guard Bureau");
				returnValue += fixAgency("NIG","National Indian Gaming Commission");
				returnValue += fixAgency("Office of Surface Mining","Office of Surface Mining Reclamation and Enforcement");
				returnValue += fixAgency("OSMRE","Office of Surface Mining Reclamation and Enforcement");
				returnValue += fixAgency("UCG","U.S. Coast Guard");
				returnValue += fixAgency("USAF","United States Air Force");
				returnValue += fixAgency("URC","Utah Reclamation Mitigation and Conservation Commission");
				returnValue += fixAgency("WAP","Western Area Power Administration");
			} catch(Exception e) {
				e.printStackTrace();
				return new ResponseEntity<String>(returnValue + "\n" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			return new ResponseEntity<String>(returnValue, HttpStatus.UNAUTHORIZED);
		}
		return new ResponseEntity<String>(returnValue, HttpStatus.OK);
	}
	
	/** Turns UploadInputs into valid, current EISDoc and updates it, returns 200 (OK) or 500 (error), 
	 * 404 is no current EISDoc for ID, 400 if title, type or date are missing,
	 * 204 if the .save itself was somehow rejected (database deemed it invalid or no connection?) */
	private ResponseEntity<Void> updateFromDTO(UploadInputs itr, String token) {
		
		Optional<EISDoc> maybeRecord = docService.findById(Long.parseLong(itr.id));
		if(maybeRecord.isPresent()) {
			EISDoc recordToUpdate = maybeRecord.get();

			// Log the important fields of the record from before the update, not after
			
	        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, ""))).getId();
//			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
	        
	        // Save log of this record before the fields are changed
			UpdateLog updateLog = updateLogService.newUpdateLogFromEIS(recordToUpdate,id);
			updateLogService.save(updateLog);
			
			// translate
			recordToUpdate.setAgency(Globals.normalizeSpace(itr.agency));
			recordToUpdate.setCooperatingAgency(Globals.normalizeSpace(itr.cooperating_agency));
			recordToUpdate.setDepartment(Globals.normalizeSpace(itr.department));
			recordToUpdate.setDocumentType(Globals.normalizeSpace(itr.document));
			recordToUpdate.setFilename(Globals.normalizeSpace(itr.filename));
			recordToUpdate.setRegisterDate(LocalDate.parse(itr.federal_register_date));
			recordToUpdate.setCommentsFilename(Globals.normalizeSpace(itr.comments_filename));
			
			if(itr.epa_comment_letter_date == null || itr.epa_comment_letter_date.isBlank()) {
				// okay, if you insist, remove it
				recordToUpdate.setCommentDate(null);
			} else {
				LocalDate parsedDate = parseDate(itr.epa_comment_letter_date);
				
				itr.epa_comment_letter_date = parsedDate.toString();
				recordToUpdate.setCommentDate(LocalDate.parse(itr.epa_comment_letter_date));
			}
			
			recordToUpdate.setState(Globals.normalizeSpace(itr.state));
			recordToUpdate.setTitle(Globals.normalizeSpace(itr.title));
			recordToUpdate.setFolder(Globals.normalizeSpace(itr.eis_identifier));
			recordToUpdate.setLink(Globals.normalizeSpace(itr.link));
			recordToUpdate.setNotes(Globals.normalizeSpace(itr.notes));
			if(itr.process_id == null || itr.process_id.isBlank()) {
				recordToUpdate.setProcessId(null);
			} else {
				recordToUpdate.setProcessId(Long.parseLong(itr.process_id));
			}

			recordToUpdate.setSubtype(Globals.normalizeSpace(itr.subtype));
			recordToUpdate.setCounty(Globals.normalizeSpace(itr.county));
			recordToUpdate.setStatus(Globals.normalizeSpace(itr.status));
			
			if(recordToUpdate.getTitle().isBlank() || recordToUpdate.getDocumentType().isBlank() 
					|| recordToUpdate.getRegisterDate() == null) {
				return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
			}
			
			EISDoc updatedRecord = docService.saveEISDoc(recordToUpdate); // save to db
			
			if(updatedRecord != null) {
				return new ResponseEntity<Void>(HttpStatus.OK);
			} else {
				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
			}
		} else {
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(path = "/add_rods", method = RequestMethod.POST)
	public ResponseEntity<List<String>> addRodsFromFinals(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.isAdmin(token)) {
			List<EISDoc> finals = docService.findAllFinalsWithFirstRodDates();
			List<String> results = new ArrayList<String>();
			for(EISDoc doc : finals) {
				results.add(this.addRodFromFinal(doc));
			}
			
			return new ResponseEntity<List<String>>(results,HttpStatus.OK);
		} else {
			
			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
			
		}
	}
	
	public String addRodFromFinal(EISDoc finalRecord) {
		if(!docService.existsByTitleTypeDate(
				finalRecord.getTitle(),
				"ROD",
				finalRecord.getFirstRodDate())
		) {
			EISDoc rodRecord = new EISDoc();
			
			rodRecord.setTitle(finalRecord.getTitle());
			rodRecord.setDocumentType("ROD");
			rodRecord.setRegisterDate(finalRecord.getFirstRodDate());
			
			rodRecord.setAgency(Globals.normalizeSpace(finalRecord.getAgency()));
			rodRecord.setCooperatingAgency(Globals.normalizeSpace(finalRecord.getCooperatingAgency()));
			rodRecord.setDepartment(Globals.normalizeSpace(finalRecord.getDepartment()));
			rodRecord.setState(Globals.normalizeSpace(finalRecord.getState()));
			rodRecord.setCounty(Globals.normalizeSpace(finalRecord.getCounty()));
			rodRecord.setProcessId(finalRecord.getProcessId());
			
			docService.saveEISDoc(rodRecord);
			
			return ( "OK:" + rodRecord.getTitle() 
					+ ":FINAL_ID:"+finalRecord.getId() 
					+ ":ROD_DATE:"+rodRecord.getRegisterDate()
					+ ":ROD_ID:"+rodRecord.getId() );
		} else {
			// else we already have this, so don't create a duplicate ROD.
			return ( "Already exists:"+finalRecord.getTitle() + ":FINAL_ID:"+finalRecord.getId() );
		}
	}

	/**
	 * Attempts to return valid parsed LocalDate from String argument, based on formats specified in  
	 * DateTimeFormatter[] parseFormatters
	 * @param date
	 * @throws IllegalArgumentException
	 */
	private LocalDate parseDate(String date) {
		for (DateTimeFormatter formatter : parseFormatters) {
			try {
				return LocalDate.parse(date, formatter);
			} catch (DateTimeParseException dtpe) {
				// ignore, try next
			}
		}
		throw new IllegalArgumentException("Couldn't parse date (preferred format is yyyy-MM-dd): " + date);
	}
	
	private BigDecimal validateInput(BigDecimal decInput) {
		BigDecimal match_percent;
		if(decInput.compareTo(new BigDecimal("0.01")) < 0) {
			match_percent = new BigDecimal("0.01");
		} else if(decInput.compareTo(new BigDecimal("1.00")) > 0) {
			match_percent = new BigDecimal("1.00");
		} else {
			match_percent = decInput;
		}
		return match_percent;
	}

}
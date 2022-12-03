package nepaBackend.controller;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.ApplicationUserService;
import nepaBackend.DeleteRequestRepository;
import nepaBackend.DocRepository;
import nepaBackend.EISMatchService;
import nepaBackend.EmailLogRepository;
import nepaBackend.FileLogRepository;
import nepaBackend.Globals;
import nepaBackend.NEPAFileRepository;
import nepaBackend.TextRepository;
import nepaBackend.UpdateLogRepository;
import nepaBackend.UpdateLogService;
import nepaBackend.UserStatusLogRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.DeleteRequest;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.model.EISMatch;
import nepaBackend.model.EmailLog;
import nepaBackend.model.FileLog;
import nepaBackend.model.NEPAFile;
import nepaBackend.model.UpdateLog;
import nepaBackend.model.UserStatusLog;
import nepaBackend.pojo.EmailOut;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/admin")
public class AdminController {

	@Autowired
    private JavaMailSender sender;
	
    @Autowired
    private ApplicationUserService applicationUserService;
    @Autowired
    private NEPAFileRepository nepaFileRepository;
    @Autowired
    private DocRepository docRepository;
    @Autowired
    private FileLogRepository fileLogRepository;
    @Autowired
    private TextRepository textRepository;
    @Autowired
    private EmailLogRepository emailLogRepository;
    @Autowired
    private UpdateLogRepository updateLogRepository;
    @Autowired
    private EISMatchService matchService;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private DeleteRequestRepository deleteReqRepo;
    @Autowired
    private UpdateLogService updateLogService;
    @Autowired
    private UserStatusLogRepository userStatusLogRepository;

	private static String deleteURL = Globals.UPLOAD_URL.concat("delete_file");
	private static String deleteTestURL = "http://localhost:5309/delete_file";

    public AdminController() {
    }
    
    @PostMapping("/send_email")
    private @ResponseBody ResponseEntity<String> sendEmail(@RequestHeader Map<String, String> headers,
    			@RequestBody EmailOut emailOut) {
		String token = headers.get("authorization");
		
    	if(!applicationUserService.isAdmin(token)) {
			return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
    	} else {
        	try {
                MimeMessage message = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message);
                 
                helper.setTo(emailOut.recipientEmail);
                message.setFrom(new InternetAddress("NEPAccess <NEPAccess@NEPAccess.org>"));
                helper.setSubject(emailOut.subject);
                helper.setText(emailOut.body);
                 
                sender.send(message);
	    		String rsp = "Sent email to " + emailOut.recipientEmail + " about " + emailOut.subject + " with body:\n" + emailOut.body;
	    		
				return new ResponseEntity<String>(rsp, HttpStatus.OK);
	    	} catch (MailAuthenticationException e) {
	            logEmail(emailOut.recipientEmail, e.toString(), "sendEmail", false);
	
	//            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailAuthenticationException");
	
				return new ResponseEntity<String>(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
	    	} catch (MailSendException e) {
	            logEmail(emailOut.recipientEmail, e.toString(), "sendEmail", false);
	
	//            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailSendException");
	
				return new ResponseEntity<String>(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
	    	} catch (MailException e) {
	            logEmail(emailOut.recipientEmail, e.toString(), "sendEmail", false);
	            
	//            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailException");
	
				return new ResponseEntity<String>(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
	    	} catch (Exception e) {
	            logEmail(emailOut.recipientEmail, e.toString(), "sendEmail", false);
	            
	//            emailAdmin(resetUser.getEmail(), e.getMessage(), "Exception");
	
				return new ResponseEntity<String>(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
	    	}
    	}
    }

    /** likely one-time use only */
    @PostMapping("/fix_garbage")
    private @ResponseBody ResponseEntity<String> fixGarbage(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		
    	if(applicationUserService.isAdmin(token)) {
    		int count = 1;
    		// get garbage
	    	List<NEPAFile> garbageFiles = nepaFileRepository.getGarbage();
	    	
	    	String results = "";
	    	
	    	for(NEPAFile garb : garbageFiles) {
	    		results += "Item " + count + " : ";
	    		List<DocumentText> garbageTexts = textRepository.findAllByEisdocAndFilenameIn(
	    				garb.getEisdoc(),
	    				garb.getFilename());
	    		
	    		if(garbageTexts.size() == 2) {
	    			// we have a duplicate text for this, so delete one, and delete the nepafile.

	    			results += "Deleting " + garb.getRelativePath() + garb.getFilename();
	    			
	    			DocumentText trash = garbageTexts.get(1);
	    			textRepository.delete(trash);
	    			nepaFileRepository.delete(garb);
	    			
	    			results += "; no error\r\n";
	    		} else if(garbageTexts.size() == 1 || garbageTexts.size() == 0) {
	    			// (if 0 then Tika failed to convert it)
	    			
	    			// Delete the path only
	    			results += "Deleting " + garb.getRelativePath() + garb.getFilename()
	    					+ "; no extra text to delete";
	    			
	    			nepaFileRepository.delete(garb);
	    			
	    			results += "; no error\r\n";
	    		} else {
	    			// 3+, probably impossible so let's look at it manually
	    			results += "Skipped::Count : " + garbageTexts.size() + " path : " 
	    					+ garb.getRelativePath() + garb.getFilename() + " id : " + garb.getEisdoc().getId()
	    					+ "\r\n";
	    		}
	    		
	    		count++;
	    	}
	    	
			return new ResponseEntity<String>(results, HttpStatus.OK);
    	} else {
			return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
		}
    }
	
    @GetMapping("/findAllEmailLogs")
    private @ResponseBody ResponseEntity<List<EmailLog>> findAllEmailLogs(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		
    	if(applicationUserService.isAdmin(token)) {
    		return new ResponseEntity<List<EmailLog>>(emailLogRepository.findAll(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<EmailLog>>(new ArrayList<EmailLog>(), HttpStatus.UNAUTHORIZED);
		}
    }
	
    @GetMapping("/findAllFileLogs")
    private @ResponseBody ResponseEntity<List<FileLog>> findAllFileLogs(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		
    	if(applicationUserService.isAdmin(token)) {
    		return new ResponseEntity<List<FileLog>>(fileLogRepository.findAll(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<FileLog>>(new ArrayList<FileLog>(), HttpStatus.UNAUTHORIZED);
		}
    }
	
    @GetMapping("/findAllUpdateLogs")
    private @ResponseBody ResponseEntity<List<UpdateLog>> findAllUpdateLogs(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		
    	if(applicationUserService.isAdmin(token)) {
    		return new ResponseEntity<List<UpdateLog>>(updateLogRepository.findAll(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<UpdateLog>>(new ArrayList<UpdateLog>(), HttpStatus.UNAUTHORIZED);
		}
    }
	
    @GetMapping("/findAllUserStatusLogs")
    private @ResponseBody ResponseEntity<List<UserStatusLog>> findAllUserStatusLogs(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		
    	if(applicationUserService.isAdmin(token)) {
    		return new ResponseEntity<List<UserStatusLog>>(userStatusLogRepository.findAll(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<UserStatusLog>>(HttpStatus.UNAUTHORIZED);
		}
    }
	
    @GetMapping("/findAllDeleteRequests")
    private @ResponseBody ResponseEntity<List<DeleteRequest>> findAllDeleteRequests(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		
    	if(applicationUserService.isAdmin(token)) {
    		return new ResponseEntity<List<DeleteRequest>>(deleteReqRepo.findAll(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<DeleteRequest>>(HttpStatus.UNAUTHORIZED);
		}
    }
    

    @RequestMapping(path = "/exec_delete_requests", method = RequestMethod.POST)
    ResponseEntity<String> execDeleteRequests(@RequestHeader Map<String,String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.isAdmin(token)) {
			String results = "";
			
			List<DeleteRequest> requests = deleteReqRepo.findAll();
			
			for(DeleteRequest req: requests) {
				if(req.getFulfilled()) {
					// already processed
				}
				else if( req.getIdType().contentEquals("nepafile") ) {
					String result = this.deleteNepaFileById(req.getIdToDelete().toString(), headers)
							.toString();

					results += result + "\r\n";
					
					req.setFulfilled(true);
					deleteReqRepo.save(req);
				} else if( req.getIdType().contentEquals("document_text") ) {
					String result = this.deleteFileByDocumentTextId(req.getIdToDelete().toString(), headers)
							.toString();

					results += result + "\r\n";
					
					req.setFulfilled(true);
					deleteReqRepo.save(req);
				} else if( req.getIdType().contentEquals("entire_record") ) {
					// Note that this deletes everything - texts, metadata, title matching scores
					String result = this.deleteDoc(req.getIdToDelete().toString(), headers)
							.toString();

					results += result + "\r\n";
					
					req.setFulfilled(true);
					deleteReqRepo.save(req);
				} else {
					results += "Unknown type: " + req.getIdType() + "\r\n";
				}
			}
			return new ResponseEntity<String>(results, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
		}
    }
    
    
    /** Given DocumentText ID, delete the DocumentText and any NEPAFile(s) for it from the linked EISDoc.
     * File is also deleted */
    @RequestMapping(path = "/delete_text", method = RequestMethod.POST)
    ResponseEntity<String> deleteFileByDocumentTextId(@RequestBody String id, @RequestHeader Map<String, String> headers) {
    	// Normally probably want to go by NEPAFile ID but for the original files there are no NEPAFiles anyway
    	try {
    		String token = headers.get("authorization");
			ApplicationUser user = applicationUserService.getUserFromToken(token);
    		if(applicationUserService.isAdmin(user)) {
    			
    			// Get DocumentText by ID
    			Optional<DocumentText> textRecord = textRepository.findById(Long.valueOf(id));
    			if(textRecord.isEmpty()) {
        			return new ResponseEntity<String>("No text record found", HttpStatus.BAD_REQUEST);
    			}
    			
    			// Get DocumentText by eisdoc and filename from NEPAFile
//    			Optional<DocumentText> textRecord = textRepository.findByEisdocAndFilenameIn(eisDoc, presentFile.getFilename());
    			
    			DocumentText presentText = textRecord.get();
    			EISDoc eisDoc = presentText.getEisdoc();
    			// Get NEPAFile by filename and EISDoc
    			Optional<NEPAFile> nepaFile = nepaFileRepository.findByEisdocAndFilenameIn(eisDoc, presentText.getFilename());
    			
    			if(nepaFile.isPresent()) {
    				NEPAFile presentFile = nepaFile.get();
        			
        			// Log + Delete NEPAFile, file from disk
    				fileDeleter(headers, presentFile.getRelativePath() + presentFile.getFilename());
    				logDelete(presentFile.getEisdoc(), "Deleted: NEPAFile", user, presentFile.getFilename());
    				nepaFileRepository.delete(presentFile);
    			}
    			
    			// Log + Delete DocumentText
				logDelete(presentText.getEisdoc(), "Deleted: DocumentText", user, presentText.getFilename());
				textRepository.delete(presentText);
				
    			return new ResponseEntity<String>(HttpStatus.OK);
    		} else if(applicationUserService.isCurator(user)) {
    			deleteReqRepo.save(
    					new DeleteRequest("document_text", Long.valueOf(id), user.getId())
				);
    			return new ResponseEntity<String>("Request entered", HttpStatus.ACCEPTED);
    		} else {
    			return new ResponseEntity<String>("Access denied", HttpStatus.UNAUTHORIZED);
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    		
			return new ResponseEntity<String>("Error: " + e.getStackTrace().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    		
    }

    
    // Deletes NEPAFile, file on disk and related text, sets as not imported
    @RequestMapping(path = "/delete_nepa_file", method = RequestMethod.POST)
    ResponseEntity<String> deleteNepaFileById(@RequestBody String id, @RequestHeader Map<String, String> headers) {
    	try {
    		String token = headers.get("authorization");
			ApplicationUser user = applicationUserService.getUserFromToken(token);
    		if(applicationUserService.isAdmin(user)) {
    			
    			// Get NEPAFile by ID
    			Optional<NEPAFile> nepaFile = nepaFileRepository.findById(Long.valueOf(id));
    			if(nepaFile.isEmpty()) {
        			return new ResponseEntity<String>("No file record found", HttpStatus.BAD_REQUEST);
    			}
    			
    			NEPAFile presentFile = nepaFile.get();
    			EISDoc eisDoc = presentFile.getEisdoc();
    			
    			// Get DocumentText by eisdoc and filename from NEPAFile
    			try {
        			Optional<DocumentText> textRecord = textRepository.findByEisdocAndFilenameIn(eisDoc, presentFile.getFilename());
        			
        			if(textRecord.isPresent()) {
        				DocumentText presentText = textRecord.get();
            			
            			// Log + Delete DocumentText
        				logDelete(presentText.getEisdoc(), "Deleted: DocumentText", user, presentText.getFilename());
        				textRepository.delete(presentText);
        			}
        			
        			// Log + Delete NEPAFile, file on disk
    				fileDeleter(headers, presentFile.getRelativePath() + presentFile.getFilename());
    				logDelete(presentFile.getEisdoc(), "Deleted: NEPAFile", user, presentFile.getFilename());
    				nepaFileRepository.delete(presentFile);
    			} catch(org.springframework.dao.IncorrectResultSizeDataAccessException e) {
    				// Duplicate filename: Can't differentiate now, have to delete one arbitrarily
    				List<DocumentText> records = textRepository.findAllByEisdocAndFilenameIn(eisDoc, presentFile.getFilename());
    				DocumentText presentText = records.get(0);
    				
    				logDelete(presentText.getEisdoc(), "Deleted: DocumentText", user, presentText.getFilename());
    				textRepository.delete(presentText);
    				fileDeleter(headers, presentFile.getRelativePath() + presentFile.getFilename());
    				logDelete(presentFile.getEisdoc(), "Deleted: NEPAFile", user, presentFile.getFilename());
    				nepaFileRepository.delete(presentFile);
    			}
				
    			return new ResponseEntity<String>(HttpStatus.OK);
    		} else if(applicationUserService.isCurator(user)) {
    			deleteReqRepo.save(
    					new DeleteRequest("nepafile", Long.valueOf(id), user.getId())
				);
    			return new ResponseEntity<String>("Request filled", HttpStatus.ACCEPTED);
    		} else {
    			return new ResponseEntity<String>("Access denied", HttpStatus.UNAUTHORIZED);
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    		
			return new ResponseEntity<String>("Error: " + e.getStackTrace().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    		
    }

    /** Delete all NEPAFiles and DocumentTexts from an EISDoc by its ID, then delete the actual files on disk, 
     * and finally delete the Folder field for the EISDoc and update it.
     * Using ORM to delete allows Lucene to automatically also delete the relevant data from its index. */
    @RequestMapping(path = "/deleteAllFiles", method = RequestMethod.POST)
    ResponseEntity<String> deleteAllFiles(@RequestBody String id, @RequestHeader Map<String, String> headers) {
    	List<String> deletedList = new ArrayList<String>();
    	
    	
    	try {
    		String token = headers.get("authorization");
    		if(!applicationUserService.isAdmin(token)) {
    			return new ResponseEntity<String>("Access denied", HttpStatus.UNAUTHORIZED);
    		}
    		
    		
    		else {

    	    	Long idToDelete = Long.valueOf(id);

    			ApplicationUser user = applicationUserService.getUserFromToken(token);
    			
    			// Delete documenttexts and nepafiles and possibly eisdoc.filename, then clear folder name from eisdoc, then log

    			Optional<EISDoc> doc = docRepository.findById(idToDelete);
    			
    			if(doc.isEmpty()) {
    				return new ResponseEntity<String>("No such document for ID " + idToDelete, HttpStatus.NOT_FOUND);
    			}
    			
    			EISDoc foundDoc = doc.get();
    			
    			// Delete full texts
    			List<DocumentText> textList = textRepository.findAllByEisdoc(foundDoc);
    			for(DocumentText text : textList) {
    				deletedList.add("DocumentText " + text.getFilename());
    				textRepository.deleteById(text.getId());
    				
    				// Log 
    				logDelete(foundDoc, "Deleted: DocumentText", user, text.getFilename());
    			}
    			
    			// Delete NEPAFiles
    			List<NEPAFile> nepaFileList = nepaFileRepository.findAllByEisdoc(foundDoc);
    			for(NEPAFile nepaFile : nepaFileList) {
    				
    				// Delete from disk here
    				fileDeleter(headers, nepaFile.getRelativePath() + nepaFile.getFilename());
    				
    				// if matching (single file link), delete filename from record.
    				if(nepaFile.getFilename().contentEquals(foundDoc.getFilename())) {
    					foundDoc.setFilename("");
    				}

    				deletedList.add("NEPAFile " + nepaFile.getFilename());
    				
    				nepaFileRepository.deleteById(nepaFile.getId());
    				
    				// Log
    				logDelete(foundDoc, "Deleted: NEPAFile", user, nepaFile.getFilename());
    			}

    			// Remove folder link
    			foundDoc.setFolder("");
    			
    			// Update EISDoc
    			docRepository.save(foundDoc);
    			
    			String result = "";
    			for(String item : deletedList) {
    				result += item + "\n";
    			}
    			return new ResponseEntity<String>(result, HttpStatus.OK);
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    		
			return new ResponseEntity<String>("Error: " + e.getStackTrace().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    }

    // Deletes the eisdoc itself after calling deleteAllFiles and deleteTitleAlignmentScores
    @RequestMapping(path = "/deleteDoc", method = RequestMethod.POST)
    ResponseEntity<String> deleteDoc(@RequestBody String id, @RequestHeader Map<String, String> headers) {

		String token = headers.get("authorization");

		ApplicationUser user = applicationUserService.getUserFromToken(token);
		
		if(!applicationUserService.isAdmin(user)) {
			// Delete request logic if curator
			if(applicationUserService.isCurator(user)) {
    			deleteReqRepo.save(
    					new DeleteRequest("entire_record", Long.valueOf(id), user.getId())
				);
    			return new ResponseEntity<String>("Request entered", HttpStatus.ACCEPTED);
			} else {
				return new ResponseEntity<String>("Access denied", HttpStatus.UNAUTHORIZED);
			}
		}
    	
    	try {

        	// First, ensure we delete the associated files, or they will be orphaned
        	ResponseEntity<String> deleteAllResponse = this.deleteAllFiles(id, headers);
        	if(deleteAllResponse.getStatusCodeValue() != 200) { // Couldn't delete?
        		// Return response
    			return deleteAllResponse;
        	}

        	Long idToDelete = Long.valueOf(id);
			
			// Try to get by ID
			Optional<EISDoc> doc = docRepository.findById(idToDelete);
			if(doc.isEmpty()) {
				return new ResponseEntity<String>("No such document for ID " + idToDelete, HttpStatus.NOT_FOUND);
			}
			EISDoc foundDoc = doc.get();
			
			// delete useless match data
			deleteTitleAlignmentScores(foundDoc);
			
			// Delete metadata
			try {
    			docRepository.delete(foundDoc);
			} catch (IllegalArgumentException e) {
				return new ResponseEntity<String>("Error deleting: " + e.getStackTrace().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
			// Log
			logDelete(foundDoc, "Deleted: EISDoc", user, "EISDoc");
			
			return new ResponseEntity<String>("Deleted " + id, HttpStatus.OK);
    	} catch(Exception e) {
    		e.printStackTrace();
    		
			return new ResponseEntity<String>("Error: " + e.getStackTrace().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    }
    
	/** Delete all files, scores, EISDocs matching list of EISDoc IDs */
    @PostMapping(path = "/delete_all", consumes = "multipart/form-data")
    public @ResponseBody ResponseEntity<String> deleteAllDocs(@RequestParam String[] deleteList,
			@RequestHeader Map<String, String> headers) {

    	String token = headers.get("authorization");
    	if(!applicationUserService.isAdmin(token)) {
    		return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
    	}
    	
    	if(Globals.TESTING) {
    		for(String id : deleteList) {
    			System.out.println("ID: " + id);
    		}
    	}

		ApplicationUser user = applicationUserService.getUserFromToken(token);
    	String serverResponse = "";
    	
    	for(String id : deleteList) {
    		try {
    			EISDoc doc = docRepository.findById(Long.parseLong(id)).get();
    			
    			serverResponse.concat(this.deleteAllFiles(id, headers).getBody());
    			
    			this.deleteTitleAlignmentScores(doc);
    			
    			// Delete metadata
    			try {
        			docRepository.delete(doc);
    			} catch (IllegalArgumentException e) {
    				return new ResponseEntity<String>("Error deleting: " + e.getStackTrace().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    			}
    			
    			// Log
				logDelete(doc, "Deleted: EISDoc", user, "EISDoc");
    			
    			serverResponse += "\nDeleted: " + id;
    			
    		} catch(Exception e) {
    			serverResponse += "\nSkipped (exception): " + id;
    		}
    	}
    	
    	return new ResponseEntity<String>(serverResponse, HttpStatus.OK);
    }
    
	private String fileDeleter(Map<String, String> headers, String path) {
		String token = headers.get("authorization");
		if(!applicationUserService.isCurator(token) && !applicationUserService.isAdmin(token)) 
		{
			return null;
		}
	
	    try {
	    	// In a single-machine setup, this could be done within Java and not with external services
		    HttpPost request = new HttpPost(deleteURL);
		    if(Globals.TESTING) { request = new HttpPost(deleteTestURL); }
		    request.setHeader("key", SecurityConstants.APP_KEY);
		    request.setHeader("filename", path);
	
		    HttpClient client = HttpClientBuilder.create().build();
		    HttpResponse response = client.execute(request);
		    
		    return response.getStatusLine().toString();
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	return e.getLocalizedMessage();
	    }
	}
    
    private void deleteTitleAlignmentScores(EISDoc doc) {
    	List<EISMatch> toDelete1 = matchService.findAllByDocument1( java.lang.Math.toIntExact(doc.getId()) );
    	matchService.deleteInBatch(
    			toDelete1
    	);
    	
    	List<EISMatch> toDelete2 = matchService.findAllByDocument2( java.lang.Math.toIntExact(doc.getId()) );
    	matchService.deleteInBatch(
    			toDelete2
    	);
    }

    // for admin, set password of non-admin user
    @PostMapping("/set_password")
    private @ResponseBody ResponseEntity<Boolean> setPassword(@RequestParam Long userId, 
    			@RequestParam String password, 
    			@RequestHeader Map<String, String> headers) {

		String token = headers.get("authorization");
    	if(!applicationUserService.isAdmin(token)) {
    		return new ResponseEntity<Boolean>(false, HttpStatus.UNAUTHORIZED);
    	} else if (!Globals.validPassword(password)) {
    		return new ResponseEntity<Boolean>(false, HttpStatus.BAD_REQUEST);
    	} else {
    		ApplicationUser user = applicationUserService.findById(Long.valueOf(userId)).get();
    		if(user.getRole().contentEquals("ADMIN")) {
        		return new ResponseEntity<Boolean>(false, HttpStatus.UNAUTHORIZED);
    		} else {
                user.setPassword(bCryptPasswordEncoder.encode(password));
                applicationUserService.save(user);

        		return new ResponseEntity<Boolean>(true, HttpStatus.OK);
    		}
    	}
    }
    
    /** Just saves an UpdateLog with a special "Deleted" status so we can potentially restore
     * deletes the same way we can restore updates */
	private void logDelete(EISDoc foundDoc, String message, ApplicationUser user, String filename) {
		UpdateLog updateLog = updateLogService.newUpdateLogFromEIS(foundDoc, user.getId());
		updateLog.setNotes("~DELETED~" + updateLog.getNotes());
		
		updateLogService.save(updateLog);
	}

	
    /**
     * @param email
     * @param errorString
     * @param emailType
     * @param sent
     * @return
     */
    private boolean logEmail(String email, String errorString, String emailType, Boolean sent) {
    	try {
        	EmailLog log = new EmailLog();
    		log.setEmail(email);
    		log.setErrorType(errorString);
    		log.setEmailType(emailType); // ie "Reset"
    		log.setSent(sent);
    		log.setLogTime(LocalDateTime.now());
    		emailLogRepository.save(log);
    		return true;
    	} catch (Exception e) {
    		// do nothing
    	}
    	return false;
    }
}
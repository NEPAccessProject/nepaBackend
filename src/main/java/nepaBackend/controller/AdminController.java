package nepaBackend.controller;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.DocRepository;
import nepaBackend.FileLogRepository;
import nepaBackend.NEPAFileRepository;
import nepaBackend.TextRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.model.FileLog;
import nepaBackend.model.NEPAFile;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/admin")
public class AdminController {
	
    private ApplicationUserRepository applicationUserRepository;
    private NEPAFileRepository nepaFileRepository;
    private DocRepository docRepository;
    private FileLogRepository fileLogRepository;
    private TextRepository textRepository;

    public AdminController(DocRepository docRepository,
			TextRepository textRepository,
			FileLogRepository fileLogRepository,
			ApplicationUserRepository applicationUserRepository,
			NEPAFileRepository nepaFileRepository) {
		this.docRepository = docRepository;
		this.textRepository = textRepository;
		this.fileLogRepository = fileLogRepository;
		this.applicationUserRepository = applicationUserRepository;
		this.nepaFileRepository = nepaFileRepository;
    }
    
    
    /** Given DocumentText ID, delete the DocumentText and any NEPAFile(s) for it from the linked EISDoc.
     * Does not delete the actual file, so a bulk file process call will pick it back up unless
     * the file is deleted manually */
    @CrossOrigin
    @RequestMapping(path = "/delete_text", method = RequestMethod.POST)
    ResponseEntity<String> deleteFileByDocumentTextId(@RequestBody String id, @RequestHeader Map<String, String> headers) {
    	System.out.println(id);
    	// Normally probably want to go by NEPAFile ID but for the original files there are no NEPAFiles anyway
    	try {
    		String token = headers.get("authorization");
    		if(!isAdmin(token)) {
    			return new ResponseEntity<String>("Access denied", HttpStatus.UNAUTHORIZED);
    		}
    		else {
    			ApplicationUser user = getUser(token);
    			
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
        			String fullPath = presentFile.getRelativePath() + presentFile.getFilename();
        			
        			// Get FileLog by filename and EISDoc and imported = true
        			Optional<FileLog> fileLog = fileLogRepository.findByDocumentIdAndFilenameAndImportedIn(eisDoc.getId(), fullPath, true);
        			if(fileLog.isPresent()) {
        				FileLog presentLog = fileLog.get();
            			// Mark as no longer Imported in the filelog by EISDoc ID and relative path + filename from NEPAFile
            			presentLog.setImported(false);
        				fileLogRepository.save(presentLog); // Ensure it's updated
        			}
        			
        			// Log + Delete NEPAFile
    				logDelete(presentFile.getEisdoc(), "Deleted: NEPAFile", user, presentFile.getFilename());
    				nepaFileRepository.delete(presentFile);
    			}
    			
    			// Log + Delete DocumentText
				logDelete(presentText.getEisdoc(), "Deleted: DocumentText", user, presentText.getFilename());
				textRepository.delete(presentText);
				
				// If we decide to delete the file itself, we would want to clear the filename
//				eisDoc.setFilename("");
//				docRepository.save(eisDoc);
				
				// since we aren't right now, this file would be re-processed if we called sync()
				// and therefore it's very reversible.
				// If we didn't set the file log to imported=false, and we had logic to not re-import if
				// logged as imported, then it would never be re-processed.
				
    			return new ResponseEntity<String>(HttpStatus.OK);
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    		
			return new ResponseEntity<String>("Error: " + e.getStackTrace().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    		
    }

    
    // Deletes NEPAFile and related texts, sets as not imported
    @CrossOrigin
    @RequestMapping(path = "/delete_nepa_file", method = RequestMethod.POST)
    ResponseEntity<String> deleteNepaFileById(@RequestBody String id, @RequestHeader Map<String, String> headers) {
    	// TODO: Verify logic in this and in deleteAll with respect to zip files
    	// TODO: "Orphaned files" algorithm to list files we aren't using, which we could then feed to a new process to delete them all
    	// or delete manually
    	try {
    		String token = headers.get("authorization");
    		if(!isAdmin(token)) {
    			return new ResponseEntity<String>("Access denied", HttpStatus.UNAUTHORIZED);
    		}
    		else {
    			ApplicationUser user = getUser(token);
    			
    			// Get NEPAFile by ID
    			Optional<NEPAFile> nepaFile = nepaFileRepository.findById(Long.valueOf(id));
    			if(nepaFile.isEmpty()) {
        			return new ResponseEntity<String>("No file record found", HttpStatus.BAD_REQUEST);
    			}
    			
    			NEPAFile presentFile = nepaFile.get();
    			EISDoc eisDoc = presentFile.getEisdoc();
    			
    			String fullPath = presentFile.getRelativePath() + presentFile.getFilename();
    			
    			// Get FileLog by filename and EISDoc and imported = true
    			Optional<FileLog> fileLog = fileLogRepository.findByDocumentIdAndFilenameAndImportedIn(eisDoc.getId(), fullPath, true);
    			if(fileLog.isPresent()) {
    				FileLog presentLog = fileLog.get();
    				
        			// Mark as no longer Imported in the filelog by EISDoc ID and relative path + filename from NEPAFile
        			presentLog.setImported(false);
    				fileLogRepository.save(presentLog); // Ensure it's updated
    			}
    			
    			// Get DocumentText by eisdoc and filename from NEPAFile
    			Optional<DocumentText> textRecord = textRepository.findByEisdocAndFilenameIn(eisDoc, presentFile.getFilename());
    			
    			if(textRecord.isPresent()) {
    				DocumentText presentText = textRecord.get();
        			
        			// Log + Delete DocumentText
    				logDelete(presentText.getEisdoc(), "Deleted: DocumentText", user, presentText.getFilename());
    				textRepository.delete(presentText);
    			}
    			
    			// Log + Delete NEPAFile
				logDelete(presentFile.getEisdoc(), "Deleted: NEPAFile", user, presentFile.getFilename());
				nepaFileRepository.delete(presentFile);
				
    			return new ResponseEntity<String>(HttpStatus.OK);
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    		
			return new ResponseEntity<String>("Error: " + e.getStackTrace().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    		
    }

    /** Delete all NEPAFiles and DocumentTexts from an EISDoc by its ID, then delete the actual files on disk, and finally delete
     * the Folder field for the EISDoc and update it.
     * Using ORM to delete allows Lucene to automatically also delete the relevant data from its index. */
    @CrossOrigin
    @RequestMapping(path = "/deleteAllFiles", method = RequestMethod.POST)
    ResponseEntity<String> deleteAllFiles(@RequestBody String id, @RequestHeader Map<String, String> headers) {
    	List<String> deletedList = new ArrayList<String>();
    	
    	Long idToDelete = Long.valueOf(id);
    	
    	try {
    		String token = headers.get("authorization");
    		if(!isAdmin(token)) {
    			return new ResponseEntity<String>("Access denied", HttpStatus.UNAUTHORIZED);
    		}
    		
    		else {

    			ApplicationUser user = getUser(token);
    			
    			// Delete documenttexts, then disk and nepafiles and possibly eisdoc.filename, then clearing folder name from eisdoc, then log

    			// TODO: No link between nepafile-listed archives and documenttext entries for its existing files.
    			// Can add a foreign key to nepafile from documenttext.  For now, work on deleting all Folder entries for an EISDoc.
    			
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
    			
    			// Update logs
    			// Because FileLog is used to verify if we have imported something or not, we can either update the log records
    			// to show imported=0 (false), delete the logs, or we can add a new isDeleted column to exclude the logs from the verification
    			List<FileLog> fileLogList = fileLogRepository.findAllByDocumentId(foundDoc.getId());
    			for(FileLog log : fileLogList) {
    				log.setImported(false);
    				fileLogRepository.save(log);
    			}
    			
    			// Delete NEPAFiles
    			List<NEPAFile> nepaFileList = nepaFileRepository.findAllByEisdoc(foundDoc);
    			for(NEPAFile nepaFile : nepaFileList) {
    				
    				// TODO: Delete from disk here?
    				
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

    // Deletes the eisdoc itself after calling deleteAllFiles
    // TODO: Test
    @CrossOrigin
    @RequestMapping(path = "/deleteDoc", method = RequestMethod.POST)
    ResponseEntity<String> deleteDoc(@RequestBody String id, @RequestHeader Map<String, String> headers) {
    	
    	// First, ensure we delete the associated files, or they will be orphaned
    	ResponseEntity<String> deleteAllResponse = this.deleteAllFiles(id, headers);
    	if(deleteAllResponse.getStatusCodeValue() != 200) { // Couldn't delete?
    		// Return response
			return deleteAllResponse;
    	}

    	Long idToDelete = Long.valueOf(id);
    	
    	try {
    		String token = headers.get("authorization");
    		if(!isAdmin(token)) {
    			return new ResponseEntity<String>("Access denied", HttpStatus.UNAUTHORIZED);
    		}
    		
    		else {

    			ApplicationUser user = getUser(token);
    			
    			// Try to get by ID
    			Optional<EISDoc> doc = docRepository.findById(idToDelete);
    			if(doc.isEmpty()) {
    				return new ResponseEntity<String>("No such document for ID " + idToDelete, HttpStatus.NOT_FOUND);
    			}
    			EISDoc foundDoc = doc.get();
    			
    			// Delete 
    			try {
        			docRepository.delete(foundDoc);
    			} catch (IllegalArgumentException e) {
    				return new ResponseEntity<String>("Error deleting: " + e.getStackTrace().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    			}
    			
    			// Log
				logDelete(foundDoc, "Deleted: EISDoc", user, "EISDoc");
    			
    			return new ResponseEntity<String>("Deleted " + id, HttpStatus.OK);
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    		
			return new ResponseEntity<String>("Error: " + e.getStackTrace().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    }
    

	private void logDelete(EISDoc foundDoc, String message, ApplicationUser user, String filename) {
		FileLog log = new FileLog();
		
		log.setDocumentId(foundDoc.getId());
		log.setErrorType(message);
		log.setUser(user);
		log.setFilename(filename);
		log.setLogTime(LocalDateTime.now());
		
		fileLogRepository.save(log);
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
		
	/** Return whether trusted JWT is from Admin role */
	private boolean isAdmin(String token) {
		boolean result = false;
		ApplicationUser user = getUser(token);
		// get user
		if(user != null) {
			if(user.getRole().contentEquals("ADMIN")) {
				result = true;
			}
		}
		return result;
	}
}
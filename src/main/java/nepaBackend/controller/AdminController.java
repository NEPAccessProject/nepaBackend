package nepaBackend.controller;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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

@Controller
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

    /** Delete all NEPAFiles and DocumentTexts from an EISDoc by its ID, then delete the actual files on disk, and finally delete
     * the Folder field for the EISDoc and update it.
     * Using ORM to delete allows Lucene to automatically also delete the relevant data from its index. */
    @CrossOrigin
    @RequestMapping(path = "/deleteAllFiles", method = RequestMethod.GET)
    ResponseEntity<String> deleteAllFiles(@RequestParam String id, @RequestHeader Map<String, String> headers) {
    	
    	List<String> deletedList = new ArrayList<String>();
    	
    	Long idToDelete = Long.valueOf(id);
    	
    	try {
    		String token = headers.get("authorization");
    		if(!isAdmin(token)) {
    			return new ResponseEntity<String>("Access denied", HttpStatus.UNAUTHORIZED);
    		}
    		
    		else {

    			// Delete documenttexts, then disk and nepafiles and possibly eisdoc.filename, then clearing folder name from eisdoc, then log

    			// TODO: No link between nepafile-listed archives and documenttext entries for its existing files.
    			// Can add a foreign key to nepafile from documenttext.  For now, work on deleting all Folder entries for an EISDoc.
    			
    			Optional<EISDoc> doc = docRepository.findById(idToDelete);
    			
    			if(doc.isEmpty()) {
    				return new ResponseEntity<String>("No such document for ID " + idToDelete, HttpStatus.NOT_FOUND);
    			}
    			
    			EISDoc foundDoc = doc.get();
    			
    			List<DocumentText> textList = textRepository.findAllByEisdoc(foundDoc);
    			
    			ApplicationUser user = getUser(token);
    			
    			for(DocumentText text : textList) {
    				deletedList.add("DocumentText " + text.getFilename());
    				textRepository.deleteById(text.getId());
    				
    				// Log 
    				logDelete(foundDoc, "Deleted: DocumentText", user, text.getFilename());
    			}
    			
    			List<NEPAFile> nepaFileList = nepaFileRepository.findAllByEisdoc(foundDoc);
    			
    			for(NEPAFile nepaFile : nepaFileList) {
    				
    				// TODO: Delete from disk here
    				
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
    

	private void logDelete(EISDoc foundDoc, String message, ApplicationUser user, String filename) {
		FileLog log = new FileLog();
		
		log.setDocumentId(foundDoc.getId());
		log.setErrorType(message);
		log.setUser(user);
		log.setFilename(filename);
		log.setLogTime(LocalDateTime.now());
		
		fileLogRepository.save(log);
	}
	

	/** Return ApplicationUser given JWT String */
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
		
	/** Return whether JWT is from Admin role */
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
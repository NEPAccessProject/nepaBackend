package nepaBackend.controller;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.DocRepository;
import nepaBackend.EmailLogRepository;
import nepaBackend.FileLogRepository;
import nepaBackend.Globals;
import nepaBackend.NEPAFileRepository;
import nepaBackend.ProcessRepository;
import nepaBackend.TextRepository;
import nepaBackend.UpdateLogRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.model.EmailLog;
import nepaBackend.model.FileLog;
import nepaBackend.model.NEPAFile;
import nepaBackend.model.NEPAProcess;
import nepaBackend.model.SearchLog;
import nepaBackend.model.UpdateLog;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/process")
public class NEPAProcessController {

	@Autowired
    private DocRepository docRepository;
	@Autowired
	private ProcessRepository processRepository;
	@Autowired
	private ApplicationUserRepository applicationUserRepository;

    public NEPAProcessController() {
    }

    
	// TODO: Complete and make this a route
    // - should note that this does a lot of unnecessary work and could be optimized.
	// So the issue with this is generating the process IDs trying to predict and avoid overlap, and then giving them back to curators
	public void linkFolders() {
		// logic: if has folder
		List<EISDoc> missingList = docRepository.findAllWithFolderMissingProcess();
		
		for(EISDoc missing: missingList) {
			// get all with that folder hasFolderList
			List<EISDoc> hasFolderList = docRepository.findAllByFolder(missing.getFolder());
			
			// if any have a process ID already, set all in hasFolderList to that and move on
			boolean linkedFolders = false;
			for(int i = 0; i < hasFolderList.size(); i++) {
				if(hasFolderList.get(i).getProcessId() != null) {
					this.addProcessToDocs(hasFolderList, hasFolderList.get(i).getProcessId());
					linkedFolders = true;
					i = hasFolderList.size();
				}
			}
			
			// no process ID for any of them
			if(!linkedFolders) {
				// generate our own process ID and create an official process connecting them, initialized at 2000000
				long goodProcessId = 2000000;
				
				Optional<EISDoc> maybeExists = docRepository.findByProcessId(goodProcessId);
				
				if(maybeExists.isPresent()) {
					// okay, we've previously initialized 2000000 (existing processes were initialized at 1000000, so we won't overlap with those
					// unless someone curates a million of them),
					// so just go with max + 1
					goodProcessId = (this.findMaxProcessId() + 1);
				}
				
				this.addNewProcessWithDocs(hasFolderList, goodProcessId);
				this.addProcessToDocs(hasFolderList, goodProcessId);
			}
		}
	}

	private void addNewProcessWithDocs(List<EISDoc> listDocs, Long processId) {
		NEPAProcess newProcess = new NEPAProcess(processId);
		
		for(EISDoc doc : listDocs) {
			newProcess = this.setCorrectType(doc, newProcess);
		}
		
		processRepository.save(newProcess);
	}
	
	// TODO
	private NEPAProcess setCorrectType(EISDoc doc, NEPAProcess newProcess) {
		if(doc.getDocumentType().contentEquals("draft")) {
			newProcess.setDocDraft(doc);
		}

		// TODO: etc
		
		return newProcess;
	}



	private void addProcessToDocs(List<EISDoc> listDocs, Long processId) {
		for(EISDoc doc : listDocs) {
			doc.setProcessId(processId);
			docRepository.save(doc);
		}
	}

	private long findMaxProcessId() {
		return docRepository.findMaxProcessId();
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
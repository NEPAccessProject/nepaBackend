package nepaBackend.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tika.Tika;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.DocRepository;
import nepaBackend.FileLogRepository;
import nepaBackend.TextRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.model.FileLog;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/file")
public class FileController {

    private DocRepository docRepository;
    private TextRepository textRepository;
    private FileLogRepository fileLogRepository;
    private ApplicationUserRepository applicationUserRepository;

    public FileController(DocRepository docRepository,
    		TextRepository textRepository,
    		FileLogRepository fileLogRepository,
    		ApplicationUserRepository applicationUserRepository) {
        this.docRepository = docRepository;
        this.textRepository = textRepository;
        this.fileLogRepository = fileLogRepository;
        this.applicationUserRepository = applicationUserRepository;
    }
	
	// TODO: Set this as a global constant somewhere?  May be changed to SBS and then elsewhere in future
	// Can also have a backup set up for use if primary fails
    Boolean testing = true;
	String dbURL = "http://mis-jvinaldbl1.catnet.arizona.edu:80/test/";
    String testURL = "http://localhost:5000/";

	@CrossOrigin
	@RequestMapping(path = "/downloadFile", method = RequestMethod.GET)
	public ResponseEntity<Void> downloadFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam String filename) {
		try {
			// TODO: if not .zip try adding .pdf first?  Client will need file type and we need to capture all the files to deliver
			// potentially in a zip
			// TODO: Eventually going to need a lot of logic for exploring folder structures
			// and capturing multiple files
	        URL fileURL = new URL(dbURL + filename);
	        InputStream in = new BufferedInputStream(fileURL.openStream());
	        long length = getFileSize(fileURL); // for Content-Length for progress bar

	        response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\""); 
	        response.addHeader("Content-Length", length + ""); 

	        ServletOutputStream out = response.getOutputStream();
	        IOUtils.copy(in, out);
	        
	        response.flushBuffer();
	        return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
	    } catch (Exception e) {
	    	// TODO: Log missing file errors somewhere
//	    	StringWriter sw = new StringWriter();
//	    	PrintWriter pw = new PrintWriter(sw);
//	    	e.printStackTrace(pw);
//	    	e.printStackTrace();
//	    	String sStackTrace = sw.toString();
	        return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
	    }
	}

	/** Run convertRecord for all IDs in db.  (Conversion handles empty filenames and deduplication) */
	@CrossOrigin
	@RequestMapping(path = "/bulk", method = RequestMethod.GET)
	public ResponseEntity<ArrayList<String>> bulk(@RequestHeader Map<String, String> headers) {
		
    	String token = headers.get("authorization");
    	if(!isAdmin(token)) {
    		return new ResponseEntity<ArrayList<String>>(HttpStatus.UNAUTHORIZED);
    	}
    	
		ArrayList<String> resultList = new ArrayList<String>();
		List<EISDoc> convertList = docRepository.findByFilenameNotNull();
		for(EISDoc doc : convertList) {
			resultList.add(doc.getId().toString() + ": " + this.convertRecord(doc.getId().toString()).getStatusCodeValue());
		}
		return new ResponseEntity<ArrayList<String>>(resultList, HttpStatus.I_AM_A_TEAPOT);
	}
	
	// TODO: Generalize for entries with folder or multiple files instead of simple filename
	@RequestMapping(path = "/convert", method = RequestMethod.GET)
	private ResponseEntity<Void> convertRecord(@RequestParam String recordId) {
    	
		long documentId;
    	FileLog fileLog = new FileLog();
		
		try {
			documentId = Long.parseLong(recordId);
			fileLog.setDocumentId(documentId);
		} catch(Exception e) {
	        return new ResponseEntity<Void>(HttpStatus.I_AM_A_TEAPOT);
		}
		if(documentId < 1) {
	        return new ResponseEntity<Void>(HttpStatus.UNPROCESSABLE_ENTITY);
		}

		// Note: There can be multiple files per archive, resulting in multiple documenttext records for the same ID
		// but presumably different filenames with hopefully different conents
		final int BUFFER = 2048;
		
		try {
	    	Tika tikaParser = new Tika();
	    	tikaParser.setMaxStringLength(-1); // disable limit
	    
			EISDoc eis = docRepository.getById(documentId);
	    	// Check to make sure this record exists.
			if(eis == null) {
		        return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
			}
			// TODO: Make sure there is a file (for current data, no filename means nothing to convert for this record)
			// TODO: Handle folders/multiple files for future (currently only archives)
			if(eis.getFilename() == null || eis.getFilename().length() == 0) {
		        return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
			}
			
			String relevantURL = dbURL;
			if(testing) {
		        relevantURL = testURL;
			}
	        URL fileURL = new URL(relevantURL + eis.getFilename());
	        
	        fileLog.setFilename(eis.getFilename());
	        
	        if(testing) {
		        System.out.println("FileURL " + fileURL.toString());
	        }
	    	
	        // 1: Download the archive
	        // TODO: Handle non-archive case (easier, but currently we only have archives)
	        InputStream in = new BufferedInputStream(fileURL.openStream());
	        ZipInputStream zis = new ZipInputStream(in);
	        ZipEntry ze;
	        
	        while((ze = zis.getNextEntry()) != null) {
	        	int count;
	        	byte[] data = new byte[BUFFER];
	        	
		        // TODO: Can check filename for .pdf extension
		        String filename = ze.getName();
		        
		        
	    		// TODO: Check to make sure we don't already have this document in the system.
	        	// Deduplication: Ignore when filename and document ID combination already exists in EISDoc table.
	        	
		        // Handle directory - can ignore them if we're just converting PDFs
	        	if(!ze.isDirectory()) {
	        		if(testing) {
		        		System.out.println(textRepository.existsByDocumentIdAndFilename(documentId, filename));
	        		}
	        		
	        		if(textRepository.existsByDocumentIdAndFilename(documentId, filename)) {
		    	        zis.closeEntry();
		        	} else {
		        		
		        		if(testing) {
				        	System.out.println("Extracting " + ze);
		        		}
		        		
			    		DocumentText docText = new DocumentText();
				        docText.setDocumentId(documentId);
				        docText.setFilename(filename);

				        fileLog.setExtractedFilename(filename);
				        
		        		// 2: Extract data and stream to Tika
		        		try {
		        			ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        			while (( count = zis.read(data)) != -1) {
		        				baos.write(data, 0, count);
		        			}
		        			
					        // 3: Convert to text
		        			String textResult = tikaParser.parseToString(new ByteArrayInputStream(baos.toByteArray()));
			        		docText.setPlaintext(textResult);
			        		
			    	        // 4: Add converted text to database for document(EISDoc) ID, filename
			    	        this.save(docText);
		        		} catch(Exception e){
		        			try {
			        			
			        			if(docText.getPlaintext() == null || docText.getPlaintext().length() == 0) {
			        				fileLog.setImported(false);
			        			} else {
			        				fileLog.setImported(true);
			        			}
			        			
			        			// Note:  This step does not index; that's a separate process.  N/A, so null would be fine.
			        			// But Tinyint is numeric, should default to 0.  Still better than the Boolean type in MySQL
			        			
			        			fileLog.setErrorType(e.getLocalizedMessage());
			        			fileLog.setLogTime(LocalDateTime.now());
			        			fileLogRepository.save(fileLog);
		        			} catch (Exception e2) {
		        				System.out.println("Error logging error...");
		        				e2.printStackTrace();
		        			}
		        			
		        		} finally { // while loop handles getNextEntry()
		    	        	zis.closeEntry();
		        		}
		        	}
	        	}

	        }
	        
	        // 5: Cleanup
	        in.close();
	        zis.close();

	        return new ResponseEntity<Void>(HttpStatus.OK);
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	// could be IO exception getting the file if it doesn't exist
	        return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	public long getFileSize(URL url) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("HEAD");
			return conn.getContentLengthLong();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
	
	private boolean save(@RequestBody DocumentText docText) {
		try {
			textRepository.save(docText);
			return true;
		} catch(Exception e) {
			return false;
		}
		
	}
	
	// Return true if admin role
	@PostMapping(path = "/checkAdmin")
	public ResponseEntity<Boolean> checkAdmin(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		boolean result = isAdmin(token);
		HttpStatus returnStatus = HttpStatus.UNAUTHORIZED;
		if(result) {
			returnStatus = HttpStatus.OK;
		}
		return new ResponseEntity<Boolean>(result, returnStatus);
	}
	
	// Helper function for checkAdmin and on-demand token admin check
	private boolean isAdmin(String token) {
		boolean result = false;
		// get ID
		if(token != null) {
	        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
	                .getId();

			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
			if(user.getRole().contentEquals("ADMIN")) {
				result = true;
			}
		}
		return result;

	}

}

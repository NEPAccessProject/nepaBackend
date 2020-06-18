package nepaBackend.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.DateValidator;
import nepaBackend.DateValidatorUsingLocalDate;
import nepaBackend.DocRepository;
import nepaBackend.FileLogRepository;
import nepaBackend.TextRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.model.FileLog;
import nepaBackend.pojo.UploadInputs;
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
	String expressURL = "http://localhost:3001/test2";

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
//			StringWriter sw = new StringWriter();
//			PrintWriter pw = new PrintWriter(sw);
//			e.printStackTrace(pw);
//			e.printStackTrace();
//			String sStackTrace = sw.toString();
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}
	}
	
	

	/** Return all file logs */
	@CrossOrigin
	@RequestMapping(path = "/logs_all", method = RequestMethod.GET)
	public ResponseEntity<List<FileLog>> getAllLogs(@RequestHeader Map<String, String> headers) {
		
		String token = headers.get("authorization");
		if(!isAdmin(token)) 
		{
			return new ResponseEntity<List<FileLog>>(HttpStatus.UNAUTHORIZED);
		} 
		else 
		{
			List<FileLog> fileLogList = fileLogRepository.findAll();
			return new ResponseEntity<List<FileLog>>(fileLogList, HttpStatus.OK);
		}
		
	}
	
	

	/** Run convertRecord for all IDs in db.  (Conversion handles null filenames 
	 * (of which there are none because they're empty strings by default) and deduplication) */
	@CrossOrigin
	@RequestMapping(path = "/bulk", method = RequestMethod.GET)
	public ResponseEntity<ArrayList<String>> bulk(@RequestHeader Map<String, String> headers) {
		
		String token = headers.get("authorization");
		if(!isAdmin(token)) 
		{
			return new ResponseEntity<ArrayList<String>>(HttpStatus.UNAUTHORIZED);
		} 
		else 
		{
			ArrayList<String> resultList = new ArrayList<String>();
			List<EISDoc> convertList = docRepository.findByFilenameNotEmpty();
			
			for(EISDoc doc : convertList) 
			{
				// ignore if no file to convert
				if(doc.getFilename().length() == 0) 
				{
					// skip
				} 
				else 
				{
					if(testing) 
					{
						resultList.add(doc.getId().toString() + ": " + this.convertRecord(doc)
						.getStatusCodeValue());
					} 
					else 
					{
						this.convertRecord(doc);
					}
				}
			}
			
			return new ResponseEntity<ArrayList<String>>(resultList, HttpStatus.OK);
		}
		
	}
	
	/** Run convertRecordSmart for all IDs in db.  */
	@CrossOrigin
	@RequestMapping(path = "/bulk2", method = RequestMethod.GET)
	public ResponseEntity<ArrayList<String>> bulkSmart(@RequestHeader Map<String, String> headers) {
		
		String token = headers.get("authorization");
		if(!isAdmin(token)) 
		{
			return new ResponseEntity<ArrayList<String>>(HttpStatus.UNAUTHORIZED);
		} 
		else 
		{
			ArrayList<String> resultList = new ArrayList<String>();
			List<EISDoc> convertList = docRepository.findByFilenameNotEmpty();
			
			for(EISDoc doc : convertList) 
			{
				// ignore if no file to convert, although should be handled by query already
				if(doc.getFilename().length() == 0) 
				{
					// skip
				} 
				else 
				{
					if(testing) 
					{
						resultList.add(doc.getId().toString() + ": " + this.convertRecordSmart(doc)
						.getStatusCodeValue());
					} 
					else 
					{
						this.convertRecordSmart(doc);
					}
				}
			}
			
			return new ResponseEntity<ArrayList<String>>(resultList, HttpStatus.OK);
		}
		
	}

	// TODO:
	// put in appropriate folder
	// add all to db, incl. filename (and folder??? or just leave that logic to download/conversion?),
	// - 
	// I think just filename is good.  Should add a standardized, standalone method that returns a list of possible
	// file locations which can be used by both downloads and Tika conversion.  Although to do conversion, it would be
	// very nice to simply receive the full path in response from the Express.js server, and hand that directly to Tika
	// -
	// Another issue: Adding files to existing records with existing file(s).  No multi-file handling yet.  
	// Lazy solution: Could separate by illegal filename characters.
	// Could redesign DB with a new table just with foreign keys and filenames.
	// This would also mean re-figuring out downloads.  Probably have to archive all the files together.
	// However, could get up to multiple gigabytes.  Maybe better to pop out list of files for download?
	// -
	// link together, 
	// convert to text (whether PDF or archive...), 
	// ensure indexing worked automatically as it should
	/** TODO: Multi-record/file upload:  Should require CSV.  Either start to handle loose files, or require uploads are named in CSV.
	 * Loose files: Add empty records for them just with filename.  Curators could fill in details.
	 * Should add a UI to see all records missing basic things like title. 
	 */
	// TODO: test remote deployed with different server URL obviously, finalize
	/**
	 * not sure I have permission to put new items onto that disk in that location
	 * Will need to make sure it is secured also (test in browser?) */
	/** Upload single file and/or metadata which is then imported to database, then converted to text and added to db (if applicable)
	 * and then indexed by Lucene.  Re-POSTS the incoming file to the DBFS via an Express.js server on there so that the
	 * original file can be downloaded (also needs to be in place for the Tika processing)
	 * 
	 * @param file
	 * @throws FileUploadException
	 * @throws IOException
	 */
	@CrossOrigin
	@RequestMapping(path = "/uploadFile", method = RequestMethod.POST, consumes = "multipart/form-data")
	private boolean[] uploadFile(@RequestPart(name="file", required=false) MultipartFile file, 
								@RequestPart(name="doc") String doc) throws IOException { 
	    System.out.println("Size " + file.getSize());
	    System.out.println("Name " + file.getOriginalFilename());
	    System.out.println(doc);
	    
	    String origFilename = file.getOriginalFilename();

	    boolean[] results = new boolean[3];
	    results[0] = false;
	    results[1] = false;
	    results[2] = false;
	    
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
		    UploadInputs dto = mapper.readValue(doc, UploadInputs.class);
		    // Ensure metadata is valid before uploading, given upload should always work.
			if(!isValid(dto)) {
				return results;
			}
	    	
			
	    	HttpEntity entity = MultipartEntityBuilder.create()
	    				.addBinaryBody("test", 
	    						file.getInputStream(), 
	    						ContentType.create("application/octet-stream"), 
	    						file.getOriginalFilename())
	    				.build();
		    HttpPost request = new HttpPost(expressURL);
		    request.setEntity(entity);

		    HttpClient client = HttpClientBuilder.create().build();
		    HttpResponse response = client.execute(request);
		    System.out.println(response.toString());
		    
		    // so far so good?
		    results[0] = (response.getStatusLine().getStatusCode() == 200);

		    // Only save if file upload succeeded.  We don't want loose files, and we don't want metadata without files.
		    if(results[0]) {
			    // Since metadata is already validated, this should always work.
		    	EISDoc saveDoc = new EISDoc();
		    	saveDoc.setAgency(dto.agency);
		    	saveDoc.setDocumentType(dto.type);
			    // TODO: Get file paths from Express.js server and link up metadata beyond just filename
		    	// TODO: Handle different file formats for download, Tika
		    	// TODO: Handle multiple files per record here and then need to also redesign downloads to allow it
		    	saveDoc.setFilename(file.getOriginalFilename());
		    	if(dto.publishDate.length()>9) {
			    	saveDoc.setRegisterDate(dto.publishDate.substring(0, 10));
		    	} else {
			    	saveDoc.setRegisterDate("");
		    	}
		    	saveDoc.setState(dto.state);
		    	saveDoc.setTitle(dto.title.trim());
		    	
		    	saveDoc.setCommentDate(""); // Useless field now?
		    	// TODO: May need to retool this if adding comments files becomes a thing
		    	saveDoc.setCommentsFilename("");
		    	
		    	// Save (ID is null at this point, but .save() picks a unique ID thanks to the model so it's good)
		    	EISDoc savedDoc = docRepository.save(saveDoc); // note: JPA .save() is safe from sql injection
		    	results[1] = true;

		    	// Run Tika on file, record if 200 or not
		    	if(origFilename.substring(origFilename.length()-3).equalsIgnoreCase("pdf")) {
		    		int status = this.convertPDF(savedDoc).getStatusCodeValue();
			    	results[2] = (status == 200);
		    	} else { // Archive or image case (not set up to attempt image conversion, may have issues with non-.zip archives)
			    	int status = this.convertRecordSmart(savedDoc).getStatusCodeValue();
			    	results[2] = (status == 200);
			    	// Note: 200 doesn't necessarily mean tika was able to convert anything
		    	}

		    	// TODO: Test.  Verify Tika converted; verify Lucene indexed (should happen automatically)
		    	// Then make an Express server on DBFS, change URL, test live
		    }
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		    file.getInputStream().close();
		}
	    
		return results;
	}


	private boolean isValid(UploadInputs dto) {
		boolean valid = true;
		
		if(dto.publishDate.length() > 9) { // Have date?
	//		DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
	//		DateValidator validator = new DateValidatorUsingLocalDate(dateFormatter);
	//		if(validator.isValid(dto.publishDate)) { // Validate date
	//			// All good, keep valid = true
	//		} else {
	//			valid = false; // Date has to be valid ISO_DATE_TIME
	//		}
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //enforce pattern
			DateValidator validator = new DateValidatorUsingLocalDate(dateFormatter);
			if(validator.isValidFormat(dto.publishDate.substring(0, 10))) { // Validate date, format
				// All good, keep valid = true
			} else {
				System.out.println("Invalid date");
				valid = false; // Date invalid
			}
		}
		
		if(dto.title.trim().length()==0) {
			valid = false; // Need title
		}
		
		if(dto.filename.length()>0) {
			// TODO: Ensure filename matches unique item on imported files list (for CSV and bulk import only if at all, 
			// otherwise should be empty string anyway for single import)
		}
		
		return valid;
	}



//	@CrossOrigin
//	@RequestMapping(path = "/uploadMeta", method = RequestMethod.POST, consumes = "multipart/form-data")
//	private ResponseEntity<Void> uploadFile(@RequestParam("meta") EISDoc doc) throws IOException { 
//	    
//	    try {
//	    	docRepository.save(doc);
//			return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
//		} catch (Exception e) {
//			e.printStackTrace();
//			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
//		} finally {
//		    System.out.println("Out");
//		}
//	}

	// TODO
	@CrossOrigin
	@RequestMapping(path = "/uploadMetaCsv", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<Void> uploadFile(@RequestParam("csv") String csv) throws IOException { 
	    
	    try {
			return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
		    System.out.println("Out");
		}
	}
	
	
	// Experimental, probably useless (was trying to get document outlines)
	@CrossOrigin
	@RequestMapping(path = "/xhtml", method = RequestMethod.GET)
	public ResponseEntity<List<String>> xhtml(@RequestHeader Map<String, String> headers) {
		
		String token = headers.get("authorization");
		if(!isAdmin(token)) 
		{
			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
		} 
		else 
		{
			return new ResponseEntity<List<String>>(convertXHTML(docRepository.findById(22)), HttpStatus.OK);
		}
		
	}
	

	private ResponseEntity<Void> convertPDF(EISDoc eis) {// Check to make sure this record exists.
		if(testing) {
			System.out.println("Converting PDF");
		}
		if(eis == null) {
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}
		
		FileLog fileLog = new FileLog();
		
		try {
			fileLog.setDocumentId(eis.getId());
		} catch(Exception e) {
			return new ResponseEntity<Void>(HttpStatus.I_AM_A_TEAPOT);
		}
		if(eis.getId() < 1) {
			return new ResponseEntity<Void>(HttpStatus.UNPROCESSABLE_ENTITY);
		}
		
		// Deduplication: Ignore when document ID already exists in EISDoc table.
		// Will need different logic to handle multiple files per record
		if(textRepository.existsByEisdoc(eis)) 
		{
			return new ResponseEntity<Void>(HttpStatus.FOUND);
		} 
		
		// Note: There can be multiple files per archive, resulting in multiple documenttext records for the same ID
		// but presumably different filenames with hopefully different conents
		final int BUFFER = 2048;
		
		try {
			Tika tikaParser = new Tika();
			tikaParser.setMaxStringLength(-1); // disable limit
		
			// TODO: Make sure there is a file (for current data, no filename means nothing to convert for this record)
			// TODO: Handle folders/multiple files for future (currently only archives)
			if(eis.getFilename() == null || eis.getFilename().length() == 0) {
				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
			}
			String filename = eis.getFilename();
			
			String relevantURL = dbURL;
			if(testing) {
				relevantURL = testURL;
			}
			URL fileURL = new URL(relevantURL + filename);
		
			fileLog.setFilename(filename);
			
			// 1: Download the archive
			// Handle non-archive case
			InputStream in = new BufferedInputStream(fileURL.openStream());
			
			int count;
			byte[] data = new byte[BUFFER];
			
			
			if(textRepository.existsByEisdoc(eis)) 
			{
				in.close();
				return new ResponseEntity<Void>(HttpStatus.ALREADY_REPORTED);
			} 
			else 
			{
				DocumentText docText = new DocumentText();
				docText.setEisdoc(eis);
				docText.setFilename(filename);
				
				fileLog.setExtractedFilename(filename);
				
				// 2: Extract data and stream to Tika
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					while (( count = in.read(data)) != -1) {
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
						e.printStackTrace();
					} catch (Exception e2) {
						System.out.println("Error logging error...");
						e2.printStackTrace();
					}
					
				} 
			}

			// 5: Cleanup
			in.close();

			return new ResponseEntity<Void>(HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				fileLog.setImported(false);
				fileLog.setErrorType(e.getLocalizedMessage());
				fileLog.setLogTime(LocalDateTime.now());
				fileLogRepository.save(fileLog);
			} catch (Exception e2) {
				System.out.println("Error logging error...");
				e2.printStackTrace();
				return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			// could be IO exception getting the file if it doesn't exist
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	// TODO: Generalize for entries with folder or multiple files instead of simple filename
	private ResponseEntity<Void> convertRecord(EISDoc eis) {

		// Check to make sure this record exists.
		if(eis == null) {
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}
		
		FileLog fileLog = new FileLog();
		
		try {
			fileLog.setDocumentId(eis.getId());
		} catch(Exception e) {
			return new ResponseEntity<Void>(HttpStatus.I_AM_A_TEAPOT);
		}
		if(eis.getId() < 1) {
			return new ResponseEntity<Void>(HttpStatus.UNPROCESSABLE_ENTITY);
		}
		
		// Note: There can be multiple files per archive, resulting in multiple documenttext records for the same ID
		// but presumably different filenames with hopefully different conents
		final int BUFFER = 2048;
		
		try {
			Tika tikaParser = new Tika();
			tikaParser.setMaxStringLength(-1); // disable limit
		
			// TODO: Make sure there is a file (for current data, no filename means nothing to convert for this record)
			// TODO: Handle folders/multiple files for future (currently only archives)
			if(eis.getFilename() == null || eis.getFilename().length() == 0) {
				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
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
						System.out.println(textRepository.existsByEisdocAndFilename(eis, filename));
					}
					
					if(textRepository.existsByEisdocAndFilename(eis, filename)) {
						zis.closeEntry();
					} else {
						
						if(testing) {
							System.out.println("Extracting " + ze);
						}
						
						DocumentText docText = new DocumentText();
						docText.setEisdoc(eis);
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
								if(testing) {
									System.out.println("Error logging error...");
									e2.printStackTrace();
								}
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
//			e.printStackTrace();
			try {
				fileLog.setImported(false);
				
				fileLog.setErrorType(e.getLocalizedMessage());
				fileLog.setLogTime(LocalDateTime.now());
				fileLogRepository.save(fileLog);
			} catch (Exception e2) {
				System.out.println("Error logging error...");
				e2.printStackTrace();
				return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			// could be IO exception getting the file if it doesn't exist
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	// Skips if document ID present (aka assumes no partial archive imports to complete) for running when new archives are added
	private ResponseEntity<Void> convertRecordSmart(EISDoc eis) {

		// Check to make sure this record exists.
		if(eis == null) {
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}
		
		FileLog fileLog = new FileLog();
		
		try {
			fileLog.setDocumentId(eis.getId());
		} catch(Exception e) {
			return new ResponseEntity<Void>(HttpStatus.I_AM_A_TEAPOT);
		}
		if(eis.getId() < 1) {
			return new ResponseEntity<Void>(HttpStatus.UNPROCESSABLE_ENTITY);
		}
		if(textRepository.existsByEisdoc(eis)) 
		{
			return new ResponseEntity<Void>(HttpStatus.FOUND);
		} 
		
		// Note: There can be multiple files per archive, resulting in multiple documenttext records for the same ID
		// but presumably different filenames with hopefully different conents
		final int BUFFER = 2048;
		
		try {
			Tika tikaParser = new Tika();
			tikaParser.setMaxStringLength(-1); // disable limit
		
			// TODO: Make sure there is a file (for current data, no filename means nothing to convert for this record)
			// TODO: Handle folders/multiple files for future (currently only archives)
			if(eis.getFilename() == null || eis.getFilename().length() == 0) {
				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
			}
			
			String relevantURL = dbURL;
			if(testing) {
				relevantURL = testURL;
			}
			URL fileURL = new URL(relevantURL + eis.getFilename());
		
			fileLog.setFilename(eis.getFilename());

			
			// 1: Download the archive
			// TODO: Handle non-archive case (easier, but currently we only have archives)
			InputStream in = new BufferedInputStream(fileURL.openStream());
			ZipInputStream zis = new ZipInputStream(in);
			ZipEntry ze;
			
			// Note: If every entry null, equivalent to failing silently
			while((ze = zis.getNextEntry()) != null) {
				if(testing) {
					System.out.println("Processing non null entry " + ze.getName());
				}
				int count;
				byte[] data = new byte[BUFFER];
				
				// TODO: Can check filename for .pdf extension
				String filename = ze.getName();

				
				// Deduplication: Ignore when document ID already exists in EISDoc table.
				
				// Handle directory - can ignore them if we're just converting PDFs
				if(!ze.isDirectory()) {

					if(textRepository.existsByEisdoc(eis)) 
					{
						zis.closeEntry();
					} 
					else 
					{
						DocumentText docText = new DocumentText();
						docText.setEisdoc(eis);
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
							e.printStackTrace();
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
							
						} 
						finally 
						{ // while loop handles getNextEntry()
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
			try {
				fileLog.setImported(false);
				fileLog.setErrorType(e.getLocalizedMessage());
				fileLog.setLogTime(LocalDateTime.now());
				fileLogRepository.save(fileLog);
			} catch (Exception e2) {
				System.out.println("Error logging error...");
				e2.printStackTrace();
				return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			// could be IO exception getting the file if it doesn't exist
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	// Probably useless
	private List<String> convertXHTML(EISDoc eis) {

		// Note: There can be multiple files per archive, resulting in multiple documenttext records for the same ID
		// but presumably different filenames with hopefully different contents
		final int BUFFER = 2048;
		
		List<String> results = new ArrayList<String>();
		
		try {
			Tika tikaParser = new Tika();
			tikaParser.setMaxStringLength(-1); // disable limit
			
			String relevantURL = dbURL;
			if(testing) {
				relevantURL = testURL;
			}
			URL fileURL = new URL(relevantURL + eis.getFilename());
			
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
						System.out.println(textRepository.existsByEisdocAndFilename(eis, filename));
					}
					
					if(textRepository.existsByEisdocAndFilename(eis, filename) && !testing) {
						zis.closeEntry();
					} else {
						
						if(testing) {
							System.out.println("Extracting " + ze);
						}
						
						// 2: Extract data and stream to Tika
						try {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							while (( count = zis.read(data)) != -1) {
								baos.write(data, 0, count);
							}
							
							// 3: Convert to text
							System.out.println(pdfParseToXML(new ByteArrayInputStream(baos.toByteArray())));
							results.add(pdfParseToXML(new ByteArrayInputStream(baos.toByteArray())));
							
						} catch(Exception e){
							e.printStackTrace();
						} finally { // while loop handles getNextEntry()
							zis.closeEntry();
						}
					}
				}
			
			}

			// 5: Cleanup
			in.close();
			zis.close();

			return results;
		} catch (Exception e) {
			e.printStackTrace();
			return results;
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
		if(docText.getPlaintext().trim().length()>0) {
			try {
				textRepository.save(docText);
				return true;
			} catch(Exception e) {
				if(testing) {
					System.out.println("Error saving");
					e.printStackTrace();
				}
				return false;
			}
		} else {
			if(testing) {
				System.out.println("No text converted");
			}
			return false;
		}
		
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

	// Probably useless
	private String pdfParseToXML(ByteArrayInputStream inputstream) {
		ContentHandler handler = new ToXMLContentHandler();
		Metadata metadata = new Metadata();
		ParseContext pcontext = new ParseContext();

		//parsing the document using PDF parser
		PDFParser pdfparser = new PDFParser(); 
		try {
			pdfparser.parse(inputstream, handler, metadata,pcontext);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TikaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//getting the content of the document
//		System.out.println("Contents of the PDF :" + handler.toString());
		return handler.toString();
		
		//getting metadata of the document
//		System.out.println("Metadata of the PDF:");
//		String[] metadataNames = metadata.names();
//		
//		for(String name : metadataNames) {
//		 System.out.println(name+ " : " + metadata.get(name));
//		}
	}
	
}

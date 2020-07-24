package nepaBackend.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
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
//import org.apache.tika.exception.TikaException;
//import org.apache.tika.metadata.Metadata;
//import org.apache.tika.parser.ParseContext;
//import org.apache.tika.parser.pdf.PDFParser;
//import org.apache.tika.sax.ToXMLContentHandler;
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
//import org.xml.sax.ContentHandler;
//import org.xml.sax.SAXException;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import nepaBackend.pojo.UploadInputs;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/file")
public class FileController {
	
	private DocRepository docRepository;
	private TextRepository textRepository;
	private FileLogRepository fileLogRepository;
	private ApplicationUserRepository applicationUserRepository;
	private NEPAFileRepository nepaFileRepository;
	
	private static DateTimeFormatter[] parseFormatters = Stream.of("yyyy-MM-dd", "MM-dd-yyyy", 
			"yyyy/MM/dd", "MM/dd/yyyy", 
			"M/dd/yyyy", "yyyy/M/dd", "M-dd-yyyy", "yyyy-M-dd",
			"MM/d/yyyy", "yyyy/MM/d", "MM-d-yyyy", "yyyy-MM-d",
			"M/d/yyyy", "yyyy/M/d", "M-d-yyyy", "yyyy-M-d",
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
			.map(DateTimeFormatter::ofPattern)
			.toArray(DateTimeFormatter[]::new);

	public FileController(DocRepository docRepository,
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
	
	// TODO: Set this as a global constant somewhere?  May be changed to SBS and then elsewhere in future
	// Can also have a backup set up for use if primary fails
	Boolean testing = true;
	static String dbURL = "http://mis-jvinaldbl1.catnet.arizona.edu:80/test/";
	String testURL = "http://localhost:5000/";
	String uploadTestURL = "http://localhost:5309/uploadFilesTest";
	static String uploadURL = "http://mis-jvinaldbl1.catnet.arizona.edu:5309/upload";

	@CrossOrigin
	@RequestMapping(path = "/downloadFile", method = RequestMethod.GET)
	public ResponseEntity<Void> downloadFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam String filename) {
		try {
			// TODO: Rewrite to support file paths provided by NEPAFile table
			// TODO: if not .zip try adding .pdf first?  Client will need file type and we need to capture all the files to deliver
			// potentially in a zip
			// TODO: Eventually going to need a lot of logic for exploring folder structures
			// and capturing multiple files
			URL fileURL = new URL(dbURL + filename);
			if(testing) {
				fileURL = new URL(testURL + filename);
			}
			InputStream in = new BufferedInputStream(fileURL.openStream());
			long length = getFileSize(fileURL); // for Content-Length for progress bar
			
			response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\""); 
			response.addHeader("Content-Length", length + ""); 
			
			ServletOutputStream out = response.getOutputStream();
			IOUtils.copy(in, out);
			
			response.flushBuffer();
			return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
		} catch (Exception e) {
		// TODO: Log missing file errors in db file log?
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
	private ResponseEntity<boolean[]> importDocument(@RequestPart(name="file") MultipartFile file, 
								@RequestPart(name="doc") String doc, @RequestHeader Map<String, String> headers) 
										throws IOException { 
//		if(testing) {
//			System.out.println(doc);
//			return new ResponseEntity<boolean[]>(HttpStatus.OK);
//		}

		// TODO: Should we save a NepaFile record for single file imports?

	    HttpStatus returnStatus = HttpStatus.OK;
		
	    boolean[] results = new boolean[3];
		String token = headers.get("authorization");
		if(!isCurator(token) && !isAdmin(token)) 
		{
			return new ResponseEntity<boolean[]>(results, HttpStatus.UNAUTHORIZED);
		} 
		
//	    System.out.println("Size " + file.getSize());
//	    System.out.println("Name " + file.getOriginalFilename());
//	    System.out.println(doc);
	    
	    String origFilename = file.getOriginalFilename();

	    results[0] = false;
	    results[1] = false;
	    results[2] = false;
    	FileLog uploadLog = new FileLog();
	    
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
		    UploadInputs dto = mapper.readValue(doc, UploadInputs.class);
//		    System.out.println(dto.document);
//		    System.out.println(dto.register_date);
		    LocalDate parsedDate = parseDate(dto.federal_register_date);
			dto.federal_register_date = parsedDate.toString();
		    dto.filename = origFilename;
		    
		    // Validate: File must exist, valid fields, and can't be duplicate
			if(file == null || !isValid(dto) || recordExists(dto.title, dto.document, dto.federal_register_date)) {
				return new ResponseEntity<boolean[]>(results, HttpStatus.BAD_REQUEST);
			}
			
	    	// Start log
		    uploadLog.setUser(getUser(token));
		    uploadLog.setLogTime(LocalDateTime.now());
		    
		    // Prefer empty string over null
		    if(dto.eis_identifier == null) {
		    	dto.eis_identifier = "";
		    }
		    if(dto.link == null) {
		    	dto.link = "";
		    }
		    if(dto.notes == null) {
		    	dto.notes = "";
		    }
		    
		    // Since metadata is already validated, this should always save successfully with a db connection.
	    	EISDoc savedDoc = saveMetadata(dto); // note: JPA .save() is safe from sql injection

	    	// saved?
	    	results[0] = (savedDoc != null);
	    	if(results[0]) {
	    		uploadLog.setErrorType("Saved");

			    uploadLog.setDocumentId(savedDoc.getId());
			    
			    HttpEntity entity = MultipartEntityBuilder.create()
						.addTextBody("filepath",
								savedDoc.getId().toString()) // Feed Express path to use
	    				.addBinaryBody("test", // This name can be used by multer
	    						file.getInputStream(), 
	    						ContentType.create("application/octet-stream"), 
	    						file.getOriginalFilename())
	    				.build();
			    HttpPost request = new HttpPost(uploadURL);
			    if(testing) { request = new HttpPost(uploadTestURL); }
			    request.setEntity(entity);
			    HttpClient client = HttpClientBuilder.create().build();
			    HttpResponse response = client.execute(request);
			    
			    System.out.println(response.toString());
			    
			    // uploaded?
			    results[1] = (response.getStatusLine().getStatusCode() == 200);

			    if(results[1]) {
		    		uploadLog.setErrorType("Uploaded");
		    		uploadLog.setFilename(origFilename);
				    
			    	// Run Tika on file, record if 200 or not
				    if(origFilename.length() > 4
			    			&& origFilename.substring(origFilename.length()-4).equalsIgnoreCase(".pdf")) 
				    {
			    		int status = this.convertPDF(savedDoc).getStatusCodeValue();
				    	results[2] = (status == 200);
			    	} else { // Archive or image case (not set up to attempt image conversion, may have issues with non-.zip archives)
				    	int status = this.convertRecordSmart(savedDoc).getStatusCodeValue();
				    	results[2] = (status == 200);
				    	// Note: 200 doesn't necessarily mean tika was able to convert anything
			    	}
				    
			    	// converted to fulltext?  Or at least no Tika errors converting
			    	if(results[2]) {
					    uploadLog.setImported(true);
			    	}
			    }
		    	
		    } else {
		    	returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		    }
			
		} catch (Exception e) {
			uploadLog.setErrorType(e.getStackTrace().toString());
			e.printStackTrace();
		} finally {
		    file.getInputStream().close();
	    	fileLogRepository.save(uploadLog);
		}

		return new ResponseEntity<boolean[]>(results, returnStatus);
	}
	
	
	

	/** TODO: 
	 * - express service given path, ensure exists, creates dir if not, then puts file in the deepest folder
	 * 
	 * - handle disparate directories case: in order to associate files properly, will probably need a Docs table
	 * containing Type, foreign key to EISDoc, filename, and relative path
	 * 
	 * - express server needs to return path where everything was saved
	 * 
	 * - need to handle no relative path case (no folder)
	 * - if no folder, express will have to make the path based on the new ID from saving the metadata doc, 
	 * type, and agency, so will need to send those values also...  but it also has to ensure no collisions,
	 * so it needs to actually make sure the identifying directory does NOT exist already and iterate until it finds
	 * one that doesn't exist, then express has to return the unique name it came up with, then this controller has to
	 * save that folder name to the record
	 * 
	 * - next, download logic needs to change to expect structure of agency/doc.folder name/type, and multiple files
	 * - Finally, for bulk upload with or without CSV, can use same dropzone, same path as filename logic, different route
	 */
	
	
	
	
	/** 
	 * Upload a record with more than one file or directory associated with it
	 * For each file, assumes a base folder that ends in a number (identifying folder)
	 * If that doesn't exist, NEPAFile folder field instead uses the new ID from saving the metadata EISDoc
	 * */
	@CrossOrigin
	@RequestMapping(path = "/uploadFiles", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<String> importDocuments(@RequestPart(name="files") MultipartFile[] files, 
								@RequestPart(name="doc") String doc, @RequestHeader Map<String, String> headers) 
										throws IOException 
	{ 
		if(testing) {
			System.out.println(files.length);
			for(int i = 0; i < files.length; i++) {
				System.out.println(files[i].getOriginalFilename());
				System.out.println(files[i].getBytes());
			}
		}
		
		
		
		/** Validation: Files; auth; record; record shouldn't exist */
		
		if(files == null || files.length == 0) { // 400
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
		
		String token = headers.get("authorization");
		if(!isCurator(token) && !isAdmin(token)) // 401
		{
			return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
		} 
	    
    	ObjectMapper mapper = new ObjectMapper();
	    UploadInputs dto = mapper.readValue(doc, UploadInputs.class);
	    LocalDate parsedDate = parseDate(dto.federal_register_date);
		dto.federal_register_date = parsedDate.toString();
		dto.filename = "";

		if(!isValid(dto) || recordExists(dto.title, dto.document, dto.federal_register_date)) {
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
		
		
		
		/** Save metadata or else return error */
		
		EISDoc savedDoc = null;
		try {
			// In order to associate and save files with doc, need the ID, which means we need to first save it.
	    	savedDoc = saveMetadata(dto);
	    	if(savedDoc == null) {
				return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
	    	}
		} catch(Exception e) {
			// Couldn't save
			return new ResponseEntity<String>(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		/** Finally, upload files, save to files table and log */
		
		for(int i = 0; i < files.length; i++) {
			
	    	FileLog uploadLog = new FileLog();
	    	
		    try {

		    	// In this case, we don't want relative paths.  So, strip those out.
			    String origFilename = files[i].getOriginalFilename();
			    if(testing) {
			    	System.out.println(origFilename);
			    }
			    
			    String savePath = "";
			    if(getUniqueFolderName(origFilename, savedDoc).equalsIgnoreCase(savedDoc.toString())) { // If we generated the folder ourselves
			    	savePath = "/" + getUniqueFolderName(origFilename, savedDoc) + "/"; // Assume saved to /{ID}/
		    	} else { // Otherwise use provided path
		    		savePath = getPathOnly(origFilename);
		    	}
			    
			    // Upload file
			    // Note: NOT leaving logic to Express server to create directory based on ID/Type, if no unique foldername
		    	HttpEntity entity = MultipartEntityBuilder.create()
	    					.addTextBody("filepath",
	    							savePath) // Feed Express path to use
		    				.addBinaryBody("file", //fieldname
		    						files[i].getInputStream(), 
		    						ContentType.create("application/octet-stream"), 
		    						origFilename)
		    				.build();
			    HttpPost request = new HttpPost(uploadURL);
			    if(testing) { request = new HttpPost(uploadTestURL); }
			    request.setEntity(entity);
	
			    HttpClient client = HttpClientBuilder.create().build();
			    HttpResponse response = client.execute(request);
			    
			    if(testing) {
				    System.out.println(response.toString());
			    }
			    
			    boolean uploaded = (response.getStatusLine().getStatusCode() == 200);
			    boolean converted = false;

			    // If file uploaded, proceed to saving to table and logging
			    if(uploaded) {
			    	// Save NEPAFile
			    	handleNEPAFileSave(origFilename, savedDoc, dto.document);
			    	
			    	// Save FileLog
				    uploadLog.setFilename(origFilename);
				    uploadLog.setUser(getUser(token));
				    uploadLog.setLogTime(LocalDateTime.now());
				    uploadLog.setErrorType("Uploaded");
				    uploadLog.setDocumentId(savedDoc.getId());
				    
			    	// Run Tika on file, record if 200 or not
			    	if(origFilename.length() > 4
			    			&& origFilename.substring(origFilename.length()-4).equalsIgnoreCase(".pdf")) 
			    	{
			    		converted = (this.convertPDF(savedDoc).getStatusCodeValue() == 200);
			    	} else { // Archive or image case (not set up to attempt image conversion, may have issues with non-.zip archives)
			    		converted = (this.convertRecordSmart(savedDoc).getStatusCodeValue() == 200);
				    	// Note: 200 doesn't necessarily mean tika was able to convert anything
			    	}
			    	
			    	if(converted) {
					    uploadLog.setImported(true);
			    	}
			    }

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			    files[i].getInputStream().close();
			    if(uploadLog.getUser() != null) { 
			    	fileLogRepository.save(uploadLog);
			    }
			}
		}
		
		return new ResponseEntity<String>("OK", HttpStatus.OK);
		
		
		// TODO: Can save CSV data before NEPAFiles, and vice versa.  Therefore EISDoc needs a Folder, and 
		// NEPAFiles might have null for the foreign key.  The connection has to be enforced after we have both.
		
		// Choice so far: Overwrite everything that already exists when uploading, separate update function should be
		// the way to add/remove files with an existing record with existing files
		
		/** TODO: Same logic as uploadFile except: 
		 * - multiple Files
		 * - each File's .getOriginalFilename() should include a relative path thanks to the frontend javascript logic
		 * 
		 * - save base /folder name in the folder column, if it comes with a folder
		 * - express service will need to parse each /folder, ensure they exist, create if not, then put the /filename.ext 
		 * in the deepest folder
		 * 
		 * - handle disparate directories case: in order to associate files properly, will probably need a Docs table
		 * containing Type, foreign key to EISDoc, filename, and relative path
		 * 
		 * - express server needs to return path where everything was saved
		 * 
		 * - need to handle no relative path case (no folder)
		 * - if no folder, express will have to make the path based on the new ID from saving the metadata doc, 
		 * type, and agency, so will need to send those values also...  but it also has to ensure no collisions,
		 * so it needs to actually make sure the identifying directory does NOT exist already and iterate until it finds
		 * one that doesn't exist, then express has to return the unique name it came up with, then this controller has to
		 * save that folder name to the record
		 * 
		 * - next, download logic needs to change to expect structure of agency/doc.folder name/type, and multiple files
		 * - Finally, for bulk upload with or without CSV, can use same dropzone, same path as filename logic, different route
		 */
	}
	


	/** 
	 * Takes .csv file with required headers: title/register_date/filename/document_type and imports each valid,
	 * non-duplicate record.  
	 * 
	 * Valid records: Must have title/register_date/filename/document_type, register_date must conform to one of
	 * the formats in parseFormatters[]
	 * 
	 * @return List of strings with message per record (zero-based) indicating success/error/duplicate 
	 * and potentially more details */
	@CrossOrigin
	@RequestMapping(path = "/uploadCSV", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importCSV(@RequestPart(name="csv") String csv, @RequestHeader Map<String, String> headers) 
										throws IOException { 
		
		String token = headers.get("authorization");
		
		if(!isCurator(token) && !isAdmin(token)) 
		{
			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
		} 
		List<String> results = new ArrayList<String>();


	    // Expect these headers:
	    // Title, Document, EPA Comment Letter Date, Federal Register Date, Agency, State, EIS Identifier, Filename, Link
	    // TODO: Translate these into a standard before proceeding? Such as Type or Document Type instead of Document
		
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
		    UploadInputs dto[] = mapper.readValue(csv, UploadInputs[].class);

		    // Ensure metadata is valid
			int count = 0;
			for (UploadInputs itr : dto) {
				itr.title = org.apache.commons.lang3.StringUtils.normalizeSpace(itr.title);
			    // Choice: Need at least title, date, type for deduplication (can't verify unique item otherwise)
			    if(isValid(itr)) {

			    	// Save only valid dates
			    	boolean error = false;
					try {
						LocalDate parsedDate = parseDate(itr.federal_register_date);
						itr.federal_register_date = parsedDate.toString();
					} catch (IllegalArgumentException e) {
						results.add("Item " + count + ": " + e.getMessage());
						error = true;
					} catch (Exception e) {
						results.add("Item " + count + ": Error " + e.getMessage());
						error = true;
					}

					if(itr.epa_comment_letter_date != null && itr.epa_comment_letter_date.length() > 0) {
						itr.epa_comment_letter_date = parseDate(itr.epa_comment_letter_date).toString();
					}
					
					if(!error) {
						// Deduplication
						
						Optional<EISDoc> recordThatMayExist = getEISDocByTitleTypeDate(itr.title, itr.document, itr.federal_register_date);
						
						// If record exists but has no filename, then update it instead of skipping
						// This is because current data is based on having a filename for an archive or not,
						// so new data can add files where there are none, without adding redundant data when there is data
						if(recordThatMayExist.isPresent() && recordThatMayExist.get().getFilename().isBlank()) {
//							if(testing) {
//								// not saving for now, just pretending
//								results.add("Item " + count + ": Updated: " + itr.title);
//							} else {
							ResponseEntity<Long> status = updateDto(itr, recordThatMayExist);
							
							if(status.getStatusCodeValue() == 500) { // Error
								results.add("Item " + count + ": Error saving: " + itr.title);
					    	} else {
								results.add("Item " + count + ": Updated: " + itr.title);

					    		// Log successful record update for accountability (can know last person to update an EISDoc)
								FileLog recordLog = new FileLog();
								recordLog.setErrorType("Updated existing record because it had no filename");
					    		recordLog.setDocumentId(status.getBody());
					    		recordLog.setFilename(itr.filename);
					    		recordLog.setImported(false);
					    		recordLog.setLogTime(LocalDateTime.now());
					    		recordLog.setUser(getUser(token));
					    		fileLogRepository.save(recordLog);
					    	}
//							}
						}
						// If file doesn't exist, then create new record
						else if(!recordThatMayExist.isPresent()) { 
//							System.out.println(itr.title);
//							System.out.println(itr.document);
//							System.out.println(itr.federal_register_date);
//							if(testing) {
//								// not saving for now, just pretending
//								results.add("Item " + count + ": OK: " + itr.title);
//							} else {
						    ResponseEntity<Long> status = saveDto(itr); // save record to database
						    	// TODO: What are the most helpful results to return?  Just the failures?  Duplicates also?
					    	if(status.getStatusCodeValue() == 500) { // Error
								results.add("Item " + count + ": Error saving: " + itr.title);
					    	} else {
					    		results.add("Item " + count + ": OK: " + itr.title);

					    		// Log successful record import (need accountability for new metadata)
								FileLog recordLog = new FileLog();
					    		recordLog.setDocumentId(status.getBody());
					    		recordLog.setFilename(itr.filename);
					    		recordLog.setImported(false);
					    		recordLog.setLogTime(LocalDateTime.now());
					    		recordLog.setUser(getUser(token));
					    		fileLogRepository.save(recordLog);
					    	}
//							}
				    	} else {
							results.add("Item " + count + ": Duplicate");
				    	}
					}
			    } else {
					results.add("Item " + count + ": Missing one or more required fields: federal_register_date/document_type/eis_identifier/title");
			    }
			    count++;
			}
	    	// TODO: Run Tika on new files later, record results (need new bulk file import function for this part)
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<List<String>>(results, HttpStatus.OK);
	}



	/** 
	 * Upload more than one directory already associated with a CSV (or will be in the future and has a unique foldername)
	 * For each file, assumes a base folder that ends in a number (identifying folder)
	 * If that doesn't exist, rejects that file because it can't ever be automatically connected to a record 
	 * (would be forever unlinked/orphaned)
	 * */
	@CrossOrigin
	@RequestMapping(path = "/uploadFilesBulk", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<String> importFilesBulk(@RequestPart(name="files") MultipartFile[] files, 
								@RequestHeader Map<String, String> headers) 
										throws IOException 
	{ 
		/** Validation: Files; auth  */
		
		if(files == null || files.length == 0) { // 400
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
		
		String token = headers.get("authorization");
		if(!isCurator(token) && !isAdmin(token)) // 401
		{
			return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
		} 
		
		
		/** If valid: Upload files, save to files table, add to existing records if possible, and log */
		
		for(int i = 0; i < files.length; i++) {
			
	    	FileLog uploadLog = new FileLog();

		    try {

			    String origFilename = files[i].getOriginalFilename();
			    String folderName = getUniqueFolderNameOrEmpty(origFilename);
			    
			    if(folderName.length() == 0) { // If no folder name
			    	// Do nothing (reject file: no upload, no log)
			    } else if(metadataExists(folderName)){
			    	// If metadata exists we can link this to something, therefore proceed with upload
				    String savePath = getPathOnly(origFilename);
				    
				    // Upload file
				    // Note: NOT leaving logic to Express server to create directory based on ID/Type, if no unique foldername
			    	HttpEntity entity = MultipartEntityBuilder.create()
		    					.addTextBody("filepath",
		    							savePath) // Feed Express path to use
			    				.addBinaryBody("file", //fieldname
			    						files[i].getInputStream(), 
			    						ContentType.create("application/octet-stream"), 
			    						origFilename)
			    				.build();
				    HttpPost request = new HttpPost(uploadURL);
				    if(testing) { request = new HttpPost(uploadTestURL); }
				    request.setEntity(entity);
		
				    HttpClient client = HttpClientBuilder.create().build();
				    HttpResponse response = client.execute(request);
				    
				    if(testing) {
					    System.out.println(response.toString());
				    }
				    
				    boolean uploaded = (response.getStatusLine().getStatusCode() == 200);
				    boolean converted = false;
		
				    // If file uploaded, see if we can link it, then proceed to saving to table and logging
				    List<EISDoc> existingDocs = docRepository.findAllByFolder(folderName);
				    if(uploaded) {
				    	// Save NEPAFile

				    	// 1. Requires ability to link from previous CSV import
				    	// 2. Type would be the directory after the unique folder name, use that if it exists.
				    	// 3. If no directory after that, use existingDoc's (if existingDoc exists).  
				    	// Otherwise leave type empty
				    	// 4. While folder should be unique if non-empty ideally, it's not fully enforced
				    	// (hopefully never actually happens or else someone messed up)
				    	// If it does happen, log and save to the first in the list found by JPA

				    	NEPAFile savedNEPAFile = handleNEPAFileSave(origFilename, existingDocs);
				    	
				    	// Save FileLog
				    	
				    	if(savedNEPAFile == null) {
				    		// Duplicate, nothing else to do.  Could log that nothing happened if we want to
				    	} else {
				    		uploadLog.setFilename(origFilename); // full path incl. filename
						    uploadLog.setUser(getUser(token));
						    uploadLog.setLogTime(LocalDateTime.now());
						    uploadLog.setErrorType("Uploaded");
						    // Note: Should be impossible not to have a linked document if the logic got us here
						    if(existingDocs.size() > 0) { // If we have a linked document:
						    	// Log if size() > 1 (means we sort of have representation of a process)
						    	// hopefully we matched to the correct record via type, but if not we'll know to check maybe
						    	if(existingDocs.size() > 1) {
								    uploadLog.setErrorType("Matched 2 or more documents");
						    	}
							    uploadLog.setDocumentId(savedNEPAFile.getEisdoc().getId());
						    
						    	// Run Tika on folder, record if 200 or not
							    converted = (this.convertNEPAFile(savedNEPAFile).getStatusCodeValue() == 200);
							    
						    	if(converted) {
								    uploadLog.setImported(true);
						    	}
						    }
				    	}
				    }
			    } else {
			    	// TODO: Inform user this file can't be linked to anything, and has been rejected
			    }
	
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			    files[i].getInputStream().close();
			    if(uploadLog.getUser() != null) { 
			    	fileLogRepository.save(uploadLog);
			    }
			}
		}
		
		return new ResponseEntity<String>("OK", HttpStatus.OK);
	}
		
	/** Return whether metadata exists for a given EIS Identifier (folder) */
	private boolean metadataExists(String folderName) {
		// TODO Auto-generated method stub
		long numRecords = docRepository.countByFolder(folderName);
		boolean exists = numRecords > 0;
		return exists;
	}



	// Must have a link available, presumably added by a CSV import. 
	// Therefore EISDoc needs a Folder, and the connection has to be enforced after we have both.
	
	// Choice so far: Overwrite same-name files that already exists when uploading, separate update function should be
	// the way to add/remove files with an existing record with existing files
	
	/** Given relative path and possibly one or more EISDocs (use first EISDoc if exists), 
	 *  saves NEPAFile if it doesn't exist already.  Prefers to set document type from directory if available,
	 *  rather than from EISDoc.  Should be used after we verify a unique folder name exists
	 *  because without that field a link shouldn't be possible yet 
	 * @return */
	private NEPAFile handleNEPAFileSave(String fullPath, List<EISDoc> existingDocs) {
		boolean duplicate = nepaFileRepository.existsByFilenameAndRelativePathIn(getFilenameWithoutPath(fullPath), getPathOnly(fullPath));
		
		if(duplicate) {
			return null;
		} else {
			NEPAFile fileToSave = new NEPAFile();
	    	NEPAFile savedFile = null;
	    	
	    	String folderName = getUniqueFolderNameOrEmpty(fullPath);
	    	String documentType = getDocumentTypeOrEmpty(fullPath);
	    	
	    	fileToSave.setFilename(getFilenameWithoutPath(fullPath));
	    	fileToSave.setFolder(folderName);
	        fileToSave.setRelativePath(getPathOnly(fullPath)); 
			if(existingDocs.size()>0) {
				
				// If we matched on more than one record, we had better have a document type to differentiate them.
				// Should expect this anyway, ideally.
				// Records may be part of the same process and share a folder, but NEPAFiles are for one document type.
				Optional<EISDoc> existingDoc = docRepository.findTopByFolderAndDocumentTypeIn(folderName, documentType);
		    	if(existingDoc.isPresent() && !folderName.isBlank() && !documentType.isBlank()) {
		    		fileToSave.setEisdoc(existingDoc.get());
		    	} else { // if we fail (this is not great), just link with the top of the list
			    	fileToSave.setEisdoc(existingDocs.get(0));
		    	}
		    	
		    	// prefer to use directory document type, if exists
		    	if(documentType.isBlank()) {
			    	fileToSave.setDocumentType(existingDocs.get(0).getDocumentType());
		    	} else {
		    		fileToSave.setDocumentType(documentType);
		    	}
		    	
		    	savedFile = nepaFileRepository.save(fileToSave);
			}
			return savedFile;
		}
	}


	/** Given path, new/updated EISDoc and document_type, save new NEPAFile if not a duplicate. 
	 *  Uses getFilenameWithoutPath() to set filename and getUniqueFolderName() to set folder, uses identical logic 
	 *  for giving Express service the relative path to use (thus ensuring the download path is consistent for
	 *  both database and file directory on DBFS). */
	private void handleNEPAFileSave(String relativePath, EISDoc savedDoc, String document_type) {
		boolean duplicate = true;
		duplicate = nepaFileRepository.existsByFilenameAndEisdocIn(getFilenameWithoutPath(relativePath), savedDoc);
		
		if(duplicate) {
			return;
		} else {
	    	NEPAFile fileToSave = new NEPAFile();
	    	fileToSave.setEisdoc(savedDoc);
	    	fileToSave.setFilename(getFilenameWithoutPath(relativePath));
	    	fileToSave.setFolder(getUniqueFolderName(relativePath, savedDoc));
	    	/** TODO: Temporary logic until we get the path back from Express to guarantee consistency
	    	/* if we get the path wrong the system will fail to find the files 
	    	/* even when the NEPAFile has a correct foreign key */
	    	if(fileToSave.getFolder().equalsIgnoreCase(savedDoc.toString())) { // If we generated the folder ourselves
	    		fileToSave.setRelativePath("/" + fileToSave.getFolder() + "/"); // Assume saved to /{ID}/
	    	} else { // Otherwise use provided path
	        	fileToSave.setRelativePath(getPathOnly(relativePath)); 
	    	}
	    	fileToSave.setDocumentType(document_type);
	    	nepaFileRepository.save(fileToSave);
		}
	}



	/** Returns name of document type derived from relative path if possible, else returns empty string */
	private String getDocumentTypeOrEmpty(String relativePath) {
		// Split and get index of unique folder name, if the next index is not the last index (filename e.g. 
		// /folderIndex/file.ext then use second to last index for type
		// e.g. /folderIndex/typeIndexToUse/fileIndex.ext
		// else return empty string

		String[] sections = relativePath.split("/");
		for(int i = 0; i < sections.length; i++) {
			if(sections[i].length() > 0
					&& Character.isDigit(sections[i].charAt(sections[i].length()-1))) { // if ends in number, should be it
				if(i >= (sections.length - 2)) {
					// No Type folder found: identifying folder is either the final item or 
					// the second to last item.  Will return ""
				} else {
					// Return second to last item (deepest folder containing the file, which is the last item)
					return sections[sections.length - 2]; 
				}
			}
		}
		
		// if we get here return empty string
		return "";
	}



	private String getUniqueFolderNameOrEmpty(String origFilename) {
		String[] sections = origFilename.split("/");
		for(int i = 0; i < sections.length; i++) {
			if(sections[i].length() > 0
					&& Character.isDigit(sections[i].charAt(sections[i].length()-1))) { // if ends in number, should be it
				return sections[i];
			}
		}
		
		// if we get here return empty string
		return "";
	}



	/** Pulls the EIS identifier from the string if it exists (shallowest folder ending in 4 numbers
	 *  - there shouldn't be any other folders ending in numbers, that's an important rule for users uploading like this),
	 *  or define a new unique folder based on the ID if it doesn't exist */
	private String getUniqueFolderName(String origFilename, EISDoc eisdoc) {
		// Should be base foldername if given, but not if it's a type folder like Final or ROD
		// Basically, find the first folder that at least ends in #### and if we find one and it's wrong, the user
		// has errored greatly
		// expect min. 1 number at end
		
		String[] sections = origFilename.split("/");
		for(int i = 0; i < sections.length; i++) {
			if(sections[i].length() > 0
					&& Character.isDigit(sections[i].charAt(sections[i].length()-1))) { // if ends in number, should be it
				return sections[i];
			}
		}
		
		// if we get here we need to come up with a folder name ourselves: the provided UID serves
		return eisdoc.getId().toString();
	}

	/** e.g. C:/ex/etc/test.pdf --> C:/ex/etc/ */
	private String getPathOnly(String pathWithFilename) {
		int idx = pathWithFilename.replaceAll("\\\\", "/").lastIndexOf("/");
		return idx >= 0 ? pathWithFilename.substring(0, idx + 1) : pathWithFilename;
	}
	
	/** e.g. C:/ex/etc/test.pdf --> test.pdf */
	private String getFilenameWithoutPath(String pathWithFilename) {
		int idx = pathWithFilename.replaceAll("\\\\", "/").lastIndexOf("/");
		return idx >= 0 ? pathWithFilename.substring(idx + 1) : pathWithFilename;
	}

	/** Saves pre-validated metadata record to database and returns the EISDoc with new ID */
	private EISDoc saveMetadata(UploadInputs dto) throws org.springframework.orm.jpa.JpaSystemException{
    	EISDoc saveDoc = new EISDoc();
    	saveDoc.setAgency(dto.agency.trim());
    	saveDoc.setDocumentType(dto.document.trim());
    	
    	if(dto.federal_register_date.length()>9) {
	    	saveDoc.setRegisterDate(LocalDate.parse(dto.federal_register_date));
    	} else {
	    	saveDoc.setRegisterDate(null);
    	}
    	saveDoc.setState(dto.state);
    	saveDoc.setTitle(dto.title.trim());
    	
    	saveDoc.setCommentDate(null);
    	saveDoc.setFilename(dto.filename);
    	saveDoc.setCommentsFilename("");
    	
    	// Save (ID is null at this point, but .save() picks a unique ID thanks to the model so it's good)
    	EISDoc savedDoc = docRepository.save(saveDoc); // note: JPA .save() is safe from sql injection
    	
    	return savedDoc;
	}
	
	
	
	
	
	

	/** Check that required fields exist (doesn't verify document type from a list of acceptable types) */
	private boolean isValid(UploadInputs dto) {
//		System.out.println(dto.title);
//		System.out.println(dto.federal_register_date);
//		System.out.println(dto.document);
//		System.out.println(dto.filename);
		boolean valid = true;
		// Choice: Agency/state required also?
		// Check for null; filename should at least be "multi" but sometimes may be blank
		if(dto.filename == null) {
			dto.filename = "";
		}
		if(dto.federal_register_date == null || dto.title == null || dto.document == null) {
			valid = false;
			return valid; // Just stop here and don't have to worry about validating null values
		}
		
		if(dto.filename.contentEquals("n/a")) {
			valid = false;
		}
		
		// Expect EIS identifier
		if(dto.eis_identifier == null || dto.eis_identifier.isBlank()) {
			valid = false;
			return valid;
		}
		
		// Check for empty
		if(dto.title.isBlank()) {
			valid = false; // Need title
		}

		if(dto.document.isBlank()) {
			valid = false; // Need type
		}
		
		return valid;
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

	/** Minimal upload test to make sure uploading works (saves nothing to db, does save file to disk) */
	@CrossOrigin
	@RequestMapping(path = "/uploadTest", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<String> uploadTest(@RequestPart(name="file") MultipartFile file, 
			@RequestHeader Map<String, String> headers) 
					throws IOException { 
	    String result = "Started";
	    
		String token = headers.get("authorization");
		if(!isCurator(token) && !isAdmin(token)) 
		{
			return new ResponseEntity<String>(result, HttpStatus.UNAUTHORIZED);
		}

	    try {
			
	    	HttpEntity entity = MultipartEntityBuilder.create()
	    				.addBinaryBody("test", 
	    						file.getInputStream(), 
	    						ContentType.create("application/octet-stream"), 
	    						file.getOriginalFilename())
	    				.build();
		    HttpPost request = new HttpPost(uploadURL);
		    if(testing) { request = new HttpPost(uploadTestURL); }
		    request.setEntity(entity);

		    HttpClient client = HttpClientBuilder.create().build();
		    HttpResponse response = client.execute(request);
		    
		    // so far so good?
		    result = response.toString();
			
		} catch (Exception e) {
			result += " *** Error: " + e.toString();
		} finally {
		    file.getInputStream().close();
		}

		return new ResponseEntity<String>(result, HttpStatus.OK);
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
		
		// Deduplication: Ignore when document ID already exists in EISDoc table
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
		
			// Make sure there is a file (for current data, no filename means nothing to convert for this record)
			if(eis.getFilename() == null || eis.getFilename().length() == 0) {
				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
			}
			String filename = eis.getFilename();
			
			String relevantURL = dbURL;
			if(testing) {
				relevantURL = testURL;
			}
			URL fileURL = new URL(relevantURL + filename);
			
			// 1: Download the file
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
				} catch(Exception e) {
					// Log error
					try {
						if(docText.getPlaintext() == null || docText.getPlaintext().length() == 0) {
							fileLog.setImported(false);
						} else {
							fileLog.setImported(true);
						}
						
						fileLog.setErrorType(e.getLocalizedMessage());
						fileLog.setLogTime(LocalDateTime.now());
						fileLogRepository.save(fileLog);
						e.printStackTrace();
					} catch (Exception e2) {
						if(testing) {
							System.out.println("Error logging error...");
							e2.printStackTrace();
						}
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

	private ResponseEntity<Void> convertNEPAFile(NEPAFile savedNEPAFile) {

		// Check to make sure this record exists.
		if(savedNEPAFile == null) {
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}
		
		// Note: There can be multiple files per archive and multiple folders, 
		// resulting in multiple documenttext records for the same ID, 
		// but presumably different full paths with hopefully different files
		
		try {
			String filename = savedNEPAFile.getFilename();
		
			// Make sure there is a file (for current data, no filename means nothing to convert for this record)
			if(filename == null || filename.length() == 0) {
				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
			}
			
			String relevantURL = dbURL;
			if(testing) {
				relevantURL = testURL;
			}
			
			// Note: because the relative path always begins with / and relevantURL already has /,
			// we drop the first character of fullPath when we build it here:
			String fullPath = (savedNEPAFile.getRelativePath() + filename).substring(1);

			// Handle things like spaces in folder names:
//			URI uri = new URI(relevantURL + fullPath.replaceAll(" ", "%20"));
//			System.out.println("URI: " + uri.toString());
//			URL fileURL = uri.toURL();
			URL fileURL = new URL(relevantURL + encodeURIComponent(fullPath));
			
			
			if(testing) {
				System.out.println("FileURL " + fileURL.toString());
			}
			
			// Note: Multiple eisdocs may share the folder and therefore responsibility for this file.
			// So, the uniqueness should be determined by folder AND document type.
			// Therefore we need to get the EISDoc representing that, exactly.
			Optional<EISDoc> eis = docRepository.findTopByFolderAndDocumentTypeIn(savedNEPAFile.getFolder(), savedNEPAFile.getDocumentType());

			// If we anomalously can't get that exact match, just use the first one we find
			if(!eis.isPresent()) {
				eis = docRepository.findTopByFolder(savedNEPAFile.getFolder());
			}
			
			// Somehow no link at all?
			if(!eis.isPresent()) {
				return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
			}
			
			EISDoc foundDoc = eis.get();
			
			
			// Duplicate?  Theoretically can be identical filenames for a linked folder, or we'd check document_text
			// check file_log to see if we've Imported this file before (if full path and imported)
			// tinyInt(1) is effectively a boolean
			
			// Option: Move file log down to this scope, and don't save the log if we skipped due to duplicate?
			// Or, if HttpStatus.FOUND, parent function can choose to skip logging this or not
			if(fileLogRepository.existsByFilenameAndImported((savedNEPAFile.getRelativePath() + filename), true)) 
			{
				// 302 (bulk import file log will treat this as imported=0 but still log it)
				return new ResponseEntity<Void>(HttpStatus.FOUND);
			} 
			
			
			// PDF or archive?
			boolean fileIsArchive = true;
			if(filename.length() > 4 && filename.substring(filename.length()-4).equalsIgnoreCase(".pdf")) 
	    	{
				// file is PDF
	    		fileIsArchive = false;
	    	} else { // Archive or image case (not set up to attempt image conversion, may have issues with non-.zip archives)
	    		
	    	}

			HttpStatus importStatus = HttpStatus.OK;
			// logic is necessarily different for archives (need ZipInputStream to go through all files in archive)
			if(fileIsArchive) {
				importStatus = archiveConvertImportAndIndex(foundDoc, fileURL);
			} else { // PDF
				importStatus = pdfConvertImportAndIndex(foundDoc, fileURL, filename);
			}
			
			return new ResponseEntity<Void>(importStatus);
			
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
			
	}

	// helper method for bulk file import gets file, converts to text, indexing is then automatic
	private HttpStatus pdfConvertImportAndIndex(EISDoc foundDoc, URL fileURL, String filename) throws IOException {
		final int BUFFER = 2048;

		Tika tikaParser = new Tika();
		tikaParser.setMaxStringLength(-1); // disable limit

		// 1: Download the file
		InputStream in = new BufferedInputStream(fileURL.openStream());
		
		DocumentText docText = new DocumentText();
		docText.setEisdoc(foundDoc);
		docText.setFilename(filename);

		int count;
		byte[] data = new byte[BUFFER];
		
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
			e.printStackTrace();
		}
		
		in.close();

		return HttpStatus.OK;
	}

	// helper method for bulk file import gets file, iterates through archive contents, converts to text, auto-indexed
	private HttpStatus archiveConvertImportAndIndex(EISDoc nepaDoc, URL fileURL) throws IOException {
		final int BUFFER = 2048;

		Tika tikaParser = new Tika();
		tikaParser.setMaxStringLength(-1); // disable limit

		InputStream in = new BufferedInputStream(fileURL.openStream());
		ZipInputStream zis = new ZipInputStream(in);
		ZipEntry ze;
		
		while((ze = zis.getNextEntry()) != null) {
			
			int count;
			byte[] data = new byte[BUFFER];
			
			String extractedFilename = ze.getName();
			
			// Handle directory - can ignore them if we're just converting PDFs
			if(!ze.isDirectory()) {
				if(testing) {
					System.out.println(textRepository.existsByEisdocAndFilename(nepaDoc, extractedFilename));
				}
				
				// Skip if we have this text already (duplicate filenames associated with one EISDoc record are skipped)
				if(textRepository.existsByEisdocAndFilename(nepaDoc, extractedFilename)) {
					zis.closeEntry();
				} else {
					
					if(testing) {
						System.out.println("Extracting " + ze);
					}
					
					DocumentText docText = new DocumentText();
					docText.setEisdoc(nepaDoc);
					docText.setFilename(extractedFilename);
					
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
						// TODO: Report/log error specific to file within archive?
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

		return HttpStatus.OK;
	}


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
			InputStream in = new BufferedInputStream(fileURL.openStream());
			ZipInputStream zis = new ZipInputStream(in);
			ZipEntry ze;
			
			while((ze = zis.getNextEntry()) != null) {
				int count;
				byte[] data = new byte[BUFFER];
				
				String filename = ze.getName();
				
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
		
			// Make sure there is a file (for current data, no filename means nothing to convert for this record)
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
				
				// TODO: Can check filename for .pdf extension to determine if we should try to parse it?
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

	/** Returns if database contains at least one instance of a title/type/date combination */
	@CrossOrigin
	@RequestMapping(path = "/existsTitleTypeDate", method = RequestMethod.GET)
	private boolean recordExists(@RequestParam String title, @RequestParam String type, @RequestParam String date) {
		// Original data has no apostrophes, so a better dupe check ORs the results of comparing both with the original title, and 
		// a title without apostrophes, specifically
		String noApostropheTitle = title.replaceAll("'", "");
		return (docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(title.trim(), type.trim(), LocalDate.parse(date)).isPresent()
				|| docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(noApostropheTitle.trim(), type.trim(), LocalDate.parse(date)).isPresent()
			);
		}
	
	// Experimental, probably useless (was trying to get document outlines)
//	@CrossOrigin
//	@RequestMapping(path = "/xhtml", method = RequestMethod.GET)
//	public ResponseEntity<List<String>> xhtml(@RequestHeader Map<String, String> headers) {
//		
//		String token = headers.get("authorization");
//		if(!isAdmin(token)) 
//		{
//			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
//		} 
//		else 
//		{
//			return new ResponseEntity<List<String>>(convertXHTML(docRepository.findById(22)), HttpStatus.OK);
//		}
//		
//	}



	// Probably useless
//	private List<String> convertXHTML(EISDoc eis) {
//
//		// Note: There can be multiple files per archive, resulting in multiple documenttext records for the same ID
//		// but presumably different filenames with hopefully different contents
//		final int BUFFER = 2048;
//		
//		List<String> results = new ArrayList<String>();
//		
//		try {
//			Tika tikaParser = new Tika();
//			tikaParser.setMaxStringLength(-1); // disable limit
//			
//			String relevantURL = dbURL;
//			if(testing) {
//				relevantURL = testURL;
//			}
//			URL fileURL = new URL(relevantURL + eis.getFilename());
//			
//			if(testing) {
//				System.out.println("FileURL " + fileURL.toString());
//			}
//			
//			// 1: Download the archive
//			InputStream in = new BufferedInputStream(fileURL.openStream());
//			ZipInputStream zis = new ZipInputStream(in);
//			ZipEntry ze;
//			
//			while((ze = zis.getNextEntry()) != null) {
//				int count;
//				byte[] data = new byte[BUFFER];
//				
//				String filename = ze.getName();
//				
//				// Handle directory - can ignore them if we're just converting PDFs
//				if(!ze.isDirectory()) {
//					if(testing) {
//						System.out.println(textRepository.existsByEisdocAndFilename(eis, filename));
//					}
//					
//					if(textRepository.existsByEisdocAndFilename(eis, filename) && !testing) {
//						zis.closeEntry();
//					} else {
//						
//						if(testing) {
//							System.out.println("Extracting " + ze);
//						}
//						
//						// 2: Extract data and stream to Tika
//						try {
//							ByteArrayOutputStream baos = new ByteArrayOutputStream();
//							while (( count = zis.read(data)) != -1) {
//								baos.write(data, 0, count);
//							}
//							
//							// 3: Convert to text
//							System.out.println(pdfParseToXML(new ByteArrayInputStream(baos.toByteArray())));
//							results.add(pdfParseToXML(new ByteArrayInputStream(baos.toByteArray())));
//							
//						} catch(Exception e){
//							e.printStackTrace();
//						} finally { // while loop handles getNextEntry()
//							zis.closeEntry();
//						}
//					}
//				}
//			
//			}
//
//			// 5: Cleanup
//			in.close();
//			zis.close();
//
//			return results;
//		} catch (Exception e) {
//			e.printStackTrace();
//			return results;
//		}
//	}
	
	/** Returns top EISDoc if database contains at least one instance of a title/type/date combination */
	private Optional<EISDoc> getEISDocByTitleTypeDate(@RequestParam String title, @RequestParam String type, @RequestParam String date) {
		// Original dataset has no apostrophes
		String noApostropheTitle = title.replaceAll("'", "");
		
		Optional<EISDoc> docToReturn = docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(title.trim(), type.trim(), LocalDate.parse(date));
		if(!docToReturn.isPresent()) {
			// Try without apostrophes?
			docToReturn = docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(noApostropheTitle.trim(), type.trim(), LocalDate.parse(date));
		}
		
		return docToReturn;
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
	
	/** Turns UploadInputs into valid EISDoc and saves to database, returns new ID and 200 (OK) or null and 500 (error) */
	private ResponseEntity<Long> saveDto(UploadInputs itr) {
		
		
		// translate
		EISDoc newRecord = new EISDoc();
		newRecord.setAgency(org.apache.commons.lang3.StringUtils.normalizeSpace(itr.agency));
		newRecord.setDocumentType(org.apache.commons.lang3.StringUtils.normalizeSpace(itr.document));
		newRecord.setFilename(itr.filename);
		newRecord.setCommentsFilename(itr.comments_filename);
		newRecord.setRegisterDate(LocalDate.parse(itr.federal_register_date));
		if(itr.epa_comment_letter_date == null || itr.epa_comment_letter_date.isBlank()) {
			// skip
		} else {
			newRecord.setCommentDate(LocalDate.parse(itr.epa_comment_letter_date));
		}
		newRecord.setState(org.apache.commons.lang3.StringUtils.normalizeSpace(itr.state));
		newRecord.setTitle(org.apache.commons.lang3.StringUtils.normalizeSpace(itr.title));
		newRecord.setFolder(itr.eis_identifier.trim());
		newRecord.setLink(itr.link.trim());
		newRecord.setNotes(itr.notes.trim());
		
		EISDoc savedRecord = docRepository.save(newRecord); // save to db
		
		if(savedRecord != null) {
			return new ResponseEntity<Long>(savedRecord.getId(), HttpStatus.OK);
		} else {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Expects matching record and new data; updates; preserves comments/comments date if no new values for those */
	private ResponseEntity<Long> updateDto(UploadInputs itr, Optional<EISDoc> existingRecord) {

		if(!existingRecord.isPresent()) {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		EISDoc oldRecord = existingRecord.get();
		// translate
		// at this point we've matched on agency and document type already
		oldRecord.setFilename(itr.filename);
		if(itr.comments_filename == null || itr.comments_filename.isBlank()) {
			// skip, leave original
		} else {
			oldRecord.setCommentsFilename(itr.comments_filename);
		}
		oldRecord.setRegisterDate(LocalDate.parse(itr.federal_register_date));
		if(itr.epa_comment_letter_date == null || itr.epa_comment_letter_date.isBlank()) {
			// skip, leave original
		} else {
			oldRecord.setCommentDate(LocalDate.parse(itr.epa_comment_letter_date));
		}
		oldRecord.setState(org.apache.commons.lang3.StringUtils.normalizeSpace(itr.state));
		oldRecord.setTitle(org.apache.commons.lang3.StringUtils.normalizeSpace(itr.title));
		oldRecord.setFolder(itr.eis_identifier.strip());
		oldRecord.setLink(itr.link.strip());
		oldRecord.setNotes(itr.notes.strip());
		
		docRepository.save(oldRecord); // save to db, ID shouldn't change
		
		return new ResponseEntity<Long>(oldRecord.getId(), HttpStatus.OK);
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

	/** Return whether JWT is from Curator role */
	private boolean isCurator(String token) {
		boolean result = false;
		ApplicationUser user = getUser(token);
		// get user
		if(user != null) {
			if(user.getRole().contentEquals("CURATOR")) {
				result = true;
			}
		}
		return result;
	}
	
	/** Return ApplicationUser given JWT String */
	private ApplicationUser getUser(String token) {
		if(token != null) {
			// get ID
			try {
				String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
					.getId();
				System.out.println("ID: " + id);

				ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
				System.out.println("User ID: " + user.getId());
				return user;
			} catch (Exception e) {
				return null;
			}
		} else {
			return null;
		}
	}
	
	public static String encodeURIComponent(String s) {
	    String result;

	    try {
	        result = URLEncoder.encode(s, "UTF-8")
	                .replaceAll("\\+", "%20")
	                .replaceAll("\\%21", "!")
	                .replaceAll("\\%27", "'")
	                .replaceAll("\\%28", "(")
	                .replaceAll("\\%29", ")")
	                .replaceAll("\\%7E", "~");
	    } catch (UnsupportedEncodingException e) {
	        result = s;
	    }

	    return result;
	}

	// Probably useless
//	private String pdfParseToXML(ByteArrayInputStream inputstream) {
//		ContentHandler handler = new ToXMLContentHandler();
//		Metadata metadata = new Metadata();
//		ParseContext pcontext = new ParseContext();
//
//		//parsing the document using PDF parser
//		PDFParser pdfparser = new PDFParser(); 
//		try {
//			pdfparser.parse(inputstream, handler, metadata,pcontext);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SAXException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (TikaException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		//getting the content of the document
////		System.out.println("Contents of the PDF :" + handler.toString());
//		return handler.toString();
//		
//		//getting metadata of the document
////		System.out.println("Metadata of the PDF:");
////		String[] metadataNames = metadata.names();
////		
////		for(String name : metadataNames) {
////		 System.out.println(name+ " : " + metadata.get(name));
////		}
//	}
	
}

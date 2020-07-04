package nepaBackend.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
				ApplicationUserRepository applicationUserRepository) {
		this.docRepository = docRepository;
		this.textRepository = textRepository;
		this.fileLogRepository = fileLogRepository;
		this.applicationUserRepository = applicationUserRepository;
	}
	
	// TODO: Set this as a global constant somewhere?  May be changed to SBS and then elsewhere in future
	// Can also have a backup set up for use if primary fails
	Boolean testing = true;
	static String dbURL = "http://mis-jvinaldbl1.catnet.arizona.edu:80/test/";
	String testURL = "http://localhost:5000/";
	String uploadTestURL = "http://localhost:3001/test2";
	static String uploadURL = "http://mis-jvinaldbl1.catnet.arizona.edu:5309/upload";

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
	private ResponseEntity<boolean[]> uploadFile(@RequestPart(name="file") MultipartFile file, 
								@RequestPart(name="doc") String doc, @RequestHeader Map<String, String> headers) 
										throws IOException { 
//		if(testing) {
//			System.out.println(doc);
//			return new ResponseEntity<boolean[]>(HttpStatus.OK);
//		}
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
//		    System.out.println(dto.document_type);
//		    System.out.println(dto.register_date);
		    LocalDate parsedDate = parseDate(dto.register_date);
			dto.register_date = parsedDate.toString();
		    dto.filename = origFilename;
		    // Ensure metadata is valid and doesn't exist before uploading, given upload should always work.
		    // For the valid but exists case, will add separate function to add files and/or update existing metadata.
			if(!isValid(dto) || recordExists(dto.title, dto.document_type, dto.register_date)) {
				return new ResponseEntity<boolean[]>(results, HttpStatus.BAD_REQUEST);
			}
	    	
			
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
		    System.out.println(response.toString());
		    
		    // so far so good?
		    results[0] = (response.getStatusLine().getStatusCode() == 200);

		    // Only save if file upload succeeded.  We don't want loose files, and we don't want metadata without files.
		    if(results[0]) {
		    	// Log
			    uploadLog.setFilename(file.getOriginalFilename());
			    uploadLog.setUser(getUser(token));
			    uploadLog.setLogTime(LocalDateTime.now());
			    uploadLog.setErrorType("Uploaded");
			    
			    // Since metadata is already validated, this should always work.
		    	EISDoc saveDoc = new EISDoc();
		    	saveDoc.setAgency(dto.agency.trim());
		    	saveDoc.setDocumentType(dto.document_type.trim());
		    	
			    // TODO: Get file paths from Express.js server and link up metadata beyond just filename
		    	// TODO: Handle different file formats for download (PDF; multiple files)
		    	// TODO: Handle multiple files per record here and then need to also redesign downloads to allow it
		    	saveDoc.setFilename(file.getOriginalFilename());
		    	if(dto.register_date.length()>9) {
			    	saveDoc.setRegisterDate(LocalDate.parse(dto.register_date));
		    	} else {
			    	saveDoc.setRegisterDate(null);
		    	}
		    	saveDoc.setState(dto.state);
		    	saveDoc.setTitle(dto.title.trim());
		    	
		    	saveDoc.setCommentDate(null); // Useless field now?
		    	// TODO: May need to retool this if adding comments files becomes a thing
		    	saveDoc.setCommentsFilename("");
		    	
		    	// Save (ID is null at this point, but .save() picks a unique ID thanks to the model so it's good)
		    	EISDoc savedDoc = docRepository.save(saveDoc); // note: JPA .save() is safe from sql injection
		    	results[1] = true;

			    uploadLog.setDocumentId(savedDoc.getId());
			    
		    	// Run Tika on file, record if 200 or not
		    	if(origFilename.substring(origFilename.length()-3).equalsIgnoreCase("pdf")) {
		    		int status = this.convertPDF(savedDoc).getStatusCodeValue();
			    	results[2] = (status == 200);
		    	} else { // Archive or image case (not set up to attempt image conversion, may have issues with non-.zip archives)
			    	int status = this.convertRecordSmart(savedDoc).getStatusCodeValue();
			    	results[2] = (status == 200);
			    	// Note: 200 doesn't necessarily mean tika was able to convert anything
		    	}
		    	
		    	if(results[2]) {
				    uploadLog.setImported(true);
		    	}


		    	// TODO: Verify Tika converted; verify Lucene indexed (should happen automatically)
		    	// Then make an Express server on DBFS, change URL, test live
		    }
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		    file.getInputStream().close();
		    if(uploadLog.getUser() != null) { // if we have a user then it was at least uploaded
		    	fileLogRepository.save(uploadLog);
		    }
		}

		return new ResponseEntity<boolean[]>(results, HttpStatus.OK);
	}
	
	@CrossOrigin
	@RequestMapping(path = "/uploadFiles", method = RequestMethod.POST, consumes = "multipart/form-data")
	private void uploadFiles(@RequestPart(name="files") MultipartFile[] files, 
								@RequestPart(name="doc") String doc, @RequestHeader Map<String, String> headers) 
										{ 
		System.out.println(files.length);
		System.out.println(files[0].getOriginalFilename());
		/** TODO: Same logic as uploadFile except: 
		 * - multiple Files
		 * - each File's .getOriginalFilename() should include a relative path thanks to the frontend javascript logic
		 * - save base /folder name in the folder column, if it comes with a folder
		 * - express service will need to parse each /folder, ensure they exist, create if not, then put the /filename.ext 
		 * in the deepest folder
		 * - if no folder, express will have to make the path based on the new ID from saving the metadata doc, 
		 * type, and agency, so will need to send those values also...  but it also has to ensure no collisions,
		 * so it needs to actually make sure the identifying directory does NOT exist already and iterate until it finds
		 * one that doesn't exist, then express has to return the unique name it came up with, then this controller has to
		 * save that folder name to the record
		 * - next, download logic needs to change to expect structure of agency/doc.folder name/type, and multiple files
		 * - Finally, for bulk upload with or without CSV, can use same dropzone, same path as filename logic, different route
		 */
	}
	
	
	
	
	
	

	/** Check that required fields exist (doesn't verify document type from a list of acceptable types) */
	private boolean isValid(UploadInputs dto) {
		boolean valid = true;
		// Choice: Agency/state required also?
		// Check for null
		if(dto.register_date == null || dto.title == null || dto.document_type == null || dto.filename == null) {
			valid = false;
			return valid; // Just stop here and don't have to worry about validating null values
		}
		
		// Check for empty
		if(dto.title.trim().length()==0) {
			valid = false; // Need title
		}

		if(dto.document_type.trim().length()==0) {
			valid = false; // Need type
		}
		
		if(dto.filename.trim().length()>0) {
			// TODO: Ensure filename matches unique item on imported files list
		} else {
			valid = false;
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
	private ResponseEntity<List<String>> uploadCSV(@RequestPart(name="csv") String csv, @RequestHeader Map<String, String> headers) 
										throws IOException { 
//		System.out.println(csv);
		
		String token = headers.get("authorization");
		
		if(!isCurator(token) && !isAdmin(token)) 
		{
			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
		} 
		List<String> results = new ArrayList<String>();

	    
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
		    UploadInputs dto[] = mapper.readValue(csv, UploadInputs[].class);

		    // Ensure metadata is valid
			int count = 0;
			for (UploadInputs itr : dto) {
			    // Choice: Do we want to validate CSV entries at all?
			    if(isValid(itr)) {

			    	boolean error = false;
					try {
						LocalDate parsedDate = parseDate(itr.register_date);
						itr.register_date = parsedDate.toString();
					} catch (IllegalArgumentException e) {
						results.add("Item " + count + ": " + e.getMessage());
						error = true;
					} catch (Exception e) {
						results.add("Item " + count + ": Error " + e.getMessage());
						error = true;
					}
					if(!error) {
						if(!recordExists(itr.title, itr.document_type, itr.register_date)) { // Deduplication
						    ResponseEntity<Long> status = saveDto(itr);
					    	// TODO: What are the most helpful results to return?  Just the failures?  Duplicates also?
					    	if(status.getStatusCodeValue() == 500) { // Error
								results.add("Item " + count + ": Error saving: " + itr.title);
					    	} else {
					    		if(testing) {
						    		results.add("Item " + count + ": OK: " + itr.title);
					    		}

					    		// Log successful record import (need accountability for new metadata)
								FileLog recordLog = new FileLog();
					    		recordLog.setDocumentId(status.getBody());
					    		recordLog.setFilename(itr.filename);
					    		recordLog.setImported(false);
					    		recordLog.setLogTime(LocalDateTime.now());
					    		recordLog.setUser(getUser(token));
					    		fileLogRepository.save(recordLog);
					    	}
				    	} else {
							results.add("Item " + count + ": Duplicate");
				    	}
					}
			    } else {
					results.add("Item " + count + ": Missing one or more fields: register_date/document_type/filename/title");
			    }
			    count++;
			}
	    	// TODO: Run Tika on new files later, record results (need new bulk file import function for this part)
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<List<String>>(results, HttpStatus.OK);
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
			InputStream in = new BufferedInputStream(fileURL.openStream());
			ZipInputStream zis = new ZipInputStream(in);
			ZipEntry ze;
			
			while((ze = zis.getNextEntry()) != null) {
				int count;
				byte[] data = new byte[BUFFER];
				
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
			// TODO: Handle folders/multiple files for future (currently only archives/PDFs)
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
		return docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(title.trim(), type.trim(), LocalDate.parse(date)).isPresent();
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
	
	/** Turns UploadInputs into valid EISDoc and saves to database, returns new ID and 200 (OK) or null and 500 (error) */
	private ResponseEntity<Long> saveDto(UploadInputs itr) {
		EISDoc newRecord = new EISDoc();
		newRecord.setAgency(itr.agency.trim());
		newRecord.setDocumentType(itr.document_type.trim());
		newRecord.setFilename(itr.filename.trim());
		newRecord.setCommentsFilename(itr.comments_filename);
		newRecord.setRegisterDate(LocalDate.parse(itr.register_date));
		newRecord.setState(itr.state.trim());
		newRecord.setTitle(itr.title.trim());
		EISDoc savedRecord = docRepository.save(newRecord);
		if(savedRecord != null) {
			return new ResponseEntity<Long>(savedRecord.getId(), HttpStatus.OK);
		} else {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
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

package nepaBackend.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.tika.Tika;
//import org.apache.tika.exception.TikaException;
//import org.apache.tika.metadata.Metadata;
//import org.apache.tika.parser.ParseContext;
//import org.apache.tika.parser.pdf.PDFParser;
//import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import nepaBackend.CustomizedTextRepositoryImpl;
import nepaBackend.DocRepository;
import nepaBackend.DocService;
import nepaBackend.FileLogRepository;
import nepaBackend.Globals;
import nepaBackend.NEPAFileRepository;
import nepaBackend.TextRepository;
import nepaBackend.ZipExtractor;
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

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);
	
	private DocRepository docRepository;
	private TextRepository textRepository;
	private FileLogRepository fileLogRepository;
	private ApplicationUserRepository applicationUserRepository;
	private NEPAFileRepository nepaFileRepository;
	
	private static DateTimeFormatter[] parseFormatters = Stream.of(
			"yyyy-MM-dd", "MM-dd-yyyy", "yyyy/MM/dd", "MM/dd/yyyy", 
			"yyyy-M-dd", "M-dd-yyyy", "yyyy/M/dd", "M/dd/yyyy", 
			"yyyy-MM-d", "MM-d-yyyy", "yyyy/MM/d", "MM/d/yyyy", 
			"yyyy-M-d", "M-d-yyyy", "yyyy/M/d", "M/d/yyyy", 
			"yy-MM-dd", "MM-dd-yy", "yy/MM/dd", "MM/dd/yy", 
			"yy-M-dd", "M-dd-yy", "yy/M/dd", "M/dd/yy", 
			"yy-MM-d", "MM-d-yy", "yy/MM/d", "MM/d/yy", 
			"yy-M-d", "M-d-yy", "yy/M/d", "M/d/yy", 
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
			.map(DateTimeFormatter::ofPattern)
			.toArray(DateTimeFormatter[]::new);
	
	private static Map<String,String> agencies = new HashMap<String, String>();

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
	
	private static boolean testing = Globals.TESTING;
	
	private static String dbURL = Globals.DOWNLOAD_URL;
	private static String testURL = "http://localhost:5000/";
	
	private static String uploadURL = Globals.UPLOAD_URL.concat("uploadFilesTest");
	private static String uploadTestURL = "http://localhost:5309/uploadFilesTest";
	
//	private static String uploadTestURL = "http://localhost:5309/uploadFilesTest";

	/** Check all possible file sizes for entities with filenames or folders */
	@CrossOrigin
	@RequestMapping(path = "/filesizes", method = RequestMethod.GET)
	public ResponseEntity<String> filesizes() {
		try {
			List<EISDoc> files = docRepository.findAll();
			
			for(EISDoc doc : files) {
				String folder = doc.getFolder();
				String filename = doc.getFilename();
				if(folder != null && folder.strip().length() > 0) {
					addUpFolderSize(doc);
				}
				else if(filename != null && filename.strip().length() > 0) {
					Long response = (getFileSizeFromFilename(doc.getFilename()).getBody());
					if(response != null) {
						doc.setSize(response);
					} 
//					else {
						// If file isn't found, set size to 0? Or leave it as null?
//						doc.setSize((long) 0);
//					}
					docRepository.save(doc);
				}
			}
			
			return new ResponseEntity<String>("OK", HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	

	/** Check all missing files for entities with filenames or folders, set new size if found */
	@CrossOrigin
	@RequestMapping(path = "/filesizes_missing", method = RequestMethod.GET)
	public ResponseEntity<String> filesizesMissing() {
		try {
			List<EISDoc> files = docRepository.findMissingSize();
			
			for(EISDoc doc : files) {
				String folder = doc.getFolder();
				String filename = doc.getFilename();
				if(folder != null && folder.strip().length() > 0) {
					addUpFolderSize(doc);
				}
				else if(filename != null && filename.strip().length() > 0) {
					Long response = (getFileSizeFromFilename(doc.getFilename()).getBody());
					if(response != null) {
						doc.setSize(response);
					} 
					else {
						// If file isn't found, set size to 0
						doc.setSize((long) 0);
					}
					docRepository.save(doc);
				}
			}
			
			return new ResponseEntity<String>("OK", HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Return list of "missing" files (no size on record) */
	@CrossOrigin
	@RequestMapping(path = "/missing_files", method = RequestMethod.GET)
	public ResponseEntity<List<Object[]>> missingFiles() {
		try {
			return new ResponseEntity<List<Object[]>>(docRepository.findMissingNames(),HttpStatus.OK);
		} catch (Exception e) {
			if(testing) {
				e.printStackTrace();
			}
			return new ResponseEntity<List<Object[]>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@CrossOrigin
	@RequestMapping(path = "/filenames", method = RequestMethod.GET)
	public ResponseEntity<List<String>> filenames(@RequestParam long document_id) {
		try {
			List<String> filenames = textRepository.findFilenameByDocumentId(document_id);
			return new ResponseEntity<List<String>>(filenames, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<String>>(HttpStatus.NOT_FOUND);
		}
	}

	@CrossOrigin
	@RequestMapping(path = "/downloadFile", method = RequestMethod.GET)
	public ResponseEntity<Void> downloadFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam String filename) {
		try {
			// TODO: if not .zip try adding .pdf first?  Client will need file type and we need to capture all the files to deliver
			// potentially in a zip
			URL fileURL = new URL(dbURL + encodeURIComponent(filename));
			if(testing) {
				System.out.println("Got ask for "+ filename + ": " + encodeURIComponent(filename));
				fileURL = new URL(testURL + encodeURIComponent(filename));
			}
			InputStream in = new BufferedInputStream(fileURL.openStream());
			long length = getFileSize(fileURL); // for Content-Length for progress bar
			
			response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\""); 
			response.addHeader("Content-Length", length + ""); 
			
			ServletOutputStream out = response.getOutputStream();
			IOUtils.copy(in, out);
			
			response.flushBuffer();
			
			in.close();
			return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
		} catch (Exception e) {
		// TODO: Log missing file errors in db file log?  Verify file doesn't exist and then remove filename from record?
//			StringWriter sw = new StringWriter();
//			PrintWriter pw = new PrintWriter(sw);
//			e.printStackTrace(pw);
//			e.printStackTrace();
//			String sStackTrace = sw.toString();
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}
	}

	/** Handles multiple files to download for an EISDoc record, given its ID */
	@CrossOrigin
	@RequestMapping(path = "/downloadFolder", method = RequestMethod.GET)
	public ResponseEntity<Void> downloadFolderById(HttpServletRequest request, HttpServletResponse response,
			@RequestParam String id) {
		ZipOutputStream zip = null;

		try {

			// Get full folder path from nepafile for eis (or path would work) and then zip that folder's contents
			List<NEPAFile> nepaFiles = nepaFileRepository.findAllByEisdoc(docRepository.findById(Long.parseLong(id)).get());
//			System.out.println(nepaFiles.size());
			if(nepaFiles.size() == 0) {
				return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
			}
			String downloadFilename = nepaFiles.get(0).getFolder() + "_" + nepaFiles.get(0).getDocumentType();
			downloadFilename = downloadFilename.replaceAll(" ", "_");
			
		    zip = new ZipOutputStream(response.getOutputStream());
			response.addHeader("Content-Disposition", "attachment; filename=\"" + downloadFilename + ".zip\""); 
			response.addHeader("Access-Control-Expose-Headers", "Content-Disposition,X-Decompressed-Content-Length,Transfer-Encoding");
			
			for(NEPAFile nepaFile : nepaFiles) {

				// Format URI properly (%20 for spaces in folder, file name...)
				String fullPath = encodeURIComponent(nepaFile.getRelativePath() + nepaFile.getFilename());
				
				URL fileURL = new URL(dbURL + fullPath);
				if(testing) {
					fileURL = new URL(testURL + fullPath);
				}

				InputStream in = new BufferedInputStream(fileURL.openStream());
				
				zip.putNextEntry(new ZipEntry(nepaFile.getFilename()));
		        int length;

		        byte[] b = new byte[2048];

		        while((length = in.read(b)) > 0) {
		            zip.write(b, 0, length);
		        }
		        
		        zip.closeEntry();
		        in.close();
			}
			// Issues with content-length because of transfer-encoding: chunked
//			response.addHeader("Content-Length", "" + fileSize + ""); 
//			response.setContentLength(fileSize);

			zip.flush();
			return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
		} catch(Exception e) {
			
			if(testing) {e.printStackTrace();}
			
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
		    try {
				zip.close();
			} catch (IOException e2) {
				if(testing) {e2.printStackTrace();}
			} catch (NullPointerException e3) {
				// No such folder
			}
		}
	}
	
	
	/** Handles individual downloads using nepafile path */
	@CrossOrigin
	@RequestMapping(path = "/download_nepa_file", method = RequestMethod.GET)
	public ResponseEntity<Void> downloadNepaFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam String id, String filename) {
		try {

			// Get full folder path from nepafile for eis (or path would work) and then zip that folder's contents
			List<NEPAFile> nepaFiles = nepaFileRepository.findAllByEisdoc(docRepository.findById(Long.parseLong(id)).get());
//			System.out.println("Size " + nepaFiles.size());
			if(nepaFiles.size() == 0) {
				return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
			}
			
			// Format URI properly (%20 for spaces in folder, file name...)
			String fullPath = encodeURIComponent(nepaFiles.get(0).getRelativePath() + filename);
			
			URL fileURL = new URL(dbURL + fullPath);
			if(testing) {
				fileURL = new URL(testURL + fullPath);
			}
			
			InputStream in = new BufferedInputStream(fileURL.openStream());
			long length = getFileSize(fileURL); // for Content-Length for progress bar
			
			response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\""); 
			response.addHeader("Content-Length", length + ""); 
			
			ServletOutputStream out = response.getOutputStream();
			IOUtils.copy(in, out);
			
			response.flushBuffer();

	        in.close();
			return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
		} catch(Exception e) {
			
			if(testing) {e.printStackTrace();}
			
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
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

	/** Return all file logs for an eisdoc */
	@CrossOrigin
	@RequestMapping(path = "/nepafiles", method = RequestMethod.GET)
	public ResponseEntity<List<NEPAFile>> getAllNEPAFilesByEISDocID(@RequestParam String id, @RequestHeader Map<String, String> headers) {
		try {
			List<NEPAFile> filesList = nepaFileRepository.findAllByEisdoc(docRepository.findById(Long.parseLong(id)).get());
			return new ResponseEntity<List<NEPAFile>>(filesList, HttpStatus.OK);
		} catch (Exception e) {
			// no file(s)
			return new ResponseEntity<List<NEPAFile>>(HttpStatus.NOT_FOUND);
		}
	}
	
	/** Return all document texts for an eisdoc */
	@CrossOrigin
	@RequestMapping(path = "/doc_texts", method = RequestMethod.GET)
	public ResponseEntity<List<DocumentText>> getAllTextsByEISDocID(@RequestParam String id, @RequestHeader Map<String, String> headers) {
//		System.out.println(id);
		List<DocumentText> docList = textRepository.findAllByEisdoc(docRepository.findById(Long.parseLong(id)).get());
		return new ResponseEntity<List<DocumentText>>(docList, HttpStatus.OK);
	}
	
	

	/** Run convertRecord for all IDs in db.  (Conversion handles null filenames 
	 * (of which there are none because they're empty strings by default) and deduplication). 
	 * Does not handle folders. */
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
//					if(testing) 
//					{
						resultList.add(doc.getId().toString() + ": " + this.convertRecord(doc)
						.getStatusCodeValue());
//					} 
//					else 
//					{
//						this.convertRecord(doc);
//					}
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
		    
		    if(dto.eis_identifier == null || dto.eis_identifier.isBlank()) {
		    	// This gets set later, but it can't be blank or null for the isValid test
		    	dto.eis_identifier = "temp";
		    }
		    
		    // Validate: File must exist, valid fields, and can't be duplicate
			if(file == null || !isValid(dto) || recordExists(dto.title, dto.document, dto.federal_register_date)) {
				if(Globals.TESTING) {
					System.out.println(file == null);
					System.out.println(!isValid(dto));
					System.out.println(recordExists(dto.title, dto.document, dto.federal_register_date));
				}
				return new ResponseEntity<boolean[]>(results, HttpStatus.BAD_REQUEST);
			}
			
	    	// Start log
		    uploadLog.setUser(getUser(token));
		    uploadLog.setLogTime(LocalDateTime.now());
		    
		    if(dto.link == null) {
		    	dto.link = "";
		    }
		    if(dto.notes == null) {
		    	dto.notes = "";
		    }
		    
		    // Since metadata is already validated, this should always save successfully with a db connection.
	    	EISDoc savedDoc = saveMetadata(dto); // note: JPA .save() is safe from sql injection
	    	savedDoc.setFolder(savedDoc.getId().toString()); // Use ID as folder

	    	// saved?
	    	results[0] = (savedDoc != null);
	    	if(results[0]) {
	    		uploadLog.setErrorType("Saved");

			    uploadLog.setDocumentId(savedDoc.getId());
			    
			    HttpEntity entity = MultipartEntityBuilder.create()
						.addTextBody("filepath",
								"/" + savedDoc.getId().toString() + "/") // Feed Express path to use
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
			    	// Save NEPAFile
			    	NEPAFile savedNepaFile = handleNEPAFileSave(origFilename, savedDoc, dto.document);
		    		uploadLog.setErrorType("Uploaded");
		    		uploadLog.setFilename(origFilename);
				    
			    	// Run Tika on file, record if 200 or not
				    results[2] = (this.convertNEPAFile(savedNepaFile).getStatusCodeValue() == 200);
				    
			    	// converted to fulltext?  Or at least no Tika errors converting
				    uploadLog.setImported(results[2]);
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
		
    	// This gets set later, but it can't be blank or null for the isValid test
    	dto.eis_identifier = "temp";

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
	    	} else {
	    		savedDoc.setFolder(savedDoc.getId().toString());
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
			    if(getUniqueFolderName(origFilename, savedDoc).equalsIgnoreCase(savedDoc.getId().toString())) { // If we generated the folder ourselves
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
			    	NEPAFile savedNepaFile = handleNEPAFileSave(origFilename, savedDoc, dto.document);
			    	
			    	// Save FileLog
				    uploadLog.setFilename(origFilename);
				    uploadLog.setUser(getUser(token));
				    uploadLog.setLogTime(LocalDateTime.now());
				    uploadLog.setErrorType("Uploaded");
				    uploadLog.setDocumentId(savedDoc.getId());
				    
			    	// Run Tika on file, record if 200 or not
				    converted = (this.convertNEPAFile(savedNepaFile).getStatusCodeValue() == 200);
			    	
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
	 * Takes .csv file with required headers and imports each valid, non-duplicate record.  Updates existing records
	 * 
	 * Valid records: Must have title/register_date/type; filename or folder; 
	 * register_date must conform to one of the formats in parseFormatters[]
	 * 
	 * @return List of strings with message per record (zero-based list) indicating success/error/duplicate 
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
		
		boolean shouldImport = true;
		
		List<String> results = doCSVImport(csv, shouldImport, token);
		
		return new ResponseEntity<List<String>>(results, HttpStatus.OK);
	}



	/** 
	 * Dummy version of /uploadCSV doesn't add anything to the database.
	 * 
	 * @return List of strings with message per record (zero-based) indicating would-be 
	 * success/error/duplicate and potentially more details */
	@CrossOrigin
	@RequestMapping(path = "/uploadCSV_dummy", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importCSVDummy(@RequestPart(name="csv") String csv, 
				@RequestHeader Map<String, String> headers) 
				throws IOException { 
		
		String token = headers.get("authorization");
		
		if(!isCurator(token) && !isAdmin(token)) 
		{
			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
		} 
		
		// "don't actually import, just tell me what would happen"
		boolean shouldImport = false;
		
		List<String> results = doCSVImport(csv, shouldImport, token);
		results.set(0, "Dummy results follow (database is unchanged):\n" + results.get(0) );
		
		return new ResponseEntity<List<String>>(results, HttpStatus.OK);
	}


	/** Logic for creating/updating/skipping metadata records.
	 * 
	 * No match: Create new if valid. 
	 * Match: Update if no existing filename && folder, else skip
	 * @return list of results (dummy results if shouldImport == false)
	 * */
	private List<String> doCSVImport(String csv, boolean shouldImport, String token) {

		// this should be impossible, but just in case
		if(!isCurator(token) && !isAdmin(token)) 
		{
			return new ArrayList<String>();
		} 
		
		fillAgencies();
		
		List<String> results = new ArrayList<String>();
		
	    // Expect some or all of these headers:
	    // Title, Document, EPA Comment Letter Date, Federal Register Date, Agency, State, 
		// EIS Identifier, Filename, Link, Notes, Force Update
	    // TODO: Translate these into a standard before proceeding? Such as both Type or Document Type 
		// instead of necessarily Document (this would require editing the first line of the csv String?)
		
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
		    UploadInputs dto[] = mapper.readValue(csv, UploadInputs[].class);

		    // Ensure metadata is valid
			int count = 0;
			for (UploadInputs itr : dto) {
				
				// Handle any leading/trailing invisible characters, double spacing
				itr.title = Globals.normalizeSpace(itr.title);
				// Handle any agency abbreviations
				itr.agency = agencyAbbreviationToFull(Globals.normalizeSpace(itr.agency));
				
				itr.state = Globals.normalizeSpace(itr.state);
				itr.document = Globals.normalizeSpace(itr.document);
				
			    // Choice: Need at least title, date, type for deduplication (can't verify unique item otherwise)
			    if(isValid(itr)) {

			    	// Save only valid dates
			    	boolean error = false;
					try {
						LocalDate parsedDate = parseDate(itr.federal_register_date);
						itr.federal_register_date = parsedDate.toString();
					} catch (IllegalArgumentException e) {
						System.out.println("Threw IllegalArgumentException");
						results.add("Item " + count + ": " + e.getMessage());
						error = true;
					} catch (Exception e) {
						results.add("Item " + count + ": Error " + e.getMessage());
						error = true;
					}

					try {
						if(itr.epa_comment_letter_date != null && itr.epa_comment_letter_date.length() > 0) {
							itr.epa_comment_letter_date = parseDate(itr.epa_comment_letter_date).toString();
						}
					} catch (Exception e) {
						// Since this field is optional, we can just proceed
					}
					
					if(!error) {
						// Deduplication or get match for possible update
						
						Optional<EISDoc> recordThatMayExist = getEISDocByTitleTypeDate(itr.title, itr.document, itr.federal_register_date);
						
						
						// If record exists but has no filename and no folder, then update it instead of skipping
						// This is because current data is based on having a .zip or a folder listed, 
						// so new data can add files where there are none, without adding redundant data when there is data.
						// If the user insists (force update header exists, and value of "Yes" for it) we will update it anyway.
						if(recordThatMayExist.isPresent() && 
								((recordThatMayExist.get().getFilename().isBlank() && recordThatMayExist.get().getFolder().isBlank()) || 
										(itr.force_update != null && itr.force_update.equalsIgnoreCase("yes")) )
						) {
							ResponseEntity<Long> status = new ResponseEntity<Long>(HttpStatus.OK);
							if(shouldImport) {
								status = updateDto(itr, recordThatMayExist);
							}
							
							if(status.getStatusCodeValue() == 500) { // Error
								results.add("Item " + count + ": Error saving: " + itr.title);
					    	} else {
								results.add("Item " + count + ": Updated: " + itr.title);

								if(shouldImport) {
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
					    	}
						}
						// If file doesn't exist, then create new record
						else if(!recordThatMayExist.isPresent()) { 
							ResponseEntity<Long> status = new ResponseEntity<Long>(HttpStatus.OK);
							if(shouldImport) {
								status = updateDto(itr, recordThatMayExist);
							}
					    	// TODO: What are the most helpful results to return?  Just the failures?  Duplicates also?
					    	if(status.getStatusCodeValue() == 500) { // Error
								results.add("Item " + count + ": Error saving: " + itr.title);
					    	} else {
					    		results.add("Item " + count + ": Created: " + itr.title);

					    		if(shouldImport) {
						    		// Log successful record import (need accountability for new metadata)
									FileLog recordLog = new FileLog();
						    		recordLog.setDocumentId(status.getBody());
						    		recordLog.setFilename(itr.filename);
						    		recordLog.setImported(false);
						    		recordLog.setLogTime(LocalDateTime.now());
						    		recordLog.setUser(getUser(token));
						    		fileLogRepository.save(recordLog);
					    		}
					    	}
//							}
				    	} else {
							results.add("Item " + count + ": Duplicate (no action): " + itr.title);
				    	}
					}
			    } else {
					results.add("Item " + count + ": Missing one or more required fields: Federal Register Date/Document/EIS Identifier/Title");
			    }
			    count++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			results.add(e.getLocalizedMessage());
		}

		return results;
	}


	/** 
	 * Upload more than one directory already associated with a CSV 
	 * (or will be in the future and has a unique foldername).
	 * 
	 * For each file, assumes a base folder that ends in a number (identifying folder)
	 * If that doesn't exist, rejects that file because it can't ever be automatically connected to a record 
	 * (would be forever unlinked/orphaned)
	 * 
	 * Or, upload one or more archives. It's extracted to a self-named folder automatically and each
	 * internal file is also converted and indexed after getting their own NEPAFile record.
	 * */
	@CrossOrigin
	@RequestMapping(path = "/uploadFilesBulk", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<String[]> importFilesBulk(@RequestPart(name="files") MultipartFile[] files, 
								@RequestHeader Map<String, String> headers) 
										throws IOException 
	{ 
		/** Validation: Files; auth  */
		
		if(files == null || files.length == 0) { // 400
			return new ResponseEntity<String[]>(HttpStatus.BAD_REQUEST);
		}
		
		String token = headers.get("authorization");
		if(!isCurator(token) && !isAdmin(token)) // 401
		{
			return new ResponseEntity<String[]>(HttpStatus.UNAUTHORIZED);
		} 
		
		String[] results = new String[files.length];
		
		/** If valid: Upload files, save to files table, add to existing records if possible, and log */
		
		for(int i = 0; i < files.length; i++) {
			
	    	FileLog uploadLog = new FileLog();

		    try {
			    String origFilename = files[i].getOriginalFilename();
			    String folderName = getUniqueFolderNameOrEmpty(origFilename);
			    
			    if(folderName.length() == 0) { // If no folder name:
			    	boolean missingZip = false;
			    	
			    	// If filename, however:
			    	Optional<EISDoc> foundDoc = docRepository.findTopByFilename(origFilename);
			    	if(!foundDoc.isPresent()) {
			    		// Nothing?  Try removing .zip from filename, if it exists.
			    		if(origFilename.length() > 4 
			    				&& origFilename.substring(origFilename.length() - 4).equalsIgnoreCase(".zip")) {
				    		foundDoc = docRepository.findTopByFilename(origFilename.substring(0, origFilename.length()-4));
				    		missingZip = true;
			    		}
			    	}
			    	if(foundDoc.isPresent()) {
			    		// If found we can link this to something, therefore proceed with upload

			    		// We're setting the directory to / for the archive upload.  Plan to extract later
			    		String savePath = "/";
			    		
					    // Upload file
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
			
					    // If file uploaded, proceed to import and logging
					    if(uploaded) {
					    	results[i] = "OK: " + files[i].getOriginalFilename();

					    	// Need to update EISDoc for size and to look in the correct place.
					    	EISDoc existingDoc = foundDoc.get();
					    	existingDoc.setFolder(origFilename);
					    	if(missingZip) {
					    		// We'll want to change the database filename to include the extension
					    		existingDoc.setFilename(origFilename);
					    	}
					    	
					    	// Find out file size
							Long sizeResponse = (getFileSizeFromFilename(origFilename).getBody());
							if(sizeResponse != null) {
								existingDoc.setSize(sizeResponse);
							}
					    	
							// Update
					    	EISDoc savedDoc = docRepository.save(existingDoc);
					    	
					    	// Extract to folder, creating NEPAFiles along the way as needed,
					    	// also converting and indexing the texts when possible
					    	boolean convert = true;
					    	boolean skipIfHasFolder = false;
					    	results[i] = "Extract/convert result: " + extractOneZip(savedDoc, skipIfHasFolder, convert);
					    	
					    	// TODO: Logging
					    	
//					    	if(savedNEPAFile == null) {
//						    	results[i] = "Duplicate (File exists, nothing done): " + origFilename;
//					    		// Duplicate, nothing else to do.  Could log that nothing happened if we want to
//					    	} else {
//					    		uploadLog.setFilename(savePath + origFilename); // full path
//							    uploadLog.setUser(getUser(token));
//							    uploadLog.setLogTime(LocalDateTime.now());
//							    uploadLog.setErrorType("Uploaded");
//							    // Note: Should be impossible not to have a linked document if the logic got us here
//							    uploadLog.setDocumentId(foundDoc.get().getId());
//						    
//						    	// Run Tika on file, record if 200 or not
//							    converted = (this.convertNEPAFile(savedNEPAFile).getStatusCodeValue() == 200);
//							    
//						    	if(converted) {
//								    uploadLog.setImported(true);
//						    	}
//					    	}
					    } else {
					    	// File already exists in legacy style
					    	results[i] = "Couldn't upload: " + origFilename;
					    }
			    	} else {
				    	// Do nothing (reject file: no upload, no log)
				    	results[i] = "No folder and no filename match: " + files[i].getOriginalFilename();
			    	}
			    } else if(metadataExists(folderName)){
			    	logger.info("Uploading " + files[i].getOriginalFilename());
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
		
				    CloseableHttpClient client = HttpClientBuilder.create().build();
				    HttpResponse response = client.execute(request);
				    
				    if(testing) {
					    System.out.println(response.toString());
				    }
				    
				    boolean uploaded = (response.getStatusLine().getStatusCode() == 200);
				    boolean converted = false;
				    client.close();
		
				    // If file uploaded, see if we can link it, then proceed to saving to table and logging
				    List<EISDoc> existingDocs = docRepository.findAllByFolder(folderName);
				    if(uploaded) {
				    	logger.info("Done uploading " + files[i].getOriginalFilename());
				    	results[i] = "OK: " + files[i].getOriginalFilename();
				    	// Save NEPAFile

				    	// 1. Requires ability to link from previous CSV import
				    	// 2. Type would be the directory after the unique folder name, use that if it exists.
				    	// 3. If no directory after that, use existingDoc's (if existingDoc exists).  
				    	// Otherwise leave type empty
				    	// 4. While folder should be unique if non-empty ideally, it's not fully enforced
				    	// (hopefully never actually happens or else someone messed up)
				    	// If it does happen, log and save to the first in the list found by JPA

				    	// This can throw a java.util.NoSuchElementException: No value present 
				    	// if we can't actually find a match for this foldername and document type
				    	NEPAFile savedNEPAFile = handleNEPAFileSave(origFilename, existingDocs);
				    	
				    	// Save FileLog
				    	
				    	if(savedNEPAFile == null) {
					    	results[i] = "Duplicate (File exists, nothing done): " + origFilename;
				    		// Duplicate, nothing else to do.  Could log that nothing happened if we want to
				    	} else {
				    		uploadLog.setFilename(getPathOnly(origFilename) + getFilenameOnly(origFilename)); // full path incl. filename with agency base folder subbed in if needed
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
				    } else {
				    	logger.info("Couldn't upload: " + origFilename);
				    	results[i] = "Couldn't upload: " + origFilename;
				    }
			    } else {
			    	// Inform client this file can't be linked to anything, and has been rejected
			    	logger.info("Can't link file (no folder or filename match in metadata): " + origFilename);
			    	results[i] = "Can't link file (no folder or filename match in metadata): " + origFilename;
			    }
	
		    } catch (java.util.NoSuchElementException e) {
				results[i] = "Can't link (no match for folder with document type): " + files[i].getOriginalFilename();
			} catch (Exception e) {
				e.printStackTrace();
		    	logger.error("Exception:: " + e.getMessage() + ": " + files[i].getOriginalFilename());
				results[i] = "Exception:: " + e.getMessage() + ": " + files[i].getOriginalFilename();
			} finally {
			    files[i].getInputStream().close();
			    if(uploadLog.getUser() != null) { 
			    	fileLogRepository.save(uploadLog);
			    }
			}
		}
		
		if(testing) {
			for(String result: results) {
				System.out.println(result);
			}
		}
		
		return new ResponseEntity<String[]>(results, HttpStatus.OK);
	}

	


	/** Used for filename-only match. Returns null if duplicate */
	private NEPAFile handleNEPAFileSave(String origFilename, Optional<EISDoc> foundDoc) {
		EISDoc existingDoc = foundDoc.get();
		if(existingDoc == null) {
			// probably impossible
			return null;
		}
		
		boolean duplicate = nepaFileRepository.existsByFilenameAndEisdocIn(origFilename, existingDoc);
		
		if(duplicate) {
			return null;
		} else {
			NEPAFile fileToSave = new NEPAFile();
	    	NEPAFile savedFile = null;
	    	
	    	fileToSave.setFilename(origFilename);
	    	fileToSave.setFolder(origFilename);
	        fileToSave.setRelativePath("/" + origFilename + "/"); 

    		fileToSave.setEisdoc(foundDoc.get());
	    	
	    	fileToSave.setDocumentType(existingDoc.getDocumentType());
	    	
	    	savedFile = nepaFileRepository.save(fileToSave);
			return savedFile;
		}
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
		boolean duplicate = nepaFileRepository.existsByFilenameAndRelativePathIn(getFilenameOnly(fullPath), getPathOnly(fullPath));
		
		if(duplicate) {
			return null;
		} else {
			NEPAFile fileToSave = new NEPAFile();
	    	NEPAFile savedFile = null;
	    	
	    	String folderName = getUniqueFolderNameOrEmpty(fullPath);
	    	String documentType = getDocumentTypeOrEmpty(fullPath);
	    	
	    	fileToSave.setFilename(getFilenameOnly(fullPath));
	    	fileToSave.setFolder(folderName);
	        fileToSave.setRelativePath(getPathOnly(fullPath)); 
			if(existingDocs.size()>0) {
				
				// If we matched on more than one record, we had better have a document type to differentiate them.
				// Should expect this anyway, ideally.
				// Records may be part of the same process and share a folder, but NEPAFiles are for one document type.
				Optional<EISDoc> existingDoc = docRepository.findTopByFolderAndDocumentTypeIn(folderName, documentType);

		        EISDoc docToUse = existingDoc.get();
				if(existingDoc.isPresent() && !folderName.isBlank() && !documentType.isBlank()) {
		    		fileToSave.setEisdoc(existingDoc.get());
		    	} else { // if we fail (this is not great), just link with the top of the list
			    	fileToSave.setEisdoc(existingDocs.get(0));
			    	docToUse = existingDocs.get(0);
		    	}
		    	
		    	
		    	// prefer to use directory document type, if exists
		    	if(documentType.isBlank()) {
			    	fileToSave.setDocumentType(existingDocs.get(0).getDocumentType());
		    	} else {
		    		fileToSave.setDocumentType(documentType);
		    	}
		    	
		    	savedFile = nepaFileRepository.save(fileToSave);
		    	addUpFolderSize(docToUse);
			}
			return savedFile;
		}
	}



	// Add up filesize.  Note: Spaces must be URI'd
	private boolean addUpFolderSize(EISDoc eisDoc) {
		// 1: Get all existing NEPAFiles by folder.
		List<NEPAFile> nepaFiles = nepaFileRepository.findAllByEisdoc(eisDoc);
		Long total = 0L;
		for(NEPAFile file : nepaFiles) {
			// 2: Iterate over each NEPAFile's relative path + filename, calling
			// getFileSizeFromFilename each time and adding up the Longs
			try {
				String pathURL = URLEncoder
						.encode(file.getRelativePath()+file.getFilename(), StandardCharsets.UTF_8.toString())
						.replace("+", "%20");

				Long sizeResponse = getFileSizeFromFilename(pathURL).getBody();
				if(sizeResponse != null) {
					total = total + sizeResponse;
				}
				if(Globals.TESTING) {System.out.println("Size of "+pathURL+": "+sizeResponse);}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		// 3: Save total to EISDoc.  Need to save totals by EISDoc ID,
		// because they can have the same base folder name but different type folders.
		eisDoc.setSize(total);
		docRepository.save(eisDoc);
		
		return true;
	}

	/** Given path, new/updated EISDoc and document_type, save new NEPAFile if not a duplicate. 
	 *  Uses getFilenameWithoutPath() to set filename and getUniqueFolderName() to set folder, uses identical logic 
	 *  for giving Express service the relative path to use (thus ensuring the download path is consistent for
	 *  both database and file directory on DBFS). */
	private NEPAFile handleNEPAFileSave(String relativePath, EISDoc savedDoc, String document_type) {
		boolean duplicate = true;
		duplicate = nepaFileRepository.existsByFilenameAndEisdocIn(getFilenameOnly(relativePath), savedDoc);
		
		if(duplicate) {
			return null;
		} else {
	    	NEPAFile fileToSave = new NEPAFile();
	    	fileToSave.setEisdoc(savedDoc);
	    	fileToSave.setFilename(getFilenameOnly(relativePath));
	    	fileToSave.setFolder(getUniqueFolderName(relativePath, savedDoc));
	    	/** TODO: Temporary logic until we get the path back from Express to guarantee consistency
	    	/* if we get the path wrong the system will fail to find the files 
	    	/* even when the NEPAFile has a correct foreign key */
	    	if(fileToSave.getFolder().equalsIgnoreCase(savedDoc.getId().toString())) { // If we generated the folder ourselves
	    		fileToSave.setRelativePath("/" + fileToSave.getFolder() + "/"); // Assume saved to /{ID}/
	    	} else { // Otherwise use provided path
	        	fileToSave.setRelativePath(getPathOnly(relativePath)); 
	    	}
	    	fileToSave.setDocumentType(document_type);
	    	return nepaFileRepository.save(fileToSave);
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

	/** e.g. /NSF_0001/etc/... --> /NSF/NSF_0001/etc/ (it adds a base agency folder if missing, based on the
	 * pre-underscore text of the identifying folder */
	private String getPathOnly(String pathWithFilename) {
		String result = "";
		
		
		// Check if we have agency folder, and if not, prepend it
		
		String[] folders = pathWithFilename.replaceAll("\\\\", "/").split("/");
		// Note: folders[0] is expected to be blank, or none of this is going to work anyway.
		String probableAgencyFolder = folders[1]; // e.g. NSF_00001
		if(probableAgencyFolder.contentEquals(getUniqueFolderNameOrEmpty(pathWithFilename))) {
			// Base folder is EIS Identifier: Need to interpret base folder (agency folder)
			// First part of EIS Identifier should be agency name followed by _, or user error
			String[] segments = pathWithFilename.split("_"); // e.g. [0]: /NSF, [1]: 00001/...
			result = segments[0]; // /NSF
		} else {
			// Agency folder already included, or user error
		}
		
		
		int idx = pathWithFilename.replaceAll("\\\\", "/").lastIndexOf("/");
		result += (idx >= 0 ? pathWithFilename.substring(0, idx + 1) : pathWithFilename);
		
		return result;
	}
	
	/** e.g. C:/ex/etc/test.pdf --> test.pdf */
	private String getFilenameOnly(String pathWithFilename) {
		int idx = pathWithFilename.replaceAll("\\\\", "/").lastIndexOf("/");
		return idx >= 0 ? pathWithFilename.substring(idx + 1) : pathWithFilename;
	}

	// Never used locally and probably useless 
	// (legacy code for converting a pdf based on an eis with a filename, before folder column)
//	private ResponseEntity<Void> convertPDF(EISDoc eis) {// Check to make sure this record exists.
//		if(eis == null) {
//			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
//		}
//		
//		FileLog fileLog = new FileLog();
//		try {
//			fileLog.setDocumentId(eis.getId());
//		} catch(Exception e) {
//			return new ResponseEntity<Void>(HttpStatus.I_AM_A_TEAPOT);
//		}
//		
//		
//		if(eis.getId() < 1) {
//			return new ResponseEntity<Void>(HttpStatus.UNPROCESSABLE_ENTITY);
//		}
//		
//		// Deduplication: Ignore when document ID already exists in EISDoc table
//		if(textRepository.existsByEisdoc(eis)) 
//		{
//			return new ResponseEntity<Void>(HttpStatus.FOUND);
//		} 
//		
//		// Note: There can be multiple files per archive, resulting in multiple documenttext records for the same ID
//		// but presumably different filenames with hopefully different conents
//		final int BUFFER = 2048;
//		
//		try {
//			Tika tikaParser = new Tika();
//			tikaParser.setMaxStringLength(-1); // disable limit
//		
//			// Make sure there is a file (for current data, no filename means nothing to convert for this record)
//			if(eis.getFilename() == null || eis.getFilename().length() == 0) {
//				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
//			}
//			String filename = eis.getFilename();
//			
//			String relevantURL = dbURL;
//			if(testing) {
//				relevantURL = testURL;
//			}
//			URL fileURL = new URL(relevantURL + filename);
//			
//			// 1: Download the file
//			InputStream in = new BufferedInputStream(fileURL.openStream());
//			
//			int count;
//			byte[] data = new byte[BUFFER];
//			
//			
//			if(textRepository.existsByEisdoc(eis)) 
//			{
//				in.close();
//				return new ResponseEntity<Void>(HttpStatus.ALREADY_REPORTED);
//			} 
//			else 
//			{
//				if(testing) {
//					System.out.println("Converting PDF");
//				}
//				DocumentText docText = new DocumentText();
//				docText.setEisdoc(eis);
//				docText.setFilename(filename);
//				
//				// 2: Extract data and stream to Tika
//				try {
//					ByteArrayOutputStream baos = new ByteArrayOutputStream();
//					while (( count = in.read(data)) != -1) {
//						baos.write(data, 0, count);
//					}
//					
//					// 3: Convert to text
//					String textResult = tikaParser.parseToString(new ByteArrayInputStream(baos.toByteArray()));
//					docText.setPlaintext(textResult);
//					
//					// 4: Add converted text to database for document(EISDoc) ID, filename
//					this.save(docText);
//				} catch(Exception e) {
//					// Log error
//					try {
//						if(docText.getPlaintext() == null || docText.getPlaintext().length() == 0) {
//							fileLog.setImported(false);
//						} else {
//							fileLog.setImported(true);
//						}
//						
//						fileLog.setErrorType(e.getLocalizedMessage());
//						fileLog.setLogTime(LocalDateTime.now());
//						fileLogRepository.save(fileLog);
//						e.printStackTrace();
//					} catch (Exception e2) {
//						if(testing) {
//							System.out.println("Error logging error...");
//							e2.printStackTrace();
//						}
//					}
//				} 
//			}
//
//			// 5: Cleanup
//			in.close();
//
//			return new ResponseEntity<Void>(HttpStatus.OK);
//		} catch (Exception e) {
//			e.printStackTrace();
//			try {
//				fileLog.setImported(false);
//				fileLog.setErrorType(e.getLocalizedMessage());
//				fileLog.setLogTime(LocalDateTime.now());
//				fileLogRepository.save(fileLog);
//			} catch (Exception e2) {
//				System.out.println("Error logging error...");
//				e2.printStackTrace();
//				return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
//			}
//			// could be IO exception getting the file if it doesn't exist
//			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}

	/** Null: Return 400; no filename: Return 204; no eis/folder: Return 404 */
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
			URL fileURL = new URL(relevantURL + encodeURIComponent(fullPath));
			
			if(testing) {
				System.out.println("FileURL " + fileURL.toString());
			}
			
			// Note: Multiple eisdocs may share the folder and therefore responsibility for this file.
			// So, the uniqueness should be determined by folder AND document type.
			// Therefore we need to get the EISDoc representing that, exactly.
			Optional<EISDoc> eis = docRepository.findTopByFolderAndDocumentTypeIn(savedNEPAFile.getFolder(), savedNEPAFile.getDocumentType());

			// If we anomalously can't get that exact match, give up now or risk corruption.
//			if(!eis.isPresent()) {
//				eis = docRepository.findTopByFolder(savedNEPAFile.getFolder());
//			}
			
			// Somehow no link at all?
			if(!eis.isPresent()) {
				return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
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
//				if(testing) {
//					System.out.println(textRepository.existsByEisdocAndFilename(nepaDoc, extractedFilename));
//				}
				
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


	/** Records with folders are deliberately skipped here; they should be handled by other functions. */
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
		if(eis.getId() < 1) { // we don't have a zero ID record currently
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
			// TODO: Records with folders are deliberately skipped for now.
			if(eis.getFilename() == null || eis.getFilename().length() == 0 
					|| (eis.getFolder() != null && eis.getFolder().length() > 0)) { 
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
//					if(testing) {
//						System.out.println(textRepository.existsByEisdocAndFilename(eis, filename));
//					}
					
					// Skip if we have this file text already
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
		} catch(FileNotFoundException fnfe) {
			// We won't log missing files every time this runs, but we will return a 404
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
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

	/** Return whether metadata exists for a given EIS Identifier (folder) */
	private boolean metadataExists(String folderName) {
		// TODO Auto-generated method stub
		long numRecords = docRepository.countByFolder(folderName);
		boolean exists = numRecords > 0;
		return exists;
	}

	/** Returns if database contains at least one instance of a title/type/date combination */
	@CrossOrigin
	@RequestMapping(path = "/existsTitleTypeDate", method = RequestMethod.GET)
	private boolean recordExists(@RequestParam String title, @RequestParam String type, @RequestParam String date) {
		// Original data has no apostrophes, so a better dupe check ORs the results of comparing both with the original title, and 
		// a title without apostrophes, specifically
		String noApostropheTitle = title.replaceAll("'", "");
		return (docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(title.trim(), type.trim(), parseDate(date)).isPresent()
				|| docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(noApostropheTitle.trim(), type.trim(), parseDate(date)).isPresent()
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
	
	/** Returns top EISDoc if database contains at least one instance of a title/type/date combination, accounts for
	 * missing apostrophes and commas also. */
	private Optional<EISDoc> getEISDocByTitleTypeDate(@RequestParam String title, @RequestParam String type, @RequestParam String date) {

		// Try without apostrophes (legacy data has no apostrophes?)
		String noApostropheTitle = title.replaceAll("'", "");
		Optional<EISDoc> docToReturn = docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(
				noApostropheTitle, 
				type.strip(), 
				parseDate(date));
		
		if(!docToReturn.isPresent()) {
			
			// Try without commas (legacy data has no commas?)
			String noCommaTitle = title.replaceAll(",", "");
			docToReturn = docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(
					noCommaTitle, 
					type.strip(), 
					parseDate(date));
			
			// Try without apostrophes or commas? (legacy data has no apostrophes)
			if(!docToReturn.isPresent()) {
				String noApostrophesCommasTitle = noCommaTitle.replaceAll("'", "");
				docToReturn = docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(
						noApostrophesCommasTitle, 
						type.strip(), 
						parseDate(date));
				
				// Try with unmodified title
				if(!docToReturn.isPresent()) {
					docToReturn = docRepository.findTopByTitleAndDocumentTypeAndRegisterDateIn(
							title, 
							type.strip(), 
							parseDate(date));
				} // else no match
			}
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
			return 0; // Consumer has to regard 0-size file as no file at all
//			throw new RuntimeException(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
	

	/** 
	 * Returns file size starting from base path if available.
	 * Ubuntu seems to return 178 bytes when a file is missing entirely, whereas windows returns a -1.
	 * So if we ever care about and have 178 byte-sized files on disk, maybe this is a problem.  Until then,
	 * the frontend should ignore that size as if it's 0 or -1.
	 **/
	@CrossOrigin
	@RequestMapping(path = "/file_size", method = RequestMethod.GET)
	public ResponseEntity<Long> getFileSizeFromFilename(@RequestParam String filename) {
		
		HttpURLConnection conn = null;
		URL url = null;
		
		try {
			url = new URL(dbURL + filename);
			if(testing) {
				url = new URL(testURL + filename);
			}
		} catch (MalformedURLException e1) {
			return new ResponseEntity<Long>((long) 0, HttpStatus.BAD_REQUEST);
//			throw new RuntimeException(e1);
		}
		
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("HEAD");
			
			return new ResponseEntity<Long>(conn.getContentLengthLong(), HttpStatus.OK);
		} catch (IOException e) {
			return new ResponseEntity<Long>((long) 0, HttpStatus.INTERNAL_SERVER_ERROR);
//			throw new RuntimeException(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
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

	/** Turns UploadInputs into valid EISDoc and saves to database, returns new ID and 200 (OK) or null and 500 (error) */
	private ResponseEntity<Long> saveDto(UploadInputs itr) {
		
		// "Multi" is actually counterproductive, will have to amend spec
		if(itr.filename != null && itr.filename.equalsIgnoreCase("multi")) {
			itr.filename = "";
		}
		
		// translate
		EISDoc newRecord = new EISDoc();
		newRecord.setAgency(Globals.normalizeSpace(itr.agency));
		newRecord.setDocumentType(Globals.normalizeSpace(itr.document));
		newRecord.setFilename(itr.filename);
		newRecord.setCommentsFilename(itr.comments_filename);
		if(itr.federal_register_date == null || itr.federal_register_date.isBlank()) {
			// skip
		} else {
			newRecord.setRegisterDate(parseDate(itr.federal_register_date));
		}
		if(itr.epa_comment_letter_date == null || itr.epa_comment_letter_date.isBlank()) {
			// skip
		} else {
			newRecord.setCommentDate(parseDate(itr.epa_comment_letter_date));
		}
		newRecord.setState(Globals.normalizeSpace(itr.state));
		newRecord.setTitle(Globals.normalizeSpace(itr.title));
		newRecord.setFolder(itr.eis_identifier);
		newRecord.setLink(itr.link);
		newRecord.setNotes(itr.notes);
		
		EISDoc savedRecord = docRepository.save(newRecord); // save to db
		
		if(savedRecord != null) {
			return new ResponseEntity<Long>(savedRecord.getId(), HttpStatus.OK);
		} else {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Expects matching record and new data; updates; preserves comments/comments date, state, agency if no new values for those */
	private ResponseEntity<Long> updateDto(UploadInputs itr, Optional<EISDoc> existingRecord) {

		if(!existingRecord.isPresent()) {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		EISDoc oldRecord = existingRecord.get();

		// "Multi" is actually counterproductive, will have to amend spec
		if(itr.filename != null && itr.filename.equalsIgnoreCase("multi")) {
			itr.filename = "";
		}

		// at this point we've matched on title, date and document type already but because of how we match on title
		// it can actually be slightly different and hopefully more accurate
		oldRecord.setTitle(Globals.normalizeSpace(itr.title));
		
		// this is redundant because the outer logic doesn't set the filename when one exists without force_update
		if(itr.force_update != null && itr.force_update.equalsIgnoreCase("Yes")) {
			/** 
			 * update even if new filename is blank/null, allowing user to potentially ungracefully unlink existing
			 *  files from metadata.  Also allows bulk fixing of past mistakes if they previously added a bunch of
			 *  invalid filenames.  So this can both break and fix the database depending on user, 
			 *  which is true of the import in general
			 */
			oldRecord.setFilename(itr.filename);
		} else if(itr.filename == null || itr.filename.isBlank()) {
			// skip, leave original
		} else {
			oldRecord.setFilename(itr.filename);
		}
		
		if(itr.comments_filename == null || itr.comments_filename.isBlank()) {
			// skip, leave original
		} else {
			oldRecord.setCommentsFilename(itr.comments_filename);
		}
		
//		oldRecord.setRegisterDate(LocalDate.parse(itr.federal_register_date));
		if(itr.epa_comment_letter_date == null || itr.epa_comment_letter_date.isBlank()) {
			// skip, leave original
		} else {
			try {
				oldRecord.setCommentDate(parseDate(itr.epa_comment_letter_date));
			} catch (IllegalArgumentException e) {
				// never mind
			}
		}
		
		if(itr.state != null && !itr.state.isBlank()) {
			oldRecord.setState(Globals.normalizeSpace(itr.state));
		}
		if(itr.agency != null && !itr.agency.isBlank()) {
			oldRecord.setAgency(Globals.normalizeSpace(itr.agency));
		}
		oldRecord.setFolder(itr.eis_identifier);
		oldRecord.setLink(itr.link);
		oldRecord.setNotes(itr.notes);
		
		docRepository.save(oldRecord); // save to db, ID shouldn't change
		
		return new ResponseEntity<Long>(oldRecord.getId(), HttpStatus.OK);
	}

	/** For Buomsoo's data, updates blank fields with seven new metadatums */
	private ResponseEntity<Long> updateDtoNoOverwrite(UploadInputs itr, EISDoc existingRecord) {

		existingRecord.setTitle(Globals.normalizeSpace(itr.title));
		
		if(itr.agency == null || itr.agency.isBlank()) {
			// skip, leave original
		} else {
			existingRecord.setAgency(Globals.normalizeSpace(itr.agency));
		}
		if(itr.state == null || itr.state.isBlank()) {
			// skip, leave original
		} else {
			existingRecord.setState(Globals.normalizeSpace(itr.state));
		}
		
		if(itr.department == null || itr.department.isBlank()) {
			// skip, leave original
		} else {
			existingRecord.setDepartment(Globals.normalizeSpace(itr.department));
		}
		if(itr.cooperating_agency == null || itr.cooperating_agency.isBlank()) {
			// skip, leave original
		} else {
			existingRecord.setCooperatingAgency(Globals.normalizeSpace(itr.cooperating_agency));
		}
		if(itr.summary_text == null || itr.summary_text.isBlank()) {
			// skip, leave original
		} else {
			existingRecord.setSummaryText(Globals.normalizeSpace(itr.summary_text));
		}
		
		if(itr.noi_date == null || itr.noi_date.isBlank()) {
			// skip, leave original
		} else {
			existingRecord.setNoiDate(parseDate(itr.noi_date));
		}
		if(itr.draft_noa == null || itr.draft_noa.isBlank()) {
			// skip, leave original
		} else {
			existingRecord.setDraftNoa(parseDate(itr.draft_noa));
		}
		if(itr.final_noa == null || itr.final_noa.isBlank()) {
			// skip, leave original
		} else {
			existingRecord.setFinalNoa(parseDate(itr.final_noa));
		}
		if(itr.first_rod_date == null || itr.first_rod_date.isBlank()) {
			// skip, leave original
		} else {
			existingRecord.setFirstRodDate(parseDate(itr.first_rod_date));
		}
		
		docRepository.save(existingRecord); // save to db, ID shouldn't change
		
		return new ResponseEntity<Long>(existingRecord.getId(), HttpStatus.OK);
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

	/** Check that required fields exist (doesn't verify document type from a list of acceptable types) */
	private boolean isValid(UploadInputs dto) {
	//		System.out.println(dto.title);
	//		System.out.println(dto.federal_register_date);
	//		System.out.println(dto.document);
	//		System.out.println(dto.filename);
		boolean valid = true;

		if(dto.federal_register_date == null || dto.title == null || dto.document == null) {
			valid = false;
			return valid; // Just stop here and don't have to worry about validating null values
		}
		// Check for empty
		if(dto.title.isBlank()) {
			valid = false; // Need title
		}

		if(dto.document.isBlank()) {
			valid = false; // Need type
		}
		
		// Don't require filename or EISID if force update==yes
		if(valid && dto.force_update != null && dto.force_update.equalsIgnoreCase("yes")) {
			return true;
		}
		
		// Expect EIS identifier or filename
		if( ( dto.eis_identifier == null || dto.eis_identifier.isBlank() ) 
				&& (dto.filename == null || dto.filename.isBlank() || dto.filename.equalsIgnoreCase("n/a") || dto.filename.equalsIgnoreCase("null"))) {
			valid = false;
			System.out.println("Filename fail");
			System.out.println(dto.filename);
			return valid;
		}

		return valid;
	}
	private boolean canMatch(UploadInputs dto) {
	//		System.out.println(dto.title);
	//		System.out.println(dto.federal_register_date);
	//		System.out.println(dto.document);
	//		System.out.println(dto.filename);
		boolean valid = true;

		if(dto.federal_register_date == null || dto.title == null || dto.document == null) {
			valid = false;
			return valid; // Just stop here and don't have to worry about validating null values
		}
		// Check for empty
		if(dto.title.isBlank()) {
			valid = false; // Need title
		}

		if(dto.document.isBlank()) {
			valid = false; // Need type
		}

		if(dto.federal_register_date.isBlank()) {
			valid = false; // Need date
		}

		return valid;
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
//				if(testing) {System.out.println("ID: " + id);}

				ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
//				if(testing) {System.out.println("User ID: " + user.getId());}
				return user;
			} catch (Exception e) {
				return null;
			}
		} else {
			return null;
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

	/** 
	 * Admin-only. 
	 * Takes .csv file with required headers and imports each valid record.  Updates existing records
	 * 
	 * Valid records: Must have title
	 * 
	 * @return List of strings with message per record (zero-based) indicating success/error 
	 * and potentially more details */
	@CrossOrigin
	@RequestMapping(path = "/uploadCSV_titles", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importCSVTitlesOnly(@RequestPart(name="csv") String csv, @RequestHeader Map<String, String> headers) 
										throws IOException { 
		
		String token = headers.get("authorization");
		
		fillAgencies();
		
		if(!isAdmin(token)) 
		{
			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
		} 
		List<String> results = new ArrayList<String>();
		
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
		    UploadInputs dto[] = mapper.readValue(csv, UploadInputs[].class);

			int count = 0;
			for (UploadInputs itr : dto) {
				
				// Handle any leading/trailing invisible characters, double spacing
				itr.title = Globals.normalizeSpace(itr.title);
				// Handle any agency abbreviations
				itr.agency = agencyAbbreviationToFull(itr.agency);
				// Handle any department abbreviations
				itr.department = agencyAbbreviationToFull(itr.agency);
				// Handle any cooperating agency abbreviations
				if(itr.cooperating_agency != null && itr.cooperating_agency.length() > 0) {
					String[] cooperating = itr.cooperating_agency.split(";");
					for(int i = 0; i < cooperating.length; i++) {
						cooperating[i] = agencyAbbreviationToFull(cooperating[i]);
					}
					itr.cooperating_agency = String.join(";", cooperating);
				}
				
				// Title-only option for Buomsoo's data, to update all title matches
			    
			    if(itr.title != null && itr.title.length() > 0) {

			    	// Parse the four new date values
					try {
						itr.noi_date = parseDate(itr.noi_date).toString();
						itr.first_rod_date = parseDate(itr.first_rod_date).toString();
						itr.draft_noa = parseDate(itr.draft_noa).toString();
						itr.final_noa = parseDate(itr.final_noa).toString();
					} catch (Exception e) {
						// Since this is optional, we can just proceed
					}
					List<EISDoc> matchingRecords = docRepository.findAllByTitle(itr.title);
					
					for(EISDoc record : matchingRecords) {

						/** Special update function that only adds new data */
						ResponseEntity<Long> status = updateDtoNoOverwrite(itr, record);
						
						if(status.getStatusCodeValue() == 500) { // Error
							results.add("Item " + count + ": Error saving: " + itr.title);
				    	} else {
							results.add("Item " + count + ": Updated: " + itr.title);

				    		// Log successful record update for accountability (can know last person to update an EISDoc)
							FileLog recordLog = new FileLog();
							recordLog.setErrorType("Updated existing record by title match");
				    		recordLog.setDocumentId(status.getBody());
				    		recordLog.setFilename(itr.filename);
				    		recordLog.setImported(false);
				    		recordLog.setLogTime(LocalDateTime.now());
				    		recordLog.setUser(getUser(token));
				    		fileLogRepository.save(recordLog);
				    	}
					}
				} else {
					results.add("Item " + count + ": Missing title");
				}
			    count++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<List<String>>(results, HttpStatus.OK);
	}

	/** 
	 * Admin-only.  Built to re-add missing commas and apostrophes.  Takes .tsv file with required headers and updates titles with incoming ones.
	 * 
	 * @return List of strings with message per record (zero-based) indicating success/error 
	 * and potentially more details */
	@CrossOrigin
	@RequestMapping(path = "/title_fix", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> titleFix(@RequestPart(name="csv") String csv, @RequestHeader Map<String, String> headers) 
										throws IOException { 
		
		fillAgencies();
		
		String token = headers.get("authorization");
		
		if(!isAdmin(token)) 
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
				
				// Handle any leading/trailing invisible characters, double spacing
				itr.title = Globals.normalizeSpace(itr.title);
				// Handle any agency abbreviations
				itr.agency = agencyAbbreviationToFull(itr.agency);
				
			    // Choice: Need at least title, date, type for deduplication (can't verify unique item otherwise)
			    if(canMatch(itr)) {

			    	// Save only valid dates
			    	boolean error = false;
					try {
						LocalDate parsedDate = parseDate(itr.federal_register_date);
						itr.federal_register_date = parsedDate.toString();
					} catch (IllegalArgumentException e) {
						System.out.println("Threw IllegalArgumentException");
						results.add("Item " + count + ": " + e.getMessage());
						error = true;
					} catch (Exception e) {
						results.add("Item " + count + ": Register Date Format Error " + e.getMessage());
						error = true;
					}
					
					if(!error) {
						// Deduplication
						
						Optional<EISDoc> recordThatMayExist = getEISDocByTitleTypeDate(itr.title, itr.document, itr.federal_register_date);
						
						
						// If record exists, update the title
						if( recordThatMayExist.isPresent() ) {
							EISDoc oldRecord = recordThatMayExist.get();
							
							String oldTitle = oldRecord.getTitle();
							
							if(oldTitle.contentEquals(itr.title)) {
								results.add("Item " + count + ": No change needed:" + oldTitle + " Versus:" + itr.title);
							} else {
								
								results.add("Item " + count + ": Title Updated FROM:" + oldTitle + " TO:" + itr.title);

								// at this point we've matched on title, date and document type already, but because of how we match on title, 
								// punctuation can be different, and hopefully the incoming title is more accurate
								oldRecord.setTitle(itr.title);
								
								docRepository.save(oldRecord); // save to db, ID shouldn't change
								
							}
						} else {
							results.add("Item " + count + ": Not found (can't update if no match)");
						}
					}
			    } else {
					results.add("Item " + count + ": Missing one or more required fields: Federal Register Date/Document/Title");
			    }
			    count++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<List<String>>(results, HttpStatus.OK);
	}


	/** 
	 * Takes .csv file with required headers and imports:
	 * New records with types like ROD/NOI/...
	 * Amended: Also imports new with these types (hopefully curated):
	 * 		"Final"
			"Final Revised"
			"Second Final"
			"Revised Final"
			"Final Supplement"
			"Final Supplemental"
			"Second Final Supplemental"
			"Third Final Supplemental"
			"Draft"
			"Draft Revised"
			"Second Draft"
			"Revised Draft"
			"Draft Supplement"
			"Draft Supplemental"
			"Second Draft Supplemental"
			"Third Draft Supplemental"
	 * Updates existing records with incoming EIS identifier to records missing files, 
	 * but does NOT update state, because the incoming data has mistakes
	 * 
	 * @return List of records that are one of the draft/final types that did not match anything (implying there's a matching issue because they should
	 * already exist and we want to connect files to them)
	 * and newly created or updated records */
	@CrossOrigin
	@RequestMapping(path = "/uploadCSV_constraints", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importCSVCustomConstraints(@RequestPart(name="csv") String csv, @RequestHeader Map<String, String> headers) 
										throws IOException { 
		
		fillAgencies();
		
		String token = headers.get("authorization");
		
		if(!isAdmin(token)) 
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
				
				// Handle any leading/trailing invisible characters, double spacing
				itr.title = Globals.normalizeSpace(itr.title);
				// Handle any agency abbreviations
				itr.agency = agencyAbbreviationToFull(Globals.normalizeSpace(itr.agency));
				itr.state = Globals.normalizeSpace(itr.state);
				itr.document = Globals.normalizeSpace(itr.document);
				
			    // Need at least title, date, type for matching (can't verify unique item otherwise)
			    if(canMatchCustom(itr)) {

			    	boolean error = false;
					
					if(!error) {
						// Deduplication
						
						List<EISDoc> recordsThatMayExist = getEISDocsByTitleTypeDate(itr.title, itr.document);
						
						// If too many records exist, return special message (multi-match)
						if(recordsThatMayExist.size() > 1) {
							results.add("Item " + count + ": Too many matches ("+recordsThatMayExist.size()+"): " + itr.title);
						}
						// If record exists but has no folder, then update the folder
						else if(!recordsThatMayExist.isEmpty() && 
								(recordsThatMayExist.get(0).getFolder() == null || recordsThatMayExist.get(0).getFolder().isBlank())
						) {
							if(recordsThatMayExist.get(0).getFilename() == null || recordsThatMayExist.get(0).getFilename().isBlank())
							{
								ResponseEntity<Long> status = updateDtoJustFolder(itr, recordsThatMayExist.get(0));
								
								if(status.getStatusCodeValue() == 500) { // Error
									results.add("Item " + count + ": Error saving: " + itr.title);
						    	} else {
									results.add("Item " + count + ": Updated EIS identifier (folder name): " + itr.title);
						    	}
							} else {
								// TODO: When we have a strategy for replacement, we can add folder information.
								// Until then, skip:
								results.add("Item " + count + ": Skipped due to existing filename: " + itr.title);
							}
						}
						// If file doesn't exist, then create new record - even if it matches the draft or final types
						else if(recordsThatMayExist.isEmpty()) { 
							if(isDraftOrFinalEIS(itr.document)) {
							    ResponseEntity<Long> status = saveDto(itr); // save record to database
						    	if(status.getStatusCodeValue() == 500) { // Error
									results.add("Item " + count + ": Error creating draft or final: " + itr.title);
						    	} else {
						    		results.add("Item " + count + ": Created draft or final:"
						    				+ " #TITLE: " + itr.title 
						    				+ " #TYPE: " + itr.document 
						    				+ " #DATE: " + itr.federal_register_date
						    				+ " #FOLDER: " + itr.eis_identifier);
						    	}
							} else {
							    ResponseEntity<Long> status = saveDto(itr); // save record to database
						    	if(status.getStatusCodeValue() == 500) { // Error
									results.add("Item " + count + ": Error creating: " + itr.title);
						    	} else {
						    		results.add("Item " + count + ": Created: " + itr.title);
						    	}
							}
				    	} else {
							results.add("Item " + count + ": Already has folder (no action): #EXISTING FOLDER: " 
									+ recordsThatMayExist.get(0).getFolder() + " #INCOMING FOLDER: " + itr.eis_identifier + " #TITLE: " + itr.title);
				    	}
					}
			    } else {
					results.add("Item " + count + ": Missing one or more required fields: Federal Register Date/Document/Title");
			    }
			    count++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<List<String>>(results, HttpStatus.OK);
	}


	/** Returns top EISDoc if database contains at least one instance of a title/type/date combination, accounts for
	 * missing apostrophes and commas also. */
	private List<EISDoc> getEISDocsByTitleTypeDate(@RequestParam String title, @RequestParam String type) {

		// Try without apostrophes (legacy data has no apostrophes?)
		String noApostropheTitle = title.replaceAll("'", "");
		List<EISDoc> docsToReturn = docRepository.findAllByTitleAndDocumentTypeIn(
				noApostropheTitle, 
				type.strip());
		
		if(docsToReturn.isEmpty()) {
			
			// Try without commas (legacy data has no commas?)
			String noCommaTitle = title.replaceAll(",", "");
			docsToReturn = docRepository.findAllByTitleAndDocumentTypeIn(
					noCommaTitle, 
					type.strip());
			
			// Try without apostrophes or commas? (legacy data has no apostrophes)
			if(docsToReturn.isEmpty()) {
				String noApostrophesCommasTitle = noCommaTitle.replaceAll("'", "");
				docsToReturn = docRepository.findAllByTitleAndDocumentTypeIn(
						noApostrophesCommasTitle, 
						type.strip());
				
				// Try with unmodified title
				if(docsToReturn.isEmpty()) {
					docsToReturn = docRepository.findAllByTitleAndDocumentTypeIn(
							title, 
							type.strip());
				} // else no match
			}
		}

		return docsToReturn;
	}


	private boolean isDraftOrFinalEIS(String document) {
		boolean result = false;
		
		if(		   document.equalsIgnoreCase("Final")
				|| document.equalsIgnoreCase("Final Revised")
				|| document.equalsIgnoreCase("Second Final")
				|| document.equalsIgnoreCase("Revised Final")
				|| document.equalsIgnoreCase("Final Supplement")
				|| document.equalsIgnoreCase("Final Supplemental")
				|| document.equalsIgnoreCase("Second Final Supplemental")
				|| document.equalsIgnoreCase("Third Final Supplemental")
				|| document.equalsIgnoreCase("Draft")
				|| document.equalsIgnoreCase("Draft Revised")
				|| document.equalsIgnoreCase("Second Draft")
				|| document.equalsIgnoreCase("Revised Draft")
				|| document.equalsIgnoreCase("Draft Supplement")
				|| document.equalsIgnoreCase("Draft Supplemental")
				|| document.equalsIgnoreCase("Second Draft Supplemental")
				|| document.equalsIgnoreCase("Third Draft Supplemental")
			) 
		{
			result = true;
		}
		return result;
	}


	/** Expects matching record and new folder; updates; preserves most metadata */
	private ResponseEntity<Long> updateDtoJustFolder(UploadInputs itr, EISDoc existingRecord) {

		if(existingRecord == null) {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		existingRecord.setFolder(itr.eis_identifier);
		if(itr.link != null && itr.link.length() > 0) {
			existingRecord.setLink(itr.link.strip());
		}
		if(itr.notes != null && itr.notes.length() > 0) {
			existingRecord.setNotes(itr.notes);
		}
		
		docRepository.save(existingRecord); // save to db, ID shouldn't change
		
		return new ResponseEntity<Long>(existingRecord.getId(), HttpStatus.OK);
	}
	private ResponseEntity<Long> updateDtoJustFilename(UploadInputs itr, EISDoc existingRecord) {

		if(existingRecord == null) {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		existingRecord.setFilename(itr.filename);
		
		docRepository.save(existingRecord); // save to db, ID shouldn't change
		
		return new ResponseEntity<Long>(existingRecord.getId(), HttpStatus.OK);
	}


	private boolean canMatchCustom(UploadInputs dto) {
		boolean valid = true;
	
		if(dto.title == null || dto.document == null) {
			valid = false;
			return valid; // Just stop here and don't have to worry about validating null values
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
	 * Takes .csv file with required headers and imports each valid, non-duplicate record.  Updates existing records
	 * 
	 * Valid records: Must have title/register_date/filename or folder/document_type, register_date must conform to one of
	 * the formats in parseFormatters[]
	 * 
	 * @return List of strings with message per record (zero-based) indicating success/error/duplicate 
	 * and potentially more details */
	@CrossOrigin
	@RequestMapping(path = "/uploadCSV_filenames", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importCSVNewFilenames(@RequestPart(name="csv") String csv, @RequestHeader Map<String, String> headers) 
										throws IOException { 
		
		fillAgencies();
		
		String token = headers.get("authorization");
		
		if(!isAdmin(token)) 
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
				
				// Handle any leading/trailing invisible characters, double spacing
				itr.title = Globals.normalizeSpace(itr.title);
				itr.document = Globals.normalizeSpace(itr.document);
				// Handle any agency abbreviations
				itr.agency = agencyAbbreviationToFull(Globals.normalizeSpace(itr.agency));
				
			    // Choice: Need at least title, date, type for deduplication (can't verify unique item otherwise)
			    if(isValid(itr)) {

			    	// Save only valid dates
			    	boolean error = false;
					try {
						LocalDate parsedDate = parseDate(itr.federal_register_date);
						itr.federal_register_date = parsedDate.toString();
					} catch (IllegalArgumentException e) {
						System.out.println("Threw IllegalArgumentException");
						results.add("Item " + count + ": " + e.getMessage());
						error = true;
					} catch (Exception e) {
						results.add("Item " + count + ": Error " + e.getMessage());
						error = true;
					}

					try {
						if(itr.epa_comment_letter_date != null && itr.epa_comment_letter_date.length() > 0) {
							itr.epa_comment_letter_date = parseDate(itr.epa_comment_letter_date).toString();
						}
					} catch (Exception e) {
						// Since this field is optional, we can just proceed
					}
					
					if(!error) {
						// Deduplication
						
						Optional<EISDoc> recordThatMayExist = getEISDocByTitleTypeDate(itr.title, itr.document, itr.federal_register_date);
						
						
						// If record exists but has no filename, then update it instead of skipping
						// so new data can add files where there are none, without adding redundant data when there is data.
						// If the user insists (force update header exists, and value of "Yes" for it) we will update it anyway.
						if(recordThatMayExist.isPresent() && 
								(recordThatMayExist.get().getFilename().isBlank() || 
										(itr.force_update != null && itr.force_update.equalsIgnoreCase("yes")) )
						) {
							ResponseEntity<Long> status = updateDtoJustFilename(itr, recordThatMayExist.get());
							
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
						}
						// If file doesn't exist, then create new record
						else if(!recordThatMayExist.isPresent()) { 
						    ResponseEntity<Long> status = saveDto(itr); // save record to database
					    	if(status.getStatusCodeValue() == 500) { // Error
								results.add("Item " + count + ": Error saving: " + itr.title);
					    	} else {
					    		results.add("Item " + count + ": Created: " + itr.title);

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
							results.add("Item " + count + ": Duplicate, has filename already (no action): " + itr.title);
				    	}
					}
			    } else {
					results.add("Item " + count + ": Missing one or more required fields: Federal Register Date/Document/EIS Identifier/Title");
			    }
			    count++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<List<String>>(results, HttpStatus.OK);
	}


	/** Extracts every relevant .zip file being served into a self-named folder sans .zip extension;
	 * @returns list of all files extracted successfully
	 * */
	@CrossOrigin
	@RequestMapping(
			path = "/extract_all", 
			method = RequestMethod.POST, 
			consumes = "application/json")
	private ResponseEntity<List<String>> extractAllZip(
			@RequestHeader Map<String, String> headers) 
			throws IOException 
	{
		// 1. confirm admin

		String token = headers.get("authorization");
		
		if(!isAdmin(token)) 
		{
			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
		} 
		
		
		// 2. get list of active filenames (and/or crawl for ones that actually exist - 
		// this show both missing files and extra files)
		
		List<EISDoc> docsWithFilenames = docRepository.findAllWithFilenames();
		List<String> createdFolders = new ArrayList<String>(docsWithFilenames.size());
		

		if(testing) { 
			docsWithFilenames = new ArrayList<EISDoc>();
			docsWithFilenames.add(docRepository.findById(22).get());
		}
		
		
		// 3. If EISDoc has no folder, then:
		//		a. extract to self-named folder sans .zip extension AND
		//		b. add that folder name to eisdoc as folder field
		
		for(int i = 0; i < docsWithFilenames.size(); i++) {
			createdFolders.add( extractOneZip(docsWithFilenames.get(i), true, false) );
		}
		
		
		/** 4. NOTE: This may have consequences for any relevant document_texts or nepafiles, 
		 *			if so account for it
		 *		a. (optional) remove archive filename from eisdoc (this could prevent easily updating an
		 *	incomplete archive, and we've already seen one case where the government provided just such,
		 *  so maybe don't do this)
		 * 		
		 *		b. (optional) delete original file from disk (this should be fine if the original was 
		 *	successfully completely unpacked, and saves from using about double disk space)
		 */
		
		
		// Finally, return list of strings of folders created successfully
		
		return new ResponseEntity<List<String>>(createdFolders,HttpStatus.OK);
		
		// Note: Frontend must convert filename text to links on both results and details pages afterward
		// Note: Importing also has to be amended to follow this new process
		
	}
	
	// Overloaded extractAllZip for when we have only a filename instead of a doc
	private String extractOneZip(String _filename, boolean skipIfHasFolder, boolean convertAfterSave) {
		Optional<EISDoc> maybeDoc = docRepository.findTopByFilename(_filename);
		if(maybeDoc.isEmpty()) { // probably impossible for caller's logic
			return "***No document linked to: "+_filename+"***";
		} else {
			return extractOneZip(maybeDoc.get(), skipIfHasFolder, convertAfterSave);
		}
	}
	
	/** Helper method for extractAllZip */
	private String extractOneZip(EISDoc doc, boolean skipIfHasFolder, boolean convertAfterSave) {
		String createdFolder = null;
		
		String folder = doc.getFolder();
		String filename = doc.getFilename();
		
		try {
			if(folder != null 
					&& folder.length() > 0 
					&& skipIfHasFolder) {
				// skip if doc already has folder, and flagged to skip in that case
			} 
			else if(filename != null 
					&& filename.length() > 4 
					&& !filename.substring(filename.length()-4).equalsIgnoreCase(".zip")) {
				// skip if non-zip, like a pdf
			} 
			else { // extract, upload
				ZipExtractor unzipper = new ZipExtractor();
				
				// drop extension and use that as folder name
				folder = filename.substring(0, filename.length()-4);
				
				// result should be a list of filenames, or null if it failed
				List<String> result = unzipper.unzip(filename);
				
				if(result != null) { // save folder and add to results
					doc.setFolder(folder);
					docRepository.save(doc);
					
					// need to make a nepafile entry for every extracted file to support downloads
					for(int j = 0; j < result.size(); j++) {
						// eliminate any possibility of duplicates
						Optional<NEPAFile> possibleFile = 
							nepaFileRepository.findByDocumentTypeAndEisdocAndFolderAndFilenameAndRelativePathIn(
									doc.getDocumentType(),doc,folder,result.get(j),'/'+folder+'/'
							);
						
						if(possibleFile.isEmpty()) {
							NEPAFile x = new NEPAFile();
							x.setDocumentType(doc.getDocumentType());
							x.setEisdoc(doc);
							x.setFolder(folder);
							x.setFilename(result.get(j));
							x.setRelativePath('/'+folder+'/');
							x = nepaFileRepository.save(x);
							
							// Run through tika, add document texts to db and index them
							if(convertAfterSave) {
							    this.convertNEPAFile(x);
							}
						}
					}
					
					createdFolder = (folder);
				} else {
					createdFolder = ("**PROBLEM WITH: " + filename + "**");
				}
			}
		} catch(Exception e) {
			createdFolder = ("***EXCEPTION WITH: " + filename + "***");
			
			FileLog fLog = new FileLog();
			fLog.setDocumentId(doc.getId());
			fLog.setErrorType("extractOneZip failed: " + e.getMessage());
			fLog.setFilename("FOLDER: "+ folder + "; FILENAME: " + filename);
			fLog.setLogTime(LocalDateTime.now());
			fileLogRepository.save(new FileLog());
		}
		
		return createdFolder;
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
	
	/** Return full name for given agency abbreviation if found, else return string unchanged */
	public static String agencyAbbreviationToFull(String abbr) {
		String fullName = agencies.get(abbr);
		if(fullName == null) {
			return abbr;
		} else {
//			System.out.println("Abbr: " + abbr + " - Full: " + fullName);
			return fullName;
		}
	}
	public static void fillAgencies() {
		if(agencies.size() == 0) {
			agencies.put("ACHP","Advisory Council on Historic Preservation");
			agencies.put("USAID","Agency for International Development");
			agencies.put("ARS","Agriculture Research Service");
			agencies.put("APHIS","Animal and Plant Health Inspection Service");
			agencies.put("AFRH","Armed Forces Retirement Home");
			agencies.put("BPA","Bonneville Power Administration");
			agencies.put("BIA","Bureau of Indian Affairs");
			agencies.put("BLM","Bureau of Land Management");
			agencies.put("USBM","Bureau of Mines");
			agencies.put("BOEM","Bureau of Ocean Energy Management");
			agencies.put("BOP","Bureau of Prisons");
			agencies.put("BR","Bureau of Reclamation");
			agencies.put("Caltrans","California Department of Transportation");
			agencies.put("CHSRA","California High-Speed Rail Authority");
			agencies.put("CIA","Central Intelligence Agency");
			agencies.put("NYCOMB","City of New York, Office of Management and Budget");
			agencies.put("CDBG","Community Development Block Grant");
			agencies.put("CTDOH","Connecticut Department of Housing");
			agencies.put("BRAC","Defense Base Closure and Realignment Commission");
			agencies.put("DLA","Defense Logistics Agency");
			agencies.put("DNA","Defense Nuclear Agency");
			agencies.put("DNFSB","Defense Nuclear Fac. Safety Board");
			agencies.put("DSA","Defense Supply Agency");
			agencies.put("DRB","Delaware River Basin Commission");
			agencies.put("DC","Denali Commission");
			agencies.put("USDA","Department of Agriculture");
			agencies.put("DOC","Department of Commerce");
			agencies.put("DOD","Department of Defense");
			agencies.put("DOE","Department of Energy");
			agencies.put("HHS","Department of Health and Human Services");
			agencies.put("DHS","Department of Homeland Security");
			agencies.put("HUD","Department of Housing and Urban Development");
			agencies.put("DOJ","Department of Justice");
			agencies.put("DOL","Department of Labor");
			agencies.put("DOS","Department of State");
			agencies.put("DOT","Department of Transportation");
			agencies.put("TREAS","Department of Treasury");
			agencies.put("VA","Department of Veteran Affairs");
			agencies.put("DOI","Department of the Interior");
			agencies.put("DEA","Drug Enforcement Administration");
			agencies.put("EDA","Economic Development Administration");
			agencies.put("ERA","Energy Regulatory Administration");
			agencies.put("ERDA","Energy Research and Development Administration");
			agencies.put("EPA","Environmental Protection Agency");
			agencies.put("FSA","Farm Service Agency");
			agencies.put("FHA","Farmers Home Administration");
			agencies.put("FAA","Federal Aviation Administration");
			agencies.put("FCC","Federal Communications Commission");
			agencies.put("FEMA","Federal Emergency Management Agency");
			agencies.put("FEA","Federal Energy Administration");
			agencies.put("FERC","Federal Energy Regulatory Commission");
			agencies.put("FHWA","Federal Highway Administration");
			agencies.put("FMC","Federal Maritime Commission");
			agencies.put("FMSHRC","Federal Mine Safety and Health Review Commission");
			agencies.put("FMCSA","Federal Motor Carrier Safety Administration");
			agencies.put("FPC","Federal Power Commission");
			agencies.put("FRA","Federal Railroad Administration");
			agencies.put("FRBSF","Federal Reserve Bank of San Francisco");
			agencies.put("FTA","Federal Transit Administration");
			agencies.put("FirstNet","First Responder Network Authority");
			agencies.put("USFWS","Fish and Wildlife Service");
			agencies.put("FDOT","Florida Department of Transportation");
			agencies.put("FDA","Food and Drug Administration");
			agencies.put("USFS","Forest Service");
			agencies.put("GSA","General Services Administration");
			agencies.put("USGS","Geological Survey");
			agencies.put("GLB","Great Lakes Basin Commission");
			agencies.put("IHS","Indian Health Service");
			agencies.put("IRS","Internal Revenue Service");
			agencies.put("IBWC","International Boundary and Water Commission");
			agencies.put("ICC","Interstate Commerce Commission");
			agencies.put("JCS","Joint Chiefs of Staff");
			agencies.put("MARAD","Maritime Administration");
			agencies.put("MTB","Materials Transportation Bureau");
			agencies.put("MSHA","Mine Safety and Health Administration");
			agencies.put("MMS","Minerals Management Service");
			agencies.put("MESA","Mining Enforcement and Safety");
			agencies.put("MRB","Missouri River Basin Commission");
			agencies.put("NASA","National Aeronautics and Space Administration");
			agencies.put("NCPC","National Capital Planning Commission");
			agencies.put("NGA","National Geospatial-Intelligence Agency");
			agencies.put("NGB","National Guard Bureau");
			agencies.put("NHTSA","National Highway Traffic Safety Administration");
			agencies.put("NIGC","National Indian Gaming Commission");
			agencies.put("NIH","National Institute of Health");
			agencies.put("NMFS","National Marine Fisheries Service");
			agencies.put("NNSA","National Nuclear Security Administration");
			agencies.put("NOAA","National Oceanic and Atmospheric Administration");
			agencies.put("NPS","National Park Service");
			agencies.put("NSF","National Science Foundation");
			agencies.put("NSA","National Security Agency");
			agencies.put("NTSB","National Transportation Safety Board");
			agencies.put("NRCS","Natural Resource Conservation Service");
			agencies.put("NER","New England River Basin Commission");
			agencies.put("NJDEP","New Jersey Department of Environmental Protection");
			agencies.put("NRC","Nuclear Regulatory Commission");
			agencies.put("OCR","Office of Coal Research");
//			agencies.put("OSM","Office of Surface Mining"); // EPA mistake?
			agencies.put("OSM","Office of Surface Mining Reclamation and Enforcement"); 
			agencies.put("OSMRE","Office of Surface Mining Reclamation and Enforcement");
			agencies.put("OBR","Ohio River Basin Commission");
			agencies.put("RSPA","Research and Special Programs");
			agencies.put("REA","Rural Electrification Administration");
			agencies.put("RUS","Rural Utilities Service");
			agencies.put("SEC","Security and Exchange Commission");
			agencies.put("SBA","Small Business Administration");
			agencies.put("SCS","Soil Conservation Service");
			agencies.put("SRB","Souris-Red-Rainy River Basin Commission");
			agencies.put("STB","Surface Transportation Board");
			agencies.put("SRC","Susquehanna River Basin Commission");
			agencies.put("TVA","Tennessee Valley Authority");
			agencies.put("TxDOT","Texas Department of Transportation");
			agencies.put("TPT","The Presidio Trust");
			agencies.put("TDA","Trade and Development Agency");
			agencies.put("USACE","U.S. Army Corps of Engineers");
			agencies.put("USCG","U.S. Coast Guard");
			agencies.put("CBP","U.S. Customs and Border Protection");
			agencies.put("RRB","U.S. Railroad Retirement Board");
			agencies.put("USAF","United States Air Force");
			agencies.put("USA","United States Army");
			agencies.put("USMC","United States Marine Corps");
			agencies.put("USN","United States Navy");
			agencies.put("USPS","United States Postal Service");
			agencies.put("USTR","United States Trade Representative");
			agencies.put("UMR","Upper Mississippi Basin Commission");
			agencies.put("UMTA","Urban Mass Transportation Administration");
			agencies.put("UDOT","Utah Department of Transportation");
			agencies.put("URC","Utah Reclamation Mitigation and Conservation Commission");
			agencies.put("WAPA","Western Area Power Administration");
		}
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

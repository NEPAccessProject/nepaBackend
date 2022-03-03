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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
//import org.xml.sax.ContentHandler;
//import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import nepaBackend.ApplicationUserService;
import nepaBackend.DocRepository;
import nepaBackend.FileLogRepository;
import nepaBackend.Globals;
import nepaBackend.NEPAFileRepository;
import nepaBackend.ProcessRepository;
import nepaBackend.TextRepository;
import nepaBackend.UpdateLogRepository;
import nepaBackend.UpdateLogService;
import nepaBackend.ZipExtractor;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.model.FileLog;
import nepaBackend.model.NEPAFile;
import nepaBackend.model.NEPAProcess;
import nepaBackend.model.UpdateLog;
import nepaBackend.pojo.DumbProcessInputs;
import nepaBackend.pojo.ProcessInputs;
import nepaBackend.pojo.UploadInputs;

@RestController
@RequestMapping("/file")
public class FileController {

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);
	
	@Autowired
	private DocRepository docRepository;
	@Autowired
	private TextRepository textRepository;
	@Autowired
	private UpdateLogService updateLogService;
	@Autowired
	private UpdateLogRepository updateLogRepository;
	@Autowired
	private ApplicationUserService applicationUserService;
	@Autowired
	private NEPAFileRepository nepaFileRepository;
	@Autowired
	private ProcessRepository processRepository;
	@Autowired
    private FileLogRepository fileLogRepository;
	
	private static DateTimeFormatter[] parseFormatters = Stream.of(
			"yyyy-MM-dd", "MM-dd-yyyy", "yyyy/MM/dd", "MM/dd/yyyy", 
			"yyyy-M-dd", "M-dd-yyyy", "yyyy/M/dd", "M/dd/yyyy", 
			"yyyy-MM-d", "MM-d-yyyy", "yyyy/MM/d", "MM/d/yyyy", 
			"yyyy-M-d", "M-d-yyyy", "yyyy/M/d", "M/d/yyyy", 
//			"yy-MM-dd", "MM-dd-yy", "yy/MM/dd", "MM/dd/yy", // two-digit years predictably broke things
//			"yy-M-dd", "M-dd-yy", "yy/M/dd", "M/dd/yy", 
//			"yy-MM-d", "MM-d-yy", "yy/MM/d", "MM/d/yy", 
//			"yy-M-d", "M-d-yy", "yy/M/d", "M/d/yy", 
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
			.map(DateTimeFormatter::ofPattern)
			.toArray(DateTimeFormatter[]::new);
	
	private static Map<String,String> agencies = new HashMap<String, String>();

	public FileController() {
	}
	
	private static boolean testing = Globals.TESTING;
	
	private static String dbURL = Globals.DOWNLOAD_URL;
	private static String testURL = "http://localhost:5000/test/";
	
	private static String uploadURL = Globals.UPLOAD_URL.concat("uploadFilesTest");
	private static String uploadTestURL = "http://localhost:5309/uploadFilesTest";
	
//	private static String uploadTestURL = "http://localhost:5309/uploadFilesTest";
	
    @GetMapping("/findAllNepaFiles")
    private @ResponseBody ResponseEntity<List<NEPAFile>> findAllNepaFiles(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(applicationUserService.isAdmin(token)) {
    		return new ResponseEntity<List<NEPAFile>>(nepaFileRepository.findAll(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<NEPAFile>>(new ArrayList<NEPAFile>(), HttpStatus.UNAUTHORIZED);
		}
    }

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
					addUpAndSaveFolderSize(doc);
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
			return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
					addUpAndSaveFolderSize(doc);
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
			return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Return list of "missing" files (no size on record, has folder/filename) */
	@CrossOrigin
	@RequestMapping(path = "/missing_files", method = RequestMethod.GET)
	public ResponseEntity<List<Object[]>> missingFiles() {
		try {
			return new ResponseEntity<List<Object[]>>(docRepository.findMissingFiles(),HttpStatus.OK);
		} catch (Exception e) {
			if(testing) {
				e.printStackTrace();
			}
			return new ResponseEntity<List<Object[]>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Return list of no files (no size on record) */
	@CrossOrigin
	@RequestMapping(path = "/missing_size", method = RequestMethod.GET)
	public ResponseEntity<List<EISDoc>> missingSize() {
		try {
			return new ResponseEntity<List<EISDoc>>(docRepository.findMissingSize(),HttpStatus.OK);
		} catch (Exception e) {
			if(testing) {
				e.printStackTrace();
			}
			return new ResponseEntity<List<EISDoc>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@CrossOrigin
	@RequestMapping(path = "/filenames", method = RequestMethod.GET)
	public ResponseEntity<List<String>> filenames(@RequestParam long document_id) {
		try {
			List<String> filenames = textRepository.findFilenameByDocumentId(document_id);
			if(filenames == null || filenames.size() == 0) {
				// try the old way instead?
				filenames = textRepository.findFilenameByDocumentIdOld(document_id);
			}
			return new ResponseEntity<List<String>>(filenames, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<List<String>>(HttpStatus.NOT_FOUND);
		}
	}

	// as of August '21 this probably only operates on epa comment letters?
	@CrossOrigin
	@RequestMapping(path = "/downloadFile", method = RequestMethod.GET)
	public ResponseEntity<Void> downloadFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam String filename) {
		try {
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
			logger.error("Download failed :: " + filename);
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
			// Get nepafiles that need to be zipped
			EISDoc doc = docRepository.findById(Long.parseLong(id)).get();
			List<NEPAFile> nepaFiles = nepaFileRepository.findAllByEisdoc(doc);

			if(nepaFiles.size() == 0) {
				// this only happens if it never existed or if the archive failed to extract
				// (i.e. corrupt/invalid/empty)
				return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
			}
			String downloadFilename = nepaFiles.get(0).getFolder() + "_" + doc.getDocumentType();
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
		if(!applicationUserService.isAdmin(token)) 
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

	/** Upload single file and/or metadata which is then imported to database, then converted to text and added to db (if applicable)
	 * and then indexed by Lucene.  Re-POSTS the incoming file to the DBFS via an Express.js server on there so that the
	 * original file can be downloaded (also needs to be in place for the Tika processing)
	 * 
	 * @param file
	 * @throws FileUploadException
	 * @throws IOException
	 */
	@CrossOrigin
	@Deprecated
	@RequestMapping(path = "/uploadFile", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<boolean[]> importDocument(@RequestPart(name="file") MultipartFile file, 
								@RequestPart(name="doc") String doc, @RequestHeader Map<String, String> headers) 
										throws IOException { 
//		if(testing) {
//			System.out.println(doc);
//			return new ResponseEntity<boolean[]>(HttpStatus.OK);
//		}

	    HttpStatus returnStatus = HttpStatus.OK;
		
	    boolean[] results = new boolean[3];
		String token = headers.get("authorization");
		if(!applicationUserService.isCurator(token) && !applicationUserService.isAdmin(token)) 
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

			    	// Run Tika on file, record if 200 or not
				    results[2] = (this.convertNEPAFile(savedNepaFile).getStatusCodeValue() == 200);
			    }
		    	
		    } else {
		    	returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		    }
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		    file.getInputStream().close();
		}

		return new ResponseEntity<boolean[]>(results, returnStatus);
	}
	
	
	
	/** 
	 * Upload a record with more than one file or directory associated with it
	 * For each file, assumes a base folder that ends in a number (identifying folder)
	 * If that doesn't exist, NEPAFile folder field instead uses the new ID from saving the metadata EISDoc
	 * */
	@CrossOrigin
	@Deprecated
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
		if(!applicationUserService.isCurator(token) && !applicationUserService.isAdmin(token)) // 401
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

			    // If file uploaded, proceed to saving to table and logging
			    if(uploaded) {
			    	// Save NEPAFile
			    	NEPAFile savedNepaFile = handleNEPAFileSave(origFilename, savedDoc, dto.document);
			    	
				    this.convertNEPAFile(savedNepaFile);
			    }

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			    files[i].getInputStream().close();
			}
		}
		
		return new ResponseEntity<String>("OK", HttpStatus.OK);
		
		
		// Choice so far: Overwrite everything that already exists when uploading, separate update function should be
		// the way to add/remove files with an existing record with existing files
		
		/** Same logic as uploadFile except: 
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
		
		if(!applicationUserService.isCurator(token) && !applicationUserService.isAdmin(token)) 
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
		
		if(!applicationUserService.isCurator(token) && !applicationUserService.isAdmin(token)) 
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
		if(!applicationUserService.isCurator(token) && !applicationUserService.isAdmin(token)) 
		{
			return new ArrayList<String>();
		} 
		
		fillAgencies();
		
		List<String> results = new ArrayList<String>();
		
	    // Expect some or all of these headers:
	    // Title, Document, EPA Comment Letter Date, Federal Register Date, Agency, State, 
		// EIS Identifier, Filename, Link, Notes, Force Update, Status, Subtype, County, Cooperating Agency
	    // Translations must be handled by frontend or client to match UploadInputs.class
		
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
		    UploadInputs dto[] = mapper.readValue(csv, UploadInputs[].class);

		    // Ensure metadata is valid
			int count = 0;
			for (UploadInputs itr : dto) {
				
				itr = this.normalizeUploadInputs(itr);
				
			    // Choice: Need at least title, date, type for deduplication (can't verify unique item otherwise)
			    if(isValid(itr)) {

			    	// Save only valid dates
			    	boolean error = false;
					try {
						LocalDate parsedDate = parseDate(itr.federal_register_date);
						itr.federal_register_date = parsedDate.toString();
					} catch (IllegalArgumentException e) {
						System.out.println("Threw IllegalArgumentException");
						results.add("Item " + count + ": " + e.getLocalizedMessage());
						error = true;
					} catch (Exception e) {
						results.add("Item " + count + ": Error " + e.getLocalizedMessage());
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
						
						// Update
						if(recordThatMayExist.isPresent()) {
							ResponseEntity<Long> status = new ResponseEntity<Long>(HttpStatus.OK);
							if(shouldImport) {
								status = updateDtoExceptFile(itr, recordThatMayExist, applicationUserService.getUserFromToken(token).getId());
							}
							
							if(status.getStatusCodeValue() == 500) { // Error
								results.add("Item " + count + ": Error saving: " + itr.title);
					    	} else if(status.getStatusCodeValue() == 208) { // No change
					    		results.add("Item " + count + ": No change (all fields identical): " + itr.title);
					    	} else {
								results.add("Item " + count + ": Updated: " + itr.title);
					    	}
						}
						// If file doesn't exist, then create new record
						else if(!recordThatMayExist.isPresent()) { 
							ResponseEntity<Long> status = new ResponseEntity<Long>(HttpStatus.OK);
							if(shouldImport) {
								long userId = applicationUserService.getUserFromToken(token).getId();
								status = saveDto(itr, userId);
							}
					    	if(status.getStatusCodeValue() == 500) { // Error
								results.add("Item " + count + ": Error saving: " + itr.title);
					    	} else {
					    		results.add("Item " + count + ": Created: " + itr.title);
					    	}
				    	} else {
							results.add("Item " + count + ": Duplicate (no action): " + itr.title);
				    	}
					}
			    } else {
					results.add("Item " + count + ": Missing one or more required fields: Federal Register Date/Document/Title");
			    }
			    count++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			results.add(e.getLocalizedMessage());
		}

		return results;
	}

	/** Handle erroneous leading/trailing invisible characters, double spacing, line returns,
		convert nulls to "", convert agency abbreviations to full */
	private UploadInputs normalizeUploadInputs(UploadInputs itr) {
		// Handle any agency abbreviations
		itr.agency = agencyAbbreviationToFull(Globals.normalizeSpace(itr.agency));

		itr.cooperating_agency = Globals.normalizeSpace(itr.cooperating_agency);
		itr.county = Globals.normalizeSpace(itr.county);
		itr.department = Globals.normalizeSpace(itr.department);
		itr.document = Globals.normalizeSpace(itr.document);
		itr.link = Globals.normalizeSpace(itr.link);
		itr.notes = Globals.normalizeSpace(itr.notes);
		itr.state = Globals.normalizeSpace(itr.state);
		itr.status = Globals.normalizeSpace(itr.status);
		itr.subtype = Globals.normalizeSpace(itr.subtype);
		itr.summary_text = Globals.normalizeSpace(itr.summary_text);
		itr.title = Globals.normalizeSpace(itr.title);
		
		return itr;
	}

	/** For updating metadata states by ID, from a spreadsheet. Also updates coop. agency */	
	@CrossOrigin
	@RequestMapping(path = "/uploadCSV_id_state", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importCSVFixStatesByID(@RequestPart(name="csv") String csv, @RequestHeader Map<String, String> headers) 
										throws IOException { 
		String token = headers.get("authorization");
		
		if(!applicationUserService.isAdmin(token)) 
		{
			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
		} 
		
		fillAgencies();
		
		List<String> results = new ArrayList<String>();
		
	    // Expect these headers:
	    // id, state
		
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
		    UploadInputs dto[] = mapper.readValue(csv, UploadInputs[].class);

		    // Ensure metadata is valid
			int count = 0;
			for (UploadInputs itr : dto) {
				
				itr.state = Globals.normalizeSpace(itr.state);
				
			    // Need ID
			    if(itr.id != null && itr.id.length() > 0) {
					// get match for possible update
					
					Optional<EISDoc> recordThatMayExist = docRepository.findById(Long.parseLong(itr.id));
					
					if( recordThatMayExist.isPresent() ) {
						if(itr.state.contentEquals(recordThatMayExist.get().getState())
								&& Globals.normalizeSpace(itr.cooperating_agency).contentEquals(
										Globals.normalizeSpace(recordThatMayExist.get().getCooperatingAgency())
									)
						) {
							// never mind, no need to update
							results.add("Item " + count + ": Unchanged (value was already "+itr.state+"): " + itr.id);
						} else {
							EISDoc record = recordThatMayExist.get();
							
							UpdateLog doc = updateLogService.newUpdateLogFromEIS(record,applicationUserService.getUserFromToken(token).getId());
							updateLogRepository.save(doc);

							// we're JUST updating the state, don't mess with anything else
							if(itr.state != null && !itr.state.isBlank()) {
								String newNotes = "Fixed state FROM:"+record.getState()+";"+Globals.normalizeSpace(record.getNotes());
								record.setNotes(newNotes);
								record.setState(Globals.normalizeSpace(itr.state));
								// same problems exist for coop. agency (delimiters etc.)
								if(itr.cooperating_agency != null && !itr.cooperating_agency.isBlank()) {
									record.setCooperatingAgency(itr.cooperating_agency);
								}
								
								try {
									docRepository.save(record); // save to db
									results.add("Item " + count + ": Updated: " + itr.id);
									
								} catch(Exception e) {
									results.add("Item " + count + ": Error saving: " + itr.id);
								}
							} else {
								results.add("Item " + count + ": No action: " + itr.id);
							}
						}
					}
					
					// If id doesn't exist, then skip
					else 
					{ 
						results.add("Item " + count + ": No match on ID: " + itr.id);
			    	} 
				} else {
					results.add("Item " + count + ": Missing ID");
				}
			    count++;
			}
			
		} catch (Exception e) {
			results.add(e.getLocalizedMessage());
		}

		return new ResponseEntity<List<String>>(results,HttpStatus.OK);
	}
	
	/** 
	 * Returns if the given path can be linked to any records, either through folder+type, or filename 
	 * */
	@CrossOrigin
	@GetMapping(path = "/can_link_folder_type")
	public ResponseEntity<Boolean> canLinkFolderAndType(@RequestParam String path) {
		String folder = getUniqueFolderNameOrEmpty(path);
		String documentType = getDocumentTypeOrEmpty(path);
		
		if(!folder.isBlank() 
				&& !documentType.isBlank() 
				&& docRepository.existsByFolderAndDocumentTypeIn(folder, documentType)) {
			return new ResponseEntity<Boolean>(true,HttpStatus.OK);
		} else {
			String filename = getFilenameOnly(path);
			
			if(filename != null && filename.length() > 0 && docRepository.existsByFilename(filename)) {
				return new ResponseEntity<Boolean>(true,HttpStatus.OK);
			} else if(filename != null && filename.length() > 0 && docRepository.existsByCommentsFilename(filename)) {
				return new ResponseEntity<Boolean>(true,HttpStatus.OK);
			} else {
				return new ResponseEntity<Boolean>(false,HttpStatus.OK);
			}
		}
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
		if(!applicationUserService.isCurator(token) && !applicationUserService.isAdmin(token)) // 401
		{
			return new ResponseEntity<String[]>(HttpStatus.UNAUTHORIZED);
		} 
		
		String[] results = new String[files.length];
		
		String[] filenames = new String[files.length];
		
		/** If valid: Upload files, save to files table, add to existing records if possible */
		
		for(int i = 0; i < files.length; i++) {

		    try {
			    String origFilename = files[i].getOriginalFilename();
			    String folderName = getUniqueFolderNameOrEmpty(origFilename);
			    
			    filenames[i] = origFilename;
			    
			    if(folderName.length() == 0) { // If no folder name:
			    	boolean missingZip = false;
			    	boolean isComments = false;
			    	
			    	// If filename, however:
			    	Optional<EISDoc> foundDoc = docRepository.findTopByFilename(origFilename);
			    	if(!foundDoc.isPresent()) {
			    		// Nothing?  Try removing .zip from filename, if it exists.
			    		if(origFilename.length() > 4 
			    				&& origFilename
			    					.substring(origFilename.length() - 4)
			    					.equalsIgnoreCase(".zip")
			    			) 
			    		{
				    		foundDoc = docRepository.findTopByFilename(origFilename.substring(0, origFilename.length()-4));
				    		if(foundDoc.isPresent()) {
				    			missingZip = true;
				    		} else {
				    			// No?  Is it a comments archive?
				    			foundDoc = docRepository.findTopByCommentsFilename(origFilename);
				    			if(foundDoc.isPresent()) {
					    			isComments = true;
				    			}
				    		}
			    		}
			    	}
			    	
			    	/** TODO: Check if comment archive exists already, for convenience of bulk uploads.
			    	/* However, note that if comments become their own distinct record type, all of this
			    	/* goes away and comment filenames will simply be filenames for those records.
			    	/* (Likely scenario) */
			    	
			    	// Just upload comments
			    	if(isComments) {
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
			
					    // If file uploaded, we're done
					    if(uploaded) {
					    	results[i] = "OK: " + files[i].getOriginalFilename();
					    } else {
					    	// ???
					    	results[i] = "Couldn't upload: " + origFilename;
					    }
			    	}
			    	else if(foundDoc.isPresent()) {
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
					    	
							// Update
					    	EISDoc savedDoc = docRepository.save(existingDoc);
					    	
					    	// Extract to folder, creating NEPAFiles along the way as needed,
					    	// also converting and indexing the texts when possible
					    	boolean convert = true;
					    	boolean skipIfHasFolder = false;
					    	results[i] += " *** Extract/convert result: " + extractOneZip(savedDoc, skipIfHasFolder, convert);
					    	
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
				    	
				    	if(savedNEPAFile == null) {
					    	results[i] = "Duplicate (File exists, nothing done): " + origFilename;
				    		// Duplicate, nothing else to do.  Could log that nothing happened if we want to
				    	} else {
						    // Note: Should be impossible not to have a linked document if the logic got us here
						    if(existingDocs.size() > 0) { // If we have a linked document:
						    	// Run Tika on folder
							    this.convertNEPAFile(savedNEPAFile);
							    
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
		    	logger.error("Exception:: " + e.getLocalizedMessage() + ": " + files[i].getOriginalFilename());
				results[i] = "Exception:: " + e.getLocalizedMessage() + ": " + files[i].getOriginalFilename();
			} finally {
			    files[i].getInputStream().close();	
			    
			    // Save FileLog (just for accountability for where files are coming from)
			    try {
				    FileLog uploadLog = new FileLog();	
				    uploadLog.setExtractedFilename(String.join(";", filenames));
				    uploadLog.setUser(applicationUserService.getUserFromToken(token));
				    uploadLog.setErrorType("Uploaded");
					fileLogRepository.save(uploadLog);
			    } catch(Exception e) {
			    	logger.error("Exception :: Couldn't log upload: " + e.getLocalizedMessage());
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
		    	addUpAndSaveFolderSize(docToUse);
			}
			return savedFile;
		}
	}



	// Add up filesize.  Note: Spaces must be URI'd
	private boolean addUpAndSaveFolderSize(EISDoc eisDoc) {
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
		try {
			
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
			
		} catch(java.net.ConnectException ce) {
			logger.error("Exception :: Failed to upload and index because connection failed (directory hosting files likely offline); filename: " + filename);
		} catch(Exception e) {
			logger.error("Exception in pdfConvertImportAndIndex :: " + e.getLocalizedMessage());
		}
		

		return HttpStatus.OK;
	}

	// helper method for bulk file import gets file, iterates through archive contents, converts to text, auto-indexed
	private HttpStatus archiveConvertImportAndIndex(EISDoc nepaDoc, URL fileURL) throws IOException {
		final int BUFFER = 2048;

		Tika tikaParser = new Tika();
		tikaParser.setMaxStringLength(-1); // disable limit

		try {
			InputStream in = new BufferedInputStream(fileURL.openStream());
			ZipInputStream zis = new ZipInputStream(in);
			ZipEntry ze;
			
			while((ze = zis.getNextEntry()) != null) {
				
				int count;
				byte[] data = new byte[BUFFER];
				
				String extractedFilename = ze.getName();
				
				// nepafiles table would know if we have this filename for this record already, 
				// could check with size too, if we tracked that per file.
	//			long uncompressedSize = ze.getSize();
				
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
							logger.error("Exception in archiveConvertImportAndIndex :: filename: " + extractedFilename + " error: " + e.getLocalizedMessage());
						} finally { // while loop handles getNextEntry()
							zis.closeEntry();
						}
					}
				}
			
			}
			
			// 5: Cleanup
			in.close();
			zis.close();
			
		} catch(java.net.ConnectException ce) {
			logger.error("Exception :: Failed to upload and index because connection failed (directory hosting files likely offline); URL: " + fileURL);
		} catch(Exception e) {
			logger.error("Exception in archiveConvertImportAndIndex :: " + e.getLocalizedMessage());
		}
		


		return HttpStatus.OK;
	}


	/** Records with folders are deliberately skipped here; they should be handled by other functions. */
	private ResponseEntity<Void> convertRecord(EISDoc eis) {

		// Check to make sure this record exists.
		if(eis == null) {
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}
		if(eis.getId() < 0) {
			return new ResponseEntity<Void>(HttpStatus.UNPROCESSABLE_ENTITY);
		}
		
		// Note: There can be multiple files per archive, resulting in multiple documenttext records for the same ID
		// but presumably different filenames with hopefully different conents
		final int BUFFER = 2048;
		
		try {
			Tika tikaParser = new Tika();
			tikaParser.setMaxStringLength(-1); // disable limit
		
			// Make sure there is a file (for this function, no filename means nothing to convert for this record)
			if(eis.getFilename() == null || eis.getFilename().length() == 0 
					|| (eis.getFolder() != null && eis.getFolder().length() > 0)) { 
				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
			}
			
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
							logger.error("Exception in convertRecord in extract/stream to Tika step :: " + e.getLocalizedMessage());
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
			logger.error("Exception in convertRecord :: " + e.getLocalizedMessage());
			// could be IO exception getting the file if it doesn't exist
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Return whether metadata exists for a given EIS Identifier (folder) */
	private boolean metadataExists(String folderName) {
		long numRecords = docRepository.countByFolder(folderName);
		boolean exists = numRecords > 0;
		return exists;
	}

	/** Returns if database contains at least one instance of a title/type/date combination */
	@CrossOrigin
	@RequestMapping(path = "/existsTitleTypeDate", method = RequestMethod.GET)
	private boolean recordExists(@RequestParam String title, @RequestParam String type, @RequestParam String date) {
		return docRepository.findByTitleTypeDateCompareAlphanumericOnly(title, type, parseDate(date)).isPresent();
	}
	
	// Experimental, probably useless (was trying to get document outlines)
//	@CrossOrigin
//	@RequestMapping(path = "/xhtml", method = RequestMethod.GET)
//	public ResponseEntity<List<String>> xhtml(@RequestHeader Map<String, String> headers) {
//		
//		String token = headers.get("authorization");
//		if(!applicationUserService.isAdmin(token)) 
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
	 * punctuation differences by comparing on alphanumeric only using regexp (MySQL 8.0+ only). */
	private Optional<EISDoc> getEISDocByTitleTypeDate(
			@RequestParam String title, 
			@RequestParam String type,
			@RequestParam String date) {
		Optional<EISDoc> docToReturn = docRepository.findByTitleTypeDateCompareAlphanumericOnly(
				title,type,parseDate(date));
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
	

	/** Turns ProcessInputs into valid Process and saves to database, 
	 * saves process ID to every affiliated record,
	 * returns new ID and 200 (OK) or null and 500 (error) */
	private ResponseEntity<List<Long>> saveProcessFromInputs(ProcessInputs itr) {

		if(itr.process_id != null && !itr.process_id.isBlank()) {
			itr.process_id = Globals.normalizeSpace(itr.process_id);
		} else {
			return new ResponseEntity<List<Long>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		// need to set the processID first and foremost
		NEPAProcess prout = new NEPAProcess(Long.valueOf(itr.process_id));
		
		// translate, etc.
		return updateExistingProcessIgnoreBlank(prout, itr);
	}
	
	/** Update existing NEPAProcess with non-empty ProcessInputs, and update indicated existing EISDocs 
	 * with process ID */
	private ResponseEntity<List<Long>> updateExistingProcessIgnoreBlank(NEPAProcess prout, ProcessInputs itr) {
		
		// impossible?
		if(prout.getProcessId() == null) {
			return new ResponseEntity<List<Long>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
			List<Long> results = new ArrayList<Long>();
			
			itr = this.normalizeProcessInputs(itr);
			
			// Cases where it tried to give a process ID to an EIS that doesn't exist.
			List<Long> badResults = new ArrayList<Long>();
			
			if(itr.draft_id.isEmpty()) {
				// skip
			} else {
				Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.draft_id));
				if(doc.isPresent()) {
					EISDoc found = doc.get();
					prout.setDocDraft(found);
					found.setProcessId(prout.getProcessId());
				} else {
					// skip, but this is a bad sign.  We'll want to track these.
					badResults.add(Long.valueOf(itr.draft_id));
				}
			}
		if(itr.final_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.final_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocFinal(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.final_id));
			}
		}
		
		if(itr.draftsup_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.draftsup_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocDraftSupplement(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.draftsup_id));
			}
		}
		if(itr.epacomments_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.epacomments_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocEpaComments(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.epacomments_id));
			}
		}
		if(itr.finalsup_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.finalsup_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocFinalSupplement(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.finalsup_id));
			}
		}
		if(itr.noi_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.noi_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocNoi(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.noi_id));
			}
		}
		if(itr.revdraft_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.revdraft_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocRevisedDraft(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.revdraft_id));
			}
		}
		if(itr.revfinal_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.revfinal_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocRevisedFinal(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.revfinal_id));
			}
		}
		if(itr.rod_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.rod_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocRod(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.rod_id));
			}
		}
		if(itr.scoping_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.scoping_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocScoping(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.scoping_id));
			}
		}
		if(itr.secdraft_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.secdraft_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocSecondDraft(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.secdraft_id));
			}
		}
		if(itr.secdraftsup_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.secdraftsup_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocSecondDraftSupplement(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.secdraftsup_id));
			}
		}
		if(itr.secfinal_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.secfinal_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocSecondFinal(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.secfinal_id));
			}
		}
		if(itr.secfinalsup_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.secfinalsup_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocSecondFinalSupplement(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.secfinalsup_id));
			}
		}
		if(itr.thirddraftsup_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.thirddraftsup_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocThirdDraftSupplement(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.thirddraftsup_id));
			}
		}
		if(itr.thirdfinalsup_id.isEmpty()) {
			// skip
		} else {
			Optional<EISDoc> doc = docRepository.findById(Long.valueOf(itr.thirdfinalsup_id));
			if(doc.isPresent()) {
				EISDoc found = doc.get();
				prout.setDocThirdFinalSupplement(found);
				found.setProcessId(prout.getProcessId());
			} else {
				badResults.add(Long.valueOf(itr.thirdfinalsup_id));
			}
		}
		
		
		NEPAProcess savedRecord = processRepository.save(prout); // update
		
		if(savedRecord != null) {
			results.add(savedRecord.getId());

			// Could return special case to indicate we have bad results appended.
			// Callers would have to deal with the status and results.
//			results.addAll(badResults);
//			if(results.size() > 1) {
//				return new ResponseEntity<List<Long>>(results, HttpStatus.ACCEPTED);
//			} 
			
			return new ResponseEntity<List<Long>>(results, HttpStatus.OK);
		} else {
			return new ResponseEntity<List<Long>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** helper method normalizes process strings to "" and removes whitespace */
	private ProcessInputs normalizeProcessInputs(ProcessInputs itr) {
		// normalize (this will also change nulls to "")
		itr.noi_id = Globals.normalizeSpace(itr.noi_id);
		itr.draft_id = Globals.normalizeSpace(itr.draft_id);
		itr.revdraft_id = Globals.normalizeSpace(itr.revdraft_id);
		itr.draftsup_id = Globals.normalizeSpace(itr.draftsup_id);
		itr.secdraft_id = Globals.normalizeSpace(itr.secdraft_id);
		itr.secdraftsup_id = Globals.normalizeSpace(itr.secdraftsup_id);
		itr.thirddraftsup_id = Globals.normalizeSpace(itr.thirddraftsup_id);
		itr.final_id = Globals.normalizeSpace(itr.final_id);
		itr.revfinal_id = Globals.normalizeSpace(itr.revfinal_id);
		itr.finalsup_id = Globals.normalizeSpace(itr.finalsup_id);
		itr.secfinal_id = Globals.normalizeSpace(itr.secfinal_id);
		itr.secfinalsup_id = Globals.normalizeSpace(itr.secfinalsup_id);
		itr.thirdfinalsup_id = Globals.normalizeSpace(itr.thirdfinalsup_id);
		itr.rod_id = Globals.normalizeSpace(itr.rod_id);
		itr.scoping_id = Globals.normalizeSpace(itr.scoping_id);
		itr.epacomments_id = Globals.normalizeSpace(itr.epacomments_id);
		
		return itr;
	}

	/** Turns UploadInputs into valid EISDoc and saves to database, returns new ID and 200 (OK) or null and 500 (error) */
	private ResponseEntity<Long> saveDto(UploadInputs itr, long userId) {
		
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
		newRecord.setCounty(itr.county);
		newRecord.setStatus(itr.status);
		newRecord.setSubtype(itr.subtype);
		
		EISDoc savedRecord = docRepository.save(newRecord); // save to db

		UpdateLog doc = updateLogService.newUpdateLogFromEIS(savedRecord,userId);
		updateLogRepository.save(doc);
		
		if(savedRecord != null) {
			return new ResponseEntity<Long>(savedRecord.getId(), HttpStatus.OK);
		} else {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** updates fields if existing fields blank (returns record ID and status 200), 
	 * or updates filename or folder fields if incoming is non-blank and different than existing,
	 * if all fields equal returns status 208
	 */
	private ResponseEntity<Long> updateDtoExceptFile(UploadInputs itr, 
			Optional<EISDoc> existingRecord, 
			Long userid) {
		if(!existingRecord.isPresent()) {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		boolean isChanged = false;
		EISDoc oldRecord = existingRecord.get();
		// Save log for accountability and restore option
		UpdateLog ul = updateLogService.newUpdateLogFromEIS(oldRecord, userid);

		// "Multi" is actually counterproductive, will have to amend spec
		if(itr.filename != null && itr.filename.equalsIgnoreCase("multi")) {
			itr.filename = "";
		}
		
		// update filename if it doesn't have one yet and a new one is actually incoming,
		// OR update if there's a new, non-blank, different one than existing, and force_update
		if( (oldRecord.getFilename() == null || oldRecord.getFilename().isBlank())
				&& itr.filename != null 
				&& !itr.filename.isBlank() ) {
			oldRecord.setFilename(itr.filename.strip());
			isChanged = true;
		} else if ( itr.force_update != null && itr.force_update.equalsIgnoreCase("Yes")
				&& itr.filename != null && !itr.filename.isBlank()
				&& oldRecord.getFilename() != null
				&& !oldRecord.getFilename().contentEquals(itr.filename)) {
			oldRecord.setFilename(itr.filename.strip());
			isChanged = true;
		}
		// update folder if it doesn't have one yet and a new one is actually incoming,
		// OR update if there's a new, non-blank, different one than existing, and force_update
		if( (oldRecord.getFolder() == null || oldRecord.getFolder().isBlank())
				&& itr.eis_identifier != null 
				&& !itr.eis_identifier.isBlank() ) {
			oldRecord.setFolder(itr.eis_identifier.strip());
			isChanged = true;
		} else if ( itr.force_update != null && itr.force_update.equalsIgnoreCase("Yes")
				&& itr.eis_identifier != null && !itr.eis_identifier.isBlank()
				&& oldRecord.getFolder() != null
				&& !oldRecord.getFolder().contentEquals(itr.eis_identifier)) {
			oldRecord.setFolder(itr.eis_identifier.strip());
			isChanged = true;
		}

		// at this point we've matched on title, date and document type already;
		// use whichever title is longer and therefore probably has more intact punctuation
		if(Globals.normalizeSpace(oldRecord.getTitle()).length() < itr.title.length()) {
			oldRecord.setTitle(itr.title);
			isChanged = true;
		}
		
		if(itr.comments_filename == null 
				|| itr.comments_filename.isBlank()
				|| ( oldRecord.getCommentsFilename() != null 
					&& itr.comments_filename.contentEquals(oldRecord.getCommentsFilename()) )
		) {
			// skip, leave original
		} else {
			oldRecord.setCommentsFilename(itr.comments_filename.strip());
			isChanged = true;
		}
		
		try {
			if(itr.epa_comment_letter_date == null 
					|| itr.epa_comment_letter_date.isBlank()) {
				// skip, leave original
			} else {
				LocalDate commentDate = parseDate(itr.epa_comment_letter_date);
				if(oldRecord.getCommentDate() == null 
						|| !commentDate.isEqual(oldRecord.getCommentDate())) {
					oldRecord.setCommentDate(commentDate);
					isChanged = true;
				}
			}
		} catch (IllegalArgumentException e) {
			// never mind
		}
		
		if(itr.state != null && !itr.state.isBlank()
				&& !itr.state.contentEquals(Globals.normalizeSpace(oldRecord.getState())) ) {
			oldRecord.setState(itr.state);
			isChanged = true;
		}
		if(itr.agency != null && !itr.agency.isBlank()
				&& !itr.agency.contentEquals(Globals.normalizeSpace(oldRecord.getAgency())) ) {
			oldRecord.setAgency(itr.agency);
			isChanged = true;
		}
		if(itr.cooperating_agency != null && !itr.cooperating_agency.isBlank()
				&& !itr.cooperating_agency.contentEquals(Globals.normalizeSpace(oldRecord.getCooperatingAgency())) ) {
			oldRecord.setCooperatingAgency(itr.cooperating_agency);
			isChanged = true;
		}
		if(itr.link != null && !itr.link.isBlank()
				&& !itr.link.contentEquals(Globals.normalizeSpace(oldRecord.getLink())) ) {
			oldRecord.setLink(itr.link);
			isChanged = true;
		}
		if(itr.notes != null && !itr.notes.isBlank()
				&& !itr.notes.contentEquals(Globals.normalizeSpace(oldRecord.getNotes())) ) {
			oldRecord.setNotes(itr.notes);
			isChanged = true;
		}
		if(itr.process_id != null && !itr.process_id.isBlank()
				&& (oldRecord.getProcessId() == null 
					|| !(Long.parseLong(itr.process_id) == oldRecord.getProcessId().longValue())) ) {
			oldRecord.setProcessId(Long.parseLong(itr.process_id));
			isChanged = true;
		}
		if(itr.county != null && !itr.county.isBlank()
				&& !itr.county.contentEquals(Globals.normalizeSpace(oldRecord.getCounty())) ) {
			oldRecord.setCounty(itr.county);
			isChanged = true;
		}
		if(itr.status != null && !itr.status.isBlank()
				&& !itr.status.contentEquals(Globals.normalizeSpace(oldRecord.getStatus())) ) {
			oldRecord.setStatus(itr.status);
			isChanged = true;
		}
		if(itr.subtype != null && !itr.subtype.isBlank()
				&& !itr.subtype.contentEquals(Globals.normalizeSpace(oldRecord.getSubtype())) ) {
			oldRecord.setSubtype(itr.subtype);
			isChanged = true;
		}
		
		if(isChanged) {
			docRepository.save(oldRecord); // save to db, ID shouldn't change
			updateLogRepository.save(ul);
			return new ResponseEntity<Long>(oldRecord.getId(), HttpStatus.OK);
		} else {
			// return something special denoting no change
			return new ResponseEntity<Long>(oldRecord.getId(), HttpStatus.ALREADY_REPORTED);
		}
	}

	/** Updates but doesn't overwrite anything with a blank value */
	private ResponseEntity<Long> updateDtoNoOverwrite(UploadInputs itr, EISDoc existingRecord, Long userid) {

		// Save log for accountability and restore option
		UpdateLog ul = updateLogService.newUpdateLogFromEIS(existingRecord, userid);
		updateLogRepository.save(ul);

		// at this point we've matched on title, date and document type already;
		// use whichever title is longer and therefore probably has more intact punctuation
		if(Globals.normalizeSpace(existingRecord.getTitle()).length() < itr.title.length()) {
			existingRecord.setTitle(Globals.normalizeSpace(itr.title));
		}
		
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
		if(itr.county == null || !itr.county.isBlank()) {
			// skip
		} else {
			existingRecord.setCounty(Globals.normalizeSpace(itr.county));
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
	/** Expects matching record and new folder; updates; preserves most metadata */
	private ResponseEntity<Long> updateDtoJustRod(UploadInputs itr, EISDoc existingRecord) {

		if(existingRecord == null) {
			return new ResponseEntity<Long>(HttpStatus.INTERNAL_SERVER_ERROR);
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
		boolean valid = true;

		// Necessary values (can't be null)
		if(dto.federal_register_date == null || dto.title == null || dto.document == null) {
			valid = false;
		}
		// Can't be blank, need title/type/date
		else if(dto.title.isBlank() || dto.document.isBlank() || dto.federal_register_date.isBlank()) {
			valid = false; 
		}
		
		// Don't require filename or EISID if force update==yes
//		if(valid && dto.force_update != null && dto.force_update.equalsIgnoreCase("yes")) {
//			return valid;
//		}
		
		// Expect EIS identifier or filename: disabled on request
//		if( ( dto.eis_identifier == null || dto.eis_identifier.isBlank() ) 
//				&& (dto.filename == null || dto.filename.isBlank() || dto.filename.equalsIgnoreCase("n/a") || dto.filename.equalsIgnoreCase("null"))) {
//			valid = false;
//		}

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
		if(!applicationUserService.isCurator(token) && !applicationUserService.isAdmin(token)) 
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

	/** Special, possible one-use route. 
	 * Admin-only. 
	 * Takes .csv file with required headers and imports each valid record.  Updates existing records
	 * 
	 * Valid records: Must have process ID
	 * 
	 * @return List of strings with message per record (zero-based) indicating success/error 
	 * and potentially more details */
	@CrossOrigin
	@RequestMapping(path = "/uploadCSV_processes", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importProcessIDs(
			@RequestPart(name="csv") String csv, 
			@RequestHeader Map<String, String> headers) throws IOException { 
		
		String token = headers.get("authorization");
		if(!applicationUserService.isAdmin(token)) 
		{
			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
		} 
		
		List<String> results = new ArrayList<String>();
		
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
	    	ProcessInputs dto[] = mapper.readValue(csv, ProcessInputs[].class);
	    	
			int count = 0;
			for (ProcessInputs itr : dto) {

				// default message: generic "error"
				String result = ("Item " + count + ": Error: " + itr.process_id);
				try {
					
				    if(itr.process_id != null && itr.process_id.length() > 0) {
				    	
						Optional<NEPAProcess> match = processRepository.findByProcessId(Long.valueOf(itr.process_id));
						ResponseEntity<List<Long>> status = new ResponseEntity<List<Long>>(HttpStatus.VARIANT_ALSO_NEGOTIATES);
						
						if(match.isEmpty()) { // match doesn't exist
							
							status = saveProcessFromInputs(itr);
							if(status.getStatusCodeValue() == 200) {
								result = ("Item " + count + ": Saved new process: " + itr.process_id);
							} else {
								// time to list IDs from the status starting with process ID
							}
							
						} else { // match exists
							
							// either update all fields arbitrarily or skip missing values.
							// Each choice is assuming a different user error, basically: either they could be
							// missing past data so don't replace that with blanks, 
							// or they could be aiming to correct past errors so allow blanking out past mistakes.
							status = updateExistingProcessIgnoreBlank(match.get(), itr);
							if(status.getStatusCodeValue() == 200) {
								result = ("Item " + count + ": Updated existing process: " + itr.process_id);
							} else {
								// time to list IDs from the status starting with process ID
							}
							
						}
						
						
					} else {
						result = ("Item " + count + ": Missing process ID");
					}

				} catch(javax.persistence.EntityNotFoundException e) {
					result = ("Item " + count + ": No such document found: " + e.getLocalizedMessage());
				} catch(Exception e) {
//					e.printStackTrace();
					result = ("Item " + count + ": Error: " + e.getLocalizedMessage());
				}
				results.add(result);
			    count++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<List<String>>(results, HttpStatus.OK);
	}


	/** 
	 * Admin-only. 
	 * ONLY updates process IDs.  Doesn't save update log.
	 * 
	 * Valid records: Must have process ID and document ID
	 * 
	 * @return List of strings with message per record (zero-based) indicating success/error 
	 * and potentially more details */
	@CrossOrigin
	@RequestMapping(path = "/uploadCSV_processes_dumb", 
					method = RequestMethod.POST, 
					consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importDumbProcesses(
			@RequestPart(name="csv") String csv, 
			@RequestHeader Map<String, String> headers) throws IOException { 

		String token = headers.get("authorization");
		if(!applicationUserService.isCurator(token) && !applicationUserService.isAdmin(token)) 
		{
			return new ResponseEntity<List<String>>(HttpStatus.UNAUTHORIZED);
		} 
		
		List<String> results = new ArrayList<String>();
		
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
	    	DumbProcessInputs dto[] = mapper.readValue(csv, DumbProcessInputs[].class);
	    	
			int count = 0;
			for (DumbProcessInputs itr : dto) {

				// default message: generic "error"
				String result = ("Item " + count + ": Error: " + itr.process_id);
				try {
					
				    if(itr.process_id != null && itr.process_id.length() > 0 
				    		&& itr.id != null && itr.id.length() > 0) {
						Optional<EISDoc> docToUpdate = docRepository.findById(Long.parseLong(itr.id));
						if(docToUpdate.isPresent()) {
							EISDoc doc = docToUpdate.get();
							if(Long.parseLong(itr.process_id) == doc.getProcessId()) {
								result = ("Item " + count + ": No change");
							} else {
								doc.setProcessId(Long.parseLong(itr.process_id));
								docRepository.save(doc);
								result = ("Item " + count + ": OK: ID: " + itr.id);
							}
						} else {
							result = ("Item " + count + ": Document not found at ID " + itr.id);
						}
					} else {
						result = ("Item " + count + ": Missing ID or process ID");
					}

				} catch(Exception e) {
//					e.printStackTrace();
					result = ("Item " + count + ": Error: " + e.getLocalizedMessage());
				} finally {
					results.add(result);
				    count++;
				}
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
		
		if(!applicationUserService.isAdmin(token)) 
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
//						System.out.println("Threw IllegalArgumentException");
						results.add("Item " + count + ": " + e.getLocalizedMessage());
						error = true;
					} catch (Exception e) {
						results.add("Item " + count + ": Register Date Format Error " + e.getLocalizedMessage());
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


	/** Special, possible one-use route. 
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
		
		if(!applicationUserService.isAdmin(token)) 
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
								results.add("Item " + count + ": Skipped due to existing filename: " + itr.title);
							}
						}
						// If file doesn't exist, then create new record - even if it matches the draft or final types
						else if(recordsThatMayExist.isEmpty()) { 
							if(isDraftOrFinalEIS(itr.document)) {
								long userId = applicationUserService.getUserFromToken(token).getId();
							    ResponseEntity<Long> status = saveDto(itr, userId); // save record to database
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
								long userId = applicationUserService.getUserFromToken(token).getId();
							    ResponseEntity<Long> status = saveDto(itr, userId); // save record to database
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
		
		if(!applicationUserService.isAdmin(token)) 
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
						ResponseEntity<Long> status = updateDtoNoOverwrite(itr, record, applicationUserService.getUserFromToken(token).getId());
						
						if(status.getStatusCodeValue() == 500) { // Error
							results.add("Item " + count + ": Error saving: " + itr.title);
				    	} else {
							results.add("Item " + count + ": Updated: " + itr.title);
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
	 * Updates first rod date only for matching titles */
	@CrossOrigin
	@RequestMapping(path = "/uploadCSV_titles_rod", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importCSVTitlesOnlyRod(@RequestPart(name="csv") String csv, @RequestHeader Map<String, String> headers) 
										throws IOException { 
		
		String token = headers.get("authorization");
		
		fillAgencies();
		
		if(!applicationUserService.isAdmin(token)) 
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
				
				// Title-only option for Buomsoo's data, to update all title matches
			    
			    if(itr.title != null && itr.title.length() > 0) {

			    	// Parse the four new date values
					try {
						itr.first_rod_date = parseDate(itr.first_rod_date).toString();
					} catch (Exception e) {
						// Since this is optional, we can just proceed
					}
					List<EISDoc> matchingRecords = docRepository.findAllByTitle(itr.title);
					
					for(EISDoc record : matchingRecords) {

						/** Special update function that only adds new data */
						ResponseEntity<Long> status = updateDtoJustRod(itr, record);
						
						if(status.getStatusCodeValue() == 500) { // Error
							results.add("Item " + count + ": Error saving: " + itr.title);
				    	} else {
							results.add("Item " + count + ": Updated: " + itr.title);
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
		
		if(!applicationUserService.isAdmin(token)) 
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
						results.add("Item " + count + ": " + e.getLocalizedMessage());
						error = true;
					} catch (Exception e) {
						results.add("Item " + count + ": Error " + e.getLocalizedMessage());
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
					    	}
						}
						// If file doesn't exist, then create new record
						else if(!recordThatMayExist.isPresent()) { 
							long userId = applicationUserService.getUserFromToken(token).getId();
						    ResponseEntity<Long> status = saveDto(itr, userId); // save record to database
					    	if(status.getStatusCodeValue() == 500) { // Error
								results.add("Item " + count + ": Error saving: " + itr.title);
					    	} else {
					    		results.add("Item " + count + ": Created: " + itr.title);
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
	 * Processes .zips that were MANUALLY uploaded to turn them into folders 
	 * and updates the appropriate EISDoc while skipping eisdocs that already have folders
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
		
		if(!applicationUserService.isAdmin(token)) 
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
		
		// don't convert to text and such - this is for existing archives that have been processed alredy,
		// but aren't in folders.  Do skip entries with folders, since that should mean we've already
		// extracted them.  
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
//	private String extractOneZip(String _filename, boolean skipIfHasFolder, boolean convertAfterSave) {
//		Optional<EISDoc> maybeDoc = docRepository.findTopByFilename(_filename);
//		if(maybeDoc.isEmpty()) { // probably impossible for caller's logic
//			return "***No document linked to: "+_filename+"***";
//		} else {
//			return extractOneZip(maybeDoc.get(), skipIfHasFolder, convertAfterSave);
//		}
//	}
	
	/** Helper method for extractAllZip, also used in import process for new archives */
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
				
				if(result != null) { // save folder, add up size and add to results
					doc.setFolder(folder);
					
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
						} else {
							System.out.println("Already have " + result.get(j) + " in " + '/'+folder+'/');
							
						}
					}

					// Has to be done AFTER nepafiles exist
					addUpAndSaveFolderSize(doc);
					
					createdFolder = (folder);
				} else {
					/** So at this point we have an archive that may actually have partially
					 * extracted, but none of it is converted and indexed and no nepafiles 
					 * were created.  This is a rare data corruption case and we'll want to fix 
					 * the source archive manually, so let's log a special case for it */
					FileLog fl = new FileLog();
					fl.setErrorType("Unzip failed");
					fl.setExtractedFilename(filename);
					fl.setUser(null);
					fileLogRepository.save(fl);
					
					createdFolder = ("**PROBLEM WITH: " + filename + "**");
				}
			}
		} catch(Exception e) {
			createdFolder = ("***EXCEPTION WITH: " + filename + "***");
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
	
	
}

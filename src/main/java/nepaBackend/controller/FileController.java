package nepaBackend.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
//import java.io.PrintWriter;
//import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.DocRepository;
import nepaBackend.model.EISDoc;

@RestController
@RequestMapping("/file")
public class FileController {

    private DocRepository docRepository;

    public FileController(DocRepository docRepository) {
        this.docRepository = docRepository;
    }
	
	// TODO: Set this as a global constant somewhere?  May be changed to SBS and then elsewhere in future
	// Can also have a backup set up for use if primary fails
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
	
	// TODO: Generalize for entries with folder or multiple files instead of simple filename
	// TODO: Restrict access
	@CrossOrigin
	@RequestMapping(path = "/convert", method = RequestMethod.GET)
	public ResponseEntity<ArrayList<String>> convertRecord(HttpServletRequest request, 
			@RequestParam String recordId) {
		// TODO: Add optional pdf/text file filename to the documenttext model
		// Note: There can be multiple files per archive, resulting in multiple documenttext records for the same ID
		final int BUFFER = 2048;
		
    	// TODO: Remove test result list
        ArrayList<String> results = new ArrayList<String>();
		try {

	    	Tika tikaParser = new Tika();
	    	tikaParser.setMaxStringLength(-1); // disable limit
	    	
	        // 1: Download the archive
			EISDoc eis = docRepository.getById(Long.parseLong(recordId));
			// TODO: Make sure there is a file (for current data, no filename means nothing to convert for this record)
	        URL fileURL = new URL(testURL + eis.getFilename());
	        System.out.println("FileURL " + fileURL.toString());
	        results.add("FileURL " + fileURL.toString());

	        InputStream in = new BufferedInputStream(fileURL.openStream());
	        ZipInputStream zis = new ZipInputStream(in);
	        ZipEntry ze;
	        
	        while((ze = zis.getNextEntry()) != null) {
	        	System.out.println("Extracting " + ze);
	        	int count;
	        	byte[] data = new byte[BUFFER];
		        // Can check filename for .pdf extension
	        	
	    		// TODO: Check to make sure we don't already have this document in the system.
	        	// Deduplication: Ignore when filename and document ID combination already exists in EISDoc table.
	        	
		        String filename = ze.getName();
		        results.add("Filename " + filename);
		        // Handle directory - can ignore them if we're just converting PDFs
	        	if(!ze.isDirectory()) {
	        		// 2: Extract data and stream to Tika
	        		try {
	        			ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        			while (( count = zis.read(data)) != -1) {
	        				baos.write(data, 0, count);
	        			}
//        				System.out.println(tikaParser.parseToString(new ByteArrayInputStream(baos.toByteArray())));
				        // 3: Convert to text
	        			String textResult = tikaParser.parseToString(new ByteArrayInputStream(baos.toByteArray()));
		        		results.add(textResult);
	        		} catch(Exception e){
	        			e.printStackTrace();
	        		} finally { // while loop handles getNextEntry()
	    	        	zis.closeEntry();
	        		}
	        	}

	        }
	        
	        // TODO 4: Add converted text to database for record ID (via FulltextController? It's prepared to .save already)
	        // 5: Cleanup
	        in.close();
	        zis.close();

	        return new ResponseEntity<ArrayList<String>>(results, HttpStatus.ACCEPTED);
	    } catch (Exception e) {
	    	e.printStackTrace();
	        return new ResponseEntity<ArrayList<String>>(results, HttpStatus.NOT_FOUND);
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

}

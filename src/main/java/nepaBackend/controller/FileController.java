package nepaBackend.controller;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
//import java.io.PrintWriter;
//import java.io.StringWriter;
import java.net.URL;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file")
public class FileController {
	
	// TODO: Set this as a global constant somewhere?  May be changed to SBS and then elsewhere in future
	// Can also have a backup set up for use if primary fails
	String dbURL = "http://mis-jvinaldbl1.catnet.arizona.edu:80/test/";

	@CrossOrigin
	@RequestMapping(path = "/downloadFile", method = RequestMethod.GET)
	public ResponseEntity<Void> downloadFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam String filename) {
		try {
			// TODO: if not .zip try adding .pdf first?
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

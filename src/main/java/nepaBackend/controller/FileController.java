package nepaBackend.controller;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file")
public class FileController {
	
	String dbURL = "http://mis-jvinaldbl1.catnet.arizona.edu:80/test/";

	// TODO: import relevant files, test deployed

	@CrossOrigin
	@RequestMapping(path = "/downloadFile", method = RequestMethod.GET)
	public ResponseEntity<String> downloadFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "filename") String filename) {
	    
		try {
//	    	System.out.println("Activated for: " + dbURL + filename);
	    	
			// TODO: if not .zip try adding .pdf first?
	        URL fileURL = new URL(dbURL + filename);
	        InputStream in = new BufferedInputStream(fileURL.openStream());
	        
	        response.addHeader("Content-Disposition", "attachment; filename=" + filename); 

	        ServletOutputStream out = response.getOutputStream();
	        IOUtils.copy(in, out);
	        
	        response.flushBuffer();
	        return new ResponseEntity<String>("Whew", HttpStatus.ACCEPTED);
	    } catch (Exception e) {
	    	StringWriter sw = new StringWriter();
	    	PrintWriter pw = new PrintWriter(sw);
	    	e.printStackTrace(pw);
	    	e.printStackTrace();
	    	String sStackTrace = sw.toString();
	    	// TODO: Probably don't provide stack trace outside testing
	        return new ResponseEntity<String>(sStackTrace, HttpStatus.NOT_FOUND);
	    }
	}

}

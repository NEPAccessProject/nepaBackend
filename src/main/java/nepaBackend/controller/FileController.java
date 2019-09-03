package nepaBackend.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class FileController {

	@CrossOrigin
	@RequestMapping(path = "/downloadFile", method = RequestMethod.GET)
	public ResponseEntity<String> downloadFile(HttpServletRequest request, HttpServletResponse response) {
	    try {
//	        String filename = request.getParameter("filename");
//	        File file = new File("mis-jvinaldbl1.catnet.arizona.edu/test/"+filename);
	        File file = new File("http://mis-jvinaldbl1.catnet.arizona.edu:80/test/test.txt");
	        InputStream in = new BufferedInputStream(new FileInputStream(file));

//	        response.setContentType("application/xlsx");
//	        response.setHeader("Content-Disposition", "attachment; filename="+filename+".txt");
	        response.addHeader("Content-Disposition", "attachment; filename=test.txt"); 
	        response.setContentType("txt/plain"); 


	        ServletOutputStream out = response.getOutputStream();
	        IOUtils.copy(in, out);
	        response.flushBuffer();
	        return new ResponseEntity<String>("Whew", null);
	    } catch (Exception e) {
	    	StringWriter sw = new StringWriter();
	    	PrintWriter pw = new PrintWriter(sw);
	    	e.printStackTrace(pw);
	    	String sStackTrace = sw.toString();
	        return new ResponseEntity<String>(sStackTrace, null);
	    }
	}
	
}

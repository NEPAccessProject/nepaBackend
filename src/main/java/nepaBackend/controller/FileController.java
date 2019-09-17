package nepaBackend.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/file")
public class FileController {

	// TODO: import relevant files, test with big files, etc.

	@CrossOrigin
	@RequestMapping(path = "/downloadFile", method = RequestMethod.GET)
	public ResponseEntity<String> downloadFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "filename") String filename) {
	    
		try {
	    	System.out.println("Activated for: http://mis-jvinaldbl1.catnet.arizona.edu:80/test/" + filename);
//	        String filename = request.getParameter("filename");
//	        File file = new File("mis-jvinaldbl1.catnet.arizona.edu/test/"+filename);
	        URL fileURL = new URL("http://mis-jvinaldbl1.catnet.arizona.edu:80/test/" + filename);
	        InputStream in = new BufferedInputStream(fileURL.openStream());
	        // TODO: Try just saving the file "locally" (to backend VM) to make sure it has permission

//	        response.setContentType("application/xlsx");
	        response.addHeader("Content-Disposition", "attachment; filename=" + filename); 
//	        response.setHeader("Content-Disposition", "attachment; filename="+filename);
//	        response.setContentType("txt/plain"); 


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
	        return new ResponseEntity<String>(sStackTrace, HttpStatus.NOT_FOUND);
	    }
	}

//	@CrossOrigin
//	@RequestMapping(path = "/download", method = RequestMethod.GET)
//	public void download( HttpServletResponse response ) throws IOException {
//    	System.out.println("Activated2");
//
//	    WebClient webClient = WebClient.create();
//
//	    URI fileUrl = null;
//		try {
//			fileUrl = new URI("http://mis-jvinaldbl1.catnet.arizona.edu:80/test/test.txt");
//		} catch (URISyntaxException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	    // Request service to get file data
//	    Flux<DataBuffer> fileDataStream = webClient.get()
//	            .uri( fileUrl )
//	            .accept( MediaType.APPLICATION_OCTET_STREAM )
//	            .retrieve()
//	            .bodyToFlux( DataBuffer.class );
//
//	    // Streams the stream from response instead of loading it all in memory
//	    DataBufferUtils.write( fileDataStream, response.getOutputStream() )
//	            .map( DataBufferUtils::release )
//	            .then()
//	            .block();
//	}
	
}

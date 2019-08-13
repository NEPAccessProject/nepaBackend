package nepaBackend.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class FileController {

	@CrossOrigin
	@RequestMapping(value = "/downloadFile", method = RequestMethod.GET)
	public void downLoadFile(HttpServletRequest request, HttpServletResponse response) {
	    try {
	        String fileName = request.getParameter("fileName");
	        File file = new File("mis-jvinaldbl1.catnet.arizona.edu" +"//"+fileName);
	        InputStream in = new BufferedInputStream(new FileInputStream(file));

	        response.setContentType("application/xlsx");
	        response.setHeader("Content-Disposition", "attachment; filename="+fileName+".xlsx"); 


	        ServletOutputStream out = response.getOutputStream();
	        IOUtils.copy(in, out);
	        response.flushBuffer();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
}

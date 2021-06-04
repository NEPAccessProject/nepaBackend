package nepaBackend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Extracts files of a .zip URL to
 * a destination directory.
 *
 * Works in local test environment as of 5/26/2021
 * 
 * This doesn't actually make any sense, because it's downloading the archive from the other VM, 
 * extracting the file and then uploading that file back to the other VM.
 * 
 * Should instead hit a new Express service route and this should all be done in javascript.
 */
public class ZipExtractor {
	
	private static final Logger logger = LoggerFactory.getLogger(ZipExtractor.class);

//	private static String uploadURL = Globals.UPLOAD_URL.concat("uploadFilesTest");
//	private static String uploadTestURL = "http://localhost:5309/uploadFilesTest";

	private static final String extractURL = Globals.UPLOAD_URL.concat("extract");
	private static final String extractURLTest = "http://localhost:5309/extract";

    /**
     * Size of the buffer to read/write data
     */
//    private static final int BUFFER_SIZE = 4096;
    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFileURL
     * @param destDirectory
     * @throws IOException
     */
//    public boolean unzip(URL zipFileURL, String destDirectory) throws IOException {
//    	boolean result = false;
//    	
//		InputStream in = new BufferedInputStream(zipFileURL.openStream());
//		ZipInputStream zipIn = new ZipInputStream(in);
//        ZipEntry entry = zipIn.getNextEntry();
//        // iterates over entries in the zip file
//        while (entry != null) {
//            String filePath = destDirectory + "/" + entry.getName();
//            if (!entry.isDirectory()) {
//                // if the entry is a file, extracts it
//            	result = extractToURL(zipIn, filePath, destDirectory);
//            	result = extractRemotely(destDirectory);
//            } else {
//                // TODO: if the entry is a directory, make the directory.
//            	// This won't work since the .zip is on a different VM entirely.
////                File dir = new File(filePath);
////                dir.mkdirs();
//            }
//            zipIn.closeEntry();
//            entry = zipIn.getNextEntry();
//        }
//        zipIn.close();
//        
//        return result;
//    }
    
	// Give the relevant .js service enough info to extract this archive to the proper folder
    public List<String> unzip(String filename) throws IOException {
    	return extractRemotely(filename);
    }
    
//    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
//        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
//        byte[] bytesIn = new byte[BUFFER_SIZE];
//        int read = 0;
//        while ((read = zipIn.read(bytesIn)) != -1) {
//            bos.write(bytesIn, 0, read);
//        }
//        bos.close();
//    }

    // Wishlist: Get list of any failed filenames back
//    private boolean extractRemotely(String filename) throws IOException {
//    	System.out.println("Sending " + filename);
//    	
//
////    	HttpEntity entity = EntityBuilder.create()
////					.setText(filename) 
////    				.build(); // this didn't work
//    	
////		List<NameValuePair> pairs=new ArrayList<NameValuePair>();
////		pairs.add(new BasicNameValuePair("filename",filename));
////		HttpEntity entity = EntityBuilder.create().setParameters(pairs).build(); // this didn't work
//    	
////    	HttpEntity entity = MultipartEntityBuilder.create()
////					.addTextBody("filename",
////							filename)
////    				.build(); // this didn't work
//    	
////    	HttpEntity entity = MultipartEntityBuilder.create()
////					.addTextBody("filename",
////							filename,
////							ContentType.APPLICATION_JSON) 
////    				.build(); // this didn't work
//		
//	    HttpGet request = new HttpGet(extractURL);
//	    if(Globals.TESTING) { request = new HttpGet(extractURLTest); }
//	    request.setHeader("filename", filename);
//
//	    HttpClient client = HttpClientBuilder.create().build();
//	    HttpResponse response = client.execute(request);
//	    
//	    if(Globals.TESTING) {
//		    System.out.println(response.toString());
//	    }
//	    
//	    boolean extracted = (response.getStatusLine().getStatusCode() == 200);
//	    
//	    return extracted;
//    }

    private List<String> extractRemotely(String filename) throws IOException {
    	CloseableHttpClient httpClient = HttpClients.createDefault();
        List<NameValuePair> arguments = new ArrayList<>();
        arguments.add(new BasicNameValuePair("filename", filename));
		
    	HttpPost request = new HttpPost(extractURL);
	    if(Globals.TESTING) { request = new HttpPost(extractURLTest); }
	    request.setEntity(new UrlEncodedFormEntity(arguments));
	    
	    HttpResponse response = httpClient.execute(request);
	    boolean extracted = (response.getStatusLine().getStatusCode() == 200);
	    List<String> results = null;
	    
	    if(extracted) {
		    String jsonString = EntityUtils.toString(response.getEntity());
		    JSONObject json = new JSONObject(jsonString);
		    JSONArray x = json.getJSONArray("filenames");
		    results = new ArrayList<String>(x.length());
		    for(int i = 0; i < x.length(); i++) {
		    	if(Globals.TESTING) {
		    		System.out.println("Result " + i+":"+x.getString(i));
		    	}
		    	results.add(x.getString(i));
		    }
	    } else {
	    	String jsonString = EntityUtils.toString(response.getEntity());
	    	logger.error("Extract failed: " + jsonString);
	    }
	    
	    httpClient.close();

	    return results;
	    
    }
    
    
//    private boolean extractToURL(ZipInputStream zipIn, String filePath, String baseDir) throws IOException {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        byte[] bytesIn = new byte[BUFFER_SIZE];
//        int count = 0;
//		while (( count = zipIn.read(bytesIn)) != -1) {
//			baos.write(bytesIn, 0, count);
//		}
//		
//	    // Upload file
//	    // Note: NOT leaving logic to Express server to decide directory name
//    	HttpEntity entity = MultipartEntityBuilder.create()
//					.addTextBody("filepath",
//							"/" + baseDir) // Feed Express path to use
//    				.addBinaryBody("file", //fieldname
//    						new ByteArrayInputStream(baos.toByteArray()), 
//    						ContentType.create("application/octet-stream"), 
//    						filePath)
//    				.build();
//	    HttpPost request = new HttpPost(uploadURL);
//	    if(Globals.TESTING) { request = new HttpPost(uploadTestURL); }
//	    request.setEntity(entity);
//
//	    HttpClient client = HttpClientBuilder.create().build();
//	    HttpResponse response = client.execute(request);
//	    
//	    if(Globals.TESTING) {
//		    System.out.println(response.toString());
//	    }
//	    
//	    boolean uploaded = (response.getStatusLine().getStatusCode() == 200);
//	    
//	    return uploaded;
//    }
}
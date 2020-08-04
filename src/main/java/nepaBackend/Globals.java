package nepaBackend;

public class Globals {
    public static final boolean TESTING = false;
    // Database/file server URL to base folder containing all files exposed to DAL for download
    public static final String DOWNLOAD_URL = "http://mis-jvinaldbl1.catnet.arizona.edu:80/test/";
    // Database/file server URL for Express service which handles new file uploads (and potentially updating or deleting files)
    public static final String UPLOAD_URL = "http://mis-jvinaldbl1.catnet.arizona.edu:5309/upload";

	// Note: Can also have a backup URL set up for use if primary fails
}

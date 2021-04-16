package nepaBackend;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Globals {

    public static final boolean TESTING = false;

    private static final String INDEX_DIRECTORY_PATH = "/data/lucene/nepaBackend.model.DocumentText";
//    private static final String TESTING_INDEX_DIRECTORY_PATH = "C:/lucene/nepaBackend.model.DocumentText";
    private static final String TESTING_INDEX_DIRECTORY_PATH = "C:\\gitrepo\\uapBackend\\uapBackend\\DocumentText";
    private static final String META_INDEX_DIRECTORY_PATH = "/data/lucene/nepaBackend.model.EISDoc";
    private static final String META_TESTING_INDEX_DIRECTORY_PATH = "C:\\gitrepo\\uapBackend\\uapBackend\\EISDoc";
  	
    public static final Path getIndexPath() {
    	if(TESTING) {
    		return Path.of(TESTING_INDEX_DIRECTORY_PATH);
    	} else {
    		return Path.of(INDEX_DIRECTORY_PATH);
    	}
    }
    public static final String getIndexString() {
    	if(TESTING) {
    		return (TESTING_INDEX_DIRECTORY_PATH);
    	} else {
    		return (INDEX_DIRECTORY_PATH);
    	}
    }
	public static String getMetaIndexString() {
    	if(TESTING) {
    		return (META_TESTING_INDEX_DIRECTORY_PATH);
    	} else {
    		return (META_INDEX_DIRECTORY_PATH);
    	}
	}

    // Database/file server URL to base folder containing all files exposed to DAL for download
    public static final String DOWNLOAD_URL = "http://mis-jvinaldbl1.catnet.arizona.edu:80/test/";
    // Database/file server URL for Express service which handles new file uploads (and potentially updating or deleting files)
    public static final String UPLOAD_URL = "http://mis-jvinaldbl1.catnet.arizona.edu:5309/";
    
    public static final List<String> EIS_TYPES = Arrays.asList("Draft Supplement",
			"Final Supplement",
			"Second Draft Supplemental",
			"Second Draft",
			"Adoption",
			"LF",
			"Revised Final",
			"LD",
			"Third Draft Supplemental",
			"Second Final",
			"Second Final Supplemental",
			"DC",
			"FC",
			"RF",
			"RD",
			"Third Final Supplemental",
			"DD",
			"Revised Draft",
			"NF",
			"F2",
			"D2",
			"F3",
			"DE",
			"FD",
			"DF",
			"FE",
			"A3",
			"A1");
    
	// TODO: Smarter sanity check
	public static final boolean saneInput(String sInput) {
		if(sInput == null) {
			return false;
		}
		return (sInput.trim().length() > 0);
	}

	public static final boolean saneInput(String[] sInput) {
		if(sInput == null || sInput.length == 0) {
			return false;
		}
		return true;
	}

	public static final boolean saneInput(boolean bInput) {
		return bInput;
	}

	// TODO: Validation for everything, like Dates
	
	public static final boolean saneInput(int iInput) {
		if(iInput > 0 && iInput <= Integer.MAX_VALUE) {
			return true;
		}
		return false;
	}
	
	public static String normalizeSpace(String str) {
		return org.apache.commons.lang3.StringUtils.normalizeSpace(str);
	}


	// Note: Can also have a backup URL set up for use if primary fails
}

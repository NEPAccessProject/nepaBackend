package nepaBackend;

import java.util.Arrays;
import java.util.List;

public class Globals {
    public static final boolean TESTING = true;
    // Database/file server URL to base folder containing all files exposed to DAL for download
    public static final String DOWNLOAD_URL = "http://mis-jvinaldbl1.catnet.arizona.edu:80/test/";
    // Database/file server URL for Express service which handles new file uploads (and potentially updating or deleting files)
    public static final String UPLOAD_URL = "http://mis-jvinaldbl1.catnet.arizona.edu:5309/upload";
    
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

	// Note: Can also have a backup URL set up for use if primary fails
}

package nepaBackend.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Can add:
 * 
 * Include drafts; include finals?
 * Comment date?
 * 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadInputs {
	public String title;
	public String publishDate;
	public String state;
	public String agency;
	public String type;
	public String commentsFilename;
	public String filename;
	
	public UploadInputs() {
	}
}

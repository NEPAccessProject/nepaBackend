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
	public String register_date;
	public String state;
	public String agency;
	public String document_type;
	public String comments_filename;
	public String filename;
	
	public UploadInputs() {
	}
}

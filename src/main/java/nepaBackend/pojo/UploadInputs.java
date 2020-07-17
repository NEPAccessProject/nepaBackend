package nepaBackend.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 
 * UploadInputs explicitly defines the names of the CSV headers.  This is an important standard:
 * Failing to meet it may result in incomplete CSV imports.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadInputs {
	public String title;
	public String document;
	public String federal_register_date;
	public String epa_comment_letter_date;
	public String agency;
	public String state;
	public String eis_identifier;
	public String filename;
	public String comments_filename;
	public String link;
	public String notes;
	
	public UploadInputs() {
	}
}

package nepaBackend.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadInputsGeoLinks {
	public String meta_id; // id; required
	public String geo_id; // required
	public String process_id; // optional
	
	public UploadInputsGeoLinks() {
	}
}

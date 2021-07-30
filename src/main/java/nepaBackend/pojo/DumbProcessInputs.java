package nepaBackend.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DumbProcessInputs {
	public String id;
	public String process_id;
}

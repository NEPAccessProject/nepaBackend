package nepaBackend.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessInputs {
	public String process_id;
	public String noi_id;
	public String draft_id;
	public String revdraft_id;
	public String draftsup_id;
	public String secdraft_id;
	public String secdraftsup_id;
	public String thirddraftsup_id;
	public String final_id;
	public String revfinal_id;
	public String finalsup_id;
	public String secfinal_id;
	public String secfinalsup_id;
	public String thirdfinalsup_id;
	public String rod_id;
	public String scoping_id;
	public String epacomments_id;
}

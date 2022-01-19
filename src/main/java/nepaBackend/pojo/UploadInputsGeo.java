package nepaBackend.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadInputsGeo {
	public String id; // not expected but could potentially be used to update
	public String feature; // this is the entire geojson "feature" object in a feature collection. Contains fields, polygons, etc.
	public String name; // derived from feature beforehand so the backend doesn't have to
	public String geo_id; // derived from feature beforehand so the backend doesn't have to
	
	public UploadInputsGeo() {
	}
}

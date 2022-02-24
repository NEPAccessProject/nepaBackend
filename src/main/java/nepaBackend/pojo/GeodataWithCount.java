package nepaBackend.pojo;

public class GeodataWithCount {
	public String geojson;
	public Long count;
	
	public GeodataWithCount() { }

	public GeodataWithCount(String geojson, Long count) {
		this.geojson = geojson;
		this.count = count;
	}
}

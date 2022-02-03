package nepaBackend.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

// Note: Could store multipolygon data in its own field and enforce uniqueness there?
// That would be the strictest deduplication for this I think

@Entity
@Table(name="geojson") 
public class Geojson {
	
    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id; 
	
    @Column(name="geojson",columnDefinition="longtext")
    private String geojson;

    @Column(name="name",columnDefinition="text")
    private String name;

    @Column(name="geo_id")
    private Long geoId;
	
    

	public Geojson() {
		super();
    }

	public Geojson(String geojson, Long geoId) {
		super();
		this.geojson = geojson;
		this.geoId = geoId;
	}
	
	public Geojson(String geojson, String name, Long geoId) {
		super();
		this.geojson = geojson;
		this.name = name;
		this.geoId = geoId;
	}

	public Long getId() {
		return id;
	}
	
	public String getGeojson() {
		return geojson;
	}

	public void setGeojson(String geojson) {
		this.geojson = geojson;
	}

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getGeoId() {
		return geoId;
	}

	public void setGeoId(Long geoId) {
		this.geoId = geoId;
	}

}

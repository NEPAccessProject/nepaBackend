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
	
    @Column(name="geojson",columnDefinition="text")
    private String geojson;
	
    
    public Geojson() {
    }

	public Geojson(String geojson) {
		this.geojson = geojson;
	}

	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getGeojson() {
		return geojson;
	}

	public void setGeojson(String geojson) {
		this.geojson = geojson;
	}

	// could store additional data we want for the tooltip or other that we expect to come in and want the database to care about?
//	public String getName() {
//		return name;
//	}
//
//	public void setName(String name) {
//		this.name = name;
//	}

}

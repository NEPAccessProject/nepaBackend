package nepaBackend.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="geojson_lookup") 
public class GeojsonLookup {
	
    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id; 

    @ManyToOne
    @JoinColumn(name="geojson_id")
    private Geojson geojson;
    
    @ManyToOne
    @JoinColumn(name="eisdoc_id")
    private EISDoc eisdoc;
	
    
    public GeojsonLookup() {
    }

	public GeojsonLookup(Geojson geojson, EISDoc eisdoc) {
		this.geojson = geojson;
		this.eisdoc = eisdoc;
	}
	

	public Long getId() {
		return id;
	}
	
	public Geojson getGeojson() {
		return geojson;
	}

	public void setGeojson(Geojson geojson) {
		this.geojson = geojson;
	}

	public EISDoc getEisdoc() {
		return eisdoc;
	}

	public void setEisdoc(EISDoc eisdoc) {
		this.eisdoc = eisdoc;
	}
}

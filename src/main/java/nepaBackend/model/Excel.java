package nepaBackend.model;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Excel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "json", nullable = false, columnDefinition="LONGTEXT")
    private String json;
    
    @Column(name = "saved_time", nullable = false, columnDefinition="TIMESTAMP")
    private LocalDateTime savedTime;
    

	public Excel() { 
		this.savedTime = LocalDateTime.now();
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

	public LocalDateTime getSavedTime() {
		return savedTime;
	}

	public Excel(String json) {
		this.json = json;
		this.savedTime = LocalDateTime.now();
	}
}
package nepaBackend.model;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

// Note: user is a sql keyword, so obviously never try to define that yourself.

@Entity
public class Survey {
	public Survey() {
		this.timestamp = LocalDateTime.now();
	}
    public Survey(ApplicationUser user, String result, String searchTerms) {
		super();
		this.user = user;
		this.result = result;
		if(searchTerms == null) {
			searchTerms = "";
		}
		this.searchTerms = searchTerms.substring(0, Math.min(searchTerms.length(), 255));
		this.timestamp = LocalDateTime.now();
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @ManyToOne
    @JoinColumn(name="user_id")
	private ApplicationUser user;
    
    @Column(name = "result")
    private String result;
    
    @Column(name = "time")
    private LocalDateTime timestamp;
    
    @Column(name = "search_terms", columnDefinition="VARCHAR(255)")
    private String searchTerms;

    public String getSearchTerms() {
		return searchTerms;
	}
	public void setSearchTerms(String searchTerms) {
		this.searchTerms = searchTerms;
	}
	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public long getId() {
		return id;
	}

	public ApplicationUser getUser() {
        return user;
    }

    public void setUser(ApplicationUser user) {
        this.user = user;
    }
}
package nepaBackend.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

// Functionality shifted to the only search options being the terms and whether title-only.
@Entity
@Table(name="search_log")
public class SearchLog {
	
	@Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id; 
	
	@Column(name="user_id")
	private Long userId;
	
	@Column(name="terms", columnDefinition="VARCHAR(255)") // should we save really long terms or trim them?
    private String terms;
	
    @Column(name="search_mode", columnDefinition="VARCHAR(255)") // type of search (title-only; all; ?)
    private String searchMode;

	@Column(name = "search_time", columnDefinition="TIMESTAMP")
	private LocalDateTime searchTime;

	public SearchLog() {
		this.searchTime = LocalDateTime.now();
	}
	
    public SearchLog(Long userId, String terms, String searchMode) {
		super();
		this.userId = userId;
		this.terms = terms;
		this.searchMode = searchMode;
		this.searchTime = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}
	
	
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	

	public String getTerms() {
		return terms;
	}
	public void setTerms(String terms) {
		this.terms = terms;
	}
	

	public String getSearchMode() {
		return searchMode;
	}
	public void setSearchMode(String searchMode) {
		this.searchMode = searchMode;
	}
	

	public LocalDateTime getSearchTime() {
		return searchTime;
	}
}
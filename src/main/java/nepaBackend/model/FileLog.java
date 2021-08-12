package nepaBackend.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="file_log") // Log of files uploaded
public class FileLog {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

	@Column(name = "extracted_filename") // Optional filename if extracted from archive
	private String extractedFilename;
	
	// Optional Foreign key: User ID
    @ManyToOne
    @JoinColumn(name="user_id")
	private ApplicationUser user;

	@Column(name = "error_type", columnDefinition = "TEXT") // Optional field to describe the nature of an error if one occurred
    private String errorType;
	
	@Column(name = "log_time", columnDefinition="TIMESTAMP")
	private LocalDateTime logTime;


	public FileLog() { 
		this.logTime = LocalDateTime.now();
	}

	public FileLog(long id, String extractedFilename, ApplicationUser user, String errorType) {
		super();
		this.id = id;
		this.extractedFilename = extractedFilename;
		this.user = user;
		this.errorType = errorType;
		this.logTime = LocalDateTime.now();
	}

	public ApplicationUser getUser() {
		return user;
	}

	public void setUser(ApplicationUser user) {
		this.user = user;
	}

	public String getExtractedFilename() {
		return extractedFilename;
	}

	public void setExtractedFilename(String extractedFilename) {
		this.extractedFilename = extractedFilename;
	}


	public String getErrorType() {
		return errorType;
	}


	public void setErrorType(String errorType) {
		this.errorType = errorType;
	}


	public LocalDateTime getLogTime() {
		return logTime;
	}


	public void setLogTime(LocalDateTime logTime) {
		this.logTime = logTime;
	}


	public long getId() {
		return id;
	}

}

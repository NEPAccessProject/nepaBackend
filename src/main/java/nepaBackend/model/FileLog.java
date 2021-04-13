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
@Table(name="file_log") // Log of files extracted, converted, imported, indexed or not for housekeeping
public class FileLog {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "document_id", nullable = false)
    private long documentId;

	@Column(name = "filename", length=255) // Filename according to database if we can get it
    private String filename;

	@Column(name = "extracted_filename") // Optional filename if extracted from archive
	private String extractedFilename;
	
	// Optional Foreign key: User ID
    @ManyToOne
    @JoinColumn(name="user_id")
	private ApplicationUser user;
    
    @Column(name = "imported", columnDefinition="TINYINT(1) default 0") // Text imported to database (converted)
    private boolean imported;

	@Column(name = "error_type", columnDefinition = "TEXT") // Optional field to describe the nature of an error if one occurred
    private String errorType;
	
	@Column(name = "log_time", columnDefinition="TIMESTAMP")
	private LocalDateTime logTime;


	public FileLog() { 
		this.logTime = LocalDateTime.now();
	}

	public FileLog(long id, String filename, ApplicationUser user, boolean imported, String errorType, LocalDateTime logTime) {
		super();
		this.id = id;
		this.filename = filename;
		this.imported = imported;
		this.user = user;
		this.errorType = errorType;
		this.logTime = LocalDateTime.now();
	}


    public long getDocumentId() {
		return documentId;
	}

	public void setDocumentId(long documentId) {
		this.documentId = documentId;
	}
	
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
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

	
    public boolean isImported() {
		return imported;
	}

	public void setImported(boolean imported) {
		this.imported = imported;
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

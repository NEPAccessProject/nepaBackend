package nepaBackend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** Log updates to existing data for accountability */

@Entity
@Table(name="update_log")
public class UpdateLog {
	@Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id; 
	
	@Column(name="user_id")
	private Long userId; // ID of user

	@Column(name="document_id")
	private Long documentId; // ID of user
	
	public Long getDocumentId() {
		return documentId;
	}

	public void setDocumentId(Long documentId) {
		this.documentId = documentId;
	}

	@Column(name="title", columnDefinition ="TEXT") // Updated title
    private String title;
	
	@Column(name="document") // Updated document type, etc.
    private String document;
	
    @Column(name="agency")
    private String agency;
    
    @Column(name="department")
    private String department;
    
    @Column(name="cooperating_agency")
    private String cooperatingAgency;
  
    @Column(name="state")
    private String state;
    
    @Column(name="filename", columnDefinition ="TEXT")
    private String filename;

    @Column(name="folder")
    private String folder;

    @Column(name="web_link", columnDefinition ="TEXT")
    private String link;

    @Column(name="notes", columnDefinition ="TEXT")
    private String notes;
    
    @Column(name="summary", columnDefinition ="TEXT")
    private String summary;

	@Column(name="date",columnDefinition="DATE")
    private LocalDate date; 
	
	@Column(name="process_id")
	private Long processId;

	@Column(name = "saved_time", columnDefinition="TIMESTAMP")
	private LocalDateTime savedTime;

	public UpdateLog() {
		this.savedTime = LocalDateTime.now();
	}
	
    public UpdateLog(Long id, Long userId, Long documentId, String title, String document, String agency, String state, String filename,
    		String folder, String link, String notes) {
		super();
		this.id = id;
		this.userId = userId;
		this.documentId = documentId;
		this.title = title;
		this.document = document;
		this.agency = agency;
		this.state = state;
		this.filename = filename;
		this.folder = folder;
		this.link = link;
		this.notes = notes;
		this.savedTime = LocalDateTime.now();
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public String getAgency() {
		return agency;
	}

	public void setAgency(String agency) {
		this.agency = agency;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public LocalDateTime getSavedTime() {
		return savedTime;
	}

	public void setSavedTime(LocalDateTime savedTime) {
		this.savedTime = savedTime;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

    public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getCooperatingAgency() {
		return cooperatingAgency;
	}

	public void setCooperatingAgency(String cooperatingAgency) {
		this.cooperatingAgency = cooperatingAgency;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public Long getProcessId() {
		return processId;
	}

	public void setProcessId(Long processId) {
		this.processId = processId;
	}
}

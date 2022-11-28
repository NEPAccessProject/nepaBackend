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

	@Column(name="title", columnDefinition ="TEXT") 
    private String title;
	
	@Column(name="document",columnDefinition="VARCHAR(255)") 
    private String document;
	
    @Column(name="agency", columnDefinition = "VARCHAR(255)")
    private String agency;
    
    @Column(name="department",columnDefinition="VARCHAR(255)")
    private String department;
    
    @Column(name="cooperating_agency", columnDefinition = "TEXT")
    private String cooperatingAgency;
  
    @Column(name="state", columnDefinition = "TEXT")
    private String state;

    @Column(name="county", columnDefinition = "TEXT")
    private String county;
    
    @Column(name="filename",columnDefinition="VARCHAR(256)")
    private String filename;

    @Column(name="comments_filename",columnDefinition="VARCHAR(256)")
    private String commentsFilename;

    @Column(name="folder",columnDefinition="VARCHAR(256)")
    private String folder;

    @Column(name="web_link", columnDefinition ="TEXT")
    private String link;

    @Column(name="notes", columnDefinition ="TEXT")
    private String notes;
    
    @Column(name="summary", columnDefinition ="TEXT")
    private String summary;
    
    @Column(name="status", columnDefinition ="TEXT")
    private String status;
    
    @Column(name="subtype", columnDefinition ="TEXT")
    private String subtype;

	@Column(name="date",columnDefinition="DATE")
    private LocalDate date; 
	
	@Column(name="comments_date",columnDefinition="DATE")
    private LocalDate commentsDate; 
	
    @Column(name="noi_date",columnDefinition="DATE")
    private LocalDate noiDate; 

    @Column(name="draft_noa",columnDefinition="DATE")
    private LocalDate draftNoa; 

    @Column(name="final_noa",columnDefinition="DATE")
    private LocalDate finalNoa; 

    @Column(name="first_rod_date",columnDefinition="DATE")
    private LocalDate firstRodDate; 

	@Column(name="process_id")
	private Long processId;

	@Column(name = "saved_time", columnDefinition="TIMESTAMP")
	private LocalDateTime savedTime;
	
	@Column(name= "action_type", columnDefinition="TEXT")
	private String action;
	@Column(name= "decision", columnDefinition="TEXT")
	private String decision;

	public UpdateLog() {
		this.savedTime = LocalDateTime.now();
	}
	
    public UpdateLog(Long id, Long userId, Long documentId, 
    		String title, String document, 
    		String agency, String cooperatingAgency, String department, String state, String county, 
    		String filename, String commentsFilename, String folder, 
    		String link, String notes, String summary, String status, String subtype,
    		LocalDate date, LocalDate commentsDate,
    		String action, String decision) {
		super();
		this.id = id;
		this.userId = userId;
		this.documentId = documentId;
		this.title = title;
		this.document = document;
		this.agency = agency;
		this.cooperatingAgency = cooperatingAgency;
		this.department = department;
		this.state = state;
		this.county = county;
		this.filename = filename;
		this.commentsFilename = commentsFilename;
		this.folder = folder;
		this.link = link;
		this.notes = notes;
		this.summary = summary;
		this.status = status;
		this.subtype = subtype;
		this.date = date;
		this.commentsDate = commentsDate;
		this.action = action;
		this.decision = decision;
		this.savedTime = LocalDateTime.now();
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getDecision() {
		return decision;
	}

	public void setDecision(String decision) {
		this.decision = decision;
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

	
	public String getCounty() {
		return county;
	}

	public void setCounty(String county) {
		this.county = county;
	}

	public String getCommentsFilename() {
		return commentsFilename;
	}

	public void setCommentsFilename(String commentsFilename) {
		this.commentsFilename = commentsFilename;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSubtype() {
		return subtype;
	}

	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}

	public LocalDate getCommentsDate() {
		return commentsDate;
	}

	public void setCommentsDate(LocalDate commentsDate) {
		this.commentsDate = commentsDate;
	}

	public LocalDate getNoiDate() {
		return noiDate;
	}

	public void setNoiDate(LocalDate noiDate) {
		this.noiDate = noiDate;
	}

	public LocalDate getDraftNoa() {
		return draftNoa;
	}

	public void setDraftNoa(LocalDate draftNoa) {
		this.draftNoa = draftNoa;
	}

	public LocalDate getFinalNoa() {
		return finalNoa;
	}

	public void setFinalNoa(LocalDate finalNoa) {
		this.finalNoa = finalNoa;
	}

	public LocalDate getFirstRodDate() {
		return firstRodDate;
	}

	public void setFirstRodDate(LocalDate firstRodDate) {
		this.firstRodDate = firstRodDate;
	}
}

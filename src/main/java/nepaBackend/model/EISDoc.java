package nepaBackend.model;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

@Entity
@Table(name="eisdoc") 
@Indexed
public class EISDoc {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;  // TODO: ID/PK?
	
    @Column(name="title",columnDefinition="text") // Note: Had to do ALTER TABLE `eisdoc` ADD FULLTEXT(title) for search
    @Field(store=Store.NO)
    private String title;
  
    @Column(name="document_type")
    private String documentType;  // TODO: Enum?
  
    @Column(name="comment_date")
    private LocalDate commentDate; 
  
    @Column(name="register_date")
    private LocalDate registerDate; 
  
    @Column(name="agency")
    private String agency; // TODO: Enum?

    @Column(name="department")
    private String department; // TODO: Enum?

    @Column(name="cooperating_agency", columnDefinition="text")
    private String cooperatingAgency; 

    @Column(name="summary_text", columnDefinition="text")
    private String summaryText; 
    
	@Column(name="state")
    private String state; // TODO: Enum?
  
    @Column(name="filename")
    private String filename; // name of zip file of EIS PDF(s) (optional)

    @Column(name="comments_filename")
    private String commentsFilename; // name of zip file of comment PDF(s) (optional)

    @Column(name="folder")
    private String folder; // path to multiple associated files (optional)
    
    @Column(name="size")
    private Long size; // size of folder or archive on disk (optional)

	@Column(name="web_link")
    private String link; // source link (optional)

	@Column(name="notes", columnDefinition="text")
    private String notes; // (optional)
	
    @Column(name="noi_date")
    private LocalDate noiDate; 

    @Column(name="draft_noa")
    private LocalDate draftNoa; 

    @Column(name="final_noa")
    private LocalDate finalNoa; 

    @Column(name="first_rod_date")
    private LocalDate firstRodDate; 
    
    // String location; // Location for proposed project is desired, but don't have metadata
    // String action; // Type of action is desired, but don't have metadata
    
    // Long processId; // ID of process with related documents, if there are any, metadata doesn't exist for this yet

    public EISDoc() { }

	public EISDoc(Long id, String title, String documentType, LocalDate commentDate, LocalDate registerDate, 
			String agency, String department, String cooperatingAgency, String summaryText, String state,
			String filename, String commentsFilename, String folder, Long size, String link, String notes,
			LocalDate noiDate, LocalDate draftNoa, LocalDate finalNoa, LocalDate firstRodDate) {
		this.id = id;
		this.title = title;
		this.documentType = documentType;
		this.commentDate = commentDate;
		this.registerDate = registerDate;
		this.agency = agency;
		this.department = department;
		this.cooperatingAgency = cooperatingAgency;
		this.summaryText = summaryText;
		this.state = state;
		this.filename = filename;
		this.commentsFilename = commentsFilename;
		this.folder = folder;
		this.size = size;
		this.notes = notes;
		this.link = link;
		this.noiDate = noiDate;
		this.draftNoa = draftNoa;
		this.finalNoa = finalNoa;
		this.firstRodDate = firstRodDate;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDocumentType() {
		return documentType;
	}
	
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}
	
	public LocalDate getCommentDate() {
		return commentDate;
	}
	
	public void setCommentDate(LocalDate commentDate) {
		this.commentDate = commentDate;
	}
	
	public LocalDate getRegisterDate() {
		return registerDate;
	}
	
	public void setRegisterDate(LocalDate registerDate) {
		this.registerDate = registerDate;
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
	
	public String getCommentsFilename() {
		return commentsFilename;
	}

	public void setCommentsFilename(String commentsFilename) {
		this.commentsFilename = commentsFilename;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

    public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
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

	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}

}

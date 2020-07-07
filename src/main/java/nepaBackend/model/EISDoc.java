package nepaBackend.model;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="eisdoc") // TODO: Rename table everywhere
public class EISDoc {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;  // TODO: ID/PK?
	
    @Column(name="title",columnDefinition="text") // Note: Had to do ALTER TABLE `eis-meta` ADD FULLTEXT(title) for search
    private String title;
  
    @Column(name="document_type")
    private String documentType;  // TODO: Enum?
  
    @Column(name="comment_date")
    private LocalDate commentDate; // TODO: turn into DATE, MySQL stores Date as yyyy-mm-dd
  
    @Column(name="register_date")
    private LocalDate registerDate; 
  
    @Column(name="agency")
    private String agency; // TODO: Enum?
  
    @Column(name="state")
    private String state; // TODO: Enum?
  
    @Column(name="filename")
    private String filename; // name of zip file of EIS PDF(s) (optional)

    @Column(name="comments_filename")
    private String commentsFilename; // name of zip file of comment PDF(s) (optional)

    @Column(name="folder")
    private String folder; // path to multiple associated files (optional)
    
    // String location; // Location for proposed project is desired, but don't have metadata
    // String action; // Type of action is desired, but don't have metadata
    
    // Long processId; // ID of process with related documents, if there are any, metadata doesn't exist for this yet

    public EISDoc() { }

	public EISDoc(Long id, String title, String documentType, LocalDate commentDate, LocalDate registerDate, String agency, String state,
			String filename, String commentsFilename, String folder) {
		//super();  // TODO: Do we need this for this project?
		
		this.id = id;
		this.title = title;
		this.documentType = documentType;
		this.commentDate = commentDate;
		this.registerDate = registerDate;
		this.agency = agency;
		this.state = state;
		this.filename = filename;
		this.commentsFilename = commentsFilename;
		this.folder = folder;
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
	

}

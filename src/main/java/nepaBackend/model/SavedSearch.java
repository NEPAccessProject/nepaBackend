package nepaBackend.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="saved_search")
public class SavedSearch {
	
	@Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id; 
	
	@Column(name="user_id", nullable=false)
	private Long userId; // ID of user saving the search
	
	// Note: Timestamp could be used in place of blank names
	@Column(name="search_name")
	private String searchName; // User-defined name of saved search
	
	@Column(name="search_mode")
	private String searchMode; // Boolean mode vs. natural language mode, currently
	
	@Column(name="title") // Note: Had to do ALTER TABLE `eis-meta` ADD FULLTEXT(title) for search
    private String title;
	
//	@Column(name="keywords")
//	private String keywords; // Expecting "keywords" to be defined - would this search across both title and text?
//	
//	@Column(name="full_text")
//	private String fullText; // Expecting to be able to search for words specifically within text
  
	// Note: Some of this will become invalid as document types are inevitably removed, replaced or added
    @Column(name="document_types") // CSV list of desired document types (all/final/draft/...)
    private String documentTypes;

	@Column(name="start_comment")
    private String startComment; // mm/dd/yyyy TODO: turn into DATE, MySQL stores Date as yyyy/mm/dd
  
    @Column(name="end_comment")
    private String endComment; // mm/dd/yyyy TODO: same
  
    @Column(name="start_publish")
    private String startPublish; // mm/dd/yyyy TODO: same

    @Column(name="end_publish")
    private String endPublish; // mm/dd/yyyy TODO: same
  
    @Column(name="agency")
    private String agency;
  
    @Column(name="state")
    private String state;
  
    @Column(name="needs_document", columnDefinition="TINYINT(1)") // Would use newer BIT type for slightly smaller memory footprint, but it's associated with bugs
    private boolean needsDocument; // Require document downloads?

    @Column(name="needs_comments", columnDefinition="TINYINT(1)")
    private boolean needsComments; // Require comment downloads?
    
    @Column(name="how_many")
    private int howMany; // How many records to get AKA limit (which is obviously a reserved word)

	@Column(name = "saved_time", columnDefinition="TIMESTAMP")
	private LocalDateTime savedTime;

	public SavedSearch() {}
	
    public SavedSearch(Long id, Long userId, String searchName, String searchMode, String title, String documentTypes, String startComment,
			String endComment, String startPublish, String endPublish, String agency, String state, boolean needsDocument,
			boolean needsComments, int howMany) {
		super();
		this.id = id;
		this.userId = userId;
		this.searchMode = searchMode;
		this.searchName = searchName;
		this.title = title;
		this.documentTypes = documentTypes;
		this.startComment = startComment;
		this.endComment = endComment;
		this.startPublish = startPublish;
		this.endPublish = endPublish;
		this.agency = agency;
		this.state = state;
		this.needsDocument = needsDocument;
		this.needsComments = needsComments;
		this.howMany = howMany;
	}


	public Long getUserId() {
		return userId;
	}


	public void setUserId(Long userId) {
		this.userId = userId;
	}


	public String getSearchMode() {
		return searchMode;
	}


	public void setSearchMode(String searchMode) {
		this.searchMode = searchMode;
	}

	
	public String getSearchName() {
		return searchName;
	}

	public void setSearchName(String searchName) {
		this.searchName = searchName;
	}

	public String getTitle() {
		return title;
	}


	public void setTitle(String title) {
		this.title = title;
	}


	public String getDocumentTypes() {
		return documentTypes;
	}


	public void setDocumentTypes(String documentTypes) {
		this.documentTypes = documentTypes;
	}


	public String getStartComment() {
		return startComment;
	}


	public void setStartComment(String startComment) {
		this.startComment = startComment;
	}


	public String getEndComment() {
		return endComment;
	}


	public void setEndComment(String endComment) {
		this.endComment = endComment;
	}


	public String getStartPublish() {
		return startPublish;
	}


	public void setStartPublish(String startPublish) {
		this.startPublish = startPublish;
	}


	public String getEndPublish() {
		return endPublish;
	}


	public void setEndPublish(String endPublish) {
		this.endPublish = endPublish;
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


	public boolean isNeedsDocument() {
		return needsDocument;
	}


	public void setNeedsDocument(boolean needsDocument) {
		this.needsDocument = needsDocument;
	}


	public boolean isNeedsComments() {
		return needsComments;
	}


	public void setNeedsComments(boolean needsComments) {
		this.needsComments = needsComments;
	}


	public int getHowMany() {
		return howMany;
	}


	public void setHowMany(int howMany) {
		this.howMany = howMany;
	}
	

	public Long getId() {
		return id;
	}

	
	public LocalDateTime getSavedTime() {
		return savedTime;
	}

	public void setSavedTime(LocalDateTime savedTime) {
		this.savedTime = savedTime;
	}
}
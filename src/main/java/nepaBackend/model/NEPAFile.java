package nepaBackend.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="nepafile") 
public class NEPAFile {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;  // TODO: ID/PK?

    @Column(name="agency") // optional, might be redundant but can inform path
    private String agency;
    
    @Column(name="document_type") // optional, might be redundant but can inform path
    private String documentType;

    @Column(name="filename")
    private String filename; // name of zip file or PDF (non-optional)

    @Column(name="folder")
    private String folder; // unique foldername/eis identifier E.G. EPA_10000 
    
    @Column(name="relative_path")
    private String relativePath; // relative path (optional, could be in root folder)

	// Mandatory Foreign key: EISDoc
    @ManyToOne
    @JoinColumn(columnDefinition="INT(11)", name="document_id")
	private EISDoc eisdoc;
    
    public NEPAFile() { }

	public NEPAFile(Long id, String agency, String documentType, String filename, String folder, String relativePath, 
			EISDoc eisdoc) {
		this.id = id;
		this.agency = agency;
		this.documentType = documentType;
		this.filename = filename;
		this.folder = folder;
		this.relativePath = relativePath;
		this.eisdoc = eisdoc;
	}

	public String getAgency() {
		return agency;
	}

	public void setAgency(String agency) {
		this.agency = agency;
	}
	
	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public Long getId() {
		return id;
	}

	public String getDocumentType() {
		return documentType;
	}
	
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public EISDoc getEisdoc() {
		return eisdoc;
	}

	public void setEisdoc(EISDoc eisdoc) {
		this.eisdoc = eisdoc;
	}
	

}

package nepaBackend.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Table(name="document_text")
@Entity
@Indexed
public class DocumentText {
	
	public DocumentText() { }

	public DocumentText(Long id, Long documentId, String plaintext, String filename) {
		this.id = id;
		this.documentId = documentId;
		this.plaintext = plaintext;
		this.filename = filename;
	}

    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	// Foreign key: EISDoc ID
    @Column(name="document_id")
	private Long documentId;
	
	// Actual converted text from file (can be multiple files for one EISDoc, and that's okay, but ordering them correctly programmatically could be tricky)
	@Column(name="plaintext",columnDefinition="text")
    @Field
	private String plaintext;
	
	@Column(name="filename",columnDefinition="text")
    @Field
	private String filename;


	public Long getId() {
		return id;
	}
	
	public Long getDocumentId() {
		return documentId;
	}

	public void setDocumentId(Long documentId) {
		this.documentId = documentId;
	}

	public String getPlaintext() {
		return plaintext;
	}

	public void setPlaintext(String plaintext) {
		this.plaintext = plaintext;
	}
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

}

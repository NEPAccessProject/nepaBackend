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

	public DocumentText(Long id, Long document_id, String plaintext) {
		this.id = id;
		this.document_id = document_id;
		this.plaintext = plaintext;
	}

    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	// Foreign key: EISDoc ID
    @Column(name="document_id")
	private Long document_id;
	
	// Actual converted text from file (can be multiple files for one EISDoc, and that's okay, but ordering them correctly programmatically could be tricky)
	@Column(name="plaintext",columnDefinition="text")
    @Field
	private String plaintext;


	public Long getId() {
		return id;
	}
	
	public Long getDocument_id() {
		return document_id;
	}

	public void setDocument_id(Long document_id) {
		this.document_id = document_id;
	}

	public String getPlaintext() {
		return plaintext;
	}

	public void setPlaintext(String plaintext) {
		this.plaintext = plaintext;
	}
}

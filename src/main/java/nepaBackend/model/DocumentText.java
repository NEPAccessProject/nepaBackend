package nepaBackend.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

@Table(name="document_text")
@Entity
@Indexed
public class DocumentText {
	
	public DocumentText() { }

	public DocumentText(Long id, EISDoc eisdoc, String plaintext, String filename) {
		this.id = id;
		this.eisdoc = eisdoc;
		this.plaintext = plaintext;
		this.filename = filename;
	}

    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	// Foreign key: EISDoc ID
    @ManyToOne
    @JoinColumn(name="document_id")
	private EISDoc eisdoc;

    // norms is default YES but term vectors are default NO which is a huge problem for the highlighting efficiency of large documents
	// Actual converted text from file (can be multiple files for one EISDoc, and that's okay, but ordering them correctly programmatically could be tricky)
	@Column(name="plaintext") // Need to manually change to longtext
    @Field(store=Store.NO,norms=Norms.YES,termVector=TermVector.WITH_POSITION_OFFSETS) 
	private String plaintext;
	
	@Column(name="filename",columnDefinition="text")
    @Field(store=Store.NO)
	private String filename;


	public Long getId() {
		return id;
	}
	
	public EISDoc getEisdoc() {
		return eisdoc;
	}

	public void setEisdoc(EISDoc eisdoc) {
		this.eisdoc = eisdoc;
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

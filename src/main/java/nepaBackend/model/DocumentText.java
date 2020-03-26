package nepaBackend.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.springframework.stereotype.Indexed;

@Entity
@Table(name="document_text")
@Indexed
public class DocumentText {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	// Foreign key: EISDoc ID
    @Column(name="document_id")
	private Long document_id;
	
	// Actual converted text from file (can be multiple files for one EISDoc, and that's okay, but ordering them correctly programmatically could be tricky)
	@Column
    @Field
	private String document_text;
	
}

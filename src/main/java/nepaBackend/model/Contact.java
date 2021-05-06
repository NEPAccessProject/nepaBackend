package nepaBackend.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Table(name="contact") 
public class Contact {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id; 
	@Column(name = "created", columnDefinition="TIMESTAMP")
	private LocalDateTime created;
	
    @Column(name="body",columnDefinition="text")
    private String body;
	
    @Column(name="subject",columnDefinition="text")
    private String subject;

    @Column(name="name",columnDefinition="text")
    private String name;
    
    @Column(name="email",columnDefinition="text")
    private String email;
   
    public Contact() {
    	this.created = LocalDateTime.now();
    }

	public Contact(String body, String subject, String name, String email) {
    	this.created = LocalDateTime.now();
		this.body = body;
		this.subject = subject;
		this.name = name;
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}

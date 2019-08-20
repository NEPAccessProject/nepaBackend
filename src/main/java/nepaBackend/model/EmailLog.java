package nepaBackend.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="email_log") // Log of emails sent or not sent as the case may be for housekeeping
public class EmailLog {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "email", length=191)
    private String email;

    @Column(name = "username", length=191)
    private String username;
    
    @Column(name = "sent") // Sent or not
    private boolean sent;

    @Column(name = "email_type", length=191) // Type of email event (generate/reset...)
    private String emailType;

	@Column(name = "error_type", columnDefinition = "TEXT") // Optional field to describe the nature of an error if one occurred
    private String errorType;
	
	@Column(name = "log_time", columnDefinition="TIMESTAMP")
	private LocalDateTime logTime;

    


	public EmailLog() { }

	public EmailLog(Long id, String email, String username, boolean sent, String emailType, String errorType) {
		//super();  // TODO: Do we need this for this project?
		
		this.id = id;
		this.email = email;
		this.username = username;
		this.sent = sent;
		this.emailType = emailType;
		this.errorType = errorType;
	}
    
    
	public long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean isSent() {
		return sent;
	}

	public void setSent(boolean sent) {
		this.sent = sent;
	}

    public String getEmailType() {
		return emailType;
	}

	public void setEmailType(String emailType) {
		this.emailType = emailType;
	}
	
	public String getErrorType() {
		return errorType;
	}

	public void setErrorType(String errorType) {
		this.errorType = errorType;
	}
	

    public LocalDateTime getLogTime() {
		return logTime;
	}

	public void setLogTime(LocalDateTime logTime) {
		this.logTime = logTime;
	}
}
package nepaBackend.model;

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

    @Column(name = "error_type") // Optional field to describe the nature of an error if one occurred
    private String errorType;
}
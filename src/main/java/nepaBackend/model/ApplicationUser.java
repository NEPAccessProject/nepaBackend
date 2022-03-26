package nepaBackend.model;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

// Note: user is a sql keyword, so obviously never try to define that yourself.

@Entity
public class ApplicationUser { // application_user
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "username", length=191, unique=true)
    private String username;
    
    @Column(name = "password", length=191)
    private String password;
    
    @Column(name = "email", length=191, unique=true)
    private String email;

	@Column(name = "role")
    private String role; // role describes permissions for using the app
	
	@Column(name = "last_reset", columnDefinition="TIMESTAMP")
	private LocalDateTime lastReset;

	@Column(name = "last_login", columnDefinition="timestamp NULL DEFAULT NULL")
	private LocalDateTime lastLogin;

	@Column(name = "first_name", length=191)
    private String firstName;
    
    @Column(name = "last_name", length=191)
    private String lastName;

    @Column(name = "affiliation", length=1000)
    private String affiliation;
    
    @Column(name = "organization", length=1000)
    private String organization;

    @Column(name = "job_title", length=1000)
    private String jobTitle;

	@Column(name = "verified", columnDefinition="TINYINT(1) default 0") // User has verified email
    private boolean verified;

    @Column(name = "active", columnDefinition="TINYINT(1) default 0") // User has been approved and can use the system
    private boolean active;
    
    @Column(name = "registered_on", columnDefinition="TIMESTAMP NULL DEFAULT NULL")
    private LocalDateTime registeredOn;

	public LocalDateTime getRegisteredOn() {
		return registeredOn;
	}

	public void setRegisteredOn(LocalDateTime registeredOn) {
		this.registeredOn = registeredOn;
	}

	public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
		return email;
	}

	public void setEmailAddress(String emailAddress) {
		this.email = emailAddress;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
	
	
    public LocalDateTime getLastReset() {
		return lastReset;
	}

	public void setLastReset(LocalDateTime lastReset) {
		this.lastReset = lastReset;
	}

    public LocalDateTime getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(LocalDateTime lastLogin) {
		this.lastLogin = lastLogin;
	}


    public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	public boolean isVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	

	public boolean isAccountNonExpired() {
		return active;
	}

	public boolean isAccountNonLocked() {
		return (active && verified);
	}

	public boolean isCredentialsNonExpired() {
		return active;
	}

	public boolean isAccountEnabled() {
		return (active && verified);
	}
}
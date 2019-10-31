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

	public boolean isAccountNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean isAccountNonLocked() {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean isCredentialsNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean isAccountEnabled() {
		// TODO Auto-generated method stub
		return true;
	}
}
package nepaBackend.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="user_status_log") // Log which admin changes which user, and if user enabled, timestamped
public class UserStatusLog {


	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

	// Foreign key: User ID
    @ManyToOne
    @JoinColumn(name="user_id")
	private ApplicationUser user;
    
	// Foreign key: User ID
    @ManyToOne
    @JoinColumn(name="admin_user_id")
	private ApplicationUser adminUser;
    
    @Column(name = "user_status", columnDefinition="tinyint(1)")
	private boolean userStatus;
    
	@Column(name = "log_time")
	private LocalDateTime logTime;


	public UserStatusLog() { 
		this.logTime = LocalDateTime.now();
	}

	public UserStatusLog(ApplicationUser adminUser, ApplicationUser user,
			boolean userStatus) {
		super();
		this.adminUser = adminUser;
		this.user = user;
		this.userStatus = userStatus;
		this.logTime = LocalDateTime.now();
	}

	public ApplicationUser getUser() {
		return user;
	}

	public void setUser(ApplicationUser user) {
		this.user = user;
	}

	public LocalDateTime getLogTime() {
		return logTime;
	}
	
	public ApplicationUser getAdminUser() {
		return adminUser;
	}

	public void setAdminUser(ApplicationUser adminUser) {
		this.adminUser = adminUser;
	}

	public boolean isUserStatus() {
		return userStatus;
	}

	public void setUserStatus(boolean userStatus) {
		this.userStatus = userStatus;
	}

	public void setLogTime(LocalDateTime logTime) {
		this.logTime = logTime;
	}

	public long getId() {
		return id;
	}

}

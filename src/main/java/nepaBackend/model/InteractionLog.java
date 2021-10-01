package nepaBackend.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import nepaBackend.enums.ActionSource;
import nepaBackend.enums.ActionType;

@Entity
@Table(name="interaction_log") // Log of user/timestamp/action (e.g. download or navigation)
public class InteractionLog {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;
	
	// Optional Foreign key: User ID
    @ManyToOne(optional=true) 
    @JoinColumn(name="user_id", nullable=true)
	private ApplicationUser user;
    
	// Optional Foreign key: EISDoc ID related to interaction
    @ManyToOne(optional=true) 
    @JoinColumn(name="doc_id", nullable=true)
	private EISDoc doc;
    
    // e.g. results page or details page
	@Column(name = "action_source") 
    private ActionSource actionSource;

	// e.g. download whole archive/download individual file/navigation
	@Column(name = "action_type") 
    private ActionType actionType;
	
	@Column(name = "log_time", columnDefinition="TIMESTAMP")
	private LocalDateTime logTime;


	public InteractionLog() { 
		this.logTime = LocalDateTime.now();
	}

	public InteractionLog(ApplicationUser user, ActionSource actionSource, 
			ActionType actionType, EISDoc doc) {
		super();
		this.user = user;
		this.actionSource = actionSource;
		this.actionType = actionType;
		this.doc = doc;
		this.logTime = LocalDateTime.now();
	}

	public EISDoc getDoc() {
		return doc;
	}

	public void setDoc(EISDoc doc) {
		this.doc = doc;
	}

	public ActionSource getActionSource() {
		return actionSource;
	}

	public void setActionSource(ActionSource actionSource) {
		this.actionSource = actionSource;
	}

	public ActionType getActionType() {
		return actionType;
	}

	public void setActionType(ActionType actionType) {
		this.actionType = actionType;
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


	public void setLogTime(LocalDateTime logTime) {
		this.logTime = logTime;
	}


	public long getId() {
		return id;
	}

}

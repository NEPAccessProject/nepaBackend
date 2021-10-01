package nepaBackend.pojo;

import java.time.LocalDateTime;

import nepaBackend.model.EISDoc;

public class InteractionSearchLog {
	private String username;
    
	private EISDoc doc;

    private String actionType;
	
	private LocalDateTime logTime;

	public InteractionSearchLog() {
	}
	
	public InteractionSearchLog(String username, EISDoc doc, String actionType, LocalDateTime logTime) {
		super();
		this.username = username;
		this.doc = doc;
		this.actionType = actionType;
		this.logTime = logTime;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public EISDoc getDoc() {
		return doc;
	}

	public void setDoc(EISDoc doc) {
		this.doc = doc;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public LocalDateTime getLogTime() {
		return logTime;
	}

	public void setLogTime(LocalDateTime logTime) {
		this.logTime = logTime;
	}

}

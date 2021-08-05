package nepaBackend.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="delete_request") 
public class DeleteRequest {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id; 
    
	@Column(name = "created", columnDefinition="TIMESTAMP")
	private LocalDateTime created;
	
	// e.g. "document_text" "nepafile"
    @Column(name="id_type",columnDefinition="text")
    private String idType;
	
    @Column(name="id_to_delete")
    private Long idToDelete;
    
    @Column(name="user_id")
    private Long userId;
    
    @Column(name="fulfilled")
    private Boolean fulfilled;

	public DeleteRequest() {
    	this.created = LocalDateTime.now();
    }

	public DeleteRequest(String idType, Long idToDelete, Long userId) {
    	this.created = LocalDateTime.now();
    	this.idType = idType;
    	this.idToDelete = idToDelete;
    	this.userId = userId;
    	this.fulfilled = false;
	}

	public String getIdType() {
		return idType;
	}

	public void setIdType(String idType) {
		this.idType = idType;
	}

	public Long getIdToDelete() {
		return idToDelete;
	}

	public void setIdToDelete(Long idToDelete) {
		this.idToDelete = idToDelete;
	}

	public Long getId() {
		return id;
	}

	public LocalDateTime getCreated() {
		return created;
	}
   
    public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Boolean getFulfilled() {
		return fulfilled;
	}

	public void setFulfilled(Boolean fulfilled) {
		this.fulfilled = fulfilled;
	}

}

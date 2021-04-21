package nepaBackend.model;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

// Note: user is a sql keyword, so obviously never try to define that yourself.

@Entity
public class OptedOut {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "name")
    private String name;
    
    @Column(name = "email", length=191, unique=true)
    private String email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
		return email;
	}

	public void getEmail(String email) {
		this.email = email;
	}
}
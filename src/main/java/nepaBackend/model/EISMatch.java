package nepaBackend.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="eismatch") 
public class EISMatch {

    @Id // PK (not null)
    @GeneratedValue(strategy=GenerationType.IDENTITY) // AI
    @Column(name = "match_id")
    Long match_id; // bigint(20)
    
    @NotNull
    @Column(name = "document1")
    int document1; // int(11)
    
    @NotNull
    @Column(name = "document2")
	int document2;
    
    @NotNull
    @Column(name = "match_percent")
    int match_percent; // TODO: Don't know exactly what format the % will be

    public EISMatch() { }
    
    public EISMatch(Long match_id, int document1, int document2, int match_percent) {
		super();
		this.match_id = match_id;
		this.document1 = document1;
		this.document2 = document2;
		this.match_percent = match_percent;
	}

	public Long getMatch_id() {
		return match_id;
	}

	public void setMatch_id(Long match_id) {
		this.match_id = match_id;
	}

	public int getDocument1() {
		return document1;
	}

	public void setDocument1(int document1) {
		this.document1 = document1;
	}

	public int getDocument2() {
		return document2;
	}

	public void setDocument2(int document2) {
		this.document2 = document2;
	}

	public int getMatch_percent() {
		return match_percent;
	}

	public void setMatch_percent(int match_percent) {
		this.match_percent = match_percent;
	}
}

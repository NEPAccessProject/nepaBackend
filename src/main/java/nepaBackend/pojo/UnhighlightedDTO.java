package nepaBackend.pojo;

import java.util.List;

public class UnhighlightedDTO {
	private List<Unhighlighted> unhighlighted;
	private String terms;

	public UnhighlightedDTO() {
		
	}

	public UnhighlightedDTO(List<Unhighlighted> unhighlighted, String terms) {
		this.unhighlighted = unhighlighted;
		this.terms = terms;
	}

	public void setUnhighlighted(List<Unhighlighted> unhighlighted) {
		this.unhighlighted = unhighlighted;
	}

	public void setTerms(String terms) {
		this.terms = terms;
	}

	public List<Unhighlighted> getUnhighlighted() {
		return unhighlighted;
	}

	public String getTerms() {
		return terms;
	}
	
}

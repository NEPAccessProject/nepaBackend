package nepaBackend.pojo;

import java.util.List;

public class Unhighlighted2DTO {
	private List<Unhighlighted2> unhighlighted;
	private String terms;
	private boolean markup;

	public Unhighlighted2DTO() {
		
	}

	public Unhighlighted2DTO(List<Unhighlighted2> unhighlighted, String terms, boolean markup) {
		this.unhighlighted = unhighlighted;
		this.terms = terms;
		this.setMarkup(markup);
	}

	public void setUnhighlighted(List<Unhighlighted2> unhighlighted) {
		this.unhighlighted = unhighlighted;
	}

	public void setTerms(String terms) {
		this.terms = terms;
	}

	public List<Unhighlighted2> getUnhighlighted() {
		return unhighlighted;
	}

	public String getTerms() {
		return terms;
	}

	public boolean isMarkup() {
		return markup;
	}

	public void setMarkup(boolean markup) {
		this.markup = markup;
	}
	
}

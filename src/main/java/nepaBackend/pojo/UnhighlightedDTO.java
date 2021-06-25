package nepaBackend.pojo;

import java.util.List;

public class UnhighlightedDTO {
	private List<Unhighlighted> unhighlighted;
	private String terms;
	private boolean markup;
	private int fragmentSizeValue;

	public UnhighlightedDTO() {
		
	}

	public UnhighlightedDTO(List<Unhighlighted> unhighlighted, String terms, boolean markup, int fragmentSizeValue) {
		this.unhighlighted = unhighlighted;
		this.terms = terms;
		this.setMarkup(markup);
		this.fragmentSizeValue = fragmentSizeValue;
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

	public boolean isMarkup() {
		return markup;
	}

	public void setMarkup(boolean markup) {
		this.markup = markup;
	}

	public int getFragmentSizeValue() {
		return fragmentSizeValue;
	}

	public void setFragmentSizeValue(int fragmentSizeValue) {
		this.fragmentSizeValue = fragmentSizeValue;
	}
	
}

package nepaBackend.controller;

import java.util.List;

import nepaBackend.model.EISDoc;

public class MetadataWithContext2 {
	private final EISDoc doc;
	private List<String> highlights;
	private String filenames;
	
	public MetadataWithContext2(EISDoc doc, List<String> highlights, String filenames) {
		this.doc = doc;
		this.highlights = highlights;
		this.filenames = filenames;
	}

	public EISDoc getDoc() {
		return doc;
	}

	public List<String> getHighlights() {
		return highlights;
	}

	public String getFilenames() {
		return filenames;
	}

	public void setFilenames(String filenames) {
		this.filenames = filenames;
	}

	public void setHighlight(List<String> highlights) {
		this.highlights = highlights;
	}

	public void addHighlight(String highlight) {
		this.highlights.add(highlight);
	}
	
}

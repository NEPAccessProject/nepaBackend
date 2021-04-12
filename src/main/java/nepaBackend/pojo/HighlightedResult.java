package nepaBackend.pojo;

import java.util.ArrayList;
import java.util.List;

import nepaBackend.model.EISDoc;

public class HighlightedResult {
	private final EISDoc doc;
	private List<String> highlights;
	private List<String> filenames;
	
	public HighlightedResult(EISDoc doc, List<String> highlights, List<String> filenames) {
		this.doc = doc;
		this.highlights = highlights;
		this.filenames = filenames;
	}

	public HighlightedResult() {
		this.doc = new EISDoc();
		this.highlights = new ArrayList<String>();
		this.filenames = new ArrayList<String>();
	}

	public EISDoc getDoc() {
		return doc;
	}

	public List<String> getHighlights() {
		return highlights;
	}

	public List<String> getFilenames() {
		return filenames;
	}

	// Expect highlights to simply be built in the same order as filenames from matches, 
	// so we don't need to "link" them beyond a shared index
	public void setFilenames(List<String> filenames) {
		this.filenames = filenames;
	}

	public void addFilename(String filename) {
		this.filenames.add(filename);
	}

	public void setHighlight(List<String> highlights) {
		this.highlights = highlights;
	}

	public void addHighlight(String highlight) {
		this.highlights.add(highlight);
	}
}

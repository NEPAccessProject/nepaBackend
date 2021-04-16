package nepaBackend.controller;

import java.util.List;

import nepaBackend.model.EISDoc;

public class MetadataWithContext3 {
	private List<Integer> ids;
	private final EISDoc doc;
	private List<String> highlights;
	private String filenames;
	private float score;
	
	public MetadataWithContext3(List<Integer> ids, EISDoc doc, List<String> highlights, String filenames, float score) {
		this.ids = ids;
		this.doc = doc;
		this.highlights = highlights;
		this.filenames = filenames;
		this.setScore(score);
	}
	
	public List<Integer> getIds() {
		return ids;
	}

	public Integer getIdAt(int indx) {
		return ids.get(indx);
	}
	
	public void addId(int luceneId) {
		this.ids.add(luceneId);
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

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}
	
}

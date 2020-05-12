package nepaBackend.controller;

import nepaBackend.model.EISDoc;

public class MetadataWithContext {
	private final EISDoc doc;
	private final String highlight;
	private final String filename;
	
	public MetadataWithContext(EISDoc doc, String highlights, String filename) {
		this.doc = doc;
		this.highlight = highlights;
		this.filename = filename;
	}

	public EISDoc getDoc() {
		return doc;
	}

	public String getHighlight() {
		return highlight;
	}

	public String getFilename() {
		return filename;
	}
	
}

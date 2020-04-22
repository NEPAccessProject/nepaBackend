package nepaBackend.controller;

import nepaBackend.model.EISDoc;

public class MetadataWithContext {
	private final EISDoc doc;
	private final String highlight;
	
	public MetadataWithContext(EISDoc doc, String highlights) {
		this.doc = doc;
		this.highlight = highlights;
	}

	public EISDoc getDoc() {
		return doc;
	}

	public String getHighlight() {
		return highlight;
	}
	
}

package nepaBackend.pojo;

import nepaBackend.model.EISDoc;

public class ReducedText {
	public Long id;
	public EISDoc eisdoc;
	public String filename;
	
	public ReducedText() { }

	public ReducedText(Long id, EISDoc eisdoc, String filename) {
		this.id = id;
		this.eisdoc = eisdoc;
		this.filename = filename;
	}
}

package nepaBackend.pojo;

import java.util.List;

import nepaBackend.model.EISDoc;

/** Uses ints because apparently that's how lucene stores lucene document IDs, which this provides */
public class DocWithFilenames {
	private EISDoc doc;
	private List<String> filenames;

	public DocWithFilenames() {
		
	}

	public DocWithFilenames(EISDoc doc, List<String> filenames) {
		this.doc = doc;
		this.filenames = filenames;
	}


	public EISDoc getDoc() {
		return doc;
	}

	public void setDoc(EISDoc doc) {
		this.doc = doc;
	}

	public List<String> getFilenames() {
		return filenames;
	}

	public void setFilenames(List<String> filenames) {
		this.filenames = filenames;
	}
}

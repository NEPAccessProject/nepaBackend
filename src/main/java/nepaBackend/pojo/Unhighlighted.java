package nepaBackend.pojo;

import java.util.List;

/** Uses ints because apparently that's how lucene stores lucene document IDs, which this provides */
public class Unhighlighted {
	private List<Integer> luceneIds;
	private String filename;

	public Unhighlighted() {
		
	}

	public Unhighlighted(List<Integer> luceneIds, String filename) {
		this.luceneIds = luceneIds;
		this.filename = filename;
	}

	public List<Integer> getLuceneIds() {
		return luceneIds;
	}
	
	public Integer getId(int i) {
		return luceneIds.get(i);
	}

	public String getFilename() {
		return filename;
	}
	
}

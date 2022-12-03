package nepaBackend.pojo;

import java.util.List;

import nepaBackend.model.EISDoc;
import nepaBackend.model.EISMatch;

public class EISMatchData {
	private final List<EISMatch> matches;
	private final List<EISDoc> docs;
	
	public EISMatchData(List<EISMatch> matches, List<EISDoc> docs) {
		this.matches = matches;
		this.docs = docs;
	}
	
	public List<EISMatch> getMatches() { return matches; }
	public List<EISDoc> getDocs() { return docs; }
}

package nepaBackend;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nepaBackend.model.EISDoc;
import nepaBackend.model.EISMatch;

@Service
public class DocService {
	@Autowired
	private DocRepository docRepository;
	
	public List<EISDoc> getAllDistinctBy(int id, List<Integer> idList1, List<Integer> idList2) {
		return docRepository.queryBy(id, idList1, idList2);
	}
	
	public void saveEISDoc(EISDoc doc) {
		docRepository.save(doc);
	}
}

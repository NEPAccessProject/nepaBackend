package nepaBackend;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nepaBackend.model.EISDoc;

@Service
public class DocService {
	@Autowired
	private DocRepository docRepository;
	
	public List<EISDoc> getAllDistinctBy(int id, List<Integer> idList1, List<Integer> idList2) {
		return docRepository.queryBy(id, idList1, idList2);
	}
	
	public EISDoc saveEISDoc(EISDoc doc) {
		return docRepository.save(doc);
	}
	
//	public List<String> getAllTitles() {
//		return docRepository.queryAllTitles();
//	}
	
	// TODO: Do a natural language mode search to get top X (5-10?) suggestions
	// and then plug them into the search box as selectable suggestions
	public List<String> getByTitle(String title) {
		return docRepository.queryByTitle(title);
	}

	public Optional<EISDoc> findById(Long id) {
		return docRepository.findById(id);
	}
}

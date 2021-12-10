package nepaBackend;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nepaBackend.model.EISDoc;

@Service
public class GeojsonLookupService {
	@Autowired
	private GeojsonLookupRepository geoRepo;
	@Autowired
	private DocRepository docRepository;


	/** Restore given EISDoc from given update log, save eisdoc, save pre-restore update log; 
	 * return restored EISDoc */
	public boolean existsByProcess(String id) {
		boolean result = false;
		List<EISDoc> data = docRepository.findAllByProcessId(Long.parseLong(id));
		
		for(EISDoc datum : data) {
			if(result) {
				return result;
			} else {
				result = geoRepo.existsByEisdoc(datum);
			}
		}
		
		return result;
	}
	
}
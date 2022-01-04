package nepaBackend;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nepaBackend.model.EISDoc;
import nepaBackend.model.GeojsonLookup;

@Service
public class GeojsonLookupService {
	@Autowired
	private GeojsonLookupRepository geoRepo;
	@Autowired
	private DocRepository docRepository;


	public List<String> findAllGeojsonByEisdoc(String id) {
		List<GeojsonLookup> allData = geoRepo.findAllByEisdoc(
				docRepository.findById(Long.parseLong(id)).get()
		);
		
		List<String> geoData = new ArrayList<String>();
		for(GeojsonLookup datum : allData) {
			geoData.add(datum.getGeojson().getGeojson());
		}
		
		return geoData;
	}
	/** Given a process id, check geojson data for match on eisdoc ids from that process */
	public List<String> findAllGeojsonByEisdocIn(String id) {
		List<GeojsonLookup> data = geoRepo.findAllByEisdocIn(
				docRepository.findAllByProcessId(Long.parseLong(id))
		);
		
		List<String> results = new ArrayList<String>();
		for(GeojsonLookup datum: data) {
			results.add(datum.getGeojson().getGeojson());
		}
		
		return results;
	}

	/** Given a process id, check GeojsonLookup table for match on eisdoc ids from that process */
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
	
	
	public List<GeojsonLookup> findAllByEisdoc(String id) {
		return geoRepo.findAllByEisdoc(docRepository.findById(Long.parseLong(id)).get());
	}
	/** Given a process id, get GeojsonLookup data for eisdoc ids from that process */
	public List<GeojsonLookup> findAllByEisdocIn(String id) {
		return geoRepo.findAllByEisdocIn(docRepository.findAllByProcessId(Long.parseLong(id)));
	}
	public boolean existsByEisdoc(String id) {
		return geoRepo.existsByEisdoc(docRepository.findById(Long.parseLong(id)).get());
	}
	

	public List<GeojsonLookup> findAll() {
		return geoRepo.findAll();
	}
	
}
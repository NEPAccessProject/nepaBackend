package nepaBackend;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nepaBackend.model.EISDoc;
import nepaBackend.model.Geojson;
import nepaBackend.model.GeojsonLookup;

@Service
public class GeojsonLookupService {
	@Autowired
	private GeojsonLookupRepository geoLookupRepo;
	@Autowired
	private DocRepository docRepository;



	public List<String> findOtherGeojsonByDocList(List<Long> lids) {
		List<GeojsonLookup> allData = geoLookupRepo.findDistinctGeojsonByEisdocIdIn(lids);
		
		List<String> geoData = new ArrayList<String>();
		for(GeojsonLookup datum : allData) {
			if(datum.getGeojson().getGeoId() >= 5000000) { // geoid for "other" type is >= 5000000: non-county/state
				geoData.add(datum.getGeojson().getGeojson());
			}
		}
		
		return geoData;
	}
	// TODO: Get a List<Object[]> or something and include the counts.
	// Best approach is probably a custom join query with a subselect/group for counts
	public List<String> findAllGeojsonByDocList(List<Long> lids) {
		List<GeojsonLookup> allData = geoLookupRepo.findDistinctGeojsonByEisdocIdIn(lids);
		
		List<String> geoData = new ArrayList<String>();
		for(GeojsonLookup datum : allData) {
			geoData.add(datum.getGeojson().getGeojson());
		}
		
		return geoData;
	}
	// TODO: Get a List<Object[]> or something and include the counts.
	// Best approach is probably a custom join query with a subselect/group for counts
	public List<String> findAllStateCountyGeojsonByDocList(List<Long> lids) {
		List<GeojsonLookup> allData = geoLookupRepo.findDistinctGeojsonByEisdocIdIn(lids);
		
		List<String> geoData = new ArrayList<String>();
		for(GeojsonLookup datum : allData) {
			if(datum.getGeojson().getGeoId() < 5000000) { // geoid for "other" type is >= 5000000: non-county/state
				geoData.add(datum.getGeojson().getGeojson());
			}
		}
		
		return geoData;
	}
	public List<String> findAllGeojsonByEisdocId(String id) {
		List<GeojsonLookup> allData = geoLookupRepo.findDistinctGeojsonByEisdocId(
				Long.parseLong(id)
		);
		
		List<String> geoData = new ArrayList<String>();
		for(GeojsonLookup datum : allData) {
			geoData.add(datum.getGeojson().getGeojson());
		}
		
		return geoData;
	}
	/** Given a process id, check geojson data for match on eisdoc ids from that process */
	public List<String> findAllGeojsonByProcessId(String id) {
		List<GeojsonLookup> data = geoLookupRepo.findDistinctGeojsonByEisdocIn(
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
				result = geoLookupRepo.existsByEisdoc(datum);
			}
		}
		
		return result;
	}
	
	
	public List<GeojsonLookup> findAllByEisdoc(String id) {
		return geoLookupRepo.findAllByEisdoc(docRepository.findById(Long.parseLong(id)).get());
	}
	/** Given a process id, get GeojsonLookup data for eisdoc ids from that process */
	public List<GeojsonLookup> findAllByEisdocIn(String id) {
		return geoLookupRepo.findAllByEisdocIn(docRepository.findAllByProcessId(Long.parseLong(id)));
	}
	public boolean existsByEisdoc(String id) {
		return geoLookupRepo.existsByEisdoc(docRepository.findById(Long.parseLong(id)).get());
	}
	

	public List<GeojsonLookup> findAll() {
		return geoLookupRepo.findAll();
	}
	public void save(GeojsonLookup geoLookupForImport) {
		geoLookupRepo.save(geoLookupForImport);
	}
	public boolean existsByGeojsonAndEisdoc(Geojson geojson, EISDoc eisdoc) {
		return geoLookupRepo.existsByGeojsonAndEisdoc(geojson, eisdoc);
	}
	
}
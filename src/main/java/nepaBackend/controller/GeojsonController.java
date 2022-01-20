package nepaBackend.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import nepaBackend.ApplicationUserService;
import nepaBackend.DocRepository;
import nepaBackend.GeojsonLookupService;
import nepaBackend.GeojsonRepository;
import nepaBackend.model.EISDoc;
import nepaBackend.model.Geojson;
import nepaBackend.model.GeojsonLookup;
import nepaBackend.pojo.UploadInputs;
import nepaBackend.pojo.UploadInputsGeo;
import nepaBackend.pojo.UploadInputsGeoLinks;

@RestController
@RequestMapping("/geojson")
public class GeojsonController {

	private static final Logger logger = LoggerFactory.getLogger(GeojsonController.class);

	@Autowired
	GeojsonRepository geoRepo;
	@Autowired
	GeojsonLookupService geoLookupService;
	@Autowired
	DocRepository docRepo;
	@Autowired
	ApplicationUserService applicationUserService;

	/** Returns entire lookup table of objects */
	@CrossOrigin
	@RequestMapping(path = "/get_all", method = RequestMethod.GET)
	private ResponseEntity<List<GeojsonLookup>> getAll(@RequestHeader Map<String, String> headers) {
		try {
			List<GeojsonLookup> data = geoLookupService.findAll();
			
			return new ResponseEntity<List<GeojsonLookup>>(data,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			logger.debug("Couldn't get_all",e);
			return new ResponseEntity<List<GeojsonLookup>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Returns lookup table for a specific document */
	@CrossOrigin
	@RequestMapping(path = "/get_all_for_eisdoc", method = RequestMethod.GET)
	private ResponseEntity<List<GeojsonLookup>> getAllForEisdoc(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
			List<GeojsonLookup> data = geoLookupService.findAllByEisdoc(id);
			
			return new ResponseEntity<List<GeojsonLookup>>(data,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<GeojsonLookup>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Returns geojson strings for a specific document */
	@CrossOrigin
	@RequestMapping(path = "/get_all_geojson_for_eisdoc", method = RequestMethod.GET)
	private ResponseEntity<List<String>> getAllGeojsonForEisdoc(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
			List<String> geoData = geoLookupService.findAllGeojsonByEisdoc(id);
			
			System.out.println("Got geojson data for eisdoc, size of list: " + geoData.size());
			
			return new ResponseEntity<List<String>>(geoData,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<String>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Returns lookup table for a specific process */
	@CrossOrigin
	@RequestMapping(path = "/get_all_for_process", method = RequestMethod.GET)
	private ResponseEntity<List<GeojsonLookup>> getAllForProcess(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
			List<GeojsonLookup> data = geoLookupService.findAllByEisdocIn(id);
			
			System.out.println("Got data, size of list: " + data.size());
			
			return new ResponseEntity<List<GeojsonLookup>>(data,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<GeojsonLookup>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	/** Returns geojson for a specific process */
	@CrossOrigin
	@RequestMapping(path = "/get_all_geojson_for_process", method = RequestMethod.GET)
	private ResponseEntity<List<String>> getAllGeojsonForProcess(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
			List<String> results = geoLookupService.findAllGeojsonByEisdocIn(id);

			System.out.println("Got geojson data, size of list: " + results.size());
			
			return new ResponseEntity<List<String>>(results,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<String>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@CrossOrigin
	@RequestMapping(path = "/exists_for_eisdoc", method = RequestMethod.GET)
	private ResponseEntity<Boolean> existsForEisdoc(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
			boolean result = geoLookupService.existsByEisdoc(id);
			
			return new ResponseEntity<Boolean>(result,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<Boolean>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@CrossOrigin
	@RequestMapping(path = "/exists_for_process", method = RequestMethod.GET)
	private ResponseEntity<Boolean> existsForProcess(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
			boolean result = geoLookupService.existsByProcess(id);
			
			return new ResponseEntity<Boolean>(result,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<Boolean>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/** Logic for creating/updating/skipping metadata records.
	 * 
	 * No match: Create new if valid. 
	 * Match: Update if no existing filename && folder, else skip
	 * @return list of results (dummy results if shouldImport == false)
	 * */

	@CrossOrigin
	@RequestMapping(path = "/import_geo", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importGeo(
				@RequestPart(name="geo") String geo, 
				@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");

		if( !applicationUserService.curatorOrHigher(token) ) {
			return new ResponseEntity<List<String>>(HttpStatus.FORBIDDEN);
		} 
		

		List<String> results = new ArrayList<String>();
		int i = 1;
		
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
		    UploadInputsGeo dto[] = mapper.readValue(geo, UploadInputsGeo[].class);

			results.add("Size: " + dto.length);
			
			// Add/update.  Here's where we could also handle any extra deduplication efforts
			// which would be easy enough if we also got the polygon(s).  Then we could inform user
			// if the polygon exists for a different geo ID already, or if it exists for the same geo ID and name.
			// They would be expected to fix their data and reimport, or leave it skipped.
			for(UploadInputsGeo itr : dto) {
				Geojson geoForImport = new Geojson(itr.feature,itr.name.strip(),Long.parseLong(itr.geo_id));
				
				// Update or add new?
				if(geoRepo.existsByGeoId(geoForImport.getGeoId())) {
					// Update
					results.add("Item " + i 
							+ ": " + "Replacing existing feature:: " + itr.name 
							+ "; geo_id: " + itr.geo_id);
					
					Geojson oldGeoJson = geoRepo.findByGeoId(geoForImport.getGeoId()).get();
					oldGeoJson.setGeojson(geoForImport.getGeojson());
					oldGeoJson.setName(geoForImport.getName());
					
					geoRepo.save(oldGeoJson);
				} else { 
					// Add new
					results.add("Item " + i 
							+ ": " + "Adding new feature for:: " + itr.name 
							+ "; geo_id: " + itr.geo_id);
					
					geoRepo.save(geoForImport);
				}
				
				i++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			results.add(e.getLocalizedMessage());
		}

		results.add("Completed import.");

		return new ResponseEntity<List<String>>(results,HttpStatus.OK);
	}

	@CrossOrigin
	@RequestMapping(path = "/import_geo_links", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importGeoLinks(
				@RequestPart(name="geoLinks") String geoLinks, 
				@RequestHeader Map<String, String> headers) {
		
		String token = headers.get("authorization");

		if( !applicationUserService.curatorOrHigher(token) ) {
			return new ResponseEntity<List<String>>(HttpStatus.FORBIDDEN);
		} 
		

		List<String> results = new ArrayList<String>();
		int i = 1;
		
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
		    UploadInputsGeoLinks dto[] = mapper.readValue(geoLinks, UploadInputsGeoLinks[].class);

			results.add("Spreadsheet rows: " + dto.length);
			
			// Add/update.  Here's where we could also handle any extra deduplication efforts
			// which would be easy enough if we also got the polygon(s).  Then we could inform user
			// if the polygon exists for a different geo ID already, or if it exists for the same geo ID and name.
			// They would be expected to fix their data and reimport, or leave it skipped.
			for(UploadInputsGeoLinks itr : dto) {
				if(itr.geo_id.strip().isBlank()) {
					results.add("Item " + i + ": " + "Error:: No GeoID");
				} else {
					String[] geoIds = itr.geo_id.split(";");
					Optional<EISDoc> doc = docRepo.findById(Long.parseLong(itr.meta_id));
					
					for(int j = 0; j < geoIds.length; j++) {
						Optional<Geojson> geo = geoRepo.findByGeoId(Long.parseLong(geoIds[j].strip()));
						
						if(geo.isPresent() && doc.isPresent()) {
							
							// Skip or add new?
							if( geoLookupService.existsByGeojsonAndEisdoc(geo.get(), doc.get()) ) {
								// Skip
								results.add("Item " + i 
										+ ": " + "Skipping (exists):: " + itr.meta_id 
										+ "; geo_id: " + itr.geo_id);
							} else { 
								// Add new
								results.add("Item " + i 
										+ ": " + "Adding new connection for:: " + itr.meta_id 
										+ "; geo_id: " + itr.geo_id);
		
								GeojsonLookup geoLookupForImport = new GeojsonLookup( geo.get(),doc.get() );
								geoLookupService.save(geoLookupForImport);
							}
						} else {
							results.add("Item " + i 
									+ ": " + "Missing:: doc " + itr.meta_id + ", present? " + doc.isPresent()
									+ "; geo " + itr.geo_id + ", present? " + geo.isPresent());
						}
					}
				}
				
				
				i++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			results.add(e.getLocalizedMessage());
		}

		results.add("Completed import.");

		return new ResponseEntity<List<String>>(results,HttpStatus.OK);
	}

//	@CrossOrigin
//	@RequestMapping(path = "/get_all_geojson", method = RequestMethod.GET)
//	private ResponseEntity<List<Geojson>> getAllGeojson(@RequestHeader Map<String, String> headers) {
//		try {
//			List<GeojsonLookup> data = geoRepo.findAll();
//			
//			List<Geojson> geoData = new ArrayList<Geojson>();
//			
//			for (GeojsonLookup datum : data) {
//				geoData.add(datum.getGeojson());
//			};
//			
//			return new ResponseEntity<List<Geojson>>(geoData,HttpStatus.OK);
//		} catch(Exception e) {
//			e.printStackTrace();
//			logger.debug("Couldn't get_all",e);
//			return new ResponseEntity<List<Geojson>>(HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}
	
//	@CrossOrigin
//	@RequestMapping(path = "/save", method = RequestMethod.POST, consumes = "multipart/form-data")
//	private ResponseEntity<Boolean> save(@RequestPart(name="surveyResult") String surveyResult, 
//				@RequestPart(name="searchTerms", required = false) String searchTerms, 
//				@RequestHeader Map<String, String> headers) {
//		try {
//			String token = headers.get("authorization");
//			ApplicationUser user = applicationUserService.getUserFromToken(token);
//			
//			Survey survey = new Survey(user, surveyResult, searchTerms);
//			surveyRepo.save(survey);
//			
//			return new ResponseEntity<Boolean>(true,HttpStatus.OK);
//		} catch(Exception e) {
//			e.printStackTrace();
//			logger.debug("Couldn't save survey",e);
//			return new ResponseEntity<Boolean>(false,HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}
}

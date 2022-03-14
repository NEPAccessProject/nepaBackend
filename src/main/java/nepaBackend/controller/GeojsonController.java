package nepaBackend.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

import nepaBackend.ApplicationUserService;
import nepaBackend.DocRepository;
import nepaBackend.GeojsonLookupService;
import nepaBackend.GeojsonRepository;
import nepaBackend.model.EISDoc;
import nepaBackend.model.Geojson;
import nepaBackend.model.GeojsonLookup;
import nepaBackend.pojo.GeodataWithCount;
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

	/** Returns all state and county geojson data */
	@CrossOrigin
	@RequestMapping(path = "/get_all_state_county", method = RequestMethod.GET)
	private ResponseEntity<List<Geojson>> findAllGeojsonStateCounty(@RequestHeader Map<String, String> headers) {

		try {
			List<Geojson> data = geoRepo.findAllStateCountyGeojson();
			return new ResponseEntity<List<Geojson>>(data,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<Geojson>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Returns state and county geojson data for all documents listed 
	 * Need to use POST because the payload is somewhat large (~100kb+) */
	@CrossOrigin
	@RequestMapping(path = "/get_geodata_other_for_eisdocs", method = RequestMethod.POST)
	private ResponseEntity<List<GeodataWithCount>> findOtherGeojsonByDocList(@RequestHeader Map<String, String> headers,
				@RequestBody String ids) {
		
		try {
			JSONObject jso = new JSONObject(ids);
			JSONArray jsa = jso.getJSONArray("ids");
			List<Long> lids = new ArrayList<Long>();
			for(int i = 0; i < jsa.length(); i++) {
				lids.add(jsa.getLong(i));
			}

			List<GeodataWithCount> data = geoLookupService.findOtherGeojsonByDocList(lids);
			
			return new ResponseEntity<List<GeodataWithCount>>(data,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<GeodataWithCount>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	/** Returns state and county geojson data for all documents listed 
	 * Need to use POST because the payload is somewhat large (~100kb+) */
	@CrossOrigin
	@RequestMapping(path = "/get_all_geodata_for_eisdocs", method = RequestMethod.POST)
	private ResponseEntity<List<GeodataWithCount>> findAllGeojsonByDocList(@RequestHeader Map<String, String> headers,
				@RequestBody String ids) {
		
		try {
			JSONObject jso = new JSONObject(ids);
			JSONArray jsa = jso.getJSONArray("ids");
			List<Long> lids = new ArrayList<Long>();
			for(int i = 0; i < jsa.length(); i++) {
				lids.add(jsa.getLong(i));
			}

			List<GeodataWithCount> data = geoLookupService.findAllGeojsonByDocList(lids);
			
			return new ResponseEntity<List<GeodataWithCount>>(data,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<GeodataWithCount>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	/** Returns state and county geojson data for all documents listed 
	 * Need to use POST because the payload is somewhat large (~100kb+) */
	@CrossOrigin
	@RequestMapping(path = "/get_all_state_county_for_eisdocs", method = RequestMethod.POST)
	private ResponseEntity<List<GeodataWithCount>> findAllGeojsonStateCountyByDocList(@RequestHeader Map<String, String> headers,
				@RequestBody String ids) {

		try {
			JSONObject jso = new JSONObject(ids);
			JSONArray jsa = jso.getJSONArray("ids");
			List<Long> lids = new ArrayList<Long>();
			for(int i = 0; i < jsa.length(); i++) {
				lids.add(jsa.getLong(i));
			}

			List<GeodataWithCount> data = geoLookupService.findAllStateCountyGeojsonByDocList(lids);
			
			return new ResponseEntity<List<GeodataWithCount>>(data,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<GeodataWithCount>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/** Returns geojson strings for a specific document */
	@CrossOrigin
	@RequestMapping(path = "/get_all_geojson_for_eisdoc", method = RequestMethod.GET)
	private ResponseEntity<List<String>> getAllGeojsonForEisdoc(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
			List<String> geoData = geoLookupService.findAllGeojsonByEisdocId(id);
			
//			System.out.println("Got geojson data for eisdoc, size of list: " + geoData.size());
			
			return new ResponseEntity<List<String>>(geoData,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<String>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Returns geojson strings for a specific process */
	@CrossOrigin
	@RequestMapping(path = "/get_all_geojson_for_process", method = RequestMethod.GET)
	private ResponseEntity<List<String>> getAllGeojsonForProcess(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
			List<String> results = geoLookupService.findAllGeojsonByProcessId(id);

//			System.out.println("Got geojson data, size of list: " + results.size());
			
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
	
	/** Logic for creating/updating/skipping geojson records.
	 * 
	 * @return list of results 
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
					// skip?
					results.add("Item " + i 
							+ ": " + "Skipped (exists):: " + itr.name 
							+ "; geo_id: " + itr.geo_id);
					
					// Update
//					results.add("Item " + i 
//							+ ": " + "Replacing existing feature:: " + itr.name 
//							+ "; geo_id: " + itr.geo_id);
//					
//					Geojson oldGeoJson = geoRepo.findByGeoId(geoForImport.getGeoId()).get();
//					oldGeoJson.setGeojson(geoForImport.getGeojson());
//					oldGeoJson.setName(geoForImport.getName());
//					
//					geoRepo.save(oldGeoJson);
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
	
	
	/** Logic for creating/updating/skipping geojson records.
	 * 
	 * @return result
	 * */

	@CrossOrigin
	@RequestMapping(path = "/import_geo_one", method = RequestMethod.POST, consumes = "multipart/form-data")
	private ResponseEntity<List<String>> importGeoOne(
				@RequestPart(name="geo") String geo, 
				@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");

		if( !applicationUserService.curatorOrHigher(token) ) {
			return new ResponseEntity<List<String>>(HttpStatus.FORBIDDEN);
		} 
		

		List<String> results = new ArrayList<String>();
		
	    try {
	    	
	    	ObjectMapper mapper = new ObjectMapper();
		    UploadInputsGeo itr = mapper.readValue(geo, UploadInputsGeo.class);
			
			// Add/update.  Here's where we could also handle any extra deduplication efforts
			// which would be easy enough if we also got the polygon(s).  Then we could inform user
			// if the polygon exists for a different geo ID already, or if it exists for the same geo ID and name.
			// They would be expected to fix their data and reimport, or leave it skipped.
			Geojson geoForImport = new Geojson(itr.feature,itr.name.strip(),Long.parseLong(itr.geo_id));
			
			// Update or add new?
			if(geoRepo.existsByGeoId(geoForImport.getGeoId())) {
				// skip?
				results.add("Skipped (exists):: " + itr.name 
						+ "; geo_id: " + itr.geo_id);
				
				// Update
//				results.add("Replacing existing feature:: " + itr.name 
//						+ "; geo_id: " + itr.geo_id);
//				
//				Geojson oldGeoJson = geoRepo.findByGeoId(geoForImport.getGeoId()).get();
//				oldGeoJson.setGeojson(geoForImport.getGeojson());
//				oldGeoJson.setName(geoForImport.getName());
//				
//				geoRepo.save(oldGeoJson);
			} else { 
				// Add new
				results.add("Adding new feature for:: " + itr.name 
						+ "; geo_id: " + itr.geo_id);
				
				geoRepo.save(geoForImport);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			results.add(e.getLocalizedMessage());
		}

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
				if(itr.geo_id == null || itr.geo_id.isBlank()) {
					results.add("Item " + i + ": " + "Error:: No GeoID");
				} else {
					String[] geoIds = itr.geo_id.split(";");
					
					for(int j = 0; j < geoIds.length; j++) {
							
						// Skip or add new?
						if(geoLookupService.existsByGeojsonAndEisdoc(geoIds[j], itr.meta_id))  {
							// Skip
							results.add("Item " + i 
									+ ": " + "Skipping (exists):: " + itr.meta_id 
									+ "; geo_id: " + geoIds[j]);
						} else { 
							// Add new
							try {
								Optional<EISDoc> doc = docRepo.findById(Long.parseLong(itr.meta_id.strip()));
								Optional<Geojson> geo = geoRepo.findByGeoId(Long.parseLong(geoIds[j].strip()));
								
								results.add("Item " + i 
										+ ": " + "Adding new connection for:: " + itr.meta_id 
										+ "; geo_id: " + geoIds[j]);
		
								GeojsonLookup geoLookupForImport = new GeojsonLookup( geo.get(),doc.get() );
								geoLookupService.save(geoLookupForImport);
							} catch(java.util.NoSuchElementException e) {
								results.add("Item " + i 
										+ ": " + "Record or geodata missing:: " + itr.meta_id 
										+ "; geo_id: " + geoIds[j]);
							}
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


	// run-once county name replacer for all records with county names
	@CrossOrigin
	@RequestMapping(path = "/replace_county_names", method = RequestMethod.POST)
	private ResponseEntity<String> replace_county_names(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		if(!applicationUserService.isAdmin(token)) {
			return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
		}
		
		List<EISDoc> docs = docRepo.findAll();
		
		String result = "";
		for(EISDoc doc : docs) {
			String county = doc.getCounty();
			if(county != null && county.length() > 0) {
				result += replace_county_name(doc.getId().toString(),headers).getBody();
			}
		}

		return new ResponseEntity<String>(result, HttpStatus.OK);
		
	}
	
	/** For individually setting the county names based on linked geodata. Replaces any existing county names
	 * for the eisdoc for the id provided */
	@CrossOrigin
	@RequestMapping(path = "/replace_county_name", method = RequestMethod.POST)
	private ResponseEntity<String> replace_county_name(
				@RequestPart(name="id") String id, 
				@RequestHeader Map<String, String> headers) {
		
		String token = headers.get("authorization");
		if(!applicationUserService.isAdmin(token)) {
			return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
		}

		// Get old county value and set. Return if no doc
		String result = "";
		Optional<EISDoc> doc = docRepo.findById(Long.parseLong(id));
		if(doc.isEmpty()) {
			return new ResponseEntity<String>("No doc: "+id, HttpStatus.NOT_FOUND);
		}
		
		EISDoc docToChange = doc.get();
		result = "Old: " + docToChange.getCounty() + "\n";
		

		HashMap<Long,String> states = new HashMap<Long,String>();
		states.put((long) 1,"AL");
		states.put((long) 2,"AK");
		states.put((long) 4,"AZ");
		states.put((long) 5,"AR");
		states.put((long) 6,"CA");
		states.put((long) 8,"CO");
		states.put((long) 9,"CT");
		states.put((long) 10,"DE");
		states.put((long) 11,"DC");
		states.put((long) 12,"FL");
		states.put((long) 13,"GA");
		states.put((long) 15,"HI");
		states.put((long) 16,"ID");
		states.put((long) 17,"IL");
		states.put((long) 18,"IN");
		states.put((long) 19,"IA");
		states.put((long) 20,"KS");
		states.put((long) 21,"KY");
		states.put((long) 22,"LA");
		states.put((long) 23,"ME");
		states.put((long) 24,"MD");
		states.put((long) 25,"MA");
		states.put((long) 26,"MI");
		states.put((long) 27,"MN");
		states.put((long) 28,"MS");
		states.put((long) 29,"MO");
		states.put((long) 30,"MT");
		states.put((long) 31,"NE");
		states.put((long) 32,"NV");
		states.put((long) 33,"NH");
		states.put((long) 34,"NJ");
		states.put((long) 35,"NM");
		states.put((long) 36,"NY");
		states.put((long) 37,"NC");
		states.put((long) 38,"ND");
		states.put((long) 39,"OH");
		states.put((long) 40,"OK");
		states.put((long) 41,"OR");
		states.put((long) 42,"PA");
		states.put((long) 44,"RI");
		states.put((long) 45,"SC");
		states.put((long) 46,"SD");
		states.put((long) 47,"TN");
		states.put((long) 48,"TX");
		states.put((long) 49,"UT");
		states.put((long) 50,"VT");
		states.put((long) 51,"VA");
		states.put((long) 53,"WA");
		states.put((long) 54,"WV");
		states.put((long) 55,"WI");
		states.put((long) 56,"WY");
		states.put((long) 72,"PR");
		
		
		List<String> data = this.getAllGeojsonForEisdoc(headers, id).getBody();
		if(data.size() == 0) {
			return new ResponseEntity<String>("No geojson: "+id, HttpStatus.NOT_FOUND);
		}
		
		// e.g. data[0] = { "type":"Feature","properties":{"STATEFP":"04","STATENS":null,
		// "AFFGEOID":"0500000US04019","GEOID":"04019","STUSPS":null,"NAME":"Pima","LSAD":"06",
		// "ALAND":23794796847,"AWATER":5251370,"COUNTYFP":"019","COUNTYNS":"00025446"},
		// "geometry":{ "type":"Polygon","coordinates":[[[-113.333897,32.504938],...]] } }

		result += "New: ";
		List<String> names = new ArrayList<String>();
		for(String datum : data) {
			JSONObject newDatum = new JSONObject(datum);
			JSONObject properties = newDatum.getJSONObject("properties");
			long geoid = properties.getLong("GEOID");
			
			if(geoid > 72 && geoid < 5000000) { // county, not state or other
				String name = "";
				// STATEFP is the same as the relevant state's GEOID, so we can match it with states hashmap
				String statefp = properties.getString("STATEFP");
				try {
					name = properties.getString("NAME");
				} catch(JSONException e) {
//					System.out.println("Property case sensitivity issue?");
					name = properties.getString("name");
				} finally {
					names.add(states.get(Long.parseLong(statefp)) + ": " + name);
				}
			} 
		}
		
		
		// Convert list to single string with ; delimiter
		String newCounty = String.join(";",names);
		

		// Set new county name
		docToChange.setCounty(newCounty);
		docRepo.save(docToChange);
		
		result += newCounty + "\n";
		
		

		return new ResponseEntity<String>(result, HttpStatus.OK);
		
	}



	/** UNUSED Returns lookup table for a specific document */
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

	/** UNUSED Returns lookup table for a specific process */
	@CrossOrigin
	@RequestMapping(path = "/get_all_for_process", method = RequestMethod.GET)
	private ResponseEntity<List<GeojsonLookup>> getAllForProcess(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
			List<GeojsonLookup> data = geoLookupService.findAllByEisdocIn(id);
			
//			System.out.println("Got data, size of list: " + data.size());
			
			return new ResponseEntity<List<GeojsonLookup>>(data,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<GeojsonLookup>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
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

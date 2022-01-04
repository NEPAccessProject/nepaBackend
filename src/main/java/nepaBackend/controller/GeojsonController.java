package nepaBackend.controller;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.DocRepository;
import nepaBackend.GeojsonLookupService;
import nepaBackend.model.GeojsonLookup;

@RestController
@RequestMapping("/geojson")
public class GeojsonController {

	private static final Logger logger = LoggerFactory.getLogger(GeojsonController.class);
	
	@Autowired
	GeojsonLookupService geoService;
	@Autowired
	DocRepository docRepo;
//	@Autowired
//	ApplicationUserService applicationUserService;

	/** Returns entire lookup table of objects */
	@CrossOrigin
	@RequestMapping(path = "/get_all", method = RequestMethod.GET)
	private ResponseEntity<List<GeojsonLookup>> getAll(@RequestHeader Map<String, String> headers) {
		try {
			List<GeojsonLookup> data = geoService.findAll();
			
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
			List<GeojsonLookup> data = geoService.findAllByEisdoc(id);
			
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
			List<String> geoData = geoService.findAllGeojsonByEisdoc(id);
			
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
			List<GeojsonLookup> data = geoService.findAllByEisdocIn(id);
			
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
			List<String> results = geoService.findAllGeojsonByEisdocIn(id);

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
			boolean result = geoService.existsByEisdoc(id);
			
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
			boolean result = geoService.existsByProcess(id);
			
			return new ResponseEntity<Boolean>(result,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<Boolean>(HttpStatus.INTERNAL_SERVER_ERROR);
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

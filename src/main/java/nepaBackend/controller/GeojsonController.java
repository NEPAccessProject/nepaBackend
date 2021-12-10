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
import nepaBackend.GeojsonLookupRepository;
import nepaBackend.GeojsonLookupService;
import nepaBackend.model.GeojsonLookup;

@RestController
@RequestMapping("/geojson")
public class GeojsonController {

	private static final Logger logger = LoggerFactory.getLogger(GeojsonController.class);
	
	@Autowired
	GeojsonLookupRepository geoRepo;
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
			List<GeojsonLookup> data = geoRepo.findAll();
			
			return new ResponseEntity<List<GeojsonLookup>>(data,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			logger.debug("Couldn't get_all",e);
			return new ResponseEntity<List<GeojsonLookup>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Returns lookup table for a specific document (at this point we could probably just return 
	 * the geojson and not include the eisdoc metadata, since the caller presumably knows that already */
	@CrossOrigin
	@RequestMapping(path = "/get_all_for_eisdoc", method = RequestMethod.GET)
	private ResponseEntity<List<GeojsonLookup>> getAllForEisdoc(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
			List<GeojsonLookup> data = geoRepo.findAllByEisdoc(docRepo.findById(Long.parseLong(id)).get());
			
			return new ResponseEntity<List<GeojsonLookup>>(data,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<GeojsonLookup>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/** Returns lookup table for a specific document (at this point we could probably just return 
	 * the geojson and not include the eisdoc metadata, since the caller presumably knows that already */
	@CrossOrigin
	@RequestMapping(path = "/get_all_for_process", method = RequestMethod.GET)
	private ResponseEntity<List<GeojsonLookup>> getAllForProcess(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
//			List<EISDoc> docs = docRepo.findAllByProcessId(Long.parseLong(id));
//			List<Long> idList = new ArrayList<Long>();
//			for(EISDoc doc : docs) {
//				idList.add(doc.getId());
//			}
			List<GeojsonLookup> data = geoRepo.findAllByEisdocIn(docRepo.findAllByProcessId(Long.parseLong(id)));
			System.out.println("Got geojson data, size of list: " + data.size());
			return new ResponseEntity<List<GeojsonLookup>>(data,HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<List<GeojsonLookup>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@CrossOrigin
	@RequestMapping(path = "/exists_for_eisdoc", method = RequestMethod.GET)
	private ResponseEntity<Boolean> existsForEisdoc(@RequestHeader Map<String, String> headers,
				@RequestParam String id) {
		try {
			boolean result = geoRepo.existsByEisdoc(docRepo.findById(Long.parseLong(id)).get());
			
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

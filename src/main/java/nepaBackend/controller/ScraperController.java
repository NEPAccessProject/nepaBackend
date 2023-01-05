package nepaBackend.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.ApplicationUserService;
import nepaBackend.OSConstants;
import nepaBackend.model.ApplicationUser;

@RestController
@RequestMapping("/scraper")
public class ScraperController {

	private static final Logger logger = LoggerFactory.getLogger(ScraperController.class);

	private static final ScheduledExecutorService scheduler =
			Executors.newSingleThreadScheduledExecutor();
	
	@Autowired
	ApplicationUserService applicationUserService;
	
	public ScraperController() {
//		scheduleScraper();
	}
	
	// Schedule scraper for every Sunday, at whatever time this is called, Mountain Time
	private void scheduleScraper() {
		ZonedDateTime zonedNow = ZonedDateTime.now(ZoneId.of("America/Phoenix"));
		// there should really be a more elegant way to do this without java EE
		Map<DayOfWeek, Integer> dayToDelay = new HashMap<DayOfWeek, Integer>();
				dayToDelay.put(DayOfWeek.SUNDAY, 0);
				dayToDelay.put(DayOfWeek.MONDAY, 6);
				dayToDelay.put(DayOfWeek.TUESDAY, 5);
				dayToDelay.put(DayOfWeek.WEDNESDAY, 4);
				dayToDelay.put(DayOfWeek.THURSDAY, 3);
				dayToDelay.put(DayOfWeek.FRIDAY, 2);
				dayToDelay.put(DayOfWeek.SATURDAY, 1);
		DayOfWeek dayOfWeek = zonedNow.getDayOfWeek();
		int delayInDays = dayToDelay.get(dayOfWeek);

		// run at 1AM Phoenix time on Sunday
        ZonedDateTime zonedNextTarget = zonedNow.withHour(1).withMinute(0).withSecond(0);
        zonedNextTarget = zonedNextTarget.plusDays(delayInDays);
        
        Duration duration = Duration.between(zonedNow, zonedNextTarget);
        long initialDelay = duration.getSeconds();
        long secondsInAWeek = 604800; // time period between runs: 1 week: 60*60*24*7 seconds
        
        System.out.println("Initial scraper run scheduled in # seconds: " + initialDelay);
        
		scheduler.scheduleAtFixedRate(new Runnable() {
		    @Override
		    public void run() {
		    	System.out.println("Hello world");
		    }
		}, initialDelay, secondsInAWeek, TimeUnit.SECONDS);
	}
	
	private void restartScraper() {
		if(scheduler.isTerminated()) {
			scheduleScraper();
		}
	}
	
	private void killScraper() {
        try {
        	scheduler.shutdownNow();
        } catch (Exception ex) {
            logger.error(null, ex);
        }
	}

	// Since the Python is an external script, this probably won't actually stop it if it's active.
	// It could only stop any other code awaiting it, like if file ingestion was queued
    private void stopScraper()
    {
    	scheduler.shutdown();
        try {
        	scheduler.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            logger.error(null, ex);
        }
    }
    
    /** After this is done, we can process and connect the files 
     * @throws IOException */
    private static final void ingestMetadata() throws IOException {
        String csvFilePath = OSConstants.SCRAPER_META_OUTPUT_LOCATION_FILENAME;
        Reader reader = Files.newBufferedReader(Paths.get(csvFilePath));
        CSVFormat csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader();
        CSVParser csvParser = csvFormat.parse(reader);

        // e.g. {title=0, documentType=1, commentDate=2, registerDate=3, agency=4, state=5, filename=6}
        System.out.println(csvParser.getHeaderMap()); 
        
        // e.g. {title=Sunrise Wind Project, documentType=Draft, commentDate=, registerDate=12/16/2022, agency=Bureau of Ocean Energy Management, state=Multi, filename=EisDocuments-386741.zip}
        csvParser.getRecords()
        	.forEach(csvRecord -> System.out.println(csvRecord.toMap()));
    }
    
    private static final void ingestFiles() {
    	
    }

    /** Run external python script */
	// TODO: When complete, ingest spreadsheet and .zips, 
	// and then clean up or move any leftover files as necessary.
	private static final void scrape() {
        try{
//        	String prg = "import sys\nprint int(sys.argv[1])+int(sys.argv[2])\n";
//        	BufferedWriter out = new BufferedWriter(new FileWriter("test1.py"));
//        	out.write(prg);
//        	out.close();
//        	int number1 = 10;
//        	int number2 = 32;
        	 
//        	ProcessBuilder pb = new ProcessBuilder("python","test1.py",""+number1,""+number2);
        	String outputDir = OSConstants.SCRAPER_FILE_OUTPUT_LOCATION;
        	String metadataFile = OSConstants.SCRAPER_META_OUTPUT_LOCATION_FILENAME;
        	String pyScriptPath = OSConstants.SCRAPER_LOCATION;
        	String fromDate = "12-16-2022";
        	String toDate = "12-20-2022";
        	// -t toDate is broken on the EPA side, so it's useless to us
        	ProcessBuilder pb = new ProcessBuilder("python",
        			pyScriptPath,
        			"-d", outputDir,
        			"-m", metadataFile,
        			"-f", fromDate,
        			"-t", toDate);
//        	ProcessBuilder pb = new ProcessBuilder("python",
//        			"C:/nepaccess/scrapeepa-main/helloworld.py");
        	
        	Process p = pb.start();

            BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            System.out.println("Running Python starts: " + line);
            int exitCode = p.waitFor();
            System.out.println("Exit Code : "+exitCode);
            line = bfr.readLine();
            System.out.println("First Line: " + line);
            while ((line = bfr.readLine()) != null){
                System.out.println("Python Output: " + line);
            }
            // TODO: After detecting scraping complete, ingest metadata, and then files.
    		try {
    			ingestMetadata();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
	}
	
	@RequestMapping(path = "/is_shutdown", method = RequestMethod.GET)
	private ResponseEntity<Boolean> isShutdown(@RequestHeader Map<String, String> headers) {
		try {
			String token = headers.get("authorization");
			ApplicationUser user = applicationUserService.getUserFromToken(token);
			
			if(applicationUserService.isAdmin(user)) {
				return new ResponseEntity<Boolean>(scheduler.isShutdown(),HttpStatus.OK);
			} else {
				return new ResponseEntity<Boolean>(HttpStatus.UNAUTHORIZED);
			}
		} catch(Exception e) {
			return new ResponseEntity<Boolean>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@RequestMapping(path = "/is_terminated", method = RequestMethod.GET)
	private ResponseEntity<Boolean> isTerminated(@RequestHeader Map<String, String> headers) {
		try {
			String token = headers.get("authorization");
			ApplicationUser user = applicationUserService.getUserFromToken(token);
			
			if(applicationUserService.isAdmin(user)) {
				return new ResponseEntity<Boolean>(scheduler.isTerminated(),HttpStatus.OK);
			} else {
				return new ResponseEntity<Boolean>(HttpStatus.UNAUTHORIZED);
			}
		} catch(Exception e) {
			return new ResponseEntity<Boolean>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(path = "/shutdown", method = RequestMethod.POST)
	private ResponseEntity<Void> shutdownScraper(@RequestHeader Map<String, String> headers) {
		try {
			String token = headers.get("authorization");
			ApplicationUser user = applicationUserService.getUserFromToken(token);
			
			if(applicationUserService.isAdmin(user)) {
				stopScraper();
				return new ResponseEntity<Void>(HttpStatus.OK);
			} else {
				return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
			}
		} catch(Exception e) {
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@RequestMapping(path = "/restart", method = RequestMethod.POST)
	private ResponseEntity<Void> restartScraper(@RequestHeader Map<String, String> headers) {
		try {
			String token = headers.get("authorization");
			ApplicationUser user = applicationUserService.getUserFromToken(token);
			
			if(applicationUserService.isAdmin(user)) {
				restartScraper();
				return new ResponseEntity<Void>(HttpStatus.OK);
			} else {
				return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
			}
		} catch(Exception e) {
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}

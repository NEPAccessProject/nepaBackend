package nepaBackend.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.PasswordChange;
import nepaBackend.model.ApplicationUser;
import nepaBackend.security.PasswordGenerator;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	JdbcTemplate jdbcTemplate;
	
    private ApplicationUserRepository applicationUserRepository;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserController(ApplicationUserRepository applicationUserRepository,
                          BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.applicationUserRepository = applicationUserRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    // Disabled
    @PostMapping("/register")
    private @ResponseBody ResponseEntity<Void> register(@RequestBody ApplicationUser user) {
//    	return new ResponseEntity<Void>(HttpStatus.I_AM_A_TEAPOT);
    	
    	// email address, username are included and saved
    	// role has to be set and password has to be encrypted
    	
    	if(usernameExists(user.getUsername())) { // check for duplicates
    		return new ResponseEntity<Void>(HttpStatus.I_AM_A_TEAPOT); 
    	}
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        applicationUserRepository.save(user);
		return new ResponseEntity<Void>(HttpStatus.OK);
    }
    
    // Generate user route, with sanity and duplicate check
    // Add each user to database with encoded password
    // Return list of users with updated passwords (from BEFORE encoding, obviously)
    @PostMapping(path = "/generate", 
    		consumes = "application/json", 
    		produces = "application/json", 
    		headers = "Accept=application/json")
    public @ResponseBody ResponseEntity<ApplicationUser[]> generate(@RequestBody ApplicationUser users[],
			@RequestHeader Map<String, String> headers) {
    	System.out.println(users);
    	String token = headers.get("authorization");
    	if(!isAdmin(token)) {
    		return new ResponseEntity<ApplicationUser[]>(HttpStatus.UNAUTHORIZED);
    	}
    	
    	ApplicationUser returnUsers[] = new ApplicationUser[users.length];
		PasswordGenerator passwordGenerator = new PasswordGenerator.PasswordGeneratorBuilder()
	            .useDigits(true)
	            .useLower(true)
	            .useUpper(true)
	            .build();
    	int i = 0;
    	for(ApplicationUser user : users) {
    		
    		
    		// TODO: Sanity check
    		// TODO: Deal with no email or no username provided?
    		
        	returnUsers[i] = new ApplicationUser();
        	returnUsers[i].setUsername(user.getUsername());
        	if(usernameExists(user.getUsername())) { // check for duplicates
        		// skip, deal with it externally when you get a result with no password
        	} else {
            	returnUsers[i].setEmailAddress(user.getEmail());
                user.setRole("USER");
            	returnUsers[i].setRole("USER");
        	    String password = passwordGenerator.generate(8); // output ex.: lrU12fmM 75iwI90o
            	returnUsers[i].setPassword(password);
            		
                user.setPassword(bCryptPasswordEncoder.encode(password));
                // TODO: Save all at once instead of individually?
                applicationUserRepository.save(user);
        	}

        	System.out.println(returnUsers[i]);
        	System.out.println(i);
        	i++;    
    	}
    	
    	// Note: IDs will be 0 on the return objects
    	return new ResponseEntity<ApplicationUser[]>(returnUsers, HttpStatus.OK);
    }

    // To check if a username exists earlier than trying to register it.
    @PostMapping("/exists")
    public @ResponseBody boolean usernameExists(@RequestBody String username) {
    	String sQuery = "SELECT COUNT(*) FROM application_user WHERE username = ?";
		// Run query
		int count = jdbcTemplate.queryForObject
		(
			sQuery, new Object[] { username },
			Integer.class
		);
//		System.out.println(username);
//		System.out.println(count);
		if(count > 0) {
			return true;
		}
		return false;
    }

	@PostMapping(path = "/details") // verify user has access?
	public void checkDetails() {}
	
	@PostMapping(path = "/details/changePassword")
	public ResponseEntity<Void> changePassword(@RequestBody PasswordChange passwords,
			@RequestHeader Map<String, String> headers) {

		// TODO: Sanity check new password better, return error if invalid
		if(passwords.newPassword == null || passwords.newPassword.length() == 0 
				|| passwords.newPassword.length() > 50) {
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}

		// get token, which has already been verified
		String token = headers.get("authorization");
		// get ID
        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
                .getId();

		ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();

        Boolean matches = bCryptPasswordEncoder.matches(passwords.oldPassword,
        		user.getPassword());
		
		if(matches) {
			// update
			user.setPassword(bCryptPasswordEncoder.encode(passwords.newPassword));
			applicationUserRepository.save(user);
			return new ResponseEntity<Void>(HttpStatus.OK);
		}

		// if it doesn't match, Unauthorized
		return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
	}

	@PostMapping(path = "/checkAdmin")
	public ResponseEntity<Boolean> checkAdmin(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		boolean result = isAdmin(token);
		HttpStatus returnStatus = HttpStatus.UNAUTHORIZED;
		if(result) {
			returnStatus = HttpStatus.OK;
		}
		return new ResponseEntity<Boolean>(result, returnStatus);
	}
	
	private boolean isAdmin(String token) {
		boolean result = false;
		// get ID
		if(token != null) {
	        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
	                .getId();

			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
			if(user.getRole().contentEquals("ADMIN")) {
				result = true;
			}
		}
		return result;

	}

    // TODO: Change user details (email/username?)
    // an Admin role could optionally be allowed to change any record

    // Login is already handled by /login on base domain by JWTAuthenticationFilter
    // extending UsernamePasswordAuthenticationFilter.
}
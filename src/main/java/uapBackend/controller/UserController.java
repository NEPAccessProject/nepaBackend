package uapBackend.controller;

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

import uapBackend.ApplicationUserRepository;
import uapBackend.PasswordChange;
import uapBackend.model.ApplicationUser;
import uapBackend.security.SecurityConstants;

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
        user.setRole("ROLE_USER");
        applicationUserRepository.save(user);
		return new ResponseEntity<Void>(HttpStatus.OK);
    }
    
    // TODO: Generate user route, with sanity and duplicate check

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
		System.out.println(username);
		System.out.println(count);
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
		if(passwords.newPassword == null || passwords.newPassword.length() == 0) {
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

    // TODO: Change user details (email/username?)
    // an Admin role could optionally be allowed to change any record

    // Login is already handled by /login on base domain by JWTAuthenticationFilter
    // extending UsernamePasswordAuthenticationFilter.
}
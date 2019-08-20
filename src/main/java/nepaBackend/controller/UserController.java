package nepaBackend.controller;

import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.EmailLogRepository;
import nepaBackend.absurdity.Generate;
import nepaBackend.absurdity.PasswordChange;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.EmailLog;
import nepaBackend.security.PasswordGenerator;
import nepaBackend.security.SecurityConstants;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	JdbcTemplate jdbcTemplate;
	
    @Autowired
    private JavaMailSender sender;
	
    private ApplicationUserRepository applicationUserRepository;
    private EmailLogRepository emailLogRepository;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserController(ApplicationUserRepository applicationUserRepository,
    						EmailLogRepository emailLogRepository,
    						BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.applicationUserRepository = applicationUserRepository;
        this.emailLogRepository = emailLogRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    // Disabled
    @PostMapping("/register")
    private @ResponseBody ResponseEntity<Void> register(@RequestBody ApplicationUser user) {
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
    public @ResponseBody ResponseEntity<ApplicationUser[]> generate(@RequestBody Generate gen,
//    		@RequestBody ApplicationUser users[],
//    		@RequestBody boolean shouldSendEmail,
			@RequestHeader Map<String, String> headers) {
    	
    	// TODO: Need an option to not email user their account info
    	// TODO: Change this to true for production
    	boolean sendUserEmails = gen.shouldSend;
    	// TODO: Need an option to include role

    	String token = headers.get("authorization");
    	if(!isAdmin(token)) {
    		return new ResponseEntity<ApplicationUser[]>(HttpStatus.UNAUTHORIZED);
    	}
    	
    	ApplicationUser returnUsers[] = new ApplicationUser[gen.users.length];
		PasswordGenerator passwordGenerator = new PasswordGenerator.PasswordGeneratorBuilder()
	            .useDigits(true)
	            .useLower(true)
	            .useUpper(true)
	            .build();
    	int i = 0;
    	for(ApplicationUser user : gen.users) {
    		if(user != null) { // Don't process an empty line, in case that's possible

        		
        		if(user.getUsername() == null) {
        			user.setUsername("");
        		}
        		
        		// Need returnUsers to keep track of literal passwords
        		// because we need to ensure the new users get their credentials
            	returnUsers[i] = new ApplicationUser();
        		
        		if(user.getUsername().length() < 1) { // No username provided?
        			String[] split = (user.getEmail().split("@"));
        			if(split[1].equalsIgnoreCase("email.arizona.edu")) {
        				// Try using NetID if Arizona email (should just be first part)
        				user.setUsername(split[0]);
        			} else { // Or use the email as the username
        				user.setUsername(user.getEmail());
        			}
        		} 
        		
        		// check for duplicates, length constraints
            	if( usernameInvalid(user.getUsername()) || emailInvalid(user.getEmail()) ) { 
                	returnUsers[i].setEmailAddress(user.getEmail());
            		// skip, deal with it externally when you get a result with no password
            	} else {
                    returnUsers[i].setUsername(user.getUsername());
                	returnUsers[i].setEmailAddress(user.getEmail());
                	// TODO: Eventually may want to set roles from request
                    user.setRole("USER");
                	returnUsers[i].setRole("USER");
                	
            	    String password = passwordGenerator.generate(8); // output ex.: lrU12fmM 75iwI90o
                	returnUsers[i].setPassword(password);
                	// bCrypt internally handles salt and outputs a 60 character encoded string
                    user.setPassword(bCryptPasswordEncoder.encode(password)); 
                    
                    boolean saved = false; // No point in sending the email if not saved
                    // TODO: Save all at once instead of individually?
                    try {
                        applicationUserRepository.save(user);
                        saved = true;
                    } catch(Exception e) {
    					e.printStackTrace();
    					saved = false;
                    }
//                    System.out.println("shouldSend: " + sendUserEmails);
                    if(sendUserEmails && saved && user.getEmail() != null) {
                        try {

                            MimeMessage message = sender.createMimeMessage();
                            MimeMessageHelper helper = new MimeMessageHelper(message);
                        	// TODO: Log some email details to database?
        					helper.setTo(user.getEmail());
        	                helper.setText("An account has been created for you at http://mis-jvinalappl1.microagelab.arizona.edu "
        	                		+ "and your credentials are:\n"
        	                		+ "\nUsername: " + user.getUsername()
        	                		+ "\nPassword: " + password
        	                		+ "\n\nYou can change your password after logging in by clicking on your username in the top right.");
        	                helper.setSubject("NEPAccess Account");
        	                sender.send(message);
        		    		try {
        		    			EmailLog log = new EmailLog();
        		    			log.setEmail(user.getEmail());
        		    			log.setUsername(user.getUsername());
        		    			log.setSent(true);
        		    			log.setEmailType("Generate");
        		    			emailLogRepository.save(log);
        		    		}catch(Exception logEx) {
        		    			// Do nothing
        		    		}
//        	                System.out.println(message);
        				} catch (MessagingException e) {
        					// TODO Auto-generated catch block
        					// Log error to database
        		    		try {
        		    			EmailLog log = new EmailLog();
        		    			log.setEmail(user.getEmail());
        		    			log.setUsername(user.getUsername());
        		    			log.setSent(false);
        		    			log.setEmailType("Generate");
        		    			log.setErrorType(e.toString());
        		    			emailLogRepository.save(log);
        		    		}catch(Exception logEx) {
        		    			// Do nothing
        		    		}
        					e.printStackTrace();
        				}
                    }
                    
                     
            	}

            	i++;   
    		} 
    	}
    	
    	// Email full list to admin
    	emailUserListToAdmin(returnUsers);
    	
    	// Note: IDs will be 0 on the return objects
    	return new ResponseEntity<ApplicationUser[]>(returnUsers, HttpStatus.OK);
    }

    // TODO: Better sanity check than length and uniqueness
	// Helper method validates email
    private boolean emailInvalid(String email) {
		if(email.length() == 0 || email.length() > 191) {
			return true;
		}
		return emailExists(email);
	}

	// Helper method validates username
	private boolean usernameInvalid(String username) {
		if(username.length() == 0 || username.length() > 50) {
			return true;
		}
		return usernameExists(username);
	}

	// To check if an email exists earlier than trying to register it.
    @PostMapping("/emailexists")
    public @ResponseBody boolean emailExists(@RequestBody String email) {
    	String sQuery = "SELECT COUNT(*) FROM application_user WHERE email = ?";
		// Run query
		int count = jdbcTemplate.queryForObject
		(
			sQuery, new Object[] { email },
			Integer.class
		);
//		System.out.println(username);
//		System.out.println(count);
		if(count > 0) {
			return true;
		}
		return false;
    }

	// Helper function for generate()
    private void emailUserListToAdmin(ApplicationUser[] returnUsers) {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for(ApplicationUser user : returnUsers) {
        	sb.append("Account " + i + ":"
        			+ "\nUsername: " + user.getUsername()
        			+ "\nPassword: " + user.getPassword()
        			+ "\nEmail: " + user.getEmail() + "\n\n");
        	i++;
        }
        
        try {
    		helper.setTo(SecurityConstants.EMAIL_HANDLE);
            helper.setText(sb.toString());
            helper.setSubject("NEPAccess Account Generation");
            sender.send(message);
        } catch (MessagingException e) {
			// TODO: Log errors to database
			e.printStackTrace();
        }
		
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

	@PostMapping(path = "/details") // verify user has access (valid JWT)
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

		// Need to use .matches() to verify password manually
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

	// Return true if admin role
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
	
	// Helper function for checkAdmin
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

    // TODO: Route and tool to add/change/remove any user, for admin use only

    // Login is already handled by /login on base domain by JWTAuthenticationFilter
    // extending UsernamePasswordAuthenticationFilter.
}
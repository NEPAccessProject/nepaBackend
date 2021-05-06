package nepaBackend.controller;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;
import com.google.gson.Gson;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.ContactRepository;
import nepaBackend.EmailLogRepository;
import nepaBackend.Globals;
import nepaBackend.OptedOutRepository;
import nepaBackend.SavedSearchRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.Contact;
import nepaBackend.model.EmailLog;
import nepaBackend.model.OptedOut;
import nepaBackend.model.SavedSearch;
import nepaBackend.pojo.ContactForm;
import nepaBackend.pojo.Generate;
import nepaBackend.pojo.PasswordChange;
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
    private ContactRepository contactRepository;
    private OptedOutRepository optedOutRepository;
    private EmailLogRepository emailLogRepository;
    private SavedSearchRepository savedSearchRepository;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserController(ApplicationUserRepository applicationUserRepository,
    						ContactRepository contactRepository,
    						OptedOutRepository optedOutRepository,
    						EmailLogRepository emailLogRepository,
    						SavedSearchRepository savedSearchRepository,
    						BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.applicationUserRepository = applicationUserRepository;
        this.contactRepository = contactRepository;
        this.optedOutRepository = optedOutRepository;
        this.emailLogRepository = emailLogRepository;
        this.savedSearchRepository = savedSearchRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }
    
    @GetMapping("/getAll")
    private @ResponseBody ResponseEntity<List<Object>> getUsersLimited(@RequestHeader Map<String, String> headers) {
    	
//    	String token = headers.get("authorization");
    	
    	if(checkAdmin(headers).getBody() || checkCurator(headers).getBody() || checkApprover(headers).getBody() ) {
    		return new ResponseEntity<List<Object>>(applicationUserRepository.findLimited(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<Object>>(new ArrayList<Object>(), HttpStatus.UNAUTHORIZED);
		}
    }
    
    @PostMapping("/setUserApproved")
    private @ResponseBody ResponseEntity<Boolean> approveUser(@RequestParam Long userId, 
    			@RequestParam boolean approved, 
    			@RequestHeader Map<String, String> headers) {
    	
//    	System.out.println(userId);
//    	System.out.println(approved);
    	
    	String token = headers.get("authorization");
    	
    	if(!checkAdmin(headers).getBody() && !checkCurator(headers).getBody() && !checkApprover(headers).getBody()) {
    		return new ResponseEntity<Boolean>(false, HttpStatus.UNAUTHORIZED);
    	} else {
    		ApplicationUser user = applicationUserRepository.findById(Long.valueOf(userId)).get();

    		// Only admin can deactivate elevated roles
    		if(!approved && (isApprover(token) || isCurator(token))) {
    			if(user.getRole()=="ADMIN" || user.getRole()=="CURATOR" || user.getRole()=="APPROVER") {
    	    		return new ResponseEntity<Boolean>(false, HttpStatus.UNAUTHORIZED);
    			}
    		}
    		
    		user.setActive(approved);
    		applicationUserRepository.save(user);

    		return new ResponseEntity<Boolean>(true, HttpStatus.OK);
    	}
    }
    
    @PostMapping("/setUserVerified")
    private @ResponseBody ResponseEntity<Boolean> verifyUser(@RequestParam Long userId, 
    			@RequestParam boolean approved, 
    			@RequestHeader Map<String, String> headers) {
    	
    	String token = headers.get("authorization");
    	
    	if(!checkAdmin(headers).getBody()) {
    		return new ResponseEntity<Boolean>(false, HttpStatus.UNAUTHORIZED);
    	} else {
    		ApplicationUser user = applicationUserRepository.findById(Long.valueOf(userId)).get();

    		// Only admin can deactivate elevated roles
    		if(!approved && (isApprover(token) || isCurator(token))) {
    			if(user.getRole()=="ADMIN" || user.getRole()=="CURATOR" || user.getRole()=="APPROVER") {
    	    		return new ResponseEntity<Boolean>(false, HttpStatus.UNAUTHORIZED);
    			}
    		}
    		
    		user.setVerified(approved);
    		applicationUserRepository.save(user);

    		return new ResponseEntity<Boolean>(true, HttpStatus.OK);
    	}
    }
    
    @PostMapping("/setUserRole")
    private @ResponseBody ResponseEntity<Boolean> setUserRole(@RequestParam Long userId, 
    			@RequestParam String role, 
    			@RequestHeader Map<String, String> headers) {
    	if(!checkAdmin(headers).getBody()) {
    		return new ResponseEntity<Boolean>(false, HttpStatus.UNAUTHORIZED);
    	} else {
    		ApplicationUser user = applicationUserRepository.findById(Long.valueOf(userId)).get();
    		
    		user.setRole(role);
    		applicationUserRepository.save(user);

    		return new ResponseEntity<Boolean>(true, HttpStatus.OK);
    	}
    }
    
    // TODO: Test saved search functions
    @PostMapping("/getSearches")
    private @ResponseBody ResponseEntity<List<SavedSearch>> getSearchesByUserId(@RequestBody Long userId) {
    	List<SavedSearch> searches = null;
    	try {
        	searches = savedSearchRepository.findByUserId(userId);
    	} catch(Exception e) {
        	return new ResponseEntity<List<SavedSearch>>(HttpStatus.INTERNAL_SERVER_ERROR); 
    	}
    	return new ResponseEntity<List<SavedSearch>>(searches, HttpStatus.OK); 
    }
    
    /** Save new (or update if existing) SavedSearch */
    @PostMapping("/saveSearch")
    private @ResponseBody ResponseEntity<Void> saveSearch(@RequestBody SavedSearch savedSearch) {
    	try {
    		savedSearch.setSavedTime(LocalDateTime.now());
        	savedSearchRepository.save(savedSearch);
    	} catch(Exception e) {
        	return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR); 
    	}
    	return new ResponseEntity<Void>(HttpStatus.OK); 
    }
    
    @PostMapping("/deleteSearch")
    private @ResponseBody ResponseEntity<Void> deleteSavedSearchById(@RequestBody Long id) {
    	try {
        	savedSearchRepository.deleteById(id);
    	} catch(Exception e) {
        	return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR); 
    	}
    	return new ResponseEntity<Void>(HttpStatus.OK); 
    }

    @PostMapping("/deleteSearches")
    private @ResponseBody ResponseEntity<Void> deleteSavedSearchesByIds(@RequestBody List<Long> ids) {
    	try {
    		for (Long id : ids) {
            	savedSearchRepository.deleteById(id);
    		}
    	} catch(Exception e) {
        	return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR); 
    	}
    	return new ResponseEntity<Void>(HttpStatus.OK); 
    }

    @SuppressWarnings("unused")
	@PostMapping("/register")
    private @ResponseBody ResponseEntity<Void> register(@RequestParam String jsonUser, @RequestParam String recaptchaToken) {
    	// email address, username are included and saved
    	// first last affiliation org and job title also included and saved
    	// role has to be set and password has to be encrypted
    	if(!recaptcha(recaptchaToken) && !Globals.TESTING) {
//    		System.out.println("Recaptcha failed");
    		return new ResponseEntity<Void>(HttpStatus.FAILED_DEPENDENCY); 
    	}
    	
    	Gson gson=new Gson();
    	ApplicationUser user=gson.fromJson(jsonUser,ApplicationUser.class);
    	
    	if(usernameExists(user.getUsername().strip())) { // check for duplicates
    		return new ResponseEntity<Void>(HttpStatus.I_AM_A_TEAPOT); 
    	} else if(passwordValid(user.getPassword())) {
    		try {
    			user.setUsername(user.getUsername().strip());
                user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
                user.setFirstName(Globals.normalizeSpace(user.getFirstName()));
                user.setLastName(Globals.normalizeSpace(user.getLastName()));
                user.setEmailAddress(Globals.normalizeSpace(user.getEmail()));
                user.setVerified(false);
                user.setActive(false);
                user.setRole("USER");
                if(isValidUser(user)) { 
                    if(!Globals.TESTING) {applicationUserRepository.save(user);}
                } else {
            		return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST); 
                }
    		} catch(Exception e) {
        		return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR); // 500 if failed register
    		}
    		
    		// email user verification link
			boolean emailed = sendVerificationEmail(user);
			if(emailed) {
				sendApprovalEmail(user);
	    		return new ResponseEntity<Void>(HttpStatus.OK);
			} else {
        		return new ResponseEntity<Void>(HttpStatus.SERVICE_UNAVAILABLE); // 503 if failed email
			}
    	} else {
    		return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST); 
    	}
    }

    // TODO: Get specs for this and enforce on frontend also.
	private boolean passwordValid(String password) {
		if(password.length() > 0) {
			return true;
		}
		return false;
	}

	/** email address, username are included and saved
	* first last affiliation org and job title also included and saved
	* role has to be set and password has to be encrypted
	* comes pre-verified, active but will need to check a checkbox to login temporarily
	* and since admins are creating these the actual user won't know their password until given it 
	* */
    @PostMapping("/pre_register")
    private @ResponseBody ResponseEntity<Void> preRegister(
    			@RequestParam String jsonUser, 
    			@RequestHeader Map<String,String> headers) 
    {
    	if(		!checkAdmin(headers).getBody() 
    			&& !checkCurator(headers).getBody() 
    			&& !checkApprover(headers).getBody()) 
    	{
    		return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
    	} else {
    		Gson gson=new Gson();
        	ApplicationUser user=gson.fromJson(jsonUser,ApplicationUser.class);
        	
        	if(usernameExists(user.getUsername())) { // check for duplicates
        		return new ResponseEntity<Void>(HttpStatus.I_AM_A_TEAPOT); 
        	} else if(passwordValid(user.getPassword())) {
        		try {
                    user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
                    user.setFirstName(Globals.normalizeSpace(user.getFirstName()));
                    user.setLastName(Globals.normalizeSpace(user.getLastName()));
                    user.setEmailAddress(Globals.normalizeSpace(user.getEmail()));
                    user.setVerified(true);
                    user.setActive(true);
                    user.setRole("USER");
                    if(isValidUserPreRegister(user)) { 
                        applicationUserRepository.save(user);
                    } else { 
                		return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST); 
                    }
        		} catch(Exception e) {
        			e.printStackTrace();
            		return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR); // 500 if failed register
        		}
        		
        		return new ResponseEntity<Void>(HttpStatus.OK);
        	} else { // bad password
        		return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST); 
        	}
    	}
    }
    
    private boolean sendApprovalEmail(ApplicationUser user) {
    	
    	if(Globals.TESTING) {
    		return true;
    	} else {
    		
	    	boolean status = true;
	    	
	    	try {
	            MimeMessage message = sender.createMimeMessage();
	            MimeMessageHelper helper = new MimeMessageHelper(message);
	
	            helper.setTo(new String[] {
	            		"derbridge@email.arizona.edu", 
	            		"lauralh@email.arizona.edu"
	    		});
	            message.setFrom(new InternetAddress("NEPAccess <Eller-NepAccess@email.arizona.edu>"));
	            helper.setSubject("NEPAccess Approval Request");
	            helper.setText("This is an automatically generated email due to"
	            		+ " a new account being registered."
	            		+ "\n\nFrom username: " + user.getUsername()
	            		+ "\nEmail: " + user.getEmail()
	            		+ "\n\nUser can be approved at: https://www.nepaccess.org/approve"
	            		+ "\n(Users are not approved by default)"
	            );
	             
	            sender.send(message);
	    		
	    	} catch (MailAuthenticationException e) {
	            logEmail(user.getEmail(), e.toString(), "Approval", false);
	
	//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailAuthenticationException");
	            
	            status = false;
	    	} catch (MailSendException e) {
	            logEmail(user.getEmail(), e.toString(), "Approval", false);
	
	//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailSendException");
	            
	            status = false;
	    	} catch (MailException e) {
	            logEmail(user.getEmail(), e.toString(), "Approval", false);
	            
	//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailException");
	            
	            status = false;
	    	} catch (Exception e) {
	            logEmail(user.getEmail(), e.toString(), "Approval", false);
	            
	//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "Exception");
	            
	            status = false;
	    	}
	    	
	    	if(status) {
	    		try {
	                logEmail(user.getEmail(), "", "Approval", true);
	    		} catch (Exception ex) {
	    			// Do nothing
	    		}
	    	}
	        
	        return status;
    	}
	}

    // More lax requirements.
	private boolean isValidUserPreRegister(ApplicationUser user) {
		// TODO: More clever validation
    	// For now we'll just check for non-empty first/last/username/email/affiliation ("field")
    	boolean returnStatus = true;
    	if(user.getUsername().length() < 1) {
    		returnStatus = false;
    	}
    	if(user.getEmail().length() < 1) {
    		returnStatus = false;
    	}
		return returnStatus;
	}

	private boolean isValidUser(ApplicationUser user) {
		// TODO: More clever validation
    	// For now we'll just check for non-empty first/last/username/email/affiliation ("field")
    	boolean returnStatus = true;
    	if(user.getLastName().length() < 1) {
    		returnStatus = false;
    	}
    	if(user.getFirstName().length() < 1) {
    		returnStatus = false;
    	}
    	if(user.getUsername().length() < 1) {
    		returnStatus = false;
    	}
    	if(user.getEmail().length() < 1) {
    		returnStatus = false;
    	}
    	if(user.getAffiliation().length() < 1) {
    		returnStatus = false;
    	}
		return returnStatus;
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
        			if(split[1].equalsIgnoreCase("email.arizona.edu") || split[1].equalsIgnoreCase("arizona.edu")) {
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
    		    		try {
    		    			EmailLog log = new EmailLog();
    		    			log.setEmail(user.getEmail());
    		    			log.setUsername(user.getUsername());
    		    			log.setSent(false);
    		    			log.setEmailType("Generate");
    		    			log.setLogTime(LocalDateTime.now());
    		    			log.setErrorType(e.toString());
    		    			emailLogRepository.save(log);
    		    		}catch(Exception logEx) {
    		    			// Do nothing
    		    		}
                    }
//                    System.out.println("shouldSend: " + sendUserEmails);
                    if(sendUserEmails && saved && user.getEmail() != null) {
                        try {

                            MimeMessage message = sender.createMimeMessage();
                            MimeMessageHelper helper = new MimeMessageHelper(message);
                        	// TODO: Log some email details to database?
        					helper.setTo(user.getEmail());
        	                helper.setText("An account has been created for you at http://nepaccess.org "
        	                		+ "and your credentials are:\n"
        	                		+ "\nUsername: " + user.getUsername()
        	                		+ "\nPassword: " + password
        	                		+ "\n\nYou can change your password after logging in by clicking on the Profile link in the top right corner."
        	                		+ "\n\nIf you forget your password, you can get a reset link emailed to you by clicking on the Forgot Password? link on the login page.");
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
        		    			log.setLogTime(LocalDateTime.now());
        		    			log.setErrorType(e.toString());
        		    			emailLogRepository.save(log);
        		    		}catch(Exception logEx) {
        		    			// Do nothing
        		    		}
        					e.printStackTrace();
        				}
                    } else {
    		    		try {
    		    			EmailLog log = new EmailLog();
    		    			log.setEmail(user.getEmail());
    		    			log.setUsername(user.getUsername());
    		    			log.setSent(false);
    		    			log.setEmailType("Generate");
    		    			emailLogRepository.save(log);
    		    		}catch(Exception logEx) {
    		    			// Do nothing
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
    
    final String regex = "^(.+)@(.+)$";
    final Pattern pattern = Pattern.compile(regex);
	// Helper method validates email
    private boolean emailInvalid(String email) {
		if(email == null || email.length() == 0) {
			return true;
		} else {
	    	Matcher matcher = pattern.matcher(email);
			if(!matcher.matches() || email.length() > 191) {
				return true;
			}
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
	
	private JsonObject validateCaptcha(String response)
	{
	    JsonObject jsonObject = null;
	    URLConnection connection = null;
	    InputStream is = null;
	    String charset = java.nio.charset.StandardCharsets.UTF_8.name();

	    String url = "https://www.google.com/recaptcha/api/siteverify";
	    try {            
	        String query = String.format("secret=%s&response=%s", 
	        URLEncoder.encode(SecurityConstants.RECAPTCHA_SECRET_KEY, charset), 
	        URLEncoder.encode(response, charset));

	        connection = new URL(url + "?" + query).openConnection();
	        is = connection.getInputStream();
	        JsonReader rdr = Json.createReader(is);
	        jsonObject = rdr.readObject();

	    } catch (IOException ex) {
//	        Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
	    }
	    finally {
	        if (is != null) {
	            try {
	                is.close();
	            } catch (IOException e) {
	            }

	        }
	    }
	    return jsonObject;
	}
	
	private boolean recaptcha(String token) {
		return validateCaptcha(token).getBoolean("success");
	}

	// Test
//    @PostMapping("/recaptcha_test")
//    public @ResponseBody boolean recaptchaTest(@RequestParam String recaptcha) {
//    	return validateCaptcha(recaptcha).getBoolean("success");
//    }

	// To check if an email exists earlier than trying to register it.
    @PostMapping("/email-exists")
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
    
    private boolean sendVerificationEmail(ApplicationUser user) {
    	boolean status = true;
    	
    	try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
             
            helper.setTo(user.getEmail());
            message.setFrom(new InternetAddress("NEPAccess <Eller-NepAccess@email.arizona.edu>"));
            helper.setSubject("NEPAccess Registration Request");
            helper.setText("This is an automatically generated email in response to"
            		+ " a request to register an account linked to this email address."
            		+ "\n\nYour username is: " + user.getUsername()
            		+ "\n\nClick this link to verify your email: " + getVerificationLink(user)
            		+ "\nThe link will remain valid for ten days."
            		+ "\n\nAfter verifying your email, you will be able to use the system as soon "
            		+ "as your account is approved.");
//            helper.setText("This is an automatically generated email in response to"
//            		+ " a request to register an account linked to this email address."
//            		+ "\n\nYour username is: " + user.getUsername()
//            		+ "\n\nClick this link to verify your email: " + getVerificationLink(user)
//            		+ "\nThe link will remain valid for ten days."
//            		+ "\n\nAfter verifying your email, you will be able to use the system when logged in.");
             
            sender.send(message);
    		
    	} catch (MailAuthenticationException e) {
            logEmail(user.getEmail(), e.toString(), "Verification", false);

//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailAuthenticationException");
            
            status = false;
    	} catch (MailSendException e) {
            logEmail(user.getEmail(), e.toString(), "Verification", false);

//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailSendException");
            
            status = false;
    	} catch (MailException e) {
            logEmail(user.getEmail(), e.toString(), "Verification", false);
            
//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailException");
            
            status = false;
    	} catch (Exception e) {
            logEmail(user.getEmail(), e.toString(), "Verification", false);
            
//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "Exception");
            
            status = false;
    	}
    	
    	if(status) {
    		try {
                logEmail(user.getEmail(), "", "Verification", true);
    		} catch (Exception ex) {
    			// Do nothing
    		}
    	}
        
        return status;
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

    @CrossOrigin
	@PostMapping(path = "/verify")
	public ResponseEntity<Void> verify(@RequestHeader Map<String, String> headers) {
		
		// get token, which has already been verified
		String token = headers.get("authorization");
		// get ID
        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
                .getId();

		ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
		
		if(user != null && user.getEmail() != null && user.getEmail().length() > 0) {
			if(user.isVerified()) {
				return new ResponseEntity<Void>(HttpStatus.ALREADY_REPORTED);
			}
			// verify
			user.setVerified(true);
			applicationUserRepository.save(user);
			return new ResponseEntity<Void>(HttpStatus.OK);
		} else {
			// Valid JWT but invalid user/email, somehow
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}
	}


    @CrossOrigin
	@GetMapping(path = "/getFields")
	public ResponseEntity<ContactForm> getFields(@RequestHeader Map<String, String> headers) {

		
		// get token, which has already been verified
		String token = headers.get("authorization");
		// get ID
        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
                .getId();

		ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
        
        ContactForm cForm = new ContactForm();
        cForm.name = user.getFirstName() + " " + user.getLastName();
        cForm.email = user.getEmail();
		
		if(user != null && user.getEmail() != null && user.getEmail().length() > 0) {
			return new ResponseEntity<ContactForm>(cForm, HttpStatus.OK);
		} else {
			// Valid JWT but invalid user/email, somehow
			return new ResponseEntity<ContactForm>(HttpStatus.BAD_REQUEST);
		}
	}

    @SuppressWarnings("unused")
	@CrossOrigin
	@PostMapping(path = "/contact")
	public ResponseEntity<Boolean> contact(@RequestParam String contactData, @RequestParam String recaptchaToken, @RequestHeader Map<String, String> headers) {

    	if(!recaptcha(recaptchaToken) && !Globals.TESTING) {
    		return new ResponseEntity<Boolean>(false, HttpStatus.FAILED_DEPENDENCY); 
    	}
    	
    	ApplicationUser user = null;
    	String id = "Anonymous";

    	Gson gson=new Gson();
    	ContactForm contactForm=gson.fromJson(contactData,ContactForm.class);
    	
    	try {
//    		// get token if available
    		String token = headers.get("authorization");
    		// get ID
            id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
                    .getId();

    		user = applicationUserRepository.findById(Long.valueOf(id)).get();
    	} catch(Exception e) {
    		// no user
    	}
		
		boolean sendStatus = false;
		
		if(user != null && user.getEmail() != null && user.getEmail().length() > 0) {
			sendStatus = sendContactEmail(contactForm, id);
			return new ResponseEntity<Boolean>(sendStatus, HttpStatus.OK);
		} else if(contactFormValid(contactForm)) {
			sendStatus = sendContactEmail(contactForm);
			return new ResponseEntity<Boolean>(sendStatus, HttpStatus.OK);
		} else {
			// Valid JWT but invalid form inputs
			return new ResponseEntity<Boolean>(false, HttpStatus.BAD_REQUEST);
		}
	}

	private boolean contactFormValid(ContactForm contactForm) {
    	boolean result = true;
    	if(contactForm.body == null || contactForm.body.strip().length() < 1) {
    		result = false;
    	} else if(contactForm.subject == null || contactForm.subject.strip().length() < 1) {
    		result = false;
    	} else if(contactForm.name == null || contactForm.name.strip().length() < 1) {
    		result = false;
    	} else if(contactForm.email == null || contactForm.email.strip().length() < 1) {
    		result = false;
		}
		return result;
	}

	// Version not requiring a user ID (anonymous contact)
    private boolean sendContactEmail(ContactForm contactForm) {
		return sendContactEmail(contactForm, "N/A");
	}
    // Contact email requesting user ID
	private boolean sendContactEmail(ContactForm contactForm, String userId) {
    	boolean status = true;
    	
    	try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            
            if(Globals.TESTING) {
            	helper.setTo(SecurityConstants.EMAIL_HANDLE);
            } else {
	            helper.setTo(new String[] {
	            		"paulmirocha@arizona.edu", 
	            		"derbridge@email.arizona.edu", 
	            		"abinfordwalsh@email.arizona.edu", 
	            		SecurityConstants.EMAIL_HANDLE
	    		});
            }
            message.setFrom(new InternetAddress("NEPAccess <Eller-NepAccess@email.arizona.edu>"));
            helper.setSubject("(NEPAccess Contact) " + contactForm.subject);
            helper.setText("Contact from: " + contactForm.name
            		+ "\nEmail address: " + contactForm.email
            		+ "\nUser ID: " + userId
            		+ "\n\n Body: " + contactForm.body
            );
             
            sender.send(message);
    		
    	} catch (MailAuthenticationException e) {
            logEmail(contactForm.email, e.toString(), "Contact", false);

//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailAuthenticationException");
            
            status = false;
    	} catch (MailSendException e) {
            logEmail(contactForm.email, e.toString(), "Contact", false);

//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailSendException");
            
            status = false;
    	} catch (MailException e) {
            logEmail(contactForm.email, e.toString(), "Contact", false);
            
//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailException");
            
            status = false;
    	} catch (Exception e) {
            logEmail(contactForm.email, e.toString(), "Contact", false);
            
//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "Exception");
            
            status = false;
    	}
    	
    	if(status) {
    		try {
        		// Log contact fields to database
        		Contact contact = new Contact(
        				contactForm.body,
        				contactForm.subject,
        				contactForm.name,
        				contactForm.email);
        		contactRepository.save(contact);
                logEmail(contactForm.email, "", "Contact", true);
    		} catch (Exception ex) {
    			// Do nothing
    		}
    	}
        
        return status;
	}
	
    /** Send email with custom recipient address, subject, body */
	private boolean sendEmail(String setTo, String subj, String bodyText) {
		String[] strArray = { setTo };
		return sendEmailToList(strArray, subj, bodyText);
	}
    /** Send email with custom to: list, subject, body */
	private boolean sendEmailToList(String[] setToList, String subj, String bodyText) {
    	boolean status = true;
    	
    	String emails = String.join(", ", setToList);
    	
    	try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            
            if(Globals.TESTING) {
            	helper.setTo(SecurityConstants.EMAIL_HANDLE);
            	subj = "Test: " + subj;
            } else {
	            helper.setTo(setToList);
            }
            message.setFrom(new InternetAddress("NEPAccess <Eller-NepAccess@email.arizona.edu>"));
            helper.setSubject(subj);
            helper.setText(bodyText);
             
            sender.send(message);
    		
    	} catch (MailAuthenticationException e) {
            logEmail(emails, e.toString(), "Custom", false);

//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailAuthenticationException");
            
            status = false;
    	} catch (MailSendException e) {
            logEmail(emails, e.toString(), "Custom", false);

//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailSendException");
            
            status = false;
    	} catch (MailException e) {
            logEmail(emails, e.toString(), "Custom", false);
            
//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailException");
            
            status = false;
    	} catch (Exception e) {
            logEmail(emails, e.toString(), "Custom", false);
            
//	            emailAdmin(resetUser.getEmail(), e.getMessage(), "Exception");
            
            status = false;
    	}
    	
    	if(status) {
    		try {
                logEmail(emails, "", "Custom", true);
    		} catch (Exception ex) {
    			// Do nothing
    		}
    	}
        
        return status;
	}

	// Helper method generates a JWT that lasts for 10 days and returns a valid reset link
	private String getVerificationLink(ApplicationUser user) {
        String token = JWT.create()
                .withSubject(user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
                .withJWTId(String.valueOf(
        				(user).getId() 
        		))
                .sign(HMAC512( // Combine secret and password hash to create a single-use verification JWT
                		(SecurityConstants.SECRET + user.getPassword())
                		.getBytes()
                ));
		
		return "https://www.nepaccess.org/verify?token="+token;
	}
	

    /**
     * @param email
     * @param errorString
     * @param emailType
     * @param sent
     * @return
     */
    private boolean logEmail(String email, String errorString, String emailType, Boolean sent) {
    	try {
        	EmailLog log = new EmailLog();
    		log.setEmail(email);
    		log.setErrorType(errorString);
    		log.setEmailType(emailType); // ie "Reset"
    		log.setSent(sent);
    		log.setLogTime(LocalDateTime.now());
    		emailLogRepository.save(log);
    		return true;
    	} catch (Exception e) {
    		// do nothing
    	}
    	return false;
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
	
	// Return true if admin or curator role
	@PostMapping(path = "/checkCurator")
	public ResponseEntity<Boolean> checkCurator(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		boolean result = isAdmin(token) || isCurator(token);
		HttpStatus returnStatus = HttpStatus.UNAUTHORIZED;
		if(result) {
			returnStatus = HttpStatus.OK;
		}
		return new ResponseEntity<Boolean>(result, returnStatus);
	}
	
	// Return true if admin or curator or approver role
	@PostMapping(path = "/checkApprover")
	public ResponseEntity<Boolean> checkApprover(@RequestHeader Map<String, String> headers) {
		String token = headers.get("authorization");
		boolean result = isAdmin(token) || isCurator(token) || isApprover(token);
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
	
	// Helper function for checkCurator
	private boolean isCurator(String token) {
		boolean result = false;
		// get ID
		if(token != null) {
	        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
	                .getId();

			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
			if(user.getRole().contentEquals("CURATOR")) {
				result = true;
			}
		}
		return result;

	}

	// Helper function for checkApprover
	private boolean isApprover(String token) {
		boolean result = false;
		// get ID
		if(token != null) {
	        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
	                .getId();

			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
			if(user.getRole().contentEquals("APPROVER")) {
				result = true;
			}
		}
		return result;

	}

	/** 
	 *  Bulk user generation.  
	 *  Sends no emails.  (Purpose: Manually invite users from returned array)
     *  @returns ApplicationUser[] with generated passwords
     */
    @PostMapping(path = "/generate2", 
    		consumes = "application/json", 
    		produces = "application/json", 
    		headers = "Accept=application/json")
    public @ResponseBody ResponseEntity<ApplicationUser[]> generate2(
    		@RequestBody ApplicationUser users[], 
			@RequestHeader Map<String, String> headers) {

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
    		if(user != null) { // Don't process an empty line, in case that's possible

        		
        		if(user.getUsername() == null) {
        			user.setUsername("");
        		}
        		user.setActive(true);
        		user.setVerified(true);
        		
    			user.setUsername(user.getUsername().strip());
        		
        		if(user.getEmail() != null) {
        			user.setEmailAddress(user.getEmail().strip());
        		}
        		
        		// Need returnUsers to keep track of literal passwords
        		// because we need to ensure the new users get their credentials
            	returnUsers[i] = new ApplicationUser();
        		
        		if(user.getUsername().length() < 1 && !emailInvalid(user.getEmail())) { // No username provided?
        			String[] split = (user.getEmail().split("@"));
        			if(
        					(split[1].equalsIgnoreCase("email.arizona.edu") 
        					|| split[1].equalsIgnoreCase("arizona.edu") )
        					&& !usernameInvalid(split[0])
        			) {
        				// Try using NetID if Arizona email, if valid (should just be first part)
        				user.setUsername(split[0]);
        			} else { 
        				// Or use the email as the username
        				user.setUsername(user.getEmail());
        			}
        		} 

                returnUsers[i].setUsername(user.getUsername());
                returnUsers[i].setEmailAddress(user.getEmail());
        		
        		// validate (duplicates, length constraints, proper email)
            	if( usernameInvalid(user.getUsername()) || emailInvalid(user.getEmail()) ) { 
                	returnUsers[i].setEmailAddress(user.getEmail());
            		// skip, deal with it externally when you get a result with no password
            	} else {
            		// Generate and set password
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
                    // Here's where we could email individual users, or details to admin, etc.
                    if(saved && user.getEmail() != null) {
                    	returnUsers[i].setActive(true);
                    	returnUsers[i].setVerified(true);
                    	returnUsers[i].setAffiliation(user.getAffiliation());
                    	returnUsers[i].setFirstName(user.getFirstName());
                    	returnUsers[i].setLastName(user.getLastName());
                    	returnUsers[i].setOrganization(user.getOrganization());
                    	returnUsers[i].setJobTitle(user.getJobTitle());
                    } else {
                    	
                    }
                    
                     
            	}

            	i++;   
    		} 
    	}
    	
    	// Email full list to admin
//	    	emailUserListToAdmin(returnUsers);
    	
    	// IDs will be 0
    	return new ResponseEntity<ApplicationUser[]>(returnUsers, HttpStatus.OK);
    }

    @PostMapping(path = "/opt_out", 
    		consumes = "application/json", 
    		headers = "Accept=application/json")
    public @ResponseBody ResponseEntity<Boolean> optOut(
    		@RequestBody OptedOut optOutUser) {

		// validate length constraints of email
    	if( (optOutUser.getEmail().length() == 0 
    				|| optOutUser.getEmail().length() > 191
    				) ) { 
    		return new ResponseEntity<Boolean>(false,HttpStatus.BAD_REQUEST);
    	} else {
            try {
                optedOutRepository.save(optOutUser);
				return new ResponseEntity<Boolean>(true,HttpStatus.OK);
            } catch(org.springframework.dao.DataIntegrityViolationException e) {
				return new ResponseEntity<Boolean>(false,HttpStatus.ALREADY_REPORTED);
            } catch(Exception e) { // duplicate email?
				return new ResponseEntity<Boolean>(false,HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
             
    	}
	}
    
    @PostMapping(path = "/opted_out_email_sender", 
    		consumes = "application/json", 
    		produces = "application/json", 
    		headers = "Accept=application/json")
    public @ResponseBody ResponseEntity<String> optedOutSendEmail(
    		@RequestBody String s, 
			@RequestHeader Map<String, String> headers) 
    {
    	// TODO: Just lock out this route until we may actually want to use it
    	if(true) {
    		return new ResponseEntity<String>(HttpStatus.LOCKED);
    	}
    	
    	
    	Boolean authorized = false;
    	String token = headers.get("authorization");
    	if(isAdmin(token)) {
    		authorized = true;
    	}
    	
    	List<OptedOut> opters = optedOutRepository.findAll();
    	
    	String lastError = "";
    	
    	if(authorized) {
    		int i = 0;
    		for(OptedOut opter : opters) {
        		try {
        			// Do something with name in body or subject, send to email
        	    	String subject = "Testing";
        	    	String body = "Testing: " + opter.getName();
        			sendEmail(opter.getEmail(),subject,body);
        		} catch(Exception e) {
        			lastError = e.getMessage();
            		i++;
        		}
    		}

    		// Probably shouldn't be any errors at this level.
    		// Errors are caught and logged to database at the child level in this case.
        	String result = "Errors: " + i + " Last: " + lastError;
        	
    		return new ResponseEntity<String>(result,HttpStatus.OK);
    	} else {
    		return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
    	}
    	
    }
    
    // convenience while trying to get new config to work
    @PostMapping(path = "/email_test", 
    		consumes = "application/json", 
    		produces = "application/json", 
    		headers = "Accept=application/json")
    public @ResponseBody ResponseEntity<String> emailTest(
			@RequestHeader Map<String, String> headers) 
    {
    	
    	Boolean authorized = false;
    	String token = headers.get("authorization");
    	if(isAdmin(token)) {
    		authorized = true;
    	}
    	
    	String lastError = "";
    	
    	if(authorized) {
    		try {
    			// Do something with name in body or subject, send to email
    	    	String subject = "Test subject";
    	    	String body = "Test body";
    			sendEmail(SecurityConstants.EMAIL_HANDLE,subject,body);
    		} catch(Exception e) {
    			lastError = e.getMessage();
    		}

    		// Probably shouldn't be any errors at this level.
    		// Errors are caught and logged to database at the child level in this case.
        	String result = "Errors go here: " + lastError;
        	
    		return new ResponseEntity<String>(result,HttpStatus.OK);
    	} else {
    		return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
    	}
    	
    }
    
}
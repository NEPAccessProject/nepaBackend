package nepaBackend.controller;
import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import nepaBackend.ApplicationUserService;
import nepaBackend.EmailLogRepository;
import nepaBackend.model.ApplicationUser;
import nepaBackend.model.EmailLog;
import nepaBackend.pojo.ResetEmail;
import nepaBackend.pojo.ResetPassword;
import nepaBackend.security.SecurityConstants;
 
// TODO: Email addresses must be unique per account or else reset email will fail
// for that address.

@Controller
public class ResetEmailController {
	
	private static final Logger logger = LoggerFactory.getLogger(ResetEmailController.class);
	
	@Autowired
    private JavaMailSender sender;

    @Autowired
    private ApplicationUserService applicationUserService;
    @Autowired
    private EmailLogRepository emailLogRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public ResetEmailController() {
    }

	// TODO: Test on production server, see if email works
    // TODO: Limits on how often reset email can be sent
    // can save datetime in db and check against that
    // TODO (frontend): Captcha, landing page to set password from reset link
    // Route to generate and email a reset link to given email address
    @CrossOrigin
    @PostMapping(path = "/reset/send", 
    		consumes = "application/json", 
    		produces = "application/json", 
    		headers = "Accept=application/json")
    @ResponseBody
    ResponseEntity<String> reset(@RequestBody ResetEmail resetEmail) {
        try {
        	// Throws exception if email doesn't exist or if multiple records are found
    		ApplicationUser resetUser = applicationUserService.findByEmail(resetEmail.email);
            
    		LocalDateTime timeNow = LocalDateTime.now();
    		
    		// If there has been a reset (default null column)
    		if(resetUser.getLastReset() != null) {
    			// And if 24 hours haven't passed since the last email reset
    			if((timeNow.minusDays(1)).isBefore(resetUser.getLastReset())) {
    				// then return a special HttpStatus
    				return new ResponseEntity<String>("Too soon", HttpStatus.I_AM_A_TEAPOT);
    			}
    		}
    		
    		Boolean sent = sendResetEmail(resetUser);
    		
    		if(sent) {
                return new ResponseEntity<String>("Email sent!", HttpStatus.OK);
    		} else {
                return new ResponseEntity<String>("Email was not sent.", HttpStatus.INTERNAL_SERVER_ERROR);
    		}
        } catch(NullPointerException ex) { // user not found
        	
        	try {
        		// If @arizona.edu, try looking for @email.arizona.edu, or if @email.arizona.edu 
        		// try looking for @arizona.edu (this is a common user mistake)
                String emailService = resetEmail.email;
            	int index = resetEmail.email.indexOf('@');
            	emailService = emailService.substring(index);
            	
            	String emailFinisher = "@arizona.edu";
            	if(emailService.contentEquals("@arizona.edu")) {
            		emailFinisher = "@email.arizona.edu";
            	}
            	
            	if(emailService.contentEquals("@arizona.edu") || 
            			emailService.contentEquals("@email.arizona.edu")) {
            		ApplicationUser resetUser = 
            				applicationUserService.findByEmail(resetEmail.email.substring(0, index) + emailFinisher);
                    
            		LocalDateTime timeNow = LocalDateTime.now();
            		
            		// If there has been a reset (default null column)
            		if(resetUser.getLastReset() != null) {
            			// And if 24 hours haven't passed since the last email reset
            			if((timeNow.minusDays(1)).isBefore(resetUser.getLastReset())) {
            				// then return a special HttpStatus
            				return new ResponseEntity<String>("Too soon", HttpStatus.I_AM_A_TEAPOT);
            			}
            		}
            		
            		Boolean sent = sendResetEmail(resetUser);
            		
            		if(sent) {
                        return new ResponseEntity<String>("Email sent!", HttpStatus.OK);
            		} else {
                        return new ResponseEntity<String>("Email was not sent.", HttpStatus.INTERNAL_SERVER_ERROR);
            		}
            	}
        		
        	} catch (Exception e) {
        		try {
        			logEmail(resetEmail.email, e.toString(), "Reset", false);
        		}catch(Exception logEx) {
//        			System.out.println("Failure?" + logEx);
        		}
        	}
        	
    		// Email probably doesn't exist.
            return new ResponseEntity<String>("Error in sending email: "+ex, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
			logEmail(resetEmail.email, e.toString(), "Reset", false);
	        return new ResponseEntity<String>("Error in sending email: "+e, HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }
 
    private boolean sendResetEmail(ApplicationUser resetUser) throws Exception{
    	try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
             
            helper.setTo(resetUser.getEmail());
//          message.setFrom(new InternetAddress("NEPAccess <Eller-NepAccess@email.arizona.edu>"));
            message.setFrom(new InternetAddress("NEPAccess <NEPAccess@NEPAccess.org>"));
            helper.setSubject("NEPAccess Reset Password Request");
            helper.setText("This is an automatically generated email in response to"
            		+ " a request to reset the password for the account linked"
            		+ " to this email address."
            		+ "\n\nYour username is: " + resetUser.getUsername()
            		+ "\n\nClick this link to reset your password: " + getResetLink(resetUser)
            		+ "\n\nPlease note that anyone with this link can change your password."
            		+ "  The link will remain valid for 24 hours or until your password is changed.");
             
            sender.send(message);
            
            // Save LocalDateTime in database
            resetUser.setLastReset(LocalDateTime.now());
            applicationUserService.save(resetUser);
//            System.out.println(message.getContent().toString());
    		
    	} catch (MailAuthenticationException e) {
            logEmail(resetUser.getEmail(), e.toString(), "Reset", false);

//            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailAuthenticationException");
            
    		return false;
    	} catch (MailSendException e) {
            logEmail(resetUser.getEmail(), e.toString(), "Reset", false);

//            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailSendException");
            
    		return false;
    	} catch (MailException e) {
            logEmail(resetUser.getEmail(), e.toString(), "Reset", false);
            
//            emailAdmin(resetUser.getEmail(), e.getMessage(), "MailException");
            
    		return false;
    	} catch (Exception e) {
            logEmail(resetUser.getEmail(), e.toString(), "Reset", false);
            
//            emailAdmin(resetUser.getEmail(), e.getMessage(), "Exception");
            
    		return false;
    	}
    	
		try {
            logEmail(resetUser.getEmail(), "", "Reset", true);
		}catch(Exception ex) {
//			System.out.println("Failure?" + ex);
			// Do nothing
		}
		
        return true;
    }

    // Helper method generates a JWT that lasts for 24 hours and return a valid reset link
	private String getResetLink(ApplicationUser resetUser) {
        String token = JWT.create()
                .withSubject(resetUser.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.RESET_EXPIRATION_TIME))
                .withJWTId(String.valueOf(
        				(resetUser).getId() 
        		))
                .sign(HMAC512( // Combine secret and password hash to create a single-use password reset JWT
                		(SecurityConstants.SECRET + resetUser.getPassword())
                		.getBytes()
                ));

		// JWTs are inherently URL-safe, so this should work.
		return "https://www.nepaccess.org/reset?token="+token;
	}
	
	/** resetTest helper */
//	private String getResetTest(ApplicationUser resetUser) {
//        String token = JWT.create()
//                .withSubject(resetUser.getUsername())
//                .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.RESET_EXPIRATION_TIME))
//                .withJWTId(String.valueOf(
//        				(resetUser).getId() 
//        		))
//                .sign(HMAC512( // Combine secret and password hash to create a single-use password reset JWT
//                		(SecurityConstants.SECRET + resetUser.getPassword())
//                		.getBytes()
//                ));
//
//		// JWTs are inherently URL-safe, so this should work.
//		return "https://localhost:3000/reset_test?token="+token;
//	}

	/** Returns a reset link only for local testing - should not be deployed */
//	@CrossOrigin
//    @PostMapping(path = "/reset/test", 
//    		consumes = "application/json", 
//    		produces = "application/json", 
//    		headers = "Accept=application/json")
//    @ResponseBody
//    ResponseEntity<String> resetTest(@RequestBody ResetEmail resetEmail) {
//            try {
//            	// Throws exception if email doesn't exist or if multiple records are found
//        		ApplicationUser resetUser = applicationUserService.findByEmail(resetEmail.email);
//                
//        		LocalDateTime timeNow = LocalDateTime.now();
//        		
//        		// If there has been a reset (default null column)
//        		if(resetUser.getLastReset() != null) {
//        			// And if 24 hours haven't passed since the last email reset
//        			if((timeNow.minusDays(1)).isBefore(resetUser.getLastReset())) {
//        				// then return a special HttpStatus
//        				return new ResponseEntity<String>("Too soon", HttpStatus.I_AM_A_TEAPOT);
//        			}
//        		}
//        		
//                return new ResponseEntity<String>(getResetTest(resetUser), HttpStatus.OK);
//            } catch(NullPointerException ex) { // user not found
//            	
//            	try {
//            		// If @arizona.edu, try looking for @email.arizona.edu, or if @email.arizona.edu 
//            		// try looking for @arizona.edu (this is a common user mistake)
//                    String emailService = resetEmail.email;
//                	int index = resetEmail.email.indexOf('@');
//                	emailService = emailService.substring(index);
//                	
//                	String emailFinisher = "@arizona.edu";
//                	if(emailService.contentEquals("@arizona.edu")) {
//                		emailFinisher = "@email.arizona.edu";
//                	}
//                	
//                	if(emailService.contentEquals("@arizona.edu") || 
//                			emailService.contentEquals("@email.arizona.edu")) {
//                		ApplicationUser resetUser = 
//                				applicationUserService.findByEmail(resetEmail.email.substring(0, index) + emailFinisher);
//                        
//                		LocalDateTime timeNow = LocalDateTime.now();
//                		
//                		// If there has been a reset (default null column)
//                		if(resetUser.getLastReset() != null) {
//                			// And if 24 hours haven't passed since the last email reset
//                			if((timeNow.minusDays(1)).isBefore(resetUser.getLastReset())) {
//                				// then return a special HttpStatus
//                				return new ResponseEntity<String>("Too soon", HttpStatus.I_AM_A_TEAPOT);
//                			}
//                		}
//
//                        return new ResponseEntity<String>(getResetTest(resetUser), HttpStatus.OK);
//                	}
//            		
//            	} catch (Exception e) {
//            		e.printStackTrace();
//                    return new ResponseEntity<String>("Error in sending email: "+e, HttpStatus.NOT_FOUND);
//            	}
//            	
//        		// Email probably doesn't exist.
//            	ex.printStackTrace();
//                return new ResponseEntity<String>("Error in sending email: "+ex, HttpStatus.NOT_FOUND);
//            } catch (Exception e) {
//        		e.printStackTrace();
//                return new ResponseEntity<String>("Error in sending email: "+e, HttpStatus.NOT_FOUND);
//    		}
//    }

	// Route for custom check of custom JWT generated for password resets
    @CrossOrigin
    @PostMapping(path = "/reset/check")
    ResponseEntity<Void> resetCheck(@RequestHeader Map<String, String> headers) {

    	try {

    		// get token so we can verify it's a special reset token
    		String token = headers.get("authorization");
    		
    		boolean verified = resetTokenCheck(token);
    		
    	    if (verified) {
        		return new ResponseEntity<Void>(HttpStatus.OK);
    	    } else {
        		return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
    	    }
    	} catch(Exception ex) {
    		return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
    	}
    }
    
    // Takes a password, sends verification through custom check, saves new password
    @CrossOrigin
    @PostMapping(path = "/reset/change", 
    		consumes = "application/json", 
    		produces = "application/json", 
    		headers = "Accept=application/json")
    ResponseEntity<Void> resetPassword(@RequestBody ResetPassword resetPassword, 
    		@RequestHeader Map<String, String> headers) {
    	try {

    		// get token so we can verify it's a special reset token
    		String token = headers.get("authorization");
    		
    		boolean verified = resetTokenCheck(token);
    	
    	    if (verified) { // if verified, change password
    			// get ID
    	        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
    	                .getId();
    	        
    			ApplicationUser user = applicationUserService.findById(Long.valueOf(id)).get();
        		
				// update
				user.setPassword(bCryptPasswordEncoder.encode(resetPassword.newPassword));
				applicationUserService.save(user);

        		return new ResponseEntity<Void>(HttpStatus.OK);
    	    } else {
        		return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
    	    }
    	} catch(Exception ex) {
//    		System.out.println(ex);
    		return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
    	}
    }
    
    // Helper method verifies if custom reset token is authorized
    private boolean resetTokenCheck(String token) {
    	try {
    		// need to get the user details to verify token
            String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
                    .getId();
    		ApplicationUser user = applicationUserService.findById(Long.valueOf(id)).get();

    	    // parse the token using assumed user's one-way encryption password hash
    	    String verified = JWT.require(Algorithm.HMAC512((SecurityConstants.SECRET + user.getPassword()).getBytes()))
    	            .build()
    	            .verify(token.replace(SecurityConstants.TOKEN_PREFIX, ""))
    	            .getSubject();
    	    if(verified != null) {
    	    	return true;
    	    } else {
    	    	return false;
    	    }
    	} catch (Exception ex) {
    		logger.error("Couldn't verify reset token: ### " + token + " ###; Exception: " + ex.getLocalizedMessage());
    		ex.printStackTrace();
    		return false;
    	}
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
    
    @SuppressWarnings("unused")
	private void emailAdmin(String email, String errorType, String errorMessage) {
    	MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        
		try {
			helper.setTo(SecurityConstants.EMAIL_HANDLE);
			helper.setText("Failure for " + email + " : " + errorMessage);
        	helper.setSubject("NEPAccess Reset Password Failure: " + errorType);
		} catch (MessagingException e) {
//			e.printStackTrace();
		}
        
        sender.send(message);
    }
    
    @CrossOrigin
    @GetMapping(path = "/email/logs", 
    		produces = "application/json", 
    		headers = "Accept=application/json")
    ResponseEntity<List<EmailLog>> getEmailResetLogs(@RequestHeader Map<String, String> headers) {
    	String token = headers.get("authorization");
		
		boolean admin = applicationUserService.isAdmin(token);
		
	    if (admin) {
	    	List<EmailLog> logs = emailLogRepository.findAll();
	    	return new ResponseEntity<List<EmailLog>>(logs, HttpStatus.OK);
	    } else {
	    	return new ResponseEntity<List<EmailLog>>(new ArrayList<EmailLog>(), HttpStatus.UNAUTHORIZED);
	    }
    	
    }
    
    // TODO: User generation emails with login links like with password reset?
	// If they want to do that instead of generating passwords
}
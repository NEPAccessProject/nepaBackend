package nepaBackend.controller;
import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.absurdity.ResetEmail;
import nepaBackend.absurdity.ResetPassword;
import nepaBackend.model.ApplicationUser;
import nepaBackend.security.SecurityConstants;
 
// TODO: Email addresses must be unique per account or else reset email will fail
// for that address.

@Controller
public class ResetEmailController {
	
    @Autowired
    private JavaMailSender sender;
    
    private ApplicationUserRepository applicationUserRepository;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public ResetEmailController(ApplicationUserRepository applicationUserRepository,
            BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.applicationUserRepository = applicationUserRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
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
    		ApplicationUser resetUser = applicationUserRepository.findByEmail(resetEmail.email);
            
    		LocalDateTime timeNow = LocalDateTime.now();
    		
    		// If there has been a reset (default null column)
    		if(resetUser.getLastReset() != null) {
    			// And if 24 hours haven't passed since the last email reset
    			if((timeNow.minusDays(1)).isBefore(resetUser.getLastReset())) {
    				// then return a special HttpStatus
    				return new ResponseEntity<String>("Too soon", HttpStatus.I_AM_A_TEAPOT);
    			}
    		}
    		
    		sendResetEmail(resetUser);
            return new ResponseEntity<String>("Email sent!", HttpStatus.OK);
        }catch(Exception ex) {
            return new ResponseEntity<String>("Error in sending email: "+ex, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
 
    private void sendResetEmail(ApplicationUser resetUser) throws Exception{
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
         
        helper.setTo(resetUser.getEmail());
        helper.setText("This is an automatically generated email in response to"
        		+ " a request to reset the password for the account linked"
        		+ " to this email address."
        		+ "\n\nClick this link to reset your password: " + getResetLink(resetUser)
        		+ "\n\nPlease note that anyone with this link can change your password."
        		+ "  The link will remain valid for 24 hours or until your password is changed.");
        helper.setSubject("NEPAccess Reset Password Request");
         
        sender.send(message);
        
        // Save LocalDateTime in database
        resetUser.setLastReset(LocalDateTime.now());
        applicationUserRepository.save(resetUser);
//        System.out.println(message.getContent().toString());
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
		
		return "http://mis-jvinalappl1.microagelab.arizona.edu/reset?token="+token;
	}

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
    	        
    			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
        		
				// update
				user.setPassword(bCryptPasswordEncoder.encode(resetPassword.newPassword));
				applicationUserRepository.save(user);

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
    		ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();

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
//    		System.out.println(ex);
    		return false;
    	}
    }
    
    // TODO: User generation emails with login links like with password reset?
	// If they want to do that instead of generating passwords
}
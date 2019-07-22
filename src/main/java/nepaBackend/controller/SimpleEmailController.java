package nepaBackend.controller;
import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

import java.util.Date;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.auth0.jwt.JWT;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.ResetEmail;
import nepaBackend.UserDetailsImpl;
import nepaBackend.model.ApplicationUser;
import nepaBackend.security.SecurityConstants;
 
// TODO: Email addresses must be unique per account or else reset email will fail
// for that address.

@Controller
public class SimpleEmailController {
	
    @Autowired
    private JavaMailSender sender;
    
    private ApplicationUserRepository applicationUserRepository;

    public SimpleEmailController(ApplicationUserRepository applicationUserRepository) {
        this.applicationUserRepository = applicationUserRepository;
    }
 
    @PostMapping(path = "/reset", 
    		consumes = "application/json", 
    		produces = "application/json", 
    		headers = "Accept=application/json")
    @ResponseBody
    ResponseEntity<String> reset(@RequestBody ResetEmail resetEmail) {
        try {
        	// Throws exception if email doesn't exist or if multiple records are found
    		ApplicationUser resetUser = applicationUserRepository.findByEmail(resetEmail.email);
            
    		sendResetEmail(resetUser);
            return new ResponseEntity<String>("Email sent!", HttpStatus.OK);
        }catch(Exception ex) {
            return new ResponseEntity<String>("Error in sending email: "+ex, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
 
    // TODO: Limits on how often reset email can be sent, limit on how long before link expires
    // Reset link should probably expire in say 30 minutes and save a time in the database 
    // (can just be hour minute second) that can be checked to prevent spam, also captcha helps
    // TODO (frontend): Captcha, landing page to set password from reset link
    private void sendResetEmail(ApplicationUser resetUser) throws Exception{
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
         
        helper.setTo(resetUser.getEmail());
        helper.setText("This is an automatically generated email in response to"
        		+ " a request to reset the password for the account linked"
        		+ " to this email address."
        		+ "\n\nClick this link to reset your password: " + getResetLink(resetUser)
        		+ "\n\nPlease note that anyone with this link can change your password."
        		+ "  The link will remain valid for 24 hours.");
        helper.setSubject("NEPAccess Reset Password Request");
         
        sender.send(message);
    }

    // Generate a JWT that lasts for 24 hours and return a valid reset link
	private String getResetLink(ApplicationUser resetUser) {
		// TODO: Test
		// TODO: Force expire this JWT after password is changed (requires db work)
		
        String token = JWT.create()
                .withSubject(resetUser.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.RESET_EXPIRATION_TIME))
                .withJWTId(String.valueOf(
                				(resetUser).getId() 
                				))
                .sign(HMAC512(SecurityConstants.SECRET.getBytes()));
		
		return "http://mis-jvinalappl1.microagelab.arizona.edu/reset?token="+token;
	}
    
    // TODO: User generation emails with login links like with password reset?
	// If they want to do that instead of generating passwords
}
package nepaBackend.controller;
import javax.mail.internet.MimeMessage;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import nepaBackend.ResetEmail;
 
@Controller
public class SimpleEmailController {
     
    @Autowired
    private JavaMailSender sender;
 
    @PostMapping("/reset")
    @ResponseBody
    String home(@RequestBody ResetEmail email) {
        // TODO: Verify email is registered.
        try {
            sendResetEmail(email.email);
            return "Email Sent!";
        }catch(Exception ex) {
            return "Error in sending email: "+ex;
        }
    }
 
    // TODO: Turn this into a link that will take the user to a page that asks them
    // to put in a new password, and implement that functionality.
    // TODO: Limits on how often reset email can be sent, limit on how long before link expires
    // Reset link should probably expire in say 30 minutes and save a time in the database 
    // (can just be hour minute second) that can be checked to prevent spam, also captcha helps
    // TODO (frontend): Captcha, landing page to set password from reset link
    private void sendResetEmail(String email) throws Exception{
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
         
        helper.setTo(email);
        helper.setText("This is just a test.");
        helper.setSubject("Test");
         
        sender.send(message);
    }
    
    // TODO: User generation emails with login links like with password reset
}
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
        try {
            sendEmail(email.email);
            return "Email Sent!";
        }catch(Exception ex) {
            return "Error in sending email: "+ex;
        }
    }
 
    private void sendEmail(String email) throws Exception{
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
         
        helper.setTo(email);
        helper.setText("This is just a test.");
        helper.setSubject("Test");
         
        sender.send(message);
    }
}
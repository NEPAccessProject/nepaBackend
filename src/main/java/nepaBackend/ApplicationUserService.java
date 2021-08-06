package nepaBackend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;

import nepaBackend.model.ApplicationUser;
import nepaBackend.security.SecurityConstants;

@Service
public class ApplicationUserService {
	@Autowired
	private ApplicationUserRepository applicationUserRepository;
    
    public ApplicationUser getUserFromToken(String token) {
    	ApplicationUser user = null;

		try {
	
			if(token != null) {
		        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
		                .getId();
	
				user = applicationUserRepository.findById(Long.valueOf(id))
						.get();
			}
		} catch(Exception e) {
			return null;
		}
		
		return user;
    }
    
    public boolean approverOrHigher(ApplicationUser user) {
		return ( user.getRole().contentEquals("ADMIN") 
			|| user.getRole().contentEquals("CURATOR") 
			|| user.getRole().contentEquals("APPROVER") );
    }

    public boolean approverOrHigher(String token) {
		boolean result = false;

		if(token != null) {
	        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
	                .getId();

			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
			if(        user.getRole().equalsIgnoreCase("APPROVER") 
					|| user.getRole().equalsIgnoreCase("CURATOR") 
					|| user.getRole().equalsIgnoreCase("ADMIN") ) 
			{
				result = true;
			}
		}
		
		return result;
	}
    
    public boolean curatorOrHigher(String token) {
		boolean result = false;
		
		if(token != null) {
	        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
	                .getId();

			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
			if(user.getRole().equalsIgnoreCase("CURATOR") || user.getRole().equalsIgnoreCase("ADMIN")) {
				result = true;
			}
		}
		
		return result;
	}
    
    public boolean isAdmin(String token) {
		boolean result = false;
		
		if(token != null) {
	        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
	                .getId();

			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
			if(user.getRole().equalsIgnoreCase("ADMIN")) {
				result = true;
			}
		}
		
		return result;
	}

	public boolean isBelowAdmin(String token) {
		boolean result = false;
		
		if(token != null) {
	        String id = JWT.decode((token.replace(SecurityConstants.TOKEN_PREFIX, "")))
	                .getId();

			ApplicationUser user = applicationUserRepository.findById(Long.valueOf(id)).get();
			if(user.getRole().equalsIgnoreCase("CURATOR") || user.getRole().equalsIgnoreCase("APPROVER")) {
				result = true;
			}
		}
		
		return result;
	}
	
}
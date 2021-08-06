package nepaBackend;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;

import nepaBackend.model.ApplicationUser;
import nepaBackend.security.SecurityConstants;

@Service
public class ApplicationUserService {
	@Autowired
	private ApplicationUserRepository applicationUserRepository;
	
	public ApplicationUser save(ApplicationUser user) {
		return applicationUserRepository.save(user);
	}
	public Optional<ApplicationUser> findById(Long id) {
		return applicationUserRepository.findById(id);
	}
	public ApplicationUser findByEmail(String email) {
		return applicationUserRepository.findByEmail(email);
	}
    
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

	/** By necessity token is verified as valid via filter by this point as long as it's going through the 
	 * public API.  Alternatively you can store admin credentials in the token and hand that to the filter,
	 * but then if admin access is revoked, that token still has admin access until it expires.
	 * Therefore this is a slightly more secure flow. */
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

	public boolean isAdmin(ApplicationUser user) {
		return user.getRole().equalsIgnoreCase("ADMIN");
	}
	public boolean isCurator(ApplicationUser user) {
		return user.getRole().equalsIgnoreCase("CURATOR");
	}
	
}
package nepaBackend;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import nepaBackend.model.ApplicationUser;

//TODO: User roles
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private ApplicationUserRepository applicationUserRepository;

    public UserDetailsServiceImpl(ApplicationUserRepository applicationUserRepository) {
        this.applicationUserRepository = applicationUserRepository;
    }

    @Override
    public UserDetailsImpl loadUserByUsername(String username) throws UsernameNotFoundException {
        ApplicationUser applicationUser = applicationUserRepository.findByUsername(username);
        if (applicationUser == null) {
            throw new UsernameNotFoundException(username);
        }
        
//        Set<Role> roles = applicationUser.getRole();
//        logger.debug("role of the user" + roles);
//
//        Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
//        for(Role role: roles){
//            authorities.add(new SimpleGrantedAuthority(role.getRole()));
//            logger.debug("role" + role + " role.getRole()" + (role.getRole()));
//        }
        
        UserDetailsImpl user = new UserDetailsImpl();
        user.setUser(applicationUser);

//        user.setAuthorities(authorities);
        
        return user;
    }
}
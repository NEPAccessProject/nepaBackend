package nepaBackend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;

import nepaBackend.ApplicationUserRepository;
import nepaBackend.UserDetailsImpl;

@Service
public final class LoginSuccessHandler implements ApplicationListener<AuthenticationSuccessEvent>
{
    @Autowired
    ApplicationUserRepository applicationUserRepository;

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event)
    {
        String username = ((UserDetailsImpl) event.getAuthentication().
                getPrincipal()).getUsername();
        
        applicationUserRepository.updateLoginDate(applicationUserRepository.findByUsername(username).getId());

//        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
    }
}
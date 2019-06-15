package uapBackend;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import uapBackend.model.ApplicationUser;

public class UserDetailsImpl implements UserDetails{
	
	private static final long serialVersionUID = 1L;
	private ApplicationUser user;
	
	Set<GrantedAuthority> authorities=null;
	
	public ApplicationUser getUser() {
		return user;
	}
	
	public void setUser(ApplicationUser user) {
		this.user = user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	public void setAuthorities(Set<GrantedAuthority> authorities)
    {
        this.authorities=authorities;
    }

	@Override
    public String getPassword() {
        return user.getPassword();
    }

	@Override
    public String getUsername() {
        return user.getUsername();
    }
	
	public Long getId() {
		return user.getId();
	}

	@Override
    public boolean isAccountNonExpired() {
        return user.isAccountNonExpired();
    }

	@Override
    public boolean isAccountNonLocked() {
        return user.isAccountNonLocked();
    }

	@Override
    public boolean isCredentialsNonExpired() {
        return user.isCredentialsNonExpired();
    }

	@Override
    public boolean isEnabled() {
        return user.isAccountEnabled();
    }

}

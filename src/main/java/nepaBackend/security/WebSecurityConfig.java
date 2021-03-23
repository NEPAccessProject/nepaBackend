package nepaBackend.security;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import nepaBackend.UserDetailsServiceImpl;

import org.springframework.context.annotation.Bean;

@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private UserDetailsServiceImpl userDetailsService;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public WebSecurityConfig(UserDetailsServiceImpl userDetailsService, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userDetailsService = userDetailsService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    // Need to do this to get custom JWT (for password reset) checks through the filter.
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
//    	.antMatchers(HttpMethod.OPTIONS, "**") // does nothing?
//    	.antMatchers("/reset**") // does nothing?
//        .antMatchers("/reset") // does nothing?
        .antMatchers("/user/verify")
        .antMatchers("/reset/change")
        .antMatchers("/reset/send")
        .antMatchers("/reset/check");
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	http.cors().and().csrf().disable().authorizeRequests()
    	 .antMatchers(HttpMethod.OPTIONS, "**").permitAll()
         .antMatchers(HttpMethod.POST, SecurityConstants.SIGN_UP_URL).permitAll() // allow registration
//         .antMatchers(HttpMethod.POST, "/**").permitAll() // allow all (when post)
         .antMatchers(HttpMethod.POST, "/user/exists").permitAll()
         .antMatchers(HttpMethod.POST, "/user/email-exists").permitAll()
         .antMatchers(HttpMethod.POST, "/user/contact").permitAll()
//         .antMatchers(HttpMethod.POST, "/reset").permitAll()
//         .antMatchers(HttpMethod.POST, "/reset/check").permitAll()
//         .antMatchers(HttpMethod.POST, "/reset/**").permitAll()
//         .antMatchers(HttpMethod.POST, SecurityConstants.SIGN_UP_URL).denyAll() // deny registration
//         .antMatchers(HttpMethod.POST, "/user/generate").hasAuthority("ADMIN") // TODO: Roles for admin access to generate user
         .anyRequest().authenticated() // require authentication for the rest
         .and() // add our two custom filters to the chain:
         .addFilter(new JWTAuthenticationFilter(authenticationManager()))
         .addFilter(new JWTAuthorizationFilter(authenticationManager()))
         // this disables session creation on Spring Security
         .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
    

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder);
    }
    
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
	    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
	    return source;
    }
}
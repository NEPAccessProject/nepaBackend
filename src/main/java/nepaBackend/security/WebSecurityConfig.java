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
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import nepaBackend.Globals;
import nepaBackend.UserDetailsServiceImpl;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;

@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private UserDetailsServiceImpl userDetailsService;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public WebSecurityConfig(UserDetailsServiceImpl userDetailsService, 
    			BCryptPasswordEncoder bCryptPasswordEncoder) {
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
    	if(!Globals.TESTING) {
        	http.requiresChannel()
            .anyRequest()
            .requiresSecure();
    	}
        CorsConfiguration corsConfiguration = new CorsConfiguration();
//        corsConfiguration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setAllowedOrigins(List.of("*"));
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setExposedHeaders(List.of("Authorization"));
    	http.cors().configurationSource(request -> corsConfiguration).and().csrf().disable().authorizeRequests()
         .antMatchers(HttpMethod.POST, SecurityConstants.SIGN_UP_URL).permitAll() // allow registration

         .antMatchers(HttpMethod.POST, "/user/exists").permitAll()
         .antMatchers(HttpMethod.POST, "/user/email-exists").permitAll()
         .antMatchers(HttpMethod.POST, "/user/contact").permitAll()
         .antMatchers(HttpMethod.POST, "/user/recaptcha_test").permitAll()
         .antMatchers(HttpMethod.POST, "/user/opt_out").permitAll()
         .antMatchers(HttpMethod.POST,"user/get_role").permitAll()
         /** Here's where we're now allowing anonymous searches/details viewing: **/
         .antMatchers(HttpMethod.POST, "/text/search").permitAll()
         .antMatchers(HttpMethod.GET, "/text/search/**").permitAll()
         .antMatchers(HttpMethod.POST, "/text/search_top").permitAll()
         .antMatchers(HttpMethod.POST, "/text/search_no_context").permitAll()
         .antMatchers(HttpMethod.POST, "/text/get_highlightsFVH").permitAll()
         .antMatchers(HttpMethod.POST, "/test/check").permitAll()
         .antMatchers(HttpMethod.GET, "/test/get_process").permitAll()
         .antMatchers(HttpMethod.GET, "/test/get_process_full").permitAll()
         .antMatchers(HttpMethod.GET, "/test/get_by_id").permitAll()
         .antMatchers(HttpMethod.GET, "/file/nepafiles").permitAll()
         .antMatchers(HttpMethod.GET, "/file/filenames").permitAll()
         .antMatchers(HttpMethod.GET, "/file/doc_texts").permitAll()
         .antMatchers(HttpMethod.POST, "/interaction/set").permitAll()
         .antMatchers(HttpMethod.POST, "/survey/save").permitAll()
         .antMatchers(HttpMethod.GET, "/geojson/**").permitAll()
         .antMatchers(HttpMethod.POST,"/geojson/get_all_state_county_for_eisdocs").permitAll()
         .antMatchers(HttpMethod.POST,"/geojson/get_all_geodata_for_eisdocs").permitAll()
         .antMatchers(HttpMethod.POST,"/geojson/get_geodata_other_for_eisdocs").permitAll()
         .antMatchers(HttpMethod.GET, "/stats/eis_count").permitAll()
         .antMatchers(HttpMethod.GET, "/stats/total_count").permitAll()
         .antMatchers(HttpMethod.GET, "/stats/latest_year").permitAll()
         .antMatchers(HttpMethod.GET, "/stats/earliest_year").permitAll()
        .antMatchers(HttpMethod.POST, "/reset").permitAll()
        .antMatchers(HttpMethod.POST, "/reset/check").permitAll()
        .antMatchers(HttpMethod.POST, "/reset/**").permitAll()
        .antMatchers(HttpMethod.POST, SecurityConstants.SIGN_UP_URL).denyAll() // deny registration
        .antMatchers(HttpMethod.POST, "/user/generate").hasAuthority("ADMIN") // TODO: Roles for admin access to generate user
         //.anyRequest().authenticated() // require authentication for the rest
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
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://localhost", "http://localhost"));
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
//      configuration.setAllowCredentials(false);
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
	    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//	    source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
	    source.registerCorsConfiguration("/**", configuration);
	    return source;
    }
}
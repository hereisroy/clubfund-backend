package codes.roy.ClubFund.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	@Autowired JwtAuthorizationFilter jwtAuthorizationFilter;

	@Bean
	public BCryptPasswordEncoder bcryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
    public SecurityFilterChain filterSecurity(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
           	.authorizeHttpRequests
           	(
           			authorize -> authorize
        			.requestMatchers("/admin/**").hasAnyAuthority("ADMIN")
        			.requestMatchers("/member/**").hasAnyAuthority("ADMIN", "MEMBER")
        			.requestMatchers("/gettoken").permitAll()
        			.requestMatchers("/getpublicdata").permitAll()
        			.anyRequest().permitAll()


           		)
           	.addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class)
        	.sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        return http.build();
    }


}

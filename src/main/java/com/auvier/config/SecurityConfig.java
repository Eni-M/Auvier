package com.auvier.config;

import com.auvier.infrastructure.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.Set;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            // Get the roles as strings
            Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

            // Spring .roles() prefixing means CUSTOMER becomes ROLE_CUSTOMER
            if (roles.contains("ROLE_ADMIN")) {
                response.sendRedirect("/admin");
            } else {
                response.sendRedirect("/");
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(configurer -> configurer
                        .requestMatchers("/assets/**", "/uploads/**", "/error", "/403", "/favicon.ico").permitAll()
                        .requestMatchers("/", "/shop", "/shop/**", "/about", "/collections", "/contact", "/faq", "/shipping", "/size-guide", "/careers", "/press").permitAll()
                        .requestMatchers("/api/stripe/webhook").permitAll() // Stripe webhook - no auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // Admin API endpoints
                        .requestMatchers("/login", "/register").anonymous()
                        .requestMatchers("/admin/**", "/admin").hasRole("ADMIN")
                        .requestMatchers("/account", "/profile", "/cart", "/checkout/**", "/orders/**").authenticated()
                        .anyRequest().permitAll()
                )
                // Disable CSRF for Stripe webhook endpoint
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/stripe/webhook")
                )
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        // REPLACE defaultSuccessUrl with the handler below
                        .successHandler(customSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userService).passwordEncoder(passwordEncoder);
        return authBuilder.build();
    }

}

//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
//        httpSecurity
//                .authorizeHttpRequests(configurer ->
//                        configurer.requestMatchers("/assets/**").permitAll()
//                                .requestMatchers("/").permitAll()
//                                .requestMatchers("/login", "/register").anonymous()
//                                .requestMatchers("/admin/**", "/admin").hasRole("ADMIN")
//                                .anyRequest().authenticated()).formLogin(formLogin ->
//                        formLogin.loginPage("/login")
//                                .loginProcessingUrl("/login")
//                                .defaultSuccessUrl("/", true)
//                ).logout(LogoutConfigurer::permitAll)
//                .exceptionHandling(exception ->
//                        exception.accessDeniedPage("/403"));
//
//
//        return httpSecurity.build();
//    }
//
//}


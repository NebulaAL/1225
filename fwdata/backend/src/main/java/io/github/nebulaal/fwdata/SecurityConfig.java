package io.github.nebulaal.fwdata;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, Accounts accounts) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/api/public/forms/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/forms/*/submissions").permitAll()
                .requestMatchers("/api/accounts", "/api/accounts/**", "/api/manage/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(new OncePerRequestFilter() {
                @Override
                protected boolean shouldNotFilter(HttpServletRequest request) {
                    return !request.getServletPath().startsWith("/api/");
                }

                @Override
                protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                        throws ServletException, IOException {
                    var auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof Accounts.Identity who && !accounts.exists(who.id)) {
                        new SecurityContextLogoutHandler().logout(req, res, auth);
                    }
                    chain.doFilter(req, res);
                }
            }, AuthorizationFilter.class)
            .requestCache(AbstractHttpConfigurer::disable)
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, ex) -> res.setStatus(401))
                .accessDeniedHandler((req, res, ex) -> res.setStatus(403)))
            .formLogin(login -> login.loginPage("/").loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> res.setStatus(204))
                .failureHandler((req, res, ex) -> res.setStatus(401))
                .permitAll())
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .deleteCookies("FWDATA_SESSION")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
            .build();
    }
}

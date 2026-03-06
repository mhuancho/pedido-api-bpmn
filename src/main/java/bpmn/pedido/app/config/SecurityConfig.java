package bpmn.pedido.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static bpmn.pedido.app.utils.Constants.REQUEST_MATCHERS_ACTUATOR_HEALTH;
import static bpmn.pedido.app.utils.Constants.REQUEST_MATCHERS_ACTUATOR;
import static bpmn.pedido.app.utils.Constants.REQUEST_MATCHERS_INFO;
import static bpmn.pedido.app.utils.Constants.ROLE_ADMIN;
import static bpmn.pedido.app.utils.Constants.REQUEST_MATCHERS_API_PEDIDO;
import static bpmn.pedido.app.utils.Constants.ROLE_OPERADOR;
import static bpmn.pedido.app.utils.Constants.REALM_ACCESS;
import static bpmn.pedido.app.utils.Constants.REALM_ACCESS_ROLES;
import static bpmn.pedido.app.utils.Constants.RESOURCE_ACCESS;
import static bpmn.pedido.app.utils.Constants.ROLE_NAME;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.security.jwt.resource-client-id}")
    private String resourceClientId;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)  {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(REQUEST_MATCHERS_ACTUATOR_HEALTH, REQUEST_MATCHERS_INFO).permitAll()
                        .requestMatchers(REQUEST_MATCHERS_ACTUATOR).hasRole(ROLE_ADMIN)
                        .requestMatchers(REQUEST_MATCHERS_API_PEDIDO).hasAnyRole(ROLE_OPERADOR, ROLE_ADMIN)
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter(resourceClientId));
        return converter;
    }

    static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        private final String resourceClientId;

        KeycloakRealmRoleConverter(String resourceClientId) {
            this.resourceClientId = resourceClientId;
        }

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS);
            if (realmAccess != null) {
                addRoles(authorities, realmAccess.get(REALM_ACCESS_ROLES));
            }

            Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
            if (resourceAccess != null) {
                Object clientAccessObj = resourceAccess.get(resourceClientId);
                if (clientAccessObj instanceof Map<?, ?> clientAccessMap) {
                    addRoles(authorities, clientAccessMap.get(REALM_ACCESS_ROLES));
                }
            }

            return authorities;
        }

        private void addRoles(List<GrantedAuthority> authorities, Object rolesObj) {
            if (rolesObj instanceof List<?> roles) {
                for (Object role : roles) {
                    if (role instanceof String roleName && !roleName.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority(ROLE_NAME + roleName.toUpperCase()));
                    }
                }
            }
        }
    }
}

package com.prestobr.financeiro.infra;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtro de segurança que valida requisições vindas do API Gateway (Traefik).
 *
 * Este filtro garante que apenas requisições autenticadas pelo Gateway possam
 * acessar os endpoints protegidos do serviço fiscal.
 *
 * Headers esperados (propagados pelo Traefik ForwardAuth):
 * - X-Gateway-Secret: Secret compartilhado para validar origem da requisição
 * - X-User-Id: ID do usuário autenticado
 * - X-User-Roles: Roles do usuário (separadas por vírgula)
 *
 * @see <a href="https://doc.traefik.io/traefik/middlewares/http/forwardauth/">Traefik ForwardAuth</a>
 */
@Slf4j
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Endpoints públicos (não requerem autenticação)
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Valida o secret do Gateway
        String secret = request.getHeader(GATEWAY_SECRET_HEADER);
        if (!isValidGatewaySecret(secret)) {
            log.warn("Requisição rejeitada - Gateway secret inválido ou ausente. Path: {}", path);
            sendUnauthorizedResponse(response);
            return;
        }

        // Configura o contexto de segurança com os dados do usuário
        configureSecurityContext(request);

        filterChain.doFilter(request, response);
    }

    /**
     * Verifica se o path é público (não requer autenticação).
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    /**
     * Valida se o secret do Gateway está correto.
     */
    private boolean isValidGatewaySecret(String secret) {
        return secret != null && secret.equals(gatewaySecret);
    }

    /**
     * Configura o SecurityContext do Spring Security com os dados do usuário
     * vindos dos headers propagados pelo Gateway.
     */
    private void configureSecurityContext(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        String roles = request.getHeader(USER_ROLES_HEADER);

        if (userId != null) {
            List<SimpleGrantedAuthority> authorities = parseRoles(roles);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Usuário autenticado via Gateway: userId={}, roles={}", userId, roles);
        }
    }

    /**
     * Converte a string de roles em lista de authorities do Spring Security.
     * Formato esperado: "ADMIN,USER,fiscal.read" → [ROLE_ADMIN, ROLE_USER, ROLE_fiscal.read]
     */
    private List<SimpleGrantedAuthority> parseRoles(String roles) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if (roles != null && !roles.isEmpty()) {
            for (String role : roles.split(",")) {
                String trimmedRole = role.trim();
                if (!trimmedRole.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + trimmedRole));
                }
            }
        }

        return authorities;
    }

    /**
     * Envia resposta 401 Unauthorized.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Invalid or missing gateway authentication\"}");
    }
}
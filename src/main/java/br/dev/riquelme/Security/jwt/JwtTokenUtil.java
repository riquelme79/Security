/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.dev.riquelme.Security.jwt;

import br.dev.riquelme.Security.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe de apoio para manipular token JWT com base no objeto User. Provê
 * métodos para gerar, validar e assinar tokens.
 *
 * @author Riquelme
 */
@Component
public class JwtTokenUtil {

    private static final long EXPIRE_DURATION = 24 * 60 * 60 * 1000; // 24 hour
    @Value("${app.jwt.secret}")
    private String SECRET_KEY;
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenUtil.class);

    /**
     * Gera o token com base no usuário fornecido.
     *
     * @param user
     * @return
     */
    public String generateAccessToken(User user) {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.builder()
                .subject(String.format("%s,%s", user.getId(), user.getEmail()))
                .claim("roles", user.getRoles())
                .issuer("Riquelme")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_DURATION))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Valida o token fornecido.
     *
     * @param token
     * @return true - Token Ok false - Token ERROR
     */
    public boolean validateAccessToken(String token) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
            SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);
            // Caso falhar é lançada uma exception.
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            LOGGER.error("JWT expired", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Token is null, empty or only whitespace", ex.getMessage());
        } catch (MalformedJwtException ex) {
            LOGGER.error("JWT is invalid", ex);
        } catch (UnsupportedJwtException ex) {
            LOGGER.error("JWT is not supported", ex);
        } catch (SignatureException ex) {
            LOGGER.error("Signature validation failed");
        }
        return false;
    }

    /**
     * Extrai o subject do token O subject do token contem UserID e o email,
     * dados suficientes para recriar um objeto User.
     *
     * @param token
     * @return
     */
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extrai as Roles do Token
     *
     * @param token
     * @return
     */
    public String getRoles(String token) {
        return (String) parseClaims(token).get("roles");
    }

    /**
     * Mapeia o JSON JWT.
     *
     * @param token
     * @return
     */
    private Claims parseClaims(String token) {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parser()
                .verifyWith(secretKey).build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

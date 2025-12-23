package com.ecommerece.project.security.jwt;
import com.ecommerece.project.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${spring.app.jwtSecret}")
    private  String jwtSecret;

    @Value("${spring.app.jwtCookieName}")
    private String jwtCookie;

//    getting jwt from header   just for swagger ui
    public String getJwtFromHeader(HttpServletRequest request){
        String bearerToken=request.getHeader("Authorization");
        logger.debug("AuthorizationToken: {}",bearerToken);
        if(bearerToken!=null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
//            remove Bearer  Prefix bear+ 1 space
        }
        return null;
    }

    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie cookie= WebUtils.getCookie(request,jwtCookie);
        if(cookie!=null){
           // System.out.println("Cookie :"+cookie.getValue());
            return cookie.getValue();
        }
        else{
            return null;
        }
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal){
        String jwt=generateTokenFromUsername(userPrincipal.getUsername());
        ResponseCookie cookie=ResponseCookie.from(jwtCookie,jwt)
                .path("/api")
                .maxAge(24*60*60)
                .httpOnly(false)
                .secure(false)
                .build();
        return cookie;
    }

    public ResponseCookie getCleanJwtCookie(){
        ResponseCookie cookie=ResponseCookie.from(jwtCookie,null)
                .path("/api")
                .build();
        return cookie;
    }

//    generating Token from username
    public String generateTokenFromUsername(String username){
//        String username=userDetails.getUsername();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date().getTime()+jwtExpirationMs)))
                .signWith(key())
                .compact();
    }
//    generating username from jwt token
    public String getUsernameFromJwtToken(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }

//    generate Signing key
    public Key key(){
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret)
        );
    }

//    validate JWT token
    public boolean validatedJwtToken(String authToken){
        try{
            System.out.println("validate");
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        }
        catch(MalformedJwtException e){
            logger.error("Invalid JWT token{} ",e.getMessage());
        }
        catch(ExpiredJwtException e){
            logger.error("Expired JWT token{} ",e.getMessage());
        }
        catch(UnsupportedJwtException e){
            logger.error(" JWT token is unsupported{} ",e.getMessage());
        }
        catch(IllegalArgumentException e){
            logger.error("JWT claims String is empty{} ",e.getMessage());
        }
        return false;
    }
}

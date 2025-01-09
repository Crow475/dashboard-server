package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;

import org.dashboard.common.Request;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;

import org.dashboard.server.DBUtils;
import org.dashboard.server.defaultResponses.protectedErrors;

public class createDashboardRequest {
    public static void handle(Request req, KeyPair pair, ObjectOutputStream out) throws IOException {
        HashMap<String, String> message = req.getMessage();
        String username = message.get("username");
        String dashboardName = message.get("dashboardName");
        String token = req.getToken();

        Request response = null;
        
        if (username != null && token != null && dashboardName != null) {
            try {
                Claims claims = Jwts.parser()
                    .verifyWith(pair.getPublic())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                    
                String subject = claims.getSubject();
                String issuer = claims.getIssuer();
                Date expiration = claims.getExpiration();
    
                if (subject.equals(username)) {
                    if (issuer.equals("Dashboard Server")) {
                        if (expiration.after(new Date())) {
                            boolean exists = DBUtils.dashbaordExists(dashboardName, username);
                            
                            if (!exists) {
                                boolean success = DBUtils.createDashboard(username, dashboardName);
                                HashMap<String, String> messageContent = new HashMap<String, String>();
                                messageContent.put("username", username);
                                messageContent.put("dashboardName", dashboardName);
                                
                                if (success) {
                                    messageContent.put("success", "Dashboard created");
                                    response = new Request("Create dashboard success", messageContent);
                                } else {
                                    messageContent.put("error", "Failed to create dashboard");
                                    response = new Request("Create dashboard error", messageContent);
                                }
                            } else {
                                HashMap<String, String> messageContent = new HashMap<String, String>();
                                messageContent.put("error", "Dashboard with this name already exists");
                                response = new Request("Create dashboard error", messageContent);
                            }
                        } else {
                            response = protectedErrors.tokenExpired();
                            DBUtils.deleteSession(username);
                        }
                    } else {
                        response = protectedErrors.invalidIssuer();
                    }
                } else {
                    response = protectedErrors.notAllowed();
                }
            } catch (ExpiredJwtException e) {
                response = protectedErrors.tokenExpired();
                DBUtils.deleteSession(username);
            } catch (InvalidClaimException e) {
                response = protectedErrors.invalidRequest();
            } catch (JwtException e) {
                response = protectedErrors.invalidRequest();
            }
        } else {
            response = protectedErrors.invalidRequest();
        }

        out.writeObject(response);
    }
}

package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;

import org.dashboard.common.Request;
import org.dashboard.common.models.DashboardModel;
import org.dashboard.server.DBUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import org.dashboard.server.defaultResponses.protectedErrors;

public class renameDashboardRequest {
    public static void handle(Request req, KeyPair pair, ObjectOutputStream out) throws IOException {
        HashMap<String, String> message = req.getMessage();
        String username = message.get("username");
        String dashboardName = message.get("dashboardName");
        String newDashboardName = message.get("newDashboardName");
        String token = req.getToken();

        Request response = null;

        if (username != null && token != null && dashboardName != null && newDashboardName != null) {
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
                            DashboardModel dashboard = DBUtils.getDashboardWithJSONProps(newDashboardName, username);
                            if (dashboard == null) {
                                boolean success = DBUtils.renameDashboard(dashboardName, newDashboardName, username);
                                HashMap<String, String> messageContent = new HashMap<String, String>();
                                messageContent.put("username", username);
                                messageContent.put("dashboardName", dashboardName);
                                messageContent.put("newDashboardName", newDashboardName);
        
                                if (success) {
                                    messageContent.put("success", "Dashboard renamed");
        
                                    response = new Request("Rename dashboard success", messageContent);
                                } else {
                                    messageContent.put("error", "Failed to rename dashboard");
        
                                    response = new Request("Rename dashboard error", messageContent);
                                }
                            } else {
                                HashMap<String, String> messageContent = new HashMap<String, String>();
                                messageContent.put("error", "Dashboard with new name already exists");

                                response = new Request("Rename dashboard error", messageContent);
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

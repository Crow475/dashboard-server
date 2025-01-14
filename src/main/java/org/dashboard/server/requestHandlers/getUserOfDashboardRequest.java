package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

import org.dashboard.common.Request;
import org.dashboard.common.models.UserOfDashboard;
import org.dashboard.common.Role;

import org.dashboard.server.CheckAccess;
import org.dashboard.server.DBUtils;
import org.dashboard.server.defaultResponses.protectedErrors;

public class getUserOfDashboardRequest {
    public static void handle(Request req, KeyPair pair, ObjectOutputStream out) throws IOException {
        HashMap<String, String> message = req.getMessage();
        String username = message.get("username");
        String subjectUser = message.get("subjectUser");
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

                if (CheckAccess.isAtLeastViewer(subject, username, dashboardName)) {
                    if (issuer.equals("Dashboard Server")) {
                        if (expiration.after(new Date())) {
                            Role role = CheckAccess.getRoleIn(subjectUser, username, dashboardName);
                            UserOfDashboard user = new UserOfDashboard(subjectUser, dashboardName, role);

                            HashMap<String, String> messageContent = new HashMap<String, String>();
                            messageContent.put("username", username);
                            messageContent.put("dashboardName", dashboardName);
                           
                            messageContent.put("success", "User of dashboard retrieved");

                            response = new Request("Get user of dashboard success", messageContent, user);
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
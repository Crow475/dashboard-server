package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;

import org.dashboard.common.Request;
import org.dashboard.server.CheckAccess;
import org.dashboard.server.DBUtils;
import org.dashboard.server.defaultResponses.protectedErrors;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class getDashboardViewersRequest {
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

                if (CheckAccess.isViewer(subject, username, dashboardName) || CheckAccess.isOwner(subject, username, dashboardName)) {
                    if (issuer.equals("Dashboard Server")) {
                        if (expiration.after(new Date())) {
                            ArrayList<String> viewers = DBUtils.getDashboardViewers(username, dashboardName);
                            HashMap<String, String> messageContent = new HashMap<String, String>();
                            messageContent.put("username", username);
                            messageContent.put("dashboardName", dashboardName);

                            if (viewers != null) {
                                messageContent.put("success", "Viewers retrieved");

                                response = new Request("Get dashboard viewers success", messageContent, viewers);
                            } else {
                                messageContent.put("error", "Dashboard does not exist");

                                response = new Request("Get dashboard viewers error", messageContent);
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

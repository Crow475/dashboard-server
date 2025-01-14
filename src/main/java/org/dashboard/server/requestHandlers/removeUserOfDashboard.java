package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import org.dashboard.common.Request;
import org.dashboard.common.models.UserOfDashboard;

import org.dashboard.server.CheckAccess;
import org.dashboard.server.DBUtils;
import org.dashboard.server.defaultResponses.protectedErrors;

public class removeUserOfDashboard {
    public static void handle(Request req, KeyPair pair, ObjectOutputStream out) throws IOException {
        HashMap<String, String> message = req.getMessage();
        String username = message.get("username");
        String subjectUser = message.get("subjectUser");
        String dashboardName = message.get("dashboardName");
        String role = message.get("role");
        String token = req.getToken();

        Request response = null;

        if (username != null && token != null && dashboardName != null && subjectUser != null && role != null) {
            try {
                Claims claims = Jwts.parser()
                    .verifyWith(pair.getPublic())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

                String subject = claims.getSubject();
                String issuer = claims.getIssuer();
                Date expiration = claims.getExpiration();

                if (subject.equals(subjectUser) || CheckAccess.isAtLeastAdmin(subject, username, dashboardName)) {
                    if (issuer.equals("Dashboard Server")) {
                        if (expiration.after(new Date())) {
                            HashMap<String, String> messageContent = new HashMap<String, String>();
                            messageContent.put("username", username);
                            messageContent.put("dashboardName", dashboardName);
                            messageContent.put("subjectUser", subjectUser);

                            ArrayList<UserOfDashboard> usersOfDashboard = DBUtils.getDashboardUsers(username, dashboardName);
                            ArrayList<String> usernames = new ArrayList<>();
                            for (UserOfDashboard user : usersOfDashboard) {
                                usernames.add(user.getUsername());
                            }

                            if (usernames.contains(subjectUser)) {
                                boolean wasRemoved = false;
                                switch (role.toUpperCase()) {
                                    case "ADMIN":
                                        wasRemoved = DBUtils.removeAdmin(username, dashboardName, subjectUser);
                                        break;
                                    case "EDITOR":
                                        wasRemoved = DBUtils.removeEditor(username, dashboardName, subjectUser);
                                        break;
                                    case "VIEWER":
                                        wasRemoved = DBUtils.removeViewer(username, dashboardName, subjectUser);
                                        break;
                                    default:
                                        break;
                                }
                                if (wasRemoved) {
                                    messageContent.put("success", "User removed from dashboard");
                                        
                                    response = new Request("Remove user of dashboard success", messageContent);
                                } else {
                                    messageContent.put("error", "Error removing user of dashboard");
                                    response = new Request("Remove user of dashboard error", messageContent);
                                }
                            } else {
                                messageContent.put("error", "User is not a part of this dashboard");
                                response = new Request("Remove user of dashboard error", messageContent);
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

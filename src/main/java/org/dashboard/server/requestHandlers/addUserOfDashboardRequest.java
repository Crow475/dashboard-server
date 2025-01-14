package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.dashboard.common.Request;
import org.dashboard.common.models.UserOfDashboard;
import org.dashboard.server.CheckAccess;
import org.dashboard.server.DBUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import org.dashboard.server.defaultResponses.protectedErrors;

public class addUserOfDashboardRequest {
    public static void handle(Request req, KeyPair pair, ObjectOutputStream out) throws IOException {
        HashMap<String, String> message = req.getMessage();
        String username = message.get("username");
        String subjectUser = message.get("subjectUser");
        String dashboardName = message.get("dashboardName");
        String newRole = message.get("newRole");
        String token = req.getToken();

        Request response = null;

        if (username != null && token != null && dashboardName != null && subjectUser != null && newRole != null) {
            try {
                Claims claims = Jwts.parser()
                    .verifyWith(pair.getPublic())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

                String subject = claims.getSubject();
                String issuer = claims.getIssuer();
                Date expiration = claims.getExpiration();

                if (CheckAccess.isAtLeastAdmin(subject, username, dashboardName)) {
                    if (issuer.equals("Dashboard Server")) {
                        if (expiration.after(new Date())) {
                            HashMap<String, String> messageContent = new HashMap<String, String>();
                            messageContent.put("username", username);
                            messageContent.put("dashboardName", dashboardName);
                            messageContent.put("subjectUser", subjectUser);

                            if (DBUtils.usernameExists(subjectUser)) {
                                ArrayList<UserOfDashboard> usersOfDashboard = DBUtils.getDashboardUsers(username, dashboardName);
                                ArrayList<String> usernames = new ArrayList<>();
                                for (UserOfDashboard user : usersOfDashboard) {
                                    usernames.add(user.getUsername());
                                }
                                if (!usernames.contains(subjectUser)) {
                                    boolean roleAdded = false;
                                    switch (newRole.toUpperCase()) {
                                        case "ADMIN":
                                            roleAdded = DBUtils.addAdmin(username, dashboardName, subjectUser);
                                            break;
                                        case "EDITOR":
                                            roleAdded = DBUtils.addEditor(username, dashboardName, subjectUser);
                                            break;
                                        case "VIEWER":
                                            roleAdded = DBUtils.addViewer(username, dashboardName, subjectUser);
                                            break;
                                        default:
                                            break;
                                    }

                                    if (roleAdded) {
                                        messageContent.put("success", "User added to dashboard");
                                        
                                        response = new Request("Add user of dashboard success", messageContent);
                                    } else {
                                        messageContent.put("error", "Error adding user to dashboard");

                                        response = new Request("Add user of dashboard error", messageContent);
                                    }
                                } else {
                                    messageContent.put("error", "User already a part of dashboard");

                                    response = new Request("Add user of dashboard error", messageContent);
                                }
                            } else {
                                messageContent.put("error", "User does not exist");

                                response = new Request("Add user of dashboard error", messageContent);
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

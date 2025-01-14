package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;

import org.dashboard.common.Request;
import org.dashboard.common.Role;
import org.dashboard.server.CheckAccess;
import org.dashboard.server.DBUtils;
import org.dashboard.server.defaultResponses.protectedErrors;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class updateUserOfDashboardRequest {
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
                            Role initialRole = CheckAccess.getRoleIn(subjectUser, username, dashboardName);
                            
                            HashMap<String, String> messageContent = new HashMap<String, String>();
                            messageContent.put("username", username);
                            messageContent.put("dashboardName", dashboardName);
                            messageContent.put("subjectUser", subjectUser);

                            if (initialRole != Role.NONE) {
                                if (!initialRole.toString().equalsIgnoreCase(newRole)) {
                                    boolean removedAdmin = false;
                                    boolean removedEditor = false;
                                    boolean removedViewer = false;
                                    
                                    switch (initialRole) {
                                        case Role.ADMIN:
                                            removedAdmin = DBUtils.removeAdmin(username, dashboardName, subjectUser);
                                            break;
                                        case Role.EDITOR:
                                            removedEditor = DBUtils.removeEditor(username, dashboardName, subjectUser);
                                            break;
                                        case Role.VIEWER:
                                            removedViewer = DBUtils.removeViewer(username, dashboardName, subjectUser);
                                            break;
                                        default:
                                            break;
                                    }
                                    if (removedAdmin || removedEditor || removedViewer) {
                                        boolean addedRole = false;
                                        switch (newRole.toUpperCase()) {
                                            case "ADMIN":
                                                addedRole = DBUtils.addAdmin(username, dashboardName, subjectUser);
                                                break;
                                            case "EDITOR":
                                                addedRole = DBUtils.addEditor(username, dashboardName, subjectUser);
                                                break;
                                            case "VIEWER":
                                                addedRole = DBUtils.addViewer(username, dashboardName, subjectUser);
                                                break;
                                            default:
                                                break;
                                        }
                                        if (addedRole) {
                                            messageContent.put("success", "User role updated");
                                            response = new Request("Update user of dashboard success", messageContent);
                                        } else {
                                            messageContent.put("error", "Failed to update user role");
                                            response = new Request("Update user of dashboard error", messageContent);
                                        }
                                    } else {
                                        messageContent.put("error", "Failed to remove user from dashboard");
                                        response = new Request("Update user of dashboard error", messageContent);
                                    }
    
                                } else {
                                    messageContent.put("error", "User already has that role");
                                    response = new Request("Update user of dashboard error", messageContent);
                                }
                            } else {
                                messageContent.put("error", "User is not a part of this dashboard");
                                response = new Request("Update user of dashboard error", messageContent);
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

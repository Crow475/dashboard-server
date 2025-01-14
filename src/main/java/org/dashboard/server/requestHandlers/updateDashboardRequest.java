package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;

import org.dashboard.common.Request;
import org.dashboard.common.models.DashboardModel;
import org.dashboard.server.CheckAccess;
import org.dashboard.server.DBUtils;
import org.dashboard.server.defaultResponses.protectedErrors;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class updateDashboardRequest {
    public static void handle(Request req, KeyPair pair, ObjectOutputStream out) throws IOException {
        HashMap<String, String> message = req.getMessage();
        String username = message.get("username");
        String dashboardName = message.get("dashboardName");
        String properties = message.get("properties");
        DashboardModel dashboardModel = (DashboardModel)req.getObject();
        String token = req.getToken();

        Request response = null;

        if (username != null && token != null && dashboardName != null && dashboardModel != null && properties != null) {
            try {
                Claims claims = Jwts.parser()
                    .verifyWith(pair.getPublic())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

                String subject = claims.getSubject();
                String issuer = claims.getIssuer();
                Date expiration = claims.getExpiration();
    
                if (CheckAccess.isAtLeastEditor(subject, username, dashboardName)) {
                    if (issuer.equals("Dashboard Server")) {
                        if (expiration.after(new Date())) {
                            DashboardModel dashboardInDB = DBUtils.getDashboardWithJSONProps(dashboardName, username);
                            
                            if (dashboardInDB != null) {
                                System.out.println(subject);
                                System.out.println(DBUtils.getUserName(dashboardInDB.getOwnerId()));
                                
                                if (subject.equals(DBUtils.getUserName(dashboardInDB.getOwnerId()))) {
                                    // dashboardModel.updatePropertiesFromJSON();
                                    boolean success = DBUtils.updateDashboard(dashboardModel, properties);
                                    HashMap<String, String> messageContent = new HashMap<String, String>();
                                    messageContent.put("username", username);
                                    messageContent.put("dashboardName", dashboardName);
    
                                    if (success) {
                                        messageContent.put("success", "Dashboard updated");
                                        response = new Request("Update dashboard success", messageContent);
                                    } else {
                                        messageContent.put("error", "Failed to update dashboard");
                                        response = new Request("Update dashboard error", messageContent);
                                    }
                                } else {
                                    HashMap<String, String> messageContent = new HashMap<String, String>();
                                    messageContent.put("error", "You do not have permission to update this dashboard");
                                    response = new Request("Update dashboard error", messageContent);
                                }
                            } else {
                                HashMap<String, String> messageContent = new HashMap<String, String>();
                                messageContent.put("error", "Dashboard with this name does not exist");
                                response = new Request("Update dashboard error", messageContent);
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

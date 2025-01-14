package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;

import org.dashboard.common.Passwords;
import org.dashboard.common.Request;
import org.dashboard.server.DBUtils;
import org.dashboard.server.defaultResponses.protectedErrors;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class userDeleteRequest {
    public static void handle(Request req, KeyPair pair, ObjectOutputStream out) throws IOException {
        HashMap<String, String> message = req.getMessage();
        String username = message.get("username");
        String password = message.get("password");
        String token = req.getToken();

        Request response = null;

        if (username != null && token != null && password != null) {
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
                            Passwords.Password expectedPassword = DBUtils.getExpectedPassword(username);

                            if (expectedPassword == null) {
                                HashMap<String, String> messageContent = new HashMap<String, String>();
                                messageContent.put("error", "Account does not exist");

                                response = new Request("User delete error", messageContent);
                            } else {
                                if (!Passwords.verify(password, expectedPassword)) {
                                    HashMap<String, String> messageContent = new HashMap<String, String>();
                                    messageContent.put("error", "Incorrect password");

                                    response = new Request("User delete error", messageContent);
                                } else {
                                    HashMap<String, String> messageContent = new HashMap<String, String>();
                                    messageContent.put("username", username);
        
                                    if (DBUtils.usernameExists(username)) {
                                        if (DBUtils.deleteUser(username)) {
                                            messageContent.put("success", "Account deleted");
        
                                            response = new Request("User delete success", messageContent);
                                        }
                                    } else {
                                        messageContent.put("error", "Account does not exist");
        
                                        response = new Request("User delete error", messageContent);
                                    }
                                }
                            }
                        } else {
                            response = protectedErrors.tokenExpired();
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

package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

import org.dashboard.common.Request;

import org.dashboard.server.DBUtils;
import org.dashboard.server.defaultResponses.protectedErrors;

public class searchForUserRequest {
    public static void handle(Request req, KeyPair pair, ObjectOutputStream out) throws IOException {
        HashMap<String, String> message = req.getMessage();
        String username = message.get("username");
        String token = req.getToken();

        Request response = null;

        if (username != null && token != null) {
            try {
                Claims claims = Jwts.parser()
                    .verifyWith(pair.getPublic())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

                String issuer = claims.getIssuer();
                Date expiration = claims.getExpiration();

                if (issuer.equals("Dashboard Server")) {
                    if (expiration.after(new Date())) {
                        ArrayList<String> users = DBUtils.searchForUser(username);
                        HashMap<String, String> messageContent = new HashMap<String, String>();
                        messageContent.put("username", username);

                        if (users != null) {
                            messageContent.put("success", "Users found");

                            response = new Request("Search for user success", messageContent, users);
                        } else {
                            messageContent.put("error", "No users found");

                            response = new Request("Search for user error", messageContent);
                        }
                    } else {
                        response = protectedErrors.tokenExpired();
                        DBUtils.deleteSession(username);
                    }
                } else {
                    response = protectedErrors.invalidIssuer();
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

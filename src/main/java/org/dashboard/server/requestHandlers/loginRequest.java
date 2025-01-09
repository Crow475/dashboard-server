package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.dashboard.common.Passwords;
import org.dashboard.common.Request;
import org.dashboard.server.DBUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;

public class loginRequest {
    public static void handle(Request req, KeyPair pair, SignatureAlgorithm alg, ArrayList<String> loggedInUsername, ObjectOutputStream out) throws IOException {
        HashMap<String, String> message = req.getMessage();

        String username = message.get("username");
        String password = message.get("password");

        Request response = null;

        if (username != null && password != null) {
            Passwords.Password expectedPassword = DBUtils.getExpectedPassword(username);
            if (expectedPassword == null) {
                HashMap<String, String> messageContent = new HashMap<String, String>();
                messageContent.put("error", "User does not exist");

                response = new Request("Login error", messageContent);
            } else if (Passwords.verify(password, expectedPassword)) {
                if (!DBUtils.sessionExists(username)) {
                    String token = Jwts.builder()
                        .subject(username)
                        .issuer("Dashboard Server")
                        .issuedAt(new Date())
                        .expiration(new Date(new Date().getTime() + 1000 * 60 * 60 * 24))
                        // .expiration(new Date(new Date().getTime() + 1000 * 20))
                        .signWith(pair.getPrivate(), alg)
                        .compact();

                    if (DBUtils.registerSession(username)) {
                        HashMap<String, String> messageContent = new HashMap<String, String>();
                        messageContent.put("success", "Login success");
                        messageContent.put("token", token);

                        if (loggedInUsername.size() > 0) {
                            loggedInUsername.set(0, username);
                        } else {
                            loggedInUsername.add(username);
                        }
        
                        response = new Request("Login success", messageContent);
                    } else {
                        HashMap<String, String> messageContent = new HashMap<String, String>();
                        messageContent.put("error", "Failed to register session");
        
                        response = new Request("Login error", messageContent);
                    }
                } else {
                    HashMap<String, String> messageContent = new HashMap<String, String>();
                    messageContent.put("error", "User already logged in");

                    response = new Request("Login error", messageContent);
                }
            } else {
                HashMap<String, String> messageContent = new HashMap<String, String>();
                messageContent.put("error", "Invalid username or password");

                response = new Request("Login error", messageContent);
            }
        } else {
            HashMap<String, String> messageContent = new HashMap<String, String>();
            messageContent.put("error", "Invalid request message");
            
            response = new Request("Login error", messageContent);
        }
        
        if (response != null) {
            out.writeObject(response);
            System.out.println("Sent response: " + response.getType() + " " + response.getMessage());
        }
    }
}

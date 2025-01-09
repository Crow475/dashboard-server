package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Set;

import org.dashboard.common.Passwords;
import org.dashboard.common.Request;
import org.dashboard.server.DBUtils;

public class userCreateRequest {
    public static void handle(Request req, Set<String> reservedUsernames, ObjectOutputStream out) throws IOException {
        HashMap<String, String> message = req.getMessage();
        Object requestObject = req.getObject();
        String username = message.get("username");
        Passwords.Password password = (Passwords.Password)requestObject;
        
        Request response = null;

        boolean taken = reservedUsernames.contains(username.toLowerCase()) || DBUtils.usernameExists(username);

        if (taken) {
            HashMap<String, String> messageContent = new HashMap<String, String>();
            messageContent.put("error", "Username is taken");

            response = new Request("Create user error", messageContent);
        } else {
            boolean success = DBUtils.createUser(username, password);
            if (success) {
                HashMap<String, String> messageContent = new HashMap<String, String>();
                messageContent.put("success", "User created");
                messageContent.put("username", username);

                response = new Request("Create user success", messageContent);
            } else {
                HashMap<String, String> messageContent = new HashMap<String, String>();
                messageContent.put("error", "Failed to create user");

                response = new Request("Create user error", messageContent);
            }
        }

        out.writeObject(response);
    }
}

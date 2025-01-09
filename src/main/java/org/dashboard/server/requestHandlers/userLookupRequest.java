package org.dashboard.server.requestHandlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Set;

import org.dashboard.common.Request;
import org.dashboard.server.DBUtils;

public class userLookupRequest {
    public static void handle(Request req, Set<String> reservedUsernames, ObjectOutputStream out) throws IOException {
        HashMap<String, String> message = req.getMessage();
        String username = message.get("username");

        Request response = null;

        if (username != null) {

            if (reservedUsernames.contains(username.toLowerCase())) {
                HashMap<String, String> messageContent = new HashMap<String, String>();
                messageContent.put("username", username);
                messageContent.put("exists", "true");
                
                response = new Request("User lookup success", messageContent);
            } else {
                boolean exists = DBUtils.usernameExists(username);
                HashMap<String, String> messageContent = new HashMap<String, String>();
                messageContent.put("username", username);
                messageContent.put("exists", "" + exists);

                response = new Request("User lookup success", messageContent);
            }
            
        } else {
            HashMap<String, String> messageContent = new HashMap<String, String>();
            messageContent.put("error", "Invalid request message");
            
            response = new Request("User lookup error", messageContent);
        }

        if (response != null) {
            out.writeObject(response);
            System.out.println("Sent response: " + response.getType() + " " + response.getMessage());
        }
    }
}

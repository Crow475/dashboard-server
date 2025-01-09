package org.dashboard.server.defaultResponses;

import java.util.HashMap;

import org.dashboard.common.Request;

public class protectedErrors {
    public static Request tokenExpired() {
        HashMap<String, String> messageContent = new HashMap<String, String>();
        messageContent.put("error", "Token expired");

        return new Request("Protected operation error", messageContent);
    }

    public static Request invalidIssuer() {
        HashMap<String, String> messageContent = new HashMap<String, String>();
        messageContent.put("error", "Invalid token issuer");

        return new Request("Protected operation error", messageContent);
    }

    public static Request notAllowed() {
        HashMap<String, String> messageContent = new HashMap<String, String>();
        messageContent.put("error", "Operation not allowed");

        return new Request("Protected operation error", messageContent);
    }

    public static Request invalidRequest() {
        HashMap<String, String> messageContent = new HashMap<String, String>();
        messageContent.put("error", "Invalid request message");

        return new Request("Protected operation error", messageContent);
    }
}

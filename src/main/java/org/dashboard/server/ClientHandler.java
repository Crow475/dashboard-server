package org.dashboard.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSession;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;

import org.dashboard.common.Request;
import org.dashboard.server.requestHandlers.addUserOfDashboardRequest;
import org.dashboard.server.requestHandlers.createDashboardRequest;
import org.dashboard.server.requestHandlers.deleteDashboardRequest;
import org.dashboard.server.requestHandlers.getDashboardRequest;
import org.dashboard.server.requestHandlers.getDashboardUsersRequest;
import org.dashboard.server.requestHandlers.getDashboardViewersRequest;
import org.dashboard.server.requestHandlers.getUserOfDashboardRequest;
import org.dashboard.server.requestHandlers.loginRequest;
import org.dashboard.server.requestHandlers.logoutRequest;
import org.dashboard.server.requestHandlers.removeUserOfDashboard;
import org.dashboard.server.requestHandlers.renameDashboardRequest;
import org.dashboard.server.requestHandlers.searchForUserRequest;
import org.dashboard.server.requestHandlers.updateDashboardRequest;
import org.dashboard.server.requestHandlers.updateUserOfDashboardRequest;
import org.dashboard.server.requestHandlers.userLookupRequest;
import org.dashboard.server.requestHandlers.userCreateRequest;
import org.dashboard.server.requestHandlers.userDashboardsRequest;
import org.dashboard.server.requestHandlers.userDeleteRequest;

public class ClientHandler implements Runnable {
    private SSLSocket clientSocket;

    public ClientHandler(SSLSocket clientSocket) {
        this.clientSocket = clientSocket;
    }
    
    @Override
    public void run() {
        this.clientSocket.setEnabledCipherSuites(this.clientSocket.getSupportedCipherSuites());
        SignatureAlgorithm alg = Jwts.SIG.RS512;
        KeyPair pair = alg.keyPair().build();
        
        Set<String> reservedUsernames = new HashSet<>();
            reservedUsernames.add("admin");
            reservedUsernames.add("root");
            reservedUsernames.add("user");
            reservedUsernames.add("user");
            reservedUsernames.add("dashboard");
            reservedUsernames.add("server");
            reservedUsernames.add("client");
            reservedUsernames.add("login");
            reservedUsernames.add("username");
            reservedUsernames.add("password");
            reservedUsernames.add("test");
        
        try {
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ArrayList<String> loggedInUsername = new ArrayList<>();
            
            Request req;
            try {
                clientSocket.startHandshake();
                SSLSession session = clientSocket.getSession();
                
                loop: while ((req = (Request)in.readObject()) != null) {
                    String requestType = req.getType();

                    System.out.println(requestType);

                    switch (requestType) {
                        case "Login request":
                            loginRequest.handle(req, pair, alg, loggedInUsername, out);
                            break;
                        case "User exists":
                            userLookupRequest.handle(req, reservedUsernames, out);
                            break;
                        case "Create user":
                            userCreateRequest.handle(req, reservedUsernames, out);
                            break;
                        case "Delete user":
                            userDeleteRequest.handle(req, pair, out);
                            break;
                        case "Get user dashboards":
                            userDashboardsRequest.handle(req, pair, out);
                            break;
                        case "Create dashboard":
                            createDashboardRequest.handle(req, pair, out);
                            break;
                        case "Update dashboard":
                            updateDashboardRequest.handle(req, pair, out);
                            break;
                        case "Get dashboard":
                            getDashboardRequest.handle(req, pair, out);
                            break;
                        case "Delete dashboard":
                            deleteDashboardRequest.handle(req, pair, out);
                            break;
                        case "Rename dashboard":
                            renameDashboardRequest.handle(req, pair, out);
                            break;
                        case "Get dashboard viewers":
                            getDashboardViewersRequest.handle(req, pair, out);
                            break;
                        case "Get dashboard users":
                            getDashboardUsersRequest.handle(req, pair, out);
                            break;
                        case "Get user of dashboard":
                            getUserOfDashboardRequest.handle(req, pair, out);
                            break;
                        case "Update user of dashboard":
                            updateUserOfDashboardRequest.handle(req, pair, out);
                            break;
                        case "Add user of dashboard":
                            addUserOfDashboardRequest.handle(req, pair, out);
                            break;
                        case "Remove user of dashboard":
                            removeUserOfDashboard.handle(req, pair, out);
                            break;
                        case "Search for user":
                            searchForUserRequest.handle(req, pair, out);
                            break;
                        case "Logout request":
                            logoutRequest.handle(req, pair, out);
                            break;
                        case "Disconnect":
                            if (loggedInUsername.size() > 0) {
                                DBUtils.deleteSession(loggedInUsername.get(0));
                            }
                            in.close();
                            out.close();

                            clientSocket.close();
                            
                            break loop;
                        default:
                            break;
                    }
                }
            } catch (ClassNotFoundException e) {
                in.close();
                out.close();
                clientSocket.close();
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

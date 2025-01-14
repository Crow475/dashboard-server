package org.dashboard.server.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyStore;
import java.util.HashMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.dashboard.common.Request;

public class Client {
    public static void main(String[] args) {

        SSLContext sslContext = null;

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream("/mykeystore.jks"), "Qw847891".toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, "Qw847891".toCharArray());
            KeyManager[] km = keyManagerFactory.getKeyManagers();

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStore);
            TrustManager[] tm = trustManagerFactory.getTrustManagers();

            sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(km, tm, null);

        } catch (Exception e) {
            e.printStackTrace();
        }

        SSLSocketFactory ssf = (SSLSocketFactory)sslContext.getSocketFactory();
        
        try (SSLSocket socket = (SSLSocket) ssf.createSocket("localhost", 3000)) {
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
            
            socket.startHandshake();

            SSLSession session = socket.getSession();
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            String token = "eyJhbGciOiJSUzUxMiJ9.eyJzdWIiOiJVc2VyIiwiaXNzIjoiRGFzaGJvYXJkIFNlcnZlciIsImlhdCI6MTczNjMzMTM3MiwiZXhwIjoxNzM2NDE3NzcyfQ.cC_hjqwtq8DOEMy0Lwi1AtunfE6VWnr2f0cQ-awGzv8CSCFx25yGWz-i2rOKsBa1na8H0E9Hzcm4qrZDbwYNiqmIjv-sL3mycfTETvzZM2W2co3Zu-LKieIJbaS6ZKLgxSFiUx7UW4ClqTEr7TlitDuRe96_jOQmizGuRVAbbGBit7KZI7f1GiOtaFsslvUF7YcQAsHjDHmeBKN2fOgVMBDQxrN31jlo9uUChmWS6lbKfVGVZK0pvaXyqJFdoC62voMYUqSYPPQ2UYIHaqrV1lbZDHIdsOBxJNE9_qa1Lfsebe8iCA7LCLh9TjrFmXyS_WzaUS6GXlUfIS7BZ937BvsXtBID6mFiwRpwqKIMU3ka6vbZpMFG-LYYsqusrRiom8lmV1f_d9Rv_2WkLAGyLZKY9yJdSGtwBFuObXPjrACBukYxTkL-l4RMYn2-UwTLMURrycyu8pIVM1Voy2YrFy-oSCOavcR9yowD7v01bmnx3FfqBXhkLHK8UtQAZStqVgzZP4EGj3opXEZlTpXax2BHZ15wjk7yBzoRLWxMFUAJrXD2KcyYjZnn6K4PUcC5G1uop8CwCoC7IqWHUs3qtprz2TY5k1L19EZp-b0ur_Ag9uEvEa8uBEUG0J-yUkeMGlVQQhAsFEkmqe0zM6LJWSUR3GyO5nytalPYJz878os";

            HashMap<String, String> data = new HashMap<String, String>();
            
            data.put("username", "User");

            Request request = new Request("Get user dashboards", data, token);

            out.writeObject(request);

            try {
                Request response = (Request)in.readObject();
                System.out.println(response.getType());
                System.out.println(response.getMessage());
                System.out.println(response.getObject());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            Request disconnectRequest = new Request("Disconnect", new HashMap<String, String>());

            out.writeObject(disconnectRequest);

            out.close();
            in.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

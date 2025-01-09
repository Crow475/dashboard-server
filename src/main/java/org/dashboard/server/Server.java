package org.dashboard.server;

import java.io.IOException;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.sql.SQLException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import io.github.cdimascio.dotenv.Dotenv;

public class Server {
    public static void main(String[] args) {
        try {
            DBUtils.connect();
        } catch (SQLException e) {
            e.printStackTrace();

            return;
        }

        Dotenv dotenv = Dotenv.load();
        final String KEYSTORE_PATH = dotenv.get("KEYSTORE_PATH");
        final String KEYSTORE_PASSWORD = dotenv.get("KEYSTORE_PASSWORD");
        final String KEY_PASSWORD = dotenv.get("KEY_PASSWORD");

        SSLContext sslContext = null;

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(KEYSTORE_PATH), KEYSTORE_PASSWORD.toCharArray());
    
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, KEY_PASSWORD.toCharArray());
            KeyManager[] km = keyManagerFactory.getKeyManagers();

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStore);
            TrustManager[] tm = trustManagerFactory.getTrustManagers();
    
            sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(km, tm, null);
        } catch (Exception e) {
            e.printStackTrace();
        }


        SSLServerSocket sslServerSocket = null;
        SSLSocket sslSocket = null; 

        try {
            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            sslServerSocket = (SSLServerSocket)ssf.createServerSocket(3000);

            
            while (true) {
                sslSocket = (SSLSocket)sslServerSocket.accept();
                System.out.println("Client connected: " + sslSocket.getInetAddress().getHostAddress());
                
                ClientHandler clientHandler = new ClientHandler(sslSocket);

                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }

        } catch (Exception e) {
            try {
                sslSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }

        DBUtils.disconnect();
    }
}

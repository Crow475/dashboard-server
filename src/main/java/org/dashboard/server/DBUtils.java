package org.dashboard.server;

import java.util.ArrayList;
import java.util.Date;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.github.cdimascio.dotenv.Dotenv;

import org.dashboard.common.Pair;
import org.dashboard.common.Passwords;
import org.dashboard.common.models.DashboardModel;;

public class DBUtils {

    public static class pairKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(final String key, final DeserializationContext ctxt) throws java.io.IOException, JsonProcessingException {
            String[] parts = key.split("=");
            return new Pair<Integer, Integer>(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    private static Dotenv dotenv = Dotenv.load();

    private static final String URL = dotenv.get("DB_URL");
    private static final String USERNAME = dotenv.get("DB_USERNAME");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");

    private static Statement statement;
    private static Connection connection;

    public DBUtils() {

    }

    public static void connect() throws SQLException {
       
        Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        connection = conn;
        statement = connection.createStatement();
        System.out.println("Connected to the database");

    }

    public static void disconnect() {
        try {
            statement.getConnection().close();
            System.out.println("Disconnected from the database");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean createUser(String username, Passwords.Password password) {
        boolean success = false;
        String hash = password.getHash();
        String salt = password.getSalt();
        
        String sql = "INSERT INTO users (user_name, password, salt) VALUES (?, ?, ?)";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, hash);
            preparedStatement.setString(3, salt);
            
            int rowAffected = preparedStatement.executeUpdate();

            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    public static boolean usernameExists(String username) {
        String sql = "SELECT EXISTS(SELECT * FROM users WHERE user_name = ?) AS user_exists";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getBoolean("user_exists");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static Passwords.Password getExpectedPassword(String username) {
        String sql = "SELECT password, salt FROM users WHERE user_name = ?";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String password = resultSet.getString("password");
                String salt = resultSet.getString("salt");

                return new Passwords.Password(salt, password);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    public static boolean createDashboard(String userName, String name) {
        DashboardModel.Properties properties = new DashboardModel.Properties();
        
        boolean success = false;
        String sql = "INSERT INTO dashboards (name, properties, owner_id) VALUES (?, ?, (SELECT id FROM users WHERE user_name = ?))";

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, properties.toJSONString());
            preparedStatement.setString(3, userName);
            int rowAffected = preparedStatement.executeUpdate();
            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    public static boolean dashbaordExists(String name, String userName) {
        String sql = "SELECT EXISTS(SELECT * FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)) AS dashboard_exists";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, userName);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getBoolean("dashboard_exists");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static DashboardModel getDashboard(String name, String userName) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Pair.class, new pairKeyDeserializer());
        objectMapper.registerModule(module);
        
        String sql = "SELECT * FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, userName);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                byte[] id = resultSet.getBytes("id");
                byte[] ownerId = resultSet.getBytes("owner_id");
                String properties = resultSet.getString("properties");
                Date createdAt = resultSet.getTimestamp("created_at");
                Date editedAt = resultSet.getTimestamp("edited_at");
                
                try {
                    DashboardModel.Properties propertiesObject = objectMapper.readValue(properties, DashboardModel.Properties.class);

                    return new DashboardModel(id, ownerId, createdAt, editedAt, name, propertiesObject);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static DashboardModel getDashboardWithJSONProps(String name, String userName) {
        String sql = "SELECT * FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, userName);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                byte[] id = resultSet.getBytes("id");
                byte[] ownerId = resultSet.getBytes("owner_id");
                String properties = resultSet.getString("properties");
                Date createdAt = resultSet.getTimestamp("created_at");
                Date editedAt = resultSet.getTimestamp("edited_at");
                
                try {
                    return new DashboardModel(id, ownerId, createdAt, editedAt, name, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static DashboardModel getDashboard(byte[] id) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Pair.class, new pairKeyDeserializer());
        objectMapper.registerModule(module);
        
        String query = "SELECT * FROM dashboards WHERE id = '" + id + "'";
        
        try {
            ResultSet resultSet = statement.executeQuery(query);

            if (resultSet.next()) {
                String name = resultSet.getString("name");
                byte[] ownerId = resultSet.getBytes("owner_id");
                String properties = resultSet.getString("properties");
                Date createdAt = resultSet.getTimestamp("created_at");
                Date editedAt = resultSet.getTimestamp("edited_at");
                
                try {
                    DashboardModel.Properties propertiesObject = objectMapper.readValue(properties, DashboardModel.Properties.class);

                    return new DashboardModel(id, ownerId, createdAt, editedAt, name, propertiesObject);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ArrayList<DashboardModel> getDashboards(String userName) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Pair.class, new pairKeyDeserializer());
        objectMapper.registerModule(module);

        String sql = "SELECT * FROM dashboards WHERE owner_id = (SELECT id FROM users WHERE user_name = ?)";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, userName);
            ResultSet resultSet = preparedStatement.executeQuery();

            ArrayList<DashboardModel> dashboards = new ArrayList<DashboardModel>();

            while (resultSet.next()) {
                byte[] id = resultSet.getBytes("id");
                byte[] ownerId = resultSet.getBytes("owner_id");
                String name = resultSet.getString("name");
                String properties = resultSet.getString("properties");
                Date createdAt = resultSet.getTimestamp("created_at");
                Date editedAt = resultSet.getTimestamp("edited_at");
                
                try {
                    DashboardModel dashboard = new DashboardModel(id, ownerId, createdAt, editedAt, name, properties);

                    dashboards.add(dashboard);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return dashboards;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean updateDashboard(DashboardModel dashboard) {
        boolean success = false;
        
        String sql = "UPDATE dashboards SET properties = ?, edited_at = NOW() WHERE id = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, dashboard.getProperties().toJSONString());
            preparedStatement.setBytes(2, dashboard.getId());

            int rowAffected = preparedStatement.executeUpdate();

            if (rowAffected > 0) {
                success = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    public static boolean updateDashboard(DashboardModel dashboard, String properties) {
        boolean success = false;
        
        String sql = "UPDATE dashboards SET properties = ?, edited_at = NOW() WHERE id = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, properties);
            preparedStatement.setBytes(2, dashboard.getId());

            int rowAffected = preparedStatement.executeUpdate();

            if (rowAffected > 0) {
                success = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    public static String getUserName(byte[] id) {
        String sql = "SELECT user_name FROM users WHERE id = ?";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setBytes(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("user_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean registerSession(String username) {
        boolean success = false;
        String sql = "INSERT INTO sessions (user_id) VALUES ((SELECT id FROM users WHERE user_name = ?))";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
            int rowAffected = preparedStatement.executeUpdate();

            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    public static boolean sessionExists(String username) {
        String sql = "SELECT EXISTS(SELECT * FROM sessions WHERE user_id = (SELECT id FROM users WHERE user_name = ?)) AS session_exists";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getBoolean("session_exists");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean deleteSession(String username) {
        boolean success = false;
        String sql = "DELETE FROM sessions WHERE user_id = (SELECT id FROM users WHERE user_name = ?)";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
            int rowAffected = preparedStatement.executeUpdate();

            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    public static boolean deleteDashboard(String name, String userName) {
        boolean success = false;
        String sql = "DELETE FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, userName);
            int rowAffected = preparedStatement.executeUpdate();

            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    public static boolean renameDashboard(String oldName, String newName, String userName) {
        boolean success = false;
        String sql = "UPDATE dashboards SET name = ?, edited_at = NOW() WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, newName);
            preparedStatement.setString(2, oldName);
            preparedStatement.setString(3, userName);
            int rowAffected = preparedStatement.executeUpdate();

            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }
}

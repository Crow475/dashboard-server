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
import org.dashboard.common.Role;
import org.dashboard.common.models.DashboardModel;
import org.dashboard.common.models.UserOfDashboard;;

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

                    DashboardModel model = new DashboardModel(id, ownerId, createdAt, editedAt, name, propertiesObject);
                    model.setOwnerUsername(userName);

                    return model;
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
                    DashboardModel model = new DashboardModel(id, ownerId, createdAt, editedAt, name, properties);
                    model.setOwnerUsername(userName);
                    return model;
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

                String userName = getUserName(ownerId);
                
                try {
                    DashboardModel.Properties propertiesObject = objectMapper.readValue(properties, DashboardModel.Properties.class);

                    DashboardModel model = new DashboardModel(id, ownerId, createdAt, editedAt, name, propertiesObject);
                    model.setOwnerUsername(userName);
                    return model;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ArrayList<DashboardModel> getOwnedDashboards(String userName) {
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
                    dashboard.setOwnerUsername(userName);

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

    public static boolean addViewer(String ownerName, String dashboardName, String viewerName) {
        boolean success = false;
        String sql = "INSERT INTO viewers (dashboard_id, user_id) VALUES ((SELECT id FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)), (SELECT id FROM users WHERE user_name = ?))";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, dashboardName);
            preparedStatement.setString(2, ownerName);
            preparedStatement.setString(3, viewerName);
            int rowAffected = preparedStatement.executeUpdate();

            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    public static ArrayList<DashboardModel> getViewedDashboards(String username) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Pair.class, new pairKeyDeserializer());
        objectMapper.registerModule(module);

        String sql = "SELECT * FROM dashboards WHERE id IN (SELECT dashboard_id FROM viewers WHERE user_id = (SELECT id FROM users WHERE user_name = ?))";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
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
                    dashboard.setOwnerUsername(getUserName(ownerId));

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

    public static ArrayList<String> getDashboardViewers(String ownerName, String dashboardName) {
        String sql = "SELECT user_name FROM users WHERE id IN (SELECT user_id FROM viewers WHERE dashboard_id = (SELECT id FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)))";
        
        System.out.println("getDashboardViewers: " + ownerName + " : " + dashboardName);

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, dashboardName);
            preparedStatement.setString(2, ownerName);
            ResultSet resultSet = preparedStatement.executeQuery();

            ArrayList<String> viewers = new ArrayList<String>();

            while (resultSet.next()) {
                viewers.add(resultSet.getString("user_name"));
            }

            System.out.println(viewers);

            return viewers;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean removeViewer(String ownerName, String dashboardName, String viewerName) {
        boolean success = false;
        String sql = "DELETE FROM viewers WHERE dashboard_id = (SELECT id FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)) AND user_id = (SELECT id FROM users WHERE user_name = ?)";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, dashboardName);
            preparedStatement.setString(2, ownerName);
            preparedStatement.setString(3, viewerName);
            int rowAffected = preparedStatement.executeUpdate();

            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    public static boolean addEditor(String ownerName, String dashboardName, String editorName) {
        boolean success = false;
        String sql = "INSERT INTO editors (dashboard_id, user_id) VALUES ((SELECT id FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)), (SELECT id FROM users WHERE user_name = ?))";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, dashboardName);
            preparedStatement.setString(2, ownerName);
            preparedStatement.setString(3, editorName);
            int rowAffected = preparedStatement.executeUpdate();

            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    public static ArrayList<DashboardModel> getEditedDashboards(String username) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Pair.class, new pairKeyDeserializer());
        objectMapper.registerModule(module);

        String sql = "SELECT * FROM dashboards WHERE id IN (SELECT dashboard_id FROM editors WHERE user_id = (SELECT id FROM users WHERE user_name = ?))";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
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
                    dashboard.setOwnerUsername(getUserName(ownerId));

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

    public static ArrayList<String> getDashboardEditors(String ownerName, String dashboardName) {
        String sql = "SELECT user_name FROM users WHERE id IN (SELECT user_id FROM editors WHERE dashboard_id = (SELECT id FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)))";
        
        System.out.println("getDashboardEditors: " + ownerName + " : " + dashboardName);

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, dashboardName);
            preparedStatement.setString(2, ownerName);
            ResultSet resultSet = preparedStatement.executeQuery();

            ArrayList<String> editors = new ArrayList<String>();

            while (resultSet.next()) {
                editors.add(resultSet.getString("user_name"));
            }

            return editors;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean removeEditor(String ownerName, String dashboardName, String editorName) {
        boolean success = false;
        String sql = "DELETE FROM editors WHERE dashboard_id = (SELECT id FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)) AND user_id = (SELECT id FROM users WHERE user_name = ?)";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, dashboardName);
            preparedStatement.setString(2, ownerName);
            preparedStatement.setString(3, editorName);
            int rowAffected = preparedStatement.executeUpdate();

            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    public static boolean addAdmin(String ownerName , String dashboardName, String adminName) {
        boolean success = false;
        String sql = "INSERT INTO admins (dashboard_id, user_id) VALUES ((SELECT id FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)), (SELECT id FROM users WHERE user_name = ?))";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, dashboardName);
            preparedStatement.setString(2, ownerName);
            preparedStatement.setString(3, adminName);
            int rowAffected = preparedStatement.executeUpdate();

            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    public static ArrayList<DashboardModel> getAdminDashboards(String username) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Pair.class, new pairKeyDeserializer());
        objectMapper.registerModule(module);

        String sql = "SELECT * FROM dashboards WHERE id IN (SELECT dashboard_id FROM admins WHERE user_id = (SELECT id FROM users WHERE user_name = ?))";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
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
                    dashboard.setOwnerUsername(getUserName(ownerId));

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

    public static ArrayList<String> getDashboardAdmins(String ownerName, String dashboardName) {
        String sql = "SELECT user_name FROM users WHERE id IN (SELECT user_id FROM admins WHERE dashboard_id = (SELECT id FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)))";
        
        System.out.println("getDashboardAdmins: " + ownerName + " : " + dashboardName);

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, dashboardName);
            preparedStatement.setString(2, ownerName);
            ResultSet resultSet = preparedStatement.executeQuery();

            ArrayList<String> admins = new ArrayList<String>();

            while (resultSet.next()) {
                admins.add(resultSet.getString("user_name"));
            }

            return admins;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean removeAdmin(String ownerName, String dashboardName, String adminName) {
        boolean success = false;
        String sql = "DELETE FROM admins WHERE dashboard_id = (SELECT id FROM dashboards WHERE name = ? AND owner_id = (SELECT id FROM users WHERE user_name = ?)) AND user_id = (SELECT id FROM users WHERE user_name = ?)";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, dashboardName);
            preparedStatement.setString(2, ownerName);
            preparedStatement.setString(3, adminName);
            int rowAffected = preparedStatement.executeUpdate();

            success = rowAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    public static ArrayList<String> searchForUser(String username) {
        String sql = "SELECT user_name FROM users WHERE LOWER (user_name) LIKE ?";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, "%" + username.toLowerCase() + "%");
            ResultSet resultSet = preparedStatement.executeQuery();

            ArrayList<String> users = new ArrayList<String>();
            while (resultSet.next()) {
                users.add(resultSet.getString("user_name"));
            }

            return users;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ArrayList<DashboardModel> getAllDashboards(String username) {
        ArrayList<DashboardModel> ownedDashboards = getOwnedDashboards(username);
        ArrayList<DashboardModel> viewedDashboards = getViewedDashboards(username);
        ArrayList<DashboardModel> editedDashboards = getEditedDashboards(username);
        ArrayList<DashboardModel> adminDashboards = getAdminDashboards(username);

        ArrayList<DashboardModel> allDashboards = new ArrayList<DashboardModel>();
        allDashboards.addAll(ownedDashboards);
        allDashboards.addAll(viewedDashboards);
        allDashboards.addAll(editedDashboards);
        allDashboards.addAll(adminDashboards);

        return allDashboards;
    }

    public static ArrayList<UserOfDashboard> getDashboardUsers(String ownerName, String dashboardName) {
        if (!dashbaordExists(dashboardName, ownerName)) {
            return null;
        }

        ArrayList<String> admins = getDashboardAdmins(ownerName, dashboardName);
        ArrayList<String> editors = getDashboardEditors(ownerName, dashboardName);
        ArrayList<String> viewers = getDashboardViewers(ownerName, dashboardName);
        
        ArrayList<UserOfDashboard> users = new ArrayList<UserOfDashboard>();

        users.add(new UserOfDashboard(ownerName, dashboardName, Role.OWNER));

        for (String admin : admins) {
            users.add(new UserOfDashboard(admin, dashboardName, Role.ADMIN));
        }

        for (String editor : editors) {
            users.add(new UserOfDashboard(editor, dashboardName, Role.EDITOR));
        }

        for (String viewer : viewers) {
            users.add(new UserOfDashboard(viewer, dashboardName, Role.VIEWER));
        }

        return users;
    }

    public static boolean deleteUser(String username) {
        boolean success = false;
        String sql = "DELETE FROM users WHERE user_name = ?";
        
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
}

package org.dashboard.server;

import java.util.ArrayList;

import org.dashboard.common.Role;

public class CheckAccess {
    public static boolean isOwner (String subject, String username, String dashboardName) {
        boolean isOwner = false;
        if (DBUtils.dashbaordExists(dashboardName, username)) {
            isOwner = subject.equals(username);
        }

        return isOwner;
    }

    public static boolean isViewer (String subject, String username, String dashboardName) {
        boolean isViewer = false;
        ArrayList<String> viewers = DBUtils.getDashboardViewers(username, dashboardName);
        // System.out.println("Is viewer of: " + subject + " : " + viewers);
        if (viewers != null) {
            isViewer = viewers.contains(subject);
        }

        return isViewer;
    }

    public static boolean isEditor (String subject, String username, String dashboardName) {
        boolean isEditor = false;
        ArrayList<String> editors = DBUtils.getDashboardEditors(username, dashboardName);
        // System.out.println("Is editor of: " + subject + " : " + editors);
        if (editors != null) {
            isEditor = editors.contains(subject);
        }

        return isEditor;
    }

    public static boolean isAdmin (String subject, String username, String dashboardName) {
        boolean isAdmin = false;
        ArrayList<String> admins = DBUtils.getDashboardAdmins(username, dashboardName);
        // System.out.println("Is admin of: " + subject + " : " + admins);
        if (admins != null) {
            isAdmin = admins.contains(subject);
        }

        return isAdmin;
    }

    public static boolean isAtLeastViewer (String subject, String username, String dashboardName) {
        return isOwner(subject, username, dashboardName) || isAdmin(subject, username, dashboardName) || isEditor(subject, username, dashboardName) || isViewer(subject, username, dashboardName);
    }

    public static boolean isAtLeastEditor (String subject, String username, String dashboardName) {
        return isOwner(subject, username, dashboardName) || isEditor(subject, username, dashboardName) || isAdmin(subject, username, dashboardName);
    }

    public static boolean isAtLeastAdmin (String subject, String username, String dashboardName) {
        return isOwner(subject, username, dashboardName) || isAdmin(subject, username, dashboardName);
    }

    public static Role getRoleIn (String subject, String username, String dashboardName) {
        System.out.println("Getting role of: " + subject + " in " + username + " : " + dashboardName);
        if (isOwner(subject, username, dashboardName)) {
            // System.out.println("Owner");
            return Role.OWNER;
        } else if (isAdmin(subject, username, dashboardName)) {
            // System.out.println("Admin");
            return Role.ADMIN;
        } else if (isEditor(subject, username, dashboardName)) {
            // System.out.println("Editor");
            return Role.EDITOR;
        } else if (isViewer(subject, username, dashboardName)) {
            // System.out.println("Viewer");
            return Role.VIEWER;
        } else {
            // System.out.println("None");
            return Role.NONE;
        }
    }
}

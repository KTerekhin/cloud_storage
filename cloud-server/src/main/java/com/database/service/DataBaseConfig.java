package com.database.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBaseConfig {
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver not found.");
        }
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:cloud-server/src/main/resources/c_s_users.db");
        } catch (SQLException e) {
            throw new RuntimeException("Driver registration error.");
        }
    }
}

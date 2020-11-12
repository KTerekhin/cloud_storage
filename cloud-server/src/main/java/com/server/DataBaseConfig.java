package com.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBaseConfig {
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver not found.");
        }
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost/users_for_chat?serverTimezone=Europe/Moscow&useSSL=false", "root", "futyncvbn7");
        } catch (SQLException e) {
            throw new RuntimeException("Driver registration error.");
        }
    }
}

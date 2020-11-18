package com.database.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBaseAuthService {

    public static String getAuthByLoginAndPass(String login, String password) {
        Connection connection = DataBaseConfig.getConnection();
        String sql = String.format("SELECT nickname FROM users WHERE (`login` = '%s' AND `password` = '%s')", login, password);
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getString("nickname");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Statement Error");
        }
        return null;
    }

//    public static String checkIsLoginFree(String login) {
//        Connection connection = DataBaseConfig.getConnection();
//        String sql = String.format("SELECT login FROM users WHERE login = '%s'", login);
//        try {
//            Statement stmt = connection.createStatement();
//            ResultSet rs = stmt.executeQuery(sql);
//            if (rs.next()) {
//                return rs.getString("login");
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public static String tryRegister(String login, String password) {
        Connection connection = DataBaseConfig.getConnection();
        String sql = String.format("INSERT INTO users(login, password, nickname) VALUES('%s','%s','%s')", login, password, login);
        try {
            Statement stmt = connection.createStatement();
            int row = stmt.executeUpdate(sql);
            if (row >= 1) {
                return login;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void start() {
        System.out.println("Сервис аутентификации запущен");
    }

    public static void stop() {
        System.out.println("Сервис аутентификации остановлен");
    }
}

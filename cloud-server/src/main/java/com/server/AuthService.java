package com.server;

public interface AuthService {
    void start();
    void stop();
    String getAuthByLoginAndPass(String login, String password);
}

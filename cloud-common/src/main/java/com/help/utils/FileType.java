package com.help.utils;

public enum FileType {
    FILE("F"), DIRECTORY("D");

    private String name;

    public String getName() {
        return name;
    }

    FileType(String name) {
        this.name = name;
    }
}

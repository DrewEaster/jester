package com.dreweaster.ddd.example.domain;

public class Example {

    public static Example of(String exampleString) {
        return new Example(exampleString);
    }

    public static Example EMPTY_STATE = new Example("");

    private String exampleString;

    private Example(String exampleString) {
        this.exampleString = exampleString;
    }
}

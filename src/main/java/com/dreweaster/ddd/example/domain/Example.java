package com.dreweaster.ddd.example.domain;

public class Example {

    public static Example of(String exampleString) {
        return new Example(exampleString);
    }

    public static Example EMPTY_STATE = of("");

    private String exampleString;

    private Example(String exampleString) {
        this.exampleString = exampleString;
    }
}

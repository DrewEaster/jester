package com.dreweaster.ddd.example.domain;

public class ExampleCreated extends ExampleEvent {

    public static ExampleCreated of(String exampleString) {
        return new ExampleCreated(exampleString);
    }

    private String exampleString;

    private ExampleCreated(String exampleString) {
        this.exampleString = exampleString;
    }

    public String exampleString() {
        return exampleString;
    }

    @Override
    public String toString() {
        return "ExampleCreated{}";
    }
}

package com.dreweaster.ddd.example.domain;

/**
 */
public class CreateExample extends ExampleCommand {

    public static CreateExample of(String exampleString) {
        return new CreateExample(exampleString);
    }

    private String exampleString;

    public CreateExample(String exampleString) {
        this.exampleString = exampleString;
    }

    public String exampleString() {
        return exampleString;
    }
}

package com.dreweaster.ddd.example.domain;

import com.dreweaster.ddd.framework.DomainCommand;

/**
 */
public class CreateExample implements DomainCommand {

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

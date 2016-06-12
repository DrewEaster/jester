package com.dreweaster.ddd.example.domain;

public class GetExample extends ExampleCommand {

    private static final GetExample SINGLETON = new GetExample();

    public static GetExample get() {
        return SINGLETON;
    }

    private GetExample() {

    }
}
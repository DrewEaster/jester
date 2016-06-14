package com.dreweaster.ddd.example.domain;

import com.dreweaster.ddd.framework.ReadOnlyDomainCommand;

public class GetExample implements ReadOnlyDomainCommand<Example> {

    private static final GetExample SINGLETON = new GetExample();

    public static GetExample get() {
        return SINGLETON;
    }

    private GetExample() {

    }
}
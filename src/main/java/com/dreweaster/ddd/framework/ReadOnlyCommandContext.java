package com.dreweaster.ddd.framework;

public interface ReadOnlyCommandContext<O> {

   void success(O response);
}

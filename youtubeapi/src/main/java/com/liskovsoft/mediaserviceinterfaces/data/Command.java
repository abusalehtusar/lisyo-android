package com.liskovsoft.mediaserviceinterfaces.data;

public interface Command {
    int getType();
    String getData();
}

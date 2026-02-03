package com.liskovsoft.mediaserviceinterfaces.data;

import java.util.List;

public interface ItemGroup {
    interface Item {
        String getTitle();
        String getSubtitle();
        String getIconUrl();
        String getChannelId();
        String getVideoId();
        String getBadge();
    }

    String getId();
    String getTitle();
    String getIconUrl();
    List<Item> getItems();
    String getBadge();
    Item findItem(String channelOrVideoId);
    void add(Item item);
    void remove(Item item);
    boolean isEmpty();
}

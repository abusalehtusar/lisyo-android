package com.liskovsoft.mediaserviceinterfaces.data;

import java.util.List;

public interface MediaGroup {
    int TYPE_UNDEFINED = -1;
    int TYPE_HOME = 0;
    int TYPE_SUBSCRIPTIONS = 1;
    int TYPE_HISTORY = 2;
    int TYPE_CHANNEL = 3;
    int TYPE_CHANNEL_UPLOADS = 4;
    int TYPE_SEARCH = 5;
    int TYPE_SUGGESTIONS = 6;
    int TYPE_USER_PLAYLISTS = 7;
    int TYPE_TRENDING = 8;
    int TYPE_MUSIC = 9;
    int TYPE_GAMING = 10;
    int TYPE_NEWS = 11;
    int TYPE_MOVIES = 12;
    int TYPE_LIVE = 13;
    int TYPE_SPORTS = 14;
    int TYPE_SHORTS = 15;
    int TYPE_KIDS_HOME = 16;
    int TYPE_NOTIFICATIONS = 17;
    int TYPE_MY_VIDEOS = 18;

    List<MediaItem> getMediaItems();
    String getTitle();
    int getType();
    String getChannelId();
    String getChannelUrl();
    String getParams();
    String getReloadPageKey();
    String getNextPageKey();
    boolean isEmpty();
}

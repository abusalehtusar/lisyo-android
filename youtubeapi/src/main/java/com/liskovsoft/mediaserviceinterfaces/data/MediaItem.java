package com.liskovsoft.mediaserviceinterfaces.data;

public interface MediaItem {
    int TYPE_UNDEFINED = -1;
    int TYPE_VIDEO = 0;
    int TYPE_CHANNEL = 1;
    int TYPE_PLAYLIST = 2;
    int TYPE_MUSIC = 3;

    int getId();
    int getType();
    String getTitle();
    CharSequence getSecondTitle();
    String getVideoId();
    String getChannelId();
    String getPlaylistId();
    String getParams();
    String getCardImageUrl();
    String getBackgroundImageUrl();
    String getVideoPreviewUrl();
    String getContentType();
    long getDurationMs();
    String getProductionDate();
    String getAuthor();
    String getBadgeText();
    boolean isLive();
    boolean isUpcoming();
    boolean isShorts();
    boolean isMovie();
    int getPercentWatched();
    int getStartTimeSeconds();
    int getPlaylistIndex();
    String getReloadPageKey();
    boolean hasUploads();
    boolean hasNewContent();
    int getWidth();
    int getHeight();
    String getAudioChannelConfig();
    int getRatingStyle();
    double getRatingScore();
    String getPurchasePrice();
    String getRentalPrice();
    String getFeedbackToken();
    String getClickTrackingParams();
    long getPublishedDate();
    int getViewCount();
    String getDescription();
}

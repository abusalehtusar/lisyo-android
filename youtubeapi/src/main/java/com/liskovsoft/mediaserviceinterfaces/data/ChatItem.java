package com.liskovsoft.mediaserviceinterfaces.data;

public interface ChatItem {
    String getAuthor();
    String getMessage();
    String getPublishedDate();
    String getAuthorThumbnailUrl();
    String getAuthorChannelId();
}
